package com.github.believepxw.yigo.structure

import com.intellij.icons.AllIcons
import com.intellij.ide.structureView.*
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.indexing.FileBasedIndex
import example.index.DataObjectIndex
import example.index.FormIndex
import javax.swing.Icon

/**
 * Yigo XML 专属文件结构工厂（支持 Ctrl + F12 和 Alt + 7 Structure 视窗）
 * 按照业务语义展示：数据源与字段 (Fields)、操作与按钮 (Methods)、界面控件 (UI Controls)、嵌入表单 (Embedded Forms)、宏与逻辑 (Macros)
 * 支持全量跨文件展开嵌入表单 (Embed Form) 与嵌入数据表 (EmbedTable)
 */
class YigoStructureViewFactory : PsiStructureViewFactory {

    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        val xmlFile = psiFile as? XmlFile ?: return null
        val rootTag = xmlFile.rootTag ?: return null

        // 识别 Yigo 特征的 XML 根节点
        if (!isYigoXml(rootTag)) return null

        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel {
                return YigoStructureViewModel(xmlFile, editor)
            }
        }
    }

    companion object {
        val YIGO_ROOT_TAGS = setOf(
            "Form", "DataObject", "CommonDef", "Entity", "Map", "Dict", "Parameters"
        )

        fun isYigoXml(rootTag: XmlTag): Boolean {
            if (rootTag.name in YIGO_ROOT_TAGS) return true
            // 兜底特征识别：包含 DataSource 或 Body 或 OperationCollection
            if (rootTag.findSubTags("DataSource").isNotEmpty() ||
                rootTag.findSubTags("Body").isNotEmpty() ||
                rootTag.findSubTags("OperationCollection").isNotEmpty() ||
                rootTag.findSubTags("Table").isNotEmpty()
            ) {
                return true
            }
            return false
        }
    }
}

/**
 * Yigo XML 结构视图模型，继承 StructureViewModelBase
 */
class YigoStructureViewModel(
    private val xmlFile: XmlFile,
    editor: Editor?
) : StructureViewModelBase(xmlFile, editor, createRootTreeElement(xmlFile)),
    StructureViewModel.ElementInfoProvider {

    init {
        withSuitableClasses(XmlTag::class.java, XmlFile::class.java)
    }

    override fun getSuitableClasses(): Array<Class<*>> {
        return arrayOf(XmlTag::class.java, XmlFile::class.java)
    }

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement?): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement?): Boolean {
        val value = element?.value
        if (value is XmlTag) {
            return value.name in LEAF_TAGS
        }
        return false
    }

    companion object {
        private val LEAF_TAGS = setOf(
            "Column", "GridColumn", "Operation", "Item", "Macro",
            "TextEditor", "NumberEditor", "Dict", "Label", "Button",
            "CheckBox", "ComboBox", "DatePicker", "UTCDatePicker",
            "CheckBoxGroup", "RadioButton", "TextArea"
        )

        private fun createRootTreeElement(xmlFile: XmlFile): StructureViewTreeElement {
            val rootTag = xmlFile.rootTag ?: return YigoStructureTreeElement(
                psiElement = xmlFile,
                label = xmlFile.name,
                icon = AllIcons.Nodes.Class
            )
            return YigoStructureTreeBuilder.buildRootElement(rootTag, xmlFile)
        }
    }
}

/**
 * 通用的 Yigo 树节点元素
 */
class YigoStructureTreeElement(
    private val psiElement: PsiElement,
    private val label: String,
    private val location: String? = null,
    private val icon: Icon,
    private val childrenProvider: () -> List<TreeElement> = { emptyList() }
) : StructureViewTreeElement, Navigatable {

    override fun getValue(): Any = psiElement

    override fun navigate(requestFocus: Boolean) {
        if (psiElement is Navigatable) {
            psiElement.navigate(requestFocus)
        }
    }

    override fun canNavigate(): Boolean = (psiElement as? Navigatable)?.canNavigate() == true
    override fun canNavigateToSource(): Boolean = (psiElement as? Navigatable)?.canNavigateToSource() == true

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {
            override fun getPresentableText(): String = label
            override fun getLocationString(): String? = location
            override fun getIcon(unused: Boolean): Icon = icon
        }
    }

    override fun getChildren(): Array<TreeElement> {
        return childrenProvider().toTypedArray()
    }
}

/**
 * 业务结构树构建器：智能解析 Yigo XML 语义大纲（支持全量跨文件递归展开）
 */
object YigoStructureTreeBuilder {

    fun buildRootElement(rootTag: XmlTag, contextFile: PsiFile): StructureViewTreeElement {
        val rootKey = rootTag.getAttributeValue("Key") ?: rootTag.name
        val rootCaption = rootTag.getAttributeValue("Caption")
        val rootTitle = if (rootCaption.isNullOrBlank()) {
            "${rootTag.name}: $rootKey"
        } else {
            "${rootTag.name}: $rootKey [$rootCaption]"
        }

        val initialVisitedForms = if (!rootKey.isNullOrBlank()) setOf(rootKey) else emptySet()

        return YigoStructureTreeElement(
            psiElement = rootTag,
            label = rootTitle,
            icon = AllIcons.Nodes.Class,
            childrenProvider = {
                val groups = mutableListOf<TreeElement>()

                // 1. 数据源与字段 (DataSources & Columns) -> 对应 Java 的 Fields，支持 EmbedTable 与 RefObjectKey 全量跨文件展开
                val dsGroup = buildDataSourcesGroup(rootTag, contextFile)
                if (dsGroup != null) groups.add(dsGroup)

                // 2. 操作集合 (OperationCollection) -> 对应 Java 的 Methods
                val opsGroup = buildOperationsGroup(rootTag)
                if (opsGroup != null) groups.add(opsGroup)

                // 3. 界面控件与布局 (UI Controls) -> 支持 Embed Form 全量跨文件展开
                val uiGroup = buildUiControlsGroup(rootTag, initialVisitedForms, contextFile)
                if (uiGroup != null) groups.add(uiGroup)

                // 4. 嵌入表单专属大纲 (Embedded Forms) -> 全量跨文件展开各个嵌入子表单
                val embedGroup = buildEmbeddedFormsGroup(rootTag, initialVisitedForms, contextFile)
                if (embedGroup != null) groups.add(embedGroup)

                // 5. 宏与业务逻辑 (Macros & Formulas)
                val macrosGroup = buildMacrosGroup(rootTag)
                if (macrosGroup != null) groups.add(macrosGroup)

                // 6. 字典/映射/其他 (Map & Relations)
                val otherGroup = buildOtherGroup(rootTag)
                if (otherGroup != null) groups.add(otherGroup)

                groups
            }
        )
    }

    /**
     * 构建数据源与数据对象分组（DataSources, DataObjects, Tables, EmbedTables & Columns）
     */
    private fun buildDataSourcesGroup(rootTag: XmlTag, contextFile: PsiFile?): TreeElement? {
        // 1. 如果 rootTag 本身就是 DataObject
        if (rootTag.name == "DataObject") {
            val tables = collectTablesFromTag(rootTag)
            if (tables.isEmpty()) return null
            return createDataObjectTreeElement(rootTag, emptySet(), contextFile)
        }

        // 2. 收集本地 DataObject 以及引用的外部 DataObject
        val dataObjects = mutableListOf<XmlTag>()
        for (ds in rootTag.findSubTags("DataSource")) {
            dataObjects.addAll(ds.findSubTags("DataObject"))

            // 支持 <DataSource RefObjectKey="..."> 跨文件引用
            val refObjKey = ds.getAttributeValue("RefObjectKey")
            if (!refObjKey.isNullOrBlank()) {
                val linkedDoTag = findDataObjectTag(rootTag.project, refObjKey, contextFile)
                if (linkedDoTag != null) {
                    dataObjects.add(linkedDoTag)
                }
            }
        }
        dataObjects.addAll(rootTag.findSubTags("DataObject"))

        // 3. 收集所有独立 Table（不属于任何 DataObject 的表）
        val allTables = mutableListOf<XmlTag>()
        for (ds in rootTag.findSubTags("DataSource")) {
            allTables.addAll(collectTablesFromTag(ds))
        }
        allTables.addAll(collectTablesFromTag(rootTag))

        val doTables = dataObjects.flatMap { collectTablesFromTag(it) }.toSet()
        val standaloneTables = allTables.filter { it !in doTables }

        if (dataObjects.isEmpty() && standaloneTables.isEmpty()) return null

        val anchorTag = rootTag.findSubTags("DataSource").firstOrNull() ?: rootTag

        val totalTableCount = doTables.size + standaloneTables.size
        val locText = if (dataObjects.isNotEmpty()) {
            "${dataObjects.size} 个对象, $totalTableCount 个表"
        } else {
            "$totalTableCount 个表"
        }

        return YigoStructureTreeElement(
            psiElement = anchorTag,
            label = "数据源与对象 (DataSources)",
            location = locText,
            icon = AllIcons.Nodes.DataTables,
            childrenProvider = {
                val list = mutableListOf<TreeElement>()

                // 优先展示 DataObject（包含本地 Table 与 EmbedTable 跨文件展开）
                for (dObj in dataObjects) {
                    list.add(createDataObjectTreeElement(dObj, emptySet(), contextFile))
                }

                // 独立 Table
                for (table in standaloneTables) {
                    list.add(createTableTreeElement(table))
                }

                list
            }
        )
    }

    private fun collectTablesFromTag(container: XmlTag): List<XmlTag> {
        val tables = mutableListOf<XmlTag>()
        tables.addAll(container.findSubTags("Table"))
        for (tc in container.findSubTags("TableCollection")) {
            tables.addAll(tc.findSubTags("Table"))
        }
        return tables
    }

    /**
     * 创建 DataObject 节点，同时处理其本地 Table 与嵌入的 EmbedTable（全量跨文件展开）
     */
    private fun createDataObjectTreeElement(
        doTag: XmlTag,
        visitedDataObjects: Set<String>,
        contextFile: PsiFile?
    ): TreeElement {
        val doKey = doTag.getAttributeValue("Key") ?: "DataObject"
        val doCap = doTag.getAttributeValue("Caption")
        val primaryTable = doTag.getAttributeValue("PrimaryTableKey")
        val doTitle = if (doCap.isNullOrBlank()) "DataObject: $doKey" else "DataObject: $doKey [$doCap]"
        val doLoc = if (!primaryTable.isNullOrBlank()) "主表: $primaryTable" else null

        val tables = collectTablesFromTag(doTag)

        // 收集当前 DataObject 中的所有 EmbedTable
        val embedTables = mutableListOf<XmlTag>()
        for (etc in doTag.findSubTags("EmbedTableCollection")) {
            embedTables.addAll(etc.findSubTags("EmbedTable"))
        }
        embedTables.addAll(doTag.findSubTags("EmbedTable"))

        return YigoStructureTreeElement(
            psiElement = doTag,
            label = doTitle,
            location = doLoc,
            icon = AllIcons.Nodes.Class,
            childrenProvider = {
                val list = mutableListOf<TreeElement>()

                // 1. 本地各个 Table
                for (tableTag in tables) {
                    list.add(createTableTreeElement(tableTag))
                }

                // 2. 跨文件展开 EmbedTable（方案 A）
                for (embedTag in embedTables) {
                    val tableKey = embedTag.getAttributeValue("TableKeys")
                        ?: embedTag.getAttributeValue("Key")
                        ?: "EmbedTable"
                    val objectKey = embedTag.getAttributeValue("ObjectKey")

                    if (!objectKey.isNullOrBlank() && !visitedDataObjects.contains(objectKey)) {
                        // 四重查找外部数据对象
                        val linkedDoTag = findDataObjectTag(embedTag.project, objectKey, contextFile ?: embedTag.containingFile)

                        if (linkedDoTag != null) {
                            // 从外部数据对象中查找对应的真实 Table
                            val realTable = collectTablesFromTag(linkedDoTag).firstOrNull { it.getAttributeValue("Key") == tableKey }

                            if (realTable != null) {
                                val tableCap = realTable.getAttributeValue("Caption")
                                val embedTitle = if (tableCap.isNullOrBlank()) {
                                    "EmbedTable: $tableKey (来源: $objectKey)"
                                } else {
                                    "EmbedTable: $tableKey [$tableCap] (来源: $objectKey)"
                                }
                                val columns = realTable.findSubTags("Column")

                                list.add(
                                    YigoStructureTreeElement(
                                        psiElement = embedTag,
                                        label = embedTitle,
                                        location = "来自对象: $objectKey, ${columns.size} 列",
                                        icon = AllIcons.Nodes.DataTables,
                                        childrenProvider = {
                                            columns.map { colTag ->
                                                val colKey = colTag.getAttributeValue("Key") ?: "Col"
                                                val colCap = colTag.getAttributeValue("Caption")
                                                val dataType = colTag.getAttributeValue("DataType")
                                                    ?: colTag.getAttributeValue("Type")
                                                    ?: colTag.getAttributeValue("DataElementKey")
                                                    ?: ""
                                                val colTitle = if (colCap.isNullOrBlank()) "Column: $colKey" else "Column: $colKey [$colCap]"

                                                YigoStructureTreeElement(
                                                    psiElement = colTag,
                                                    label = colTitle,
                                                    location = dataType.ifBlank { null },
                                                    icon = AllIcons.Nodes.Field
                                                )
                                            }
                                        }
                                    )
                                )
                            } else {
                                list.add(
                                    YigoStructureTreeElement(
                                        psiElement = embedTag,
                                        label = "EmbedTable: $tableKey (外部表未找到: $objectKey)",
                                        icon = AllIcons.Nodes.DataTables
                                    )
                                )
                            }
                        } else {
                            list.add(
                                YigoStructureTreeElement(
                                    psiElement = embedTag,
                                    label = "EmbedTable: $tableKey (外部对象未找到: $objectKey)",
                                    icon = AllIcons.Nodes.DataTables
                                )
                            )
                        }
                    } else if (visitedDataObjects.contains(objectKey)) {
                        list.add(
                            YigoStructureTreeElement(
                                psiElement = embedTag,
                                label = "EmbedTable: $tableKey [循环引用: $objectKey]",
                                icon = AllIcons.Nodes.DataTables
                            )
                        )
                    } else {
                        list.add(
                            YigoStructureTreeElement(
                                psiElement = embedTag,
                                label = "EmbedTable: $tableKey",
                                icon = AllIcons.Nodes.DataTables
                            )
                        )
                    }
                }

                list
            }
        )
    }

    private fun createTableTreeElement(tableTag: XmlTag): TreeElement {
        val tableKey = tableTag.getAttributeValue("Key") ?: "Table"
        val tableCap = tableTag.getAttributeValue("Caption")
        val tableMode = tableTag.getAttributeValue("TableMode")
        val tableTitle = if (tableCap.isNullOrBlank()) "Table: $tableKey" else "Table: $tableKey [$tableCap]"
        val columns = tableTag.findSubTags("Column")

        val tableLoc = when {
            !tableMode.isNullOrBlank() -> "$tableMode, ${columns.size} 列"
            else -> "${columns.size} 列"
        }

        return YigoStructureTreeElement(
            psiElement = tableTag,
            label = tableTitle,
            location = tableLoc,
            icon = AllIcons.Nodes.DataTables,
            childrenProvider = {
                columns.map { colTag ->
                    val colKey = colTag.getAttributeValue("Key") ?: "Col"
                    val colCap = colTag.getAttributeValue("Caption")
                    val dataType = colTag.getAttributeValue("DataType")
                        ?: colTag.getAttributeValue("Type")
                        ?: colTag.getAttributeValue("DataElementKey")
                        ?: ""
                    val colTitle = if (colCap.isNullOrBlank()) "Column: $colKey" else "Column: $colKey [$colCap]"

                    YigoStructureTreeElement(
                        psiElement = colTag,
                        label = colTitle,
                        location = dataType.ifBlank { null },
                        icon = AllIcons.Nodes.Field
                    )
                }
            }
        )
    }

    /**
     * 构建操作集合与动作分组（OperationCollection & Operations）
     */
    private fun buildOperationsGroup(rootTag: XmlTag): TreeElement? {
        val opCollections = rootTag.findSubTags("OperationCollection").toList()
        val directOps = rootTag.findSubTags("Operation").toList()

        if (opCollections.isEmpty() && directOps.isEmpty()) return null

        // 如果刚好只有 1 个顶层的 OperationCollection
        if (opCollections.size == 1 && directOps.isEmpty()) {
            val topOpCol = opCollections.first()
            val colKey = topOpCol.getAttributeValue("Key")
            val colCap = topOpCol.getAttributeValue("Caption")
            val title = when {
                !colCap.isNullOrBlank() && !colKey.isNullOrBlank() -> "OperationCollection: $colKey [$colCap]"
                !colCap.isNullOrBlank() -> "OperationCollection [$colCap]"
                !colKey.isNullOrBlank() -> "OperationCollection: $colKey"
                else -> "操作集合 (OperationCollection)"
            }

            return YigoStructureTreeElement(
                psiElement = topOpCol,
                label = title,
                icon = AllIcons.Nodes.Method,
                childrenProvider = {
                    val list = mutableListOf<TreeElement>()
                    for (sub in topOpCol.subTags) {
                        when (sub.name) {
                            "Operation" -> list.add(createOperationTreeElement(sub))
                            "OperationCollection" -> list.add(createOperationCollectionTreeElement(sub))
                        }
                    }
                    list
                }
            )
        }

        // 如果有多个平级的 OperationCollection 或直接散落在 rootTag 下的 Operation
        return YigoStructureTreeElement(
            psiElement = rootTag,
            label = "操作集合 (OperationCollection)",
            icon = AllIcons.Nodes.Method,
            childrenProvider = {
                val list = mutableListOf<TreeElement>()

                for (op in directOps) {
                    list.add(createOperationTreeElement(op))
                }

                for (opCol in opCollections) {
                    list.add(createOperationCollectionTreeElement(opCol))
                }

                list
            }
        )
    }

    private fun createOperationTreeElement(opTag: XmlTag): TreeElement {
        val opKey = opTag.getAttributeValue("Key") ?: "Op"
        val opCap = opTag.getAttributeValue("Caption")
        val action = opTag.getAttributeValue("Action") ?: opTag.getAttributeValue("Formula")
        val opTitle = if (opCap.isNullOrBlank()) "Operation: $opKey" else "Operation: $opKey [$opCap]"

        return YigoStructureTreeElement(
            psiElement = opTag,
            label = opTitle,
            location = action?.ifBlank { null },
            icon = AllIcons.Nodes.Method
        )
    }

    private fun createOperationCollectionTreeElement(colTag: XmlTag): TreeElement {
        val colKey = colTag.getAttributeValue("Key") ?: "Group"
        val colCap = colTag.getAttributeValue("Caption")
        val colTitle = if (colCap.isNullOrBlank()) "OperationCollection: $colKey ▾" else "OperationCollection: $colKey [$colCap ▾]"

        return YigoStructureTreeElement(
            psiElement = colTag,
            label = colTitle,
            icon = AllIcons.Nodes.Folder,
            childrenProvider = {
                val children = mutableListOf<TreeElement>()
                for (sub in colTag.subTags) {
                    when (sub.name) {
                        "Operation" -> children.add(createOperationTreeElement(sub))
                        "OperationCollection" -> children.add(createOperationCollectionTreeElement(sub))
                    }
                }
                children
            }
        )
    }

    /**
     * 构建界面控件与布局分组（UI Controls），支持 Embed Form 全量跨文件展开
     */
    private fun buildUiControlsGroup(
        rootTag: XmlTag,
        visitedForms: Set<String>,
        contextFile: PsiFile?
    ): TreeElement? {
        val bodyTags = rootTag.findSubTags("Body") + rootTag.findSubTags("Component")
        if (bodyTags.isEmpty()) return null

        val controls = mutableListOf<XmlTag>()
        for (body in bodyTags) {
            collectNamedControls(body, controls)
        }

        if (controls.isEmpty()) return null

        return YigoStructureTreeElement(
            psiElement = bodyTags.first(),
            label = "界面控件 (UI Controls)",
            location = "${controls.size} 个控件",
            icon = AllIcons.Nodes.Property,
            childrenProvider = {
                controls.map { ctrlTag ->
                    createControlTreeElement(ctrlTag, visitedForms, contextFile)
                }
            }
        )
    }

    /**
     * 构建独立的「嵌入表单 (Embedded Forms)」大纲分组
     * 让主表单引用的所有 Embed 子表单及其内部的控件和数据源一目了然
     */
    private fun buildEmbeddedFormsGroup(
        rootTag: XmlTag,
        visitedForms: Set<String>,
        contextFile: PsiFile?
    ): TreeElement? {
        val embeds = mutableListOf<XmlTag>()
        collectAllEmbedTags(rootTag, embeds)

        if (embeds.isEmpty()) return null

        val anchorTag = embeds.first()

        return YigoStructureTreeElement(
            psiElement = anchorTag,
            label = "嵌入表单 (Embedded Forms)",
            location = "${embeds.size} 个嵌入表单",
            icon = AllIcons.Nodes.Related,
            childrenProvider = {
                embeds.map { embedTag ->
                    createEmbeddedFormDetailTreeElement(embedTag, visitedForms, contextFile)
                }
            }
        )
    }

    private fun collectAllEmbedTags(tag: XmlTag, result: MutableList<XmlTag>) {
        if (tag.name == "Embed") {
            result.add(tag)
            return
        }
        for (sub in tag.subTags) {
            collectAllEmbedTags(sub, result)
        }
    }

    /**
     * 创建嵌入表单独立大纲节点（展示其 UI Controls 与 DataSources 全量跨文件展开）
     */
    private fun createEmbeddedFormDetailTreeElement(
        embedTag: XmlTag,
        visitedForms: Set<String>,
        contextFile: PsiFile?
    ): TreeElement {
        val formKey = embedTag.getAttributeValue("FormKey") ?: ""
        val embedKey = embedTag.getAttributeValue("Key") ?: formKey.ifBlank { "Embed" }
        val ownCaption = embedTag.getAttributeValue("Caption")
        val parentCaption = embedTag.parentTag?.getAttributeValue("Caption")

        val targetFormTag = if (formKey.isNotBlank()) {
            findFormTag(embedTag.project, formKey, contextFile ?: embedTag.containingFile)
        } else null

        val formCaption = targetFormTag?.getAttributeValue("Caption")

        val effectiveCaption = when {
            !ownCaption.isNullOrBlank() -> ownCaption
            !formCaption.isNullOrBlank() && !parentCaption.isNullOrBlank() && formCaption != parentCaption -> "$parentCaption · $formCaption"
            !formCaption.isNullOrBlank() -> formCaption
            !parentCaption.isNullOrBlank() -> parentCaption
            else -> null
        }

        val title = when {
            !effectiveCaption.isNullOrBlank() -> "Embed: $embedKey [$effectiveCaption] (表单: $formKey)"
            formKey.isNotBlank() -> "Embed: $embedKey [嵌入表单: $formKey]"
            else -> "Embed: $embedKey"
        }

        return YigoStructureTreeElement(
            psiElement = embedTag,
            label = title,
            location = if (formKey.isNotBlank()) "-> $formKey" else null,
            icon = AllIcons.Nodes.Related,
            childrenProvider = {
                if (formKey.isBlank()) {
                    emptyList()
                } else if (visitedForms.contains(formKey)) {
                    listOf(
                        YigoStructureTreeElement(
                            psiElement = embedTag,
                            label = "循环引用: $formKey",
                            icon = AllIcons.General.Warning
                        )
                    )
                } else if (targetFormTag != null) {
                    val subElements = mutableListOf<TreeElement>()

                    // 1. 展开嵌入表单的界面控件
                    val subControls = mutableListOf<XmlTag>()
                    val extBodies = targetFormTag.findSubTags("Body") + targetFormTag.findSubTags("Component")
                    for (b in extBodies) {
                        collectNamedControls(b, subControls)
                    }

                    if (subControls.isNotEmpty()) {
                        subElements.add(
                            YigoStructureTreeElement(
                                psiElement = extBodies.firstOrNull() ?: targetFormTag,
                                label = "界面控件 (UI Controls)",
                                location = "${subControls.size} 个控件",
                                icon = AllIcons.Nodes.Property,
                                childrenProvider = {
                                    subControls.map { subCtrl ->
                                        createControlTreeElement(subCtrl, visitedForms + formKey, targetFormTag.containingFile)
                                    }
                                }
                            )
                        )
                    }

                    // 2. 展开嵌入表单的数据源与数据对象
                    val subDsGroup = buildDataSourcesGroup(targetFormTag, targetFormTag.containingFile)
                    if (subDsGroup != null) {
                        subElements.add(subDsGroup)
                    }

                    // 3. 展开嵌入表单的宏与操作（如有）
                    val subOps = buildOperationsGroup(targetFormTag)
                    if (subOps != null) {
                        subElements.add(subOps)
                    }

                    if (subElements.isEmpty()) {
                        listOf(
                            YigoStructureTreeElement(
                                psiElement = targetFormTag,
                                label = "(外部表单内容为空)",
                                icon = AllIcons.General.Information
                            )
                        )
                    } else {
                        subElements
                    }
                } else {
                    listOf(
                        YigoStructureTreeElement(
                            psiElement = embedTag,
                            label = "未找到外部表单: $formKey",
                            icon = AllIcons.General.Warning
                        )
                    )
                }
            }
        )
    }

    private fun collectNamedControls(tag: XmlTag, result: MutableList<XmlTag>) {
        val tagName = tag.name
        val key = tag.getAttributeValue("Key")
        val caption = tag.getAttributeValue("Caption")

        val isContainerOnly = tagName in setOf("Body", "Block", "RowDefCollection", "ColumnDefCollection", "SplitSize", "ToolBar")

        if (!isContainerOnly && (!key.isNullOrBlank() || !caption.isNullOrBlank() || tagName == "Grid" || tagName == "Embed")) {
            result.add(tag)
        }

        for (sub in tag.subTags) {
            if (tagName != "Grid" && tagName != "Embed") {
                collectNamedControls(sub, result)
            }
        }
    }

    /**
     * 创建控件节点：如果是 Embed，递归跨文件全量展开目标表单中的控件（方案 A）
     */
    private fun createControlTreeElement(
        ctrlTag: XmlTag,
        visitedForms: Set<String>,
        contextFile: PsiFile?
    ): TreeElement {
        val tagName = ctrlTag.name
        val key = ctrlTag.getAttributeValue("Key") ?: tagName
        val caption = ctrlTag.getAttributeValue("Caption")
        val binding = ctrlTag.getAttributeValue("BindingCellKey")
            ?: ctrlTag.getAttributeValue("ColumnKey")
            ?: ctrlTag.getAttributeValue("TableKey")

        // 1. 处理 Embed 嵌入表单（跨文件全量展开）
        if (tagName == "Embed") {
            val formKey = ctrlTag.getAttributeValue("FormKey") ?: ""
            val embedKey = ctrlTag.getAttributeValue("Key") ?: formKey.ifBlank { "Embed" }
            val ownCaption = ctrlTag.getAttributeValue("Caption")
            val parentCaption = ctrlTag.parentTag?.getAttributeValue("Caption")

            val targetFormTag = if (formKey.isNotBlank()) {
                findFormTag(ctrlTag.project, formKey, contextFile ?: ctrlTag.containingFile)
            } else null

            val formCaption = targetFormTag?.getAttributeValue("Caption")

            val effectiveCaption = when {
                !ownCaption.isNullOrBlank() -> ownCaption
                !formCaption.isNullOrBlank() && !parentCaption.isNullOrBlank() && formCaption != parentCaption -> "$parentCaption · $formCaption"
                !formCaption.isNullOrBlank() -> formCaption
                !parentCaption.isNullOrBlank() -> parentCaption
                else -> null
            }

            val title = when {
                !effectiveCaption.isNullOrBlank() -> "Embed: $embedKey [$effectiveCaption] (表单: $formKey)"
                formKey.isNotBlank() -> "Embed: $embedKey [嵌入表单: $formKey]"
                else -> "Embed: $embedKey"
            }

            return YigoStructureTreeElement(
                psiElement = ctrlTag,
                label = title,
                location = if (formKey.isNotBlank()) "-> $formKey" else null,
                icon = AllIcons.Nodes.Related,
                childrenProvider = {
                    if (formKey.isBlank()) {
                        emptyList()
                    } else if (visitedForms.contains(formKey)) {
                        listOf(
                            YigoStructureTreeElement(
                                psiElement = ctrlTag,
                                label = "循环引用: $formKey",
                                icon = AllIcons.General.Warning
                            )
                        )
                    } else if (targetFormTag != null) {
                        // 跨文件展开目标子表单的所有控件
                        val subControls = mutableListOf<XmlTag>()
                        val extBodies = targetFormTag.findSubTags("Body") + targetFormTag.findSubTags("Component")
                        for (b in extBodies) {
                            collectNamedControls(b, subControls)
                        }
                        subControls.map { subCtrl ->
                            createControlTreeElement(subCtrl, visitedForms + formKey, targetFormTag.containingFile)
                        }
                    } else {
                        listOf(
                            YigoStructureTreeElement(
                                psiElement = ctrlTag,
                                label = "未找到外部表单: $formKey",
                                icon = AllIcons.General.Warning
                            )
                        )
                    }
                }
            )
        }

        // 2. 普通控件或 Grid 表格
        val title = if (caption.isNullOrBlank()) "$key: $tagName" else "$key: $tagName [$caption]"
        val location = if (!binding.isNullOrBlank()) "-> $binding" else null

        val icon = when (tagName) {
            "Grid" -> AllIcons.Nodes.DataTables
            "Button" -> AllIcons.Actions.Execute
            "DatePicker", "UTCDatePicker" -> AllIcons.Nodes.Aspect
            "ComboBox", "Dict" -> AllIcons.Nodes.Enum
            "TabPanel", "SplitPanel", "FlexFlowLayoutPanel", "GridLayoutPanel", "FlexGridLayoutPanel" -> AllIcons.Nodes.Folder
            else -> AllIcons.Nodes.Property
        }

        val isGrid = (tagName == "Grid")

        return YigoStructureTreeElement(
            psiElement = ctrlTag,
            label = title,
            location = location,
            icon = icon,
            childrenProvider = {
                if (isGrid) {
                    val gridCols = ctrlTag.findSubTags("GridColumn")
                    gridCols.map { colTag ->
                        val colKey = colTag.getAttributeValue("Key") ?: "Col"
                        val colCap = colTag.getAttributeValue("Caption")
                        val colBind = colTag.getAttributeValue("ColumnKey")
                        val colTitle = if (colCap.isNullOrBlank()) "GridColumn: $colKey" else "GridColumn: $colKey [$colCap]"
                        val colLoc = if (!colBind.isNullOrBlank()) "-> $colBind" else null

                        YigoStructureTreeElement(
                            psiElement = colTag,
                            label = colTitle,
                            location = colLoc,
                            icon = AllIcons.Nodes.Field
                        )
                    }
                } else {
                    emptyList()
                }
            }
        )
    }

    /**
     * 四重保障跨文件查找 Form 根标签（确保在 projectScope、allScope、同目录及全局文件名皆能 100% 命中）
     */
    fun findFormTag(project: Project, formKey: String, contextFile: PsiFile?): XmlTag? {
        if (formKey.isBlank()) return null

        // 1. 优先使用 FormIndex（快速索引）
        try {
            val defAttr = FormIndex.findFormDefinition(project, formKey)
            if (defAttr != null) {
                val tag = PsiTreeUtil.getParentOfType(defAttr, XmlTag::class.java)
                if (tag != null && tag.name == "Form") return tag
            }
        } catch (_: Throwable) {}

        // 2. 尝试使用 FormIndex 索引扩大到 allScope
        try {
            val files = FileBasedIndex.getInstance().getContainingFiles(
                FormIndex.KEY,
                formKey,
                GlobalSearchScope.allScope(project)
            )
            for (file in files) {
                val psi = PsiManager.getInstance(project).findFile(file) as? XmlFile ?: continue
                val root = psi.rootTag ?: continue
                if (root.name == "Form" && (root.getAttributeValue("Key") == formKey || formKey.equals(file.nameWithoutExtension, ignoreCase = true))) {
                    return root
                }
            }
        } catch (_: Throwable) {}

        // 3. 上下文文件同级目录查找（针对 prompt、测试目录或未加入 sources 的目录）
        try {
            val contextDir = contextFile?.containingDirectory
            if (contextDir != null) {
                val targetFile = contextDir.findFile("$formKey.xml") as? XmlFile
                    ?: contextDir.files.filterIsInstance<XmlFile>().firstOrNull {
                        it.name.equals("$formKey.xml", ignoreCase = true)
                    }
                val root = targetFile?.rootTag
                if (root != null && root.name == "Form") {
                    return root
                }
            }
        } catch (_: Throwable) {}

        // 4. FilenameIndex 全局按文件名查找 $formKey.xml
        try {
            val filesByName = FilenameIndex.getVirtualFilesByName(
                "$formKey.xml",
                GlobalSearchScope.allScope(project)
            )
            for (vf in filesByName) {
                val psi = PsiManager.getInstance(project).findFile(vf) as? XmlFile ?: continue
                val root = psi.rootTag ?: continue
                if (root.name == "Form" && (root.getAttributeValue("Key") == formKey || formKey.equals(vf.nameWithoutExtension, ignoreCase = true))) {
                    return root
                }
            }
        } catch (_: Throwable) {}

        return null
    }

    /**
     * 四重保障跨文件查找 DataObject 根标签
     */
    fun findDataObjectTag(project: Project, objectKey: String, contextFile: PsiFile?): XmlTag? {
        if (objectKey.isBlank()) return null

        // 1. 优先使用 DataObjectIndex
        try {
            val defAttr = DataObjectIndex.findDataObjectDefinition(project, objectKey)
            if (defAttr != null) {
                val tag = PsiTreeUtil.getParentOfType(defAttr, XmlTag::class.java)
                if (tag != null) return tag
            }
        } catch (_: Throwable) {}

        // 2. 扩大到 allScope
        try {
            val files = FileBasedIndex.getInstance().getContainingFiles(
                DataObjectIndex.KEY,
                objectKey,
                GlobalSearchScope.allScope(project)
            )
            for (file in files) {
                val psi = PsiManager.getInstance(project).findFile(file) as? XmlFile ?: continue
                val root = psi.rootTag ?: continue
                val doTag = findDataObjectInTag(root, objectKey)
                if (doTag != null) return doTag
            }
        } catch (_: Throwable) {}

        // 3. 上下文文件同级目录
        try {
            val contextDir = contextFile?.containingDirectory
            if (contextDir != null) {
                val targetFile = contextDir.findFile("$objectKey.xml") as? XmlFile
                    ?: contextDir.files.filterIsInstance<XmlFile>().firstOrNull {
                        it.name.equals("$objectKey.xml", ignoreCase = true)
                    }
                val root = targetFile?.rootTag
                if (root != null) {
                    val doTag = findDataObjectInTag(root, objectKey)
                    if (doTag != null) return doTag
                }
            }
        } catch (_: Throwable) {}

        // 4. FilenameIndex 全局按文件名查找 $objectKey.xml
        try {
            val filesByName = FilenameIndex.getVirtualFilesByName(
                "$objectKey.xml",
                GlobalSearchScope.allScope(project)
            )
            for (vf in filesByName) {
                val psi = PsiManager.getInstance(project).findFile(vf) as? XmlFile ?: continue
                val root = psi.rootTag ?: continue
                val doTag = findDataObjectInTag(root, objectKey)
                if (doTag != null) return doTag
            }
        } catch (_: Throwable) {}

        return null
    }

    private fun findDataObjectInTag(root: XmlTag, objectKey: String): XmlTag? {
        if (root.name == "DataObject" && root.getAttributeValue("Key") == objectKey) {
            return root
        }
        val ds = root.findFirstSubTag("DataSource") ?: root
        for (dot in ds.findSubTags("DataObject")) {
            if (dot.getAttributeValue("Key") == objectKey) {
                return dot
            }
        }
        return null
    }

    /**
     * 构建宏与公式分组（Macros & Logic）
     */
    private fun buildMacrosGroup(rootTag: XmlTag): TreeElement? {
        val macros = mutableListOf<XmlTag>()
        for (col in rootTag.findSubTags("MacroCollection")) {
            macros.addAll(col.findSubTags("Macro"))
        }
        macros.addAll(rootTag.findSubTags("Macro"))

        if (macros.isEmpty()) return null

        val anchorTag = rootTag.findSubTags("MacroCollection").firstOrNull() ?: rootTag

        return YigoStructureTreeElement(
            psiElement = anchorTag,
            label = "宏与业务逻辑 (Macros)",
            location = "${macros.size} 个宏",
            icon = AllIcons.Nodes.Function,
            childrenProvider = {
                macros.map { macroTag ->
                    val key = macroTag.getAttributeValue("Key") ?: "Macro"
                    val caption = macroTag.getAttributeValue("Caption")
                    val title = if (caption.isNullOrBlank()) "Macro: $key()" else "Macro: $key() [$caption]"

                    YigoStructureTreeElement(
                        psiElement = macroTag,
                        label = title,
                        icon = AllIcons.Nodes.Function
                    )
                }
            }
        )
    }

    /**
     * 构建映射/数据关系分组（针对 Map / Relation 等文件）
     */
    private fun buildOtherGroup(rootTag: XmlTag): TreeElement? {
        if (rootTag.name != "Map") return null

        val tables = rootTag.findSubTags("Table") + rootTag.findSubTags("SourceTable") + rootTag.findSubTags("TargetTable")
        if (tables.isEmpty()) return null

        return YigoStructureTreeElement(
            psiElement = rootTag,
            label = "映射关系 (Mappings)",
            icon = AllIcons.Nodes.Related,
            childrenProvider = {
                tables.map { tTag ->
                    val key = tTag.getAttributeValue("Key") ?: tTag.name
                    val fields = tTag.findSubTags("Field") + tTag.findSubTags("SourceField") + tTag.findSubTags("TargetField")

                    YigoStructureTreeElement(
                        psiElement = tTag,
                        label = key,
                        location = "${fields.size} 项",
                        icon = AllIcons.Nodes.DataTables,
                        childrenProvider = {
                            fields.map { fTag ->
                                val fKey = fTag.getAttributeValue("TargetFieldKey")
                                    ?: fTag.getAttributeValue("Key")
                                    ?: fTag.name
                                val def = fTag.getAttributeValue("Definition")

                                YigoStructureTreeElement(
                                    psiElement = fTag,
                                    label = fKey,
                                    location = def?.ifBlank { null },
                                    icon = AllIcons.Nodes.Field
                                )
                            }
                        }
                    )
                }
            }
        )
    }
}

