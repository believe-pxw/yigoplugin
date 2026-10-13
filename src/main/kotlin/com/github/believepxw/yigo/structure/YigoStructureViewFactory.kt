package com.github.believepxw.yigo.structure

import com.intellij.icons.AllIcons
import com.intellij.ide.structureView.*
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
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
 * 同时实现 ElementInfoProvider 与 ExpandInfoProvider，确保 Ctrl + F12 自动展开嵌入表单、Grid 列与单元格等层级
 */
class YigoStructureViewModel(
    private val xmlFile: XmlFile,
    editor: Editor?
) : StructureViewModelBase(xmlFile, editor, createRootTreeElement(xmlFile)),
    StructureViewModel.ElementInfoProvider,
    StructureViewModel.ExpandInfoProvider {

    init {
        withSuitableClasses(XmlTag::class.java, XmlFile::class.java)
    }

    override fun getSuitableClasses(): Array<Class<*>> {
        return arrayOf(XmlTag::class.java, XmlFile::class.java)
    }

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement?): Boolean {
        if (element is YigoStructureTreeElement) {
            return !element.isLeafNode
        }
        return false
    }

    override fun isAlwaysLeaf(element: StructureViewTreeElement?): Boolean {
        if (element is YigoStructureTreeElement) {
            return element.isLeafNode
        }
        val value = element?.value
        if (value is XmlTag) {
            return value.name in LEAF_TAGS
        }
        return false
    }

    override fun isAutoExpand(element: StructureViewTreeElement): Boolean {
        if (element is YigoStructureTreeElement) {
            return !element.isLeafNode
        }
        return true
    }

    override fun isSmartExpand(): Boolean = false

    override fun getMinimumAutoExpandDepth(): Int = 4

    companion object {
        private val LEAF_TAGS = setOf(
            "Column", "GridColumn", "GridCell", "Operation", "Item", "Macro",
            "TextEditor", "NumberEditor", "Dict", "Label", "Button",
            "CheckBox", "ComboBox", "DatePicker", "UTCDatePicker",
            "CheckBoxGroup", "RadioButton", "TextArea"
        )

        private fun createRootTreeElement(xmlFile: XmlFile): StructureViewTreeElement {
            val rootTag = xmlFile.rootTag ?: return YigoStructureTreeElement(
                psiElement = xmlFile,
                label = xmlFile.name,
                icon = AllIcons.Nodes.Class,
                isLeafNode = true
            )
            return YigoStructureTreeBuilder.buildRootElement(rootTag, xmlFile)
        }
    }
}

/**
 * 用于区分同一 PsiElement 在结构树不同分支（如虚拟分组节点、跨文件嵌入表单、重复引用数据源）中的唯一标识包装器
 * 避免 Ctrl + F12 (FilteringTreeStructure.myDescriptors2Nodes) 将相同 PsiElement 的树节点当作重复节点剪枝丢弃
 */
data class ScopedTreeValue(
    val psiElement: PsiElement,
    val scopeKey: String
) : ModificationTracker {
    override fun getModificationCount(): Long {
        return if (psiElement.isValid) (psiElement.containingFile?.modificationStamp ?: 0L) else -1L
    }
}

/**
 * 通用的 Yigo 树节点元素
 */
class YigoStructureTreeElement(
    val psiElement: PsiElement,
    private val label: String,
    private val location: String? = null,
    private val icon: Icon,
    private val scopeKey: String = "",
    val isLeafNode: Boolean = false,
    private val childrenProvider: () -> List<TreeElement> = { emptyList() }
) : StructureViewTreeElement, Navigatable, SearchableTextProvider {

    private val valueObject: Any = if (scopeKey.isEmpty()) {
        psiElement
    } else {
        ScopedTreeValue(psiElement, scopeKey)
    }

    override fun getValue(): Any = valueObject

    override fun getSearchableText(): String {
        return if (location.isNullOrBlank()) label else "$label $location"
    }

    override fun navigate(requestFocus: Boolean) {
        if (psiElement.isValid && psiElement is Navigatable) {
            psiElement.navigate(requestFocus)
        }
    }

    override fun canNavigate(): Boolean = psiElement.isValid && (psiElement as? Navigatable)?.canNavigate() == true
    override fun canNavigateToSource(): Boolean = psiElement.isValid && (psiElement as? Navigatable)?.canNavigateToSource() == true

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {
            override fun getPresentableText(): String = label
            override fun getLocationString(): String? = location
            override fun getIcon(unused: Boolean): Icon = icon
        }
    }

    override fun getChildren(): Array<TreeElement> {
        if (isLeafNode) return emptyArray()
        return childrenProvider().toTypedArray()
    }
}

/**
 * 业务结构树构建器：智能解析 Yigo XML 语义大纲（支持全量跨文件递归展开）
 */
object YigoStructureTreeBuilder {

    private val NON_CONTROL_TAGS = setOf(
        "Body", "Block", "RowDefCollection", "RowDef", "ColumnDefCollection", "ColumnDef",
        "SplitSize", "ToolBar", "ToolBarItemCollection", "DataBinding", "ItemFilter",
        "Filter", "FilterValue", "Condition", "Item", "FormulaItems", "Format",
        "CheckRule", "ValueChanged", "DefaultFormulaValue", "DefaultValue",
        "OnClick", "OnRowDelete", "OnRowInsert", "OnRowClick", "OnRowDblClick",
        "GridFilter", "TraceCollection", "Trace", "Var", "ItemChanged",
        "GridColumnCollection", "GridColumn", "GridRowCollection", "GridRow", "GridCell"
    )

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
            scopeKey = "",
            isLeafNode = false,
            childrenProvider = {
                val groups = mutableListOf<TreeElement>()

                // 1. 数据源与字段 (DataSources & Columns) -> 对应 Java 的 Fields，支持 EmbedTable 与 RefObjectKey 全量跨文件展开
                val dsGroup = buildDataSourcesGroup(rootTag, contextFile, "")
                if (dsGroup != null) groups.add(dsGroup)

                // 2. 操作集合 (OperationCollection) -> 对应 Java 的 Methods
                val opsGroup = buildOperationsGroup(rootTag, "")
                if (opsGroup != null) groups.add(opsGroup)

                // 3. 界面控件与布局 (UI Controls) -> 支持 Embed Form 全量跨文件展开
                val uiGroup = buildUiControlsGroup(rootTag, initialVisitedForms, contextFile, "")
                if (uiGroup != null) groups.add(uiGroup)

                // 4. 嵌入表单专属大纲 (Embedded Forms) -> 全量跨文件展开各个嵌入子表单
                val embedGroup = buildEmbeddedFormsGroup(rootTag, initialVisitedForms, contextFile, "embed-section")
                if (embedGroup != null) groups.add(embedGroup)

                // 5. 宏与业务逻辑 (Macros & Formulas)
                val macrosGroup = buildMacrosGroup(rootTag, "")
                if (macrosGroup != null) groups.add(macrosGroup)

                // 6. 字典/映射/其他 (Map & Relations)
                val otherGroup = buildOtherGroup(rootTag, "")
                if (otherGroup != null) groups.add(otherGroup)

                groups
            }
        )
    }

    /**
     * 构建数据源与数据对象分组（DataSources, DataObjects, Tables, EmbedTables & Columns）
     */
    private fun buildDataSourcesGroup(
        rootTag: XmlTag,
        contextFile: PsiFile?,
        scopePath: String
    ): TreeElement? {
        // 1. 如果 rootTag 本身就是 DataObject
        if (rootTag.name == "DataObject") {
            val tables = collectTablesFromTag(rootTag)
            if (tables.isEmpty()) return null
            val doScope = if (scopePath.isEmpty()) "do-root" else "$scopePath/do-root"
            return createDataObjectTreeElement(rootTag, emptySet(), contextFile, doScope, isExternalDo = false)
        }

        // 2. 收集本地 DataObject 以及引用的外部 DataObject
        val localDataObjects = mutableListOf<XmlTag>()
        val externalDataObjects = mutableListOf<XmlTag>()
        for (ds in rootTag.findSubTags("DataSource")) {
            localDataObjects.addAll(ds.findSubTags("DataObject"))

            // 支持 <DataSource RefObjectKey="..."> 跨文件引用
            val refObjKey = ds.getAttributeValue("RefObjectKey")
            if (!refObjKey.isNullOrBlank()) {
                val linkedDoTag = findDataObjectTag(rootTag.project, refObjKey, contextFile)
                if (linkedDoTag != null) {
                    externalDataObjects.add(linkedDoTag)
                }
            }
        }
        localDataObjects.addAll(rootTag.findSubTags("DataObject"))

        val allDataObjects = localDataObjects + externalDataObjects

        // 3. 收集所有独立 Table（不属于任何 DataObject 的表）
        val allTables = mutableListOf<XmlTag>()
        for (ds in rootTag.findSubTags("DataSource")) {
            allTables.addAll(collectTablesFromTag(ds))
        }
        allTables.addAll(collectTablesFromTag(rootTag))

        val doTables = allDataObjects.flatMap { collectTablesFromTag(it) }.toSet()
        val standaloneTables = allTables.filter { it !in doTables }

        if (allDataObjects.isEmpty() && standaloneTables.isEmpty()) return null

        val anchorTag = rootTag.findSubTags("DataSource").firstOrNull() ?: rootTag

        val totalTableCount = doTables.size + standaloneTables.size
        val locText = if (allDataObjects.isNotEmpty()) {
            "${allDataObjects.size} 个对象, $totalTableCount 个表"
        } else {
            "$totalTableCount 个表"
        }

        return YigoStructureTreeElement(
            psiElement = anchorTag,
            label = "数据源与对象 (DataSources)",
            location = locText,
            icon = AllIcons.Nodes.DataTables,
            scopeKey = "$scopePath/group:datasources",
            isLeafNode = false,
            childrenProvider = {
                val list = mutableListOf<TreeElement>()

                // 本地 DataObject
                localDataObjects.forEachIndexed { idx, dObj ->
                    val childScope = if (scopePath.isEmpty()) "" else "$scopePath/local-do:$idx"
                    list.add(createDataObjectTreeElement(dObj, emptySet(), contextFile, childScope, isExternalDo = false))
                }

                // 跨文件引用的外部 DataObject
                externalDataObjects.forEachIndexed { idx, dObj ->
                    val doKey = dObj.getAttributeValue("Key") ?: idx.toString()
                    val childScope = "$scopePath/ext-do:$idx:$doKey"
                    list.add(createDataObjectTreeElement(dObj, emptySet(), contextFile, childScope, isExternalDo = true))
                }

                // 独立 Table
                standaloneTables.forEachIndexed { idx, table ->
                    val childScope = if (scopePath.isEmpty()) "" else "$scopePath/table:$idx"
                    list.add(createTableTreeElement(table, childScope))
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
        return tables.distinct()
    }

    /**
     * 创建 DataObject 节点，同时处理其本地 Table 与嵌入的 EmbedTable（全量跨文件展开）
     */
    private fun createDataObjectTreeElement(
        doTag: XmlTag,
        visitedDataObjects: Set<String>,
        contextFile: PsiFile?,
        scopePath: String,
        isExternalDo: Boolean
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
        val distinctEmbedTables = embedTables.distinct()

        return YigoStructureTreeElement(
            psiElement = doTag,
            label = doTitle,
            location = doLoc,
            icon = AllIcons.Nodes.Class,
            scopeKey = if (isExternalDo && scopePath.isEmpty()) "ext-do:$doKey" else scopePath,
            isLeafNode = tables.isEmpty() && distinctEmbedTables.isEmpty(),
            childrenProvider = {
                val list = mutableListOf<TreeElement>()

                // 1. 本地各个 Table
                tables.forEachIndexed { idx, tableTag ->
                    val tKey = tableTag.getAttributeValue("Key") ?: idx.toString()
                    val tableScope = if (!isExternalDo && scopePath.isEmpty()) "" else "$scopePath/table:$idx:$tKey"
                    list.add(createTableTreeElement(tableTag, tableScope))
                }

                // 2. 跨文件展开 EmbedTable
                distinctEmbedTables.forEachIndexed { idx, embedTag ->
                    val tableKey = embedTag.getAttributeValue("TableKeys")
                        ?: embedTag.getAttributeValue("Key")
                        ?: "EmbedTable"
                    val objectKey = embedTag.getAttributeValue("ObjectKey")
                    val embedScope = if (!isExternalDo && scopePath.isEmpty()) "" else "$scopePath/embed-table:$idx:$tableKey"

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
                                        scopeKey = embedScope,
                                        isLeafNode = columns.isEmpty(),
                                        childrenProvider = {
                                            columns.mapIndexed { colIdx, colTag ->
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
                                                    icon = AllIcons.Nodes.Field,
                                                    scopeKey = "$scopePath/embed-table:$idx:$tableKey/col:$colIdx:$colKey",
                                                    isLeafNode = true
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
                                        icon = AllIcons.Nodes.DataTables,
                                        scopeKey = embedScope,
                                        isLeafNode = true
                                    )
                                )
                            }
                        } else {
                            list.add(
                                YigoStructureTreeElement(
                                    psiElement = embedTag,
                                    label = "EmbedTable: $tableKey (外部对象未找到: $objectKey)",
                                    icon = AllIcons.Nodes.DataTables,
                                    scopeKey = embedScope,
                                    isLeafNode = true
                                )
                            )
                        }
                    } else if (visitedDataObjects.contains(objectKey)) {
                        list.add(
                            YigoStructureTreeElement(
                                psiElement = embedTag,
                                label = "EmbedTable: $tableKey [循环引用: $objectKey]",
                                icon = AllIcons.Nodes.DataTables,
                                scopeKey = embedScope,
                                isLeafNode = true
                            )
                        )
                    } else {
                        list.add(
                            YigoStructureTreeElement(
                                psiElement = embedTag,
                                label = "EmbedTable: $tableKey",
                                icon = AllIcons.Nodes.DataTables,
                                scopeKey = embedScope,
                                isLeafNode = true
                            )
                        )
                    }
                }

                list
            }
        )
    }

    private fun createTableTreeElement(tableTag: XmlTag, scopePath: String): TreeElement {
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
            scopeKey = scopePath,
            isLeafNode = columns.isEmpty(),
            childrenProvider = {
                columns.mapIndexed { idx, colTag ->
                    val colKey = colTag.getAttributeValue("Key") ?: "Col"
                    val colCap = colTag.getAttributeValue("Caption")
                    val dataType = colTag.getAttributeValue("DataType")
                        ?: colTag.getAttributeValue("Type")
                        ?: colTag.getAttributeValue("DataElementKey")
                        ?: ""
                    val colTitle = if (colCap.isNullOrBlank()) "Column: $colKey" else "Column: $colKey [$colCap]"
                    val colScope = if (scopePath.isEmpty()) "" else "$scopePath/col:$idx:$colKey"

                    YigoStructureTreeElement(
                        psiElement = colTag,
                        label = colTitle,
                        location = dataType.ifBlank { null },
                        icon = AllIcons.Nodes.Field,
                        scopeKey = colScope,
                        isLeafNode = true
                    )
                }
            }
        )
    }

    /**
     * 构建操作集合与动作分组（OperationCollection & Operations）
     */
    private fun buildOperationsGroup(rootTag: XmlTag, scopePath: String): TreeElement? {
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
                scopeKey = if (scopePath.isEmpty()) "" else "$scopePath/group:ops",
                isLeafNode = false,
                childrenProvider = {
                    val list = mutableListOf<TreeElement>()
                    topOpCol.subTags.forEachIndexed { idx, sub ->
                        val childScope = if (scopePath.isEmpty()) "" else "$scopePath/op:$idx"
                        when (sub.name) {
                            "Operation" -> list.add(createOperationTreeElement(sub, childScope))
                            "OperationCollection" -> list.add(createOperationCollectionTreeElement(sub, childScope))
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
            scopeKey = "$scopePath/group:ops",
            isLeafNode = false,
            childrenProvider = {
                val list = mutableListOf<TreeElement>()

                directOps.forEachIndexed { idx, op ->
                    val childScope = if (scopePath.isEmpty()) "" else "$scopePath/direct-op:$idx"
                    list.add(createOperationTreeElement(op, childScope))
                }

                opCollections.forEachIndexed { idx, opCol ->
                    val childScope = if (scopePath.isEmpty()) "" else "$scopePath/op-col:$idx"
                    list.add(createOperationCollectionTreeElement(opCol, childScope))
                }

                list
            }
        )
    }

    private fun createOperationTreeElement(opTag: XmlTag, scopePath: String): TreeElement {
        val opKey = opTag.getAttributeValue("Key") ?: "Op"
        val opCap = opTag.getAttributeValue("Caption")
        val action = opTag.getAttributeValue("Action")
            ?: opTag.getAttributeValue("Formula")
            ?: opTag.findFirstSubTag("Action")?.value?.trimmedText
        val opTitle = if (opCap.isNullOrBlank()) "Operation: $opKey" else "Operation: $opKey [$opCap]"

        return YigoStructureTreeElement(
            psiElement = opTag,
            label = opTitle,
            location = action?.ifBlank { null },
            icon = AllIcons.Nodes.Method,
            scopeKey = scopePath,
            isLeafNode = true
        )
    }

    private fun createOperationCollectionTreeElement(colTag: XmlTag, scopePath: String): TreeElement {
        val colKey = colTag.getAttributeValue("Key") ?: "Group"
        val colCap = colTag.getAttributeValue("Caption")
        val colTitle = if (colCap.isNullOrBlank()) "OperationCollection: $colKey ▾" else "OperationCollection: $colKey [$colCap ▾]"

        return YigoStructureTreeElement(
            psiElement = colTag,
            label = colTitle,
            icon = AllIcons.Nodes.Folder,
            scopeKey = scopePath,
            isLeafNode = false,
            childrenProvider = {
                val children = mutableListOf<TreeElement>()
                colTag.subTags.forEachIndexed { idx, sub ->
                    val childScope = if (scopePath.isEmpty()) "" else "$scopePath/sub:$idx"
                    when (sub.name) {
                        "Operation" -> children.add(createOperationTreeElement(sub, childScope))
                        "OperationCollection" -> children.add(createOperationCollectionTreeElement(sub, childScope))
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
        contextFile: PsiFile?,
        scopePath: String
    ): TreeElement? {
        val bodyTags = rootTag.findSubTags("Body") + rootTag.findSubTags("Component")
        if (bodyTags.isEmpty()) return null

        val controls = mutableListOf<XmlTag>()
        for (body in bodyTags) {
            collectNamedControls(body, controls)
        }
        val distinctControls = controls.distinct()

        if (distinctControls.isEmpty()) return null

        return YigoStructureTreeElement(
            psiElement = bodyTags.first(),
            label = "界面控件 (UI Controls)",
            location = "${distinctControls.size} 个控件",
            icon = AllIcons.Nodes.Property,
            scopeKey = "$scopePath/group:ui-controls",
            isLeafNode = false,
            childrenProvider = {
                distinctControls.mapIndexed { idx, ctrlTag ->
                    val ctrlScope = if (scopePath.isEmpty()) "" else "$scopePath/ctrl:$idx"
                    createControlTreeElement(ctrlTag, visitedForms, contextFile, ctrlScope)
                }
            }
        )
    }

    /**
     * 构建独立的「嵌入表单 (Embedded Forms)」大纲分组
     * 让主表单引用的所有 Embed 子表单及其内部的控件、Grid 列与单元格、数据源一目了然
     */
    private fun buildEmbeddedFormsGroup(
        rootTag: XmlTag,
        visitedForms: Set<String>,
        contextFile: PsiFile?,
        scopePath: String
    ): TreeElement? {
        val embeds = mutableListOf<XmlTag>()
        collectAllEmbedTags(rootTag, embeds)
        val distinctEmbeds = embeds.distinct()

        if (distinctEmbeds.isEmpty()) return null

        val anchorTag = distinctEmbeds.first()

        return YigoStructureTreeElement(
            psiElement = anchorTag,
            label = "嵌入表单 (Embedded Forms)",
            location = "${distinctEmbeds.size} 个嵌入表单",
            icon = AllIcons.Nodes.Related,
            scopeKey = "$scopePath/group:embedded-forms",
            isLeafNode = false,
            childrenProvider = {
                distinctEmbeds.mapIndexed { idx, embedTag ->
                    val embedKey = embedTag.getAttributeValue("Key") ?: embedTag.getAttributeValue("FormKey") ?: idx.toString()
                    createEmbeddedFormDetailTreeElement(
                        embedTag = embedTag,
                        visitedForms = visitedForms,
                        contextFile = contextFile,
                        scopePath = "$scopePath/embed:$idx:$embedKey"
                    )
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
     * 创建嵌入表单独立大纲节点（直接展开其控件、Grid 列与单元格、数据源及宏，保证在 Ctrl + F12 中层级自动展开）
     */
    private fun createEmbeddedFormDetailTreeElement(
        embedTag: XmlTag,
        visitedForms: Set<String>,
        contextFile: PsiFile?,
        scopePath: String
    ): TreeElement {
        val formKey = embedTag.getAttributeValue("FormKey")
            ?: embedTag.getAttributeValue("Key")
            ?: ""
        val embedKey = embedTag.getAttributeValue("Key") ?: formKey.ifBlank { "Embed" }
        val ownCaption = embedTag.getAttributeValue("Caption")
        val parentCaption = embedTag.parentTag?.getAttributeValue("Caption")

        val initialTargetFormTag = if (formKey.isNotBlank()) {
            findFormTag(embedTag.project, formKey, contextFile ?: embedTag.containingFile)
        } else null

        val formCaption = initialTargetFormTag?.getAttributeValue("Caption")

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
            scopeKey = scopePath,
            isLeafNode = formKey.isBlank(),
            childrenProvider = {
                if (formKey.isBlank()) {
                    emptyList()
                } else if (visitedForms.contains(formKey)) {
                    listOf(
                        YigoStructureTreeElement(
                            psiElement = embedTag,
                            label = "循环引用: $formKey",
                            icon = AllIcons.General.Warning,
                            scopeKey = "$scopePath/cycle:$formKey",
                            isLeafNode = true
                        )
                    )
                } else {
                    val targetFormTag = initialTargetFormTag?.takeIf { it.isValid }
                        ?: findFormTag(embedTag.project, formKey, contextFile ?: embedTag.containingFile)

                    if (targetFormTag != null) {
                        val subElements = mutableListOf<TreeElement>()
                        val nextVisited = visitedForms + formKey
                        val targetFile = targetFormTag.containingFile

                        // 1. 直接展开嵌入表单的界面控件（包含 Grid 及其内部的 GridColumn 和 GridCell，保持在自动展开深度内）
                        val subControls = mutableListOf<XmlTag>()
                        val extBodies = targetFormTag.findSubTags("Body") + targetFormTag.findSubTags("Component")
                        for (b in extBodies) {
                            collectNamedControls(b, subControls)
                        }
                        subControls.distinct().forEachIndexed { idx, subCtrl ->
                            subElements.add(
                                createControlTreeElement(
                                    ctrlTag = subCtrl,
                                    visitedForms = nextVisited,
                                    contextFile = targetFile,
                                    scopePath = "$scopePath/ctrl:$idx"
                                )
                            )
                        }

                        // 2. 展开嵌入表单的数据源与数据对象
                        val subDsGroup = buildDataSourcesGroup(targetFormTag, targetFile, "$scopePath/ds")
                        if (subDsGroup != null) {
                            subElements.add(subDsGroup)
                        }

                        // 3. 展开嵌入表单的操作集合（如有）
                        val subOps = buildOperationsGroup(targetFormTag, "$scopePath/ops")
                        if (subOps != null) {
                            subElements.add(subOps)
                        }

                        // 4. 展开嵌入表单的宏集合（如有）
                        val subMacros = buildMacrosGroup(targetFormTag, "$scopePath/macros")
                        if (subMacros != null) {
                            subElements.add(subMacros)
                        }

                        if (subElements.isEmpty()) {
                            listOf(
                                YigoStructureTreeElement(
                                    psiElement = targetFormTag,
                                    label = "(外部表单内容为空)",
                                    icon = AllIcons.General.Information,
                                    scopeKey = "$scopePath/empty:$formKey",
                                    isLeafNode = true
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
                                icon = AllIcons.General.Warning,
                                scopeKey = "$scopePath/missing:$formKey",
                                isLeafNode = true
                            )
                        )
                    }
                }
            }
        )
    }

    private fun collectNamedControls(tag: XmlTag, result: MutableList<XmlTag>) {
        val tagName = tag.name
        if (tagName in NON_CONTROL_TAGS && tagName != "Body" && tagName != "Block") {
            return
        }
        val key = tag.getAttributeValue("Key")
        val caption = tag.getAttributeValue("Caption")

        val isContainerOnly = tagName == "Body" || tagName == "Block"

        if (!isContainerOnly && (!key.isNullOrBlank() || !caption.isNullOrBlank() || tagName == "Grid" || tagName == "Embed")) {
            result.add(tag)
        }

        if (tagName != "Grid" && tagName != "Embed") {
            for (sub in tag.subTags) {
                collectNamedControls(sub, result)
            }
        }
    }

    private fun collectGridColumns(gridTag: XmlTag): List<XmlTag> {
        val result = mutableListOf<XmlTag>()
        fun walk(tag: XmlTag) {
            for (sub in tag.subTags) {
                when (sub.name) {
                    "GridColumn" -> {
                        result.add(sub)
                        walk(sub)
                    }
                    "GridColumnCollection" -> walk(sub)
                }
            }
        }
        walk(gridTag)
        return result.distinct()
    }

    private fun collectGridCells(gridTag: XmlTag): List<XmlTag> {
        val result = mutableListOf<XmlTag>()
        fun walk(tag: XmlTag) {
            for (sub in tag.subTags) {
                when (sub.name) {
                    "GridCell" -> result.add(sub)
                    "GridRowCollection", "GridRow" -> walk(sub)
                }
            }
        }
        walk(gridTag)
        return result.distinct()
    }

    /**
     * 创建控件节点：
     * - 如果是 Embed，递归跨文件全量展开目标表单中的控件、Grid 列与单元格
     * - 如果是 Grid，全量展开 GridColumnCollection 下的 GridColumn 以及 GridRowCollection/GridRow 下的 GridCell
     */
    private fun createControlTreeElement(
        ctrlTag: XmlTag,
        visitedForms: Set<String>,
        contextFile: PsiFile?,
        scopePath: String
    ): TreeElement {
        val tagName = ctrlTag.name
        val key = ctrlTag.getAttributeValue("Key") ?: tagName
        val caption = ctrlTag.getAttributeValue("Caption")
        val dataBindingTag = ctrlTag.findFirstSubTag("DataBinding")
        val binding = ctrlTag.getAttributeValue("BindingCellKey")
            ?: ctrlTag.getAttributeValue("ColumnKey")
            ?: dataBindingTag?.getAttributeValue("ColumnKey")
            ?: ctrlTag.getAttributeValue("TableKey")
            ?: dataBindingTag?.getAttributeValue("TableKey")

        // 1. 处理 Embed 嵌入表单（跨文件全量展开）
        if (tagName == "Embed") {
            val formKey = ctrlTag.getAttributeValue("FormKey")
                ?: ctrlTag.getAttributeValue("Key")
                ?: ""
            val embedKey = ctrlTag.getAttributeValue("Key") ?: formKey.ifBlank { "Embed" }
            val ownCaption = ctrlTag.getAttributeValue("Caption")
            val parentCaption = ctrlTag.parentTag?.getAttributeValue("Caption")

            val initialTargetFormTag = if (formKey.isNotBlank()) {
                findFormTag(ctrlTag.project, formKey, contextFile ?: ctrlTag.containingFile)
            } else null

            val formCaption = initialTargetFormTag?.getAttributeValue("Caption")

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

            val embedChildScopeBase = if (scopePath.isEmpty()) "ui-embed:${ctrlTag.textOffset}:$formKey" else "$scopePath/ui-embed:$formKey"

            return YigoStructureTreeElement(
                psiElement = ctrlTag,
                label = title,
                location = if (formKey.isNotBlank()) "-> $formKey" else null,
                icon = AllIcons.Nodes.Related,
                scopeKey = scopePath,
                isLeafNode = formKey.isBlank(),
                childrenProvider = {
                    if (formKey.isBlank()) {
                        emptyList()
                    } else if (visitedForms.contains(formKey)) {
                        listOf(
                            YigoStructureTreeElement(
                                psiElement = ctrlTag,
                                label = "循环引用: $formKey",
                                icon = AllIcons.General.Warning,
                                scopeKey = "$embedChildScopeBase/cycle",
                                isLeafNode = true
                            )
                        )
                    } else {
                        val targetFormTag = initialTargetFormTag?.takeIf { it.isValid }
                            ?: findFormTag(ctrlTag.project, formKey, contextFile ?: ctrlTag.containingFile)

                        if (targetFormTag != null) {
                            // 跨文件展开目标子表单的所有控件（包含子表单中的 Grid -> GridColumn & GridCell）
                            val subControls = mutableListOf<XmlTag>()
                            val extBodies = targetFormTag.findSubTags("Body") + targetFormTag.findSubTags("Component")
                            for (b in extBodies) {
                                collectNamedControls(b, subControls)
                            }
                            subControls.distinct().mapIndexed { idx, subCtrl ->
                                createControlTreeElement(
                                    ctrlTag = subCtrl,
                                    visitedForms = visitedForms + formKey,
                                    contextFile = targetFormTag.containingFile,
                                    scopePath = "$embedChildScopeBase/ctrl:$idx"
                                )
                            }
                        } else {
                            listOf(
                                YigoStructureTreeElement(
                                    psiElement = ctrlTag,
                                    label = "未找到外部表单: $formKey",
                                    icon = AllIcons.General.Warning,
                                    scopeKey = "$embedChildScopeBase/missing",
                                    isLeafNode = true
                                )
                            )
                        }
                    }
                }
            )
        }

        // 2. 普通控件或 Grid 表格
        val title = if (caption.isNullOrBlank()) "$key: $tagName" else "$key: $tagName [$caption]"

        val icon = when (tagName) {
            "Grid" -> AllIcons.Nodes.DataTables
            "Button" -> AllIcons.Actions.Execute
            "DatePicker", "UTCDatePicker" -> AllIcons.Nodes.Aspect
            "ComboBox", "Dict" -> AllIcons.Nodes.Enum
            "TabPanel", "SplitPanel", "FlexFlowLayoutPanel", "GridLayoutPanel", "FlexGridLayoutPanel" -> AllIcons.Nodes.Folder
            else -> AllIcons.Nodes.Property
        }

        if (tagName == "Grid") {
            val gridCols = collectGridColumns(ctrlTag)
            val gridCells = collectGridCells(ctrlTag)
            val gridTableKey = binding
                ?: ctrlTag.findFirstSubTag("GridRowCollection")?.findFirstSubTag("GridRow")?.getAttributeValue("TableKey")
                ?: ctrlTag.findFirstSubTag("GridRow")?.getAttributeValue("TableKey")

            val gridLocation = buildList {
                if (!gridTableKey.isNullOrBlank()) add("-> $gridTableKey")
                if (gridCols.isNotEmpty() || gridCells.isNotEmpty()) {
                    add("${gridCols.size} 列, ${gridCells.size} 单元格")
                }
            }.joinToString(", ").ifBlank { null }

            return YigoStructureTreeElement(
                psiElement = ctrlTag,
                label = title,
                location = gridLocation,
                icon = icon,
                scopeKey = scopePath,
                isLeafNode = gridCols.isEmpty() && gridCells.isEmpty(),
                childrenProvider = {
                    val children = mutableListOf<TreeElement>()
                    val currentCols = collectGridColumns(ctrlTag)
                    val currentCells = collectGridCells(ctrlTag)

                    // 1. 展开所有 GridColumn（来自 GridColumnCollection 或直接子标签）
                    currentCols.forEachIndexed { idx, colTag ->
                        val colKey = colTag.getAttributeValue("Key") ?: "Col"
                        val matchingCell = currentCells.firstOrNull { it.getAttributeValue("Key") == colKey }
                        val colCap = colTag.getAttributeValue("Caption")?.takeIf { it.isNotBlank() }
                            ?: matchingCell?.getAttributeValue("Caption")
                        val colBind = colTag.getAttributeValue("ColumnKey")
                            ?: colTag.findFirstSubTag("DataBinding")?.getAttributeValue("ColumnKey")
                            ?: matchingCell?.let {
                                it.getAttributeValue("ColumnKey")
                                    ?: it.findFirstSubTag("DataBinding")?.getAttributeValue("ColumnKey")
                            }
                        val colTitle = if (colCap.isNullOrBlank()) "GridColumn: $colKey" else "GridColumn: $colKey [$colCap]"
                        val colLoc = if (!colBind.isNullOrBlank()) "-> $colBind" else null
                        val colScope = if (scopePath.isEmpty()) "" else "$scopePath/grid-col:$idx:$colKey"

                        children.add(
                            YigoStructureTreeElement(
                                psiElement = colTag,
                                label = colTitle,
                                location = colLoc,
                                icon = AllIcons.Nodes.Field,
                                scopeKey = colScope,
                                isLeafNode = true
                            )
                        )
                    }

                    // 2. 展开所有 GridCell（来自 GridRowCollection -> GridRow -> GridCell）
                    currentCells.forEachIndexed { idx, cellTag ->
                        val cellKey = cellTag.getAttributeValue("Key") ?: "Cell"
                        val matchingCol = currentCols.firstOrNull { it.getAttributeValue("Key") == cellKey }
                        val cellCap = cellTag.getAttributeValue("Caption")?.takeIf { it.isNotBlank() }
                            ?: matchingCol?.getAttributeValue("Caption")
                        val cellType = cellTag.getAttributeValue("CellType")
                        val cellBind = cellTag.getAttributeValue("ColumnKey")
                            ?: cellTag.findFirstSubTag("DataBinding")?.getAttributeValue("ColumnKey")
                            ?: cellTag.getAttributeValue("BindingCellKey")
                        val cellTitle = if (cellCap.isNullOrBlank()) "GridCell: $cellKey" else "GridCell: $cellKey [$cellCap]"
                        val cellLoc = buildList {
                            if (!cellType.isNullOrBlank()) add(cellType)
                            if (!cellBind.isNullOrBlank()) add("-> $cellBind")
                        }.joinToString(" ").ifBlank { null }
                        val cellScope = if (scopePath.isEmpty()) "" else "$scopePath/grid-cell:$idx:$cellKey"

                        children.add(
                            YigoStructureTreeElement(
                                psiElement = cellTag,
                                label = cellTitle,
                                location = cellLoc,
                                icon = AllIcons.Nodes.Property,
                                scopeKey = cellScope,
                                isLeafNode = true
                            )
                        )
                    }

                    children
                }
            )
        }

        val location = if (!binding.isNullOrBlank()) "-> $binding" else null

        return YigoStructureTreeElement(
            psiElement = ctrlTag,
            label = title,
            location = location,
            icon = icon,
            scopeKey = scopePath,
            isLeafNode = true
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
    private fun buildMacrosGroup(rootTag: XmlTag, scopePath: String): TreeElement? {
        val macros = mutableListOf<XmlTag>()
        for (col in rootTag.findSubTags("MacroCollection")) {
            macros.addAll(col.findSubTags("Macro"))
        }
        macros.addAll(rootTag.findSubTags("Macro"))

        if (macros.isEmpty()) return null

        val anchorTag = rootTag.findSubTags("MacroCollection").firstOrNull() ?: rootTag
        val groupScope = "$scopePath/MacrosGroup"

        return YigoStructureTreeElement(
            psiElement = anchorTag,
            label = "宏与业务逻辑 (Macros)",
            location = "${macros.size} 个宏",
            icon = AllIcons.Nodes.Function,
            scopeKey = groupScope,
            isLeafNode = false,
            childrenProvider = {
                macros.mapIndexed { idx, macroTag ->
                    val key = macroTag.getAttributeValue("Key") ?: "Macro"
                    val caption = macroTag.getAttributeValue("Caption")
                    val title = if (caption.isNullOrBlank()) "Macro: $key()" else "Macro: $key() [$caption]"

                    YigoStructureTreeElement(
                        psiElement = macroTag,
                        label = title,
                        icon = AllIcons.Nodes.Function,
                        scopeKey = "$groupScope/$idx:$key",
                        isLeafNode = true
                    )
                }
            }
        )
    }

    /**
     * 构建映射/数据关系分组（针对 Map / Relation 等文件）
     */
    private fun buildOtherGroup(rootTag: XmlTag, scopePath: String): TreeElement? {
        if (rootTag.name != "Map") return null

        val tables = rootTag.findSubTags("Table") + rootTag.findSubTags("SourceTable") + rootTag.findSubTags("TargetTable")
        if (tables.isEmpty()) return null

        val groupScope = "$scopePath/MappingsGroup"

        return YigoStructureTreeElement(
            psiElement = rootTag,
            label = "映射关系 (Mappings)",
            icon = AllIcons.Nodes.Related,
            scopeKey = groupScope,
            isLeafNode = false,
            childrenProvider = {
                tables.mapIndexed { tIdx, tTag ->
                    val key = tTag.getAttributeValue("Key") ?: tTag.name
                    val fields = tTag.findSubTags("Field") + tTag.findSubTags("SourceField") + tTag.findSubTags("TargetField")
                    val tableScope = "$groupScope/$tIdx:$key"

                    YigoStructureTreeElement(
                        psiElement = tTag,
                        label = key,
                        location = "${fields.size} 项",
                        icon = AllIcons.Nodes.DataTables,
                        scopeKey = tableScope,
                        isLeafNode = fields.isEmpty(),
                        childrenProvider = {
                            fields.mapIndexed { fIdx, fTag ->
                                val fKey = fTag.getAttributeValue("TargetFieldKey")
                                    ?: fTag.getAttributeValue("Key")
                                    ?: fTag.name
                                val def = fTag.getAttributeValue("Definition")

                                YigoStructureTreeElement(
                                    psiElement = fTag,
                                    label = fKey,
                                    location = def?.ifBlank { null },
                                    icon = AllIcons.Nodes.Field,
                                    scopeKey = "$tableScope/$fIdx:$fKey",
                                    isLeafNode = true
                                )
                            }
                        }
                    )
                }
            }
        )
    }
}

