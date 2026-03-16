package com.github.believepxw.yigo.tool

import com.github.believepxw.yigo.util.YigoUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.xml.XmlTag
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import example.index.DataElementIndex
import example.index.DomainIndex
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class YigoControlBuilder(private val project: Project) {

    fun showAddControlDialog(parent: Component, gridTag: XmlTag, clickX: Int, clickY: Int) {
        val dialog = JDialog(SwingUtilities.getWindowAncestor(parent), "Add Control", Dialog.ModalityType.APPLICATION_MODAL)
        dialog.layout = BorderLayout()

        val panel = JPanel(GridBagLayout())
        panel.border = JBUI.Borders.empty(10)

        val tableKeyField = JTextField(20)
        val columnKeyField = SearchTextField()
        val columnList = JBList<String>()
        val listModel = DefaultListModel<String>()
        columnList.model = listModel

        var allColumns = listOf<Pair<String, String>>()

        val suggestedTableKey = ApplicationManager.getApplication().runReadAction<String?> {
            if (!gridTag.isValid) return@runReadAction null
            for (control in gridTag.subTags) {
                for (subTag in control.subTags) {
                    if (subTag.name == "DataBinding") {
                        val tableKey = subTag.getAttributeValue("TableKey")
                        if (tableKey != null) return@runReadAction tableKey
                    }
                }
            }
            null
        }
        if (suggestedTableKey != null) tableKeyField.text = suggestedTableKey

        val c = GridBagConstraints()
        c.fill = GridBagConstraints.HORIZONTAL
        c.insets = JBUI.insets(5)

        c.gridx = 0; c.gridy = 0; c.weightx = 0.0
        panel.add(JLabel("TableKey:"), c)
        c.gridx = 1; c.weightx = 1.0
        panel.add(tableKeyField, c)

        c.gridx = 0; c.gridy = 1; c.weightx = 0.0
        panel.add(JLabel("ColumnKey:"), c)
        c.gridx = 1; c.weightx = 1.0
        panel.add(columnKeyField, c)

        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.weighty = 1.0; c.fill = GridBagConstraints.BOTH
        val scrollPane = JBScrollPane(columnList)
        scrollPane.preferredSize = Dimension(400, 200)
        panel.add(scrollPane, c)

        val loadColumns = {
            val tableKey = tableKeyField.text.trim()
            if (tableKey.isNotEmpty()) {
                ApplicationManager.getApplication().runReadAction {
                    val table = YigoUtils.findTable(gridTag, tableKey)
                    if (table != null) {
                        allColumns = getAllColumn(table)
                        listModel.clear()
                        allColumns.forEach { (key, caption) ->
                            listModel.addElement("$key - $caption")
                        }
                    }
                }
            }
        }

        tableKeyField.addActionListener { loadColumns() }

        columnKeyField.addKeyboardListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                val filter = columnKeyField.text.lowercase()
                listModel.clear()
                allColumns.filter { it.first.lowercase().contains(filter) || it.second.lowercase().contains(filter) }
                    .forEach { (key, caption) -> listModel.addElement("$key - $caption") }
            }
        })

        columnList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 1) {
                    val selected = columnList.selectedValue
                    if (selected != null) {
                        columnKeyField.text = selected.substringBefore(" - ")
                    }
                }
            }
        })

        dialog.add(panel, BorderLayout.CENTER)

        val btnPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        val okBtn = JButton("OK")
        okBtn.addActionListener {
            val tableKey = tableKeyField.text.trim()
            val columnKey = columnKeyField.text.trim()
            if (tableKey.isNotEmpty() && columnKey.isNotEmpty()) {
                dialog.dispose()
                createControlFromDomain(gridTag, tableKey, columnKey, clickX, clickY)
            }
        }
        val cancelBtn = JButton("Cancel")
        cancelBtn.addActionListener { dialog.dispose() }
        btnPanel.add(okBtn)
        btnPanel.add(cancelBtn)
        dialog.add(btnPanel, BorderLayout.SOUTH)

        dialog.pack()
        dialog.setLocationRelativeTo(parent)
        loadColumns()
        dialog.isVisible = true
    }

    val ignoreList = listOf("VERID", "DVERID", "TLeft", "TRight", "Enable", "ClientID", "Creator", "CreateTime","Modifier","ModifyTime")

    fun getAllColumn(table: XmlTag) : List<Pair<String, String>>{
        val codeColumnKey = mutableListOf<String>()
        table.findSubTags("Column")?.mapNotNull {
            if(it.getAttributeValue("CodeColumnKey")?.isNotEmpty() == true){
                codeColumnKey.add(it.getAttributeValue("CodeColumnKey").orEmpty())
            }
        } ?: emptyList()
        var allColumns: List<Pair<String, String>>
        allColumns = table.findSubTags("Column")?.mapNotNull {
            val key = it.getAttributeValue("Key")
            if (ignoreList.contains(key)) {
                return@mapNotNull null
            }
            if (codeColumnKey.contains(key)) {
                return@mapNotNull null
            }
            if(it.getAttributeValue("CodeColumnKey")?.isNotEmpty() == true){
                codeColumnKey.add(it.getAttributeValue("CodeColumnKey").orEmpty())
            }
            val caption = it.getAttributeValue("Caption") ?: ""
            if (key != null) key to caption else null
        } ?: emptyList()
        return allColumns
    }

    fun showAddGridColumnDialog(parent: Component, gridTag: XmlTag) {
        val dialog = JDialog(SwingUtilities.getWindowAncestor(parent), "Add Grid Column", Dialog.ModalityType.APPLICATION_MODAL)
        dialog.layout = BorderLayout()

        val panel = JPanel(GridBagLayout())
        panel.border = JBUI.Borders.empty(10)

        val tableKeyField = JTextField(20)
        val columnKeyField = SearchTextField()
        val columnList = JBList<String>()
        val listModel = DefaultListModel<String>()
        columnList.model = listModel

        var allColumns = listOf<Pair<String, String>>()

        val suggestedTableKey = ApplicationManager.getApplication().runReadAction<String?> {
            if (!gridTag.isValid) return@runReadAction null
            val rowCollection = gridTag.findFirstSubTag("GridRowCollection")
            rowCollection?.findSubTags("GridRow")?.firstOrNull()?.getAttributeValue("TableKey")
        }
        if (suggestedTableKey != null) tableKeyField.text = suggestedTableKey

        val c = GridBagConstraints()
        c.fill = GridBagConstraints.HORIZONTAL
        c.insets = JBUI.insets(5)

        c.gridx = 0; c.gridy = 0; c.weightx = 0.0
        panel.add(JLabel("TableKey:"), c)
        c.gridx = 1; c.weightx = 1.0
        panel.add(tableKeyField, c)

        c.gridx = 0; c.gridy = 1; c.weightx = 0.0
        panel.add(JLabel("ColumnKey:"), c)
        c.gridx = 1; c.weightx = 1.0
        panel.add(columnKeyField, c)

        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.weighty = 1.0; c.fill = GridBagConstraints.BOTH
        val scrollPane = JBScrollPane(columnList)
        scrollPane.preferredSize = Dimension(400, 200)
        panel.add(scrollPane, c)

        val loadColumns = {
            val tableKey = tableKeyField.text.trim()
            if (tableKey.isNotEmpty()) {
                ApplicationManager.getApplication().runReadAction {
                    val table = YigoUtils.findTable(gridTag, tableKey)
                    if (table != null) {
                        allColumns = getAllColumn(table)
                        listModel.clear()
                        allColumns.forEach { (key, caption) ->
                            listModel.addElement("$key - $caption")
                        }
                    }
                }
            }
        }

        tableKeyField.addActionListener { loadColumns() }

        columnKeyField.addKeyboardListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                val filter = columnKeyField.text.lowercase()
                listModel.clear()
                allColumns.filter { it.first.lowercase().contains(filter) || it.second.lowercase().contains(filter) }
                    .forEach { (key, caption) -> listModel.addElement("$key - $caption") }
            }
        })

        columnList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 1) {
                    val selected = columnList.selectedValue
                    if (selected != null) {
                        columnKeyField.text = selected.substringBefore(" - ")
                    }
                }
            }
        })

        dialog.add(panel, BorderLayout.CENTER)

        val btnPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        val okBtn = JButton("OK")
        okBtn.addActionListener {
            val tableKey = tableKeyField.text.trim()
            val columnKey = columnKeyField.text.trim()
            if (tableKey.isNotEmpty() && columnKey.isNotEmpty()) {
                dialog.dispose()
                createGridColumnFromDomain(gridTag, tableKey, columnKey)
            }
        }
        val cancelBtn = JButton("Cancel")
        cancelBtn.addActionListener { dialog.dispose() }
        btnPanel.add(okBtn)
        btnPanel.add(cancelBtn)
        dialog.add(btnPanel, BorderLayout.SOUTH)

        dialog.pack()
        dialog.setLocationRelativeTo(parent)
        loadColumns()
        dialog.isVisible = true
    }


    private fun createControlFromDomain(gridTag: XmlTag, tableKey: String, columnKey: String, clickX: Int, clickY: Int) {
        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project) {
                val column = YigoUtils.findColumnInTable(gridTag, tableKey, columnKey) ?: return@runWriteCommandAction
                val deKey = column.getAttributeValue("DataElementKey") ?: return@runWriteCommandAction
                val deTag = DataElementIndex.findDEDefinition(project, deKey) ?: return@runWriteCommandAction
                val domainKey = deTag.getAttributeValue("DomainKey") ?: return@runWriteCommandAction
                val domainTag = DomainIndex.findDomainDefinition(project, domainKey) ?: return@runWriteCommandAction

                val controlType = domainTag.getAttributeValue("RefControlType") ?: "TextEditor"
                val caption = deTag.getAttributeValue("Caption") ?: columnKey

                val layout = (gridTag as? JPanel)?.layout as? GridBagLayout
                val targetX = layout?.let {
                    val dims = it.getLayoutDimensions()
                    val origin = it.getLayoutOrigin()
                    var x = 0
                    var accum = origin.x
                    for (i in dims[0].indices) {
                        if (clickX >= accum && clickX < accum + dims[0][i]) {
                            x = i
                            break
                        }
                        accum += dims[0][i]
                    }
                    x
                } ?: 0

                val targetY = layout?.let {
                    val dims = it.getLayoutDimensions()
                    val origin = it.getLayoutOrigin()
                    var y = 0
                    var accum = origin.y
                    for (i in dims[1].indices) {
                        if (clickY >= accum && clickY < accum + dims[1][i]) {
                            y = i
                            break
                        }
                        accum += dims[1][i]
                    }
                    y
                } ?: 0

                val newControl = gridTag.createChildTag(controlType, null, null, false)
                newControl.setAttribute("Key", columnKey)
                newControl.setAttribute("Caption", caption)
                newControl.setAttribute("X", targetX.toString())
                newControl.setAttribute("Y", targetY.toString())

                applyDomainAttributes(newControl, domainTag, controlType)

                val dataBinding = newControl.createChildTag("DataBinding", null, null, false)
                dataBinding.setAttribute("TableKey", tableKey)
                dataBinding.setAttribute("ColumnKey", columnKey)
                newControl.addSubTag(dataBinding, false)

                gridTag.addSubTag(newControl, false)
            }
        }
    }

    private fun createGridColumnFromDomain(gridTag: XmlTag, tableKey: String, columnKey: String) {
        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project) {
                val deKey = "${tableKey}_$columnKey"
                val deTag = DataElementIndex.findDEDefinition(project, deKey) ?: return@runWriteCommandAction
                val domainKey = deTag.getAttributeValue("DomainKey") ?: return@runWriteCommandAction
                val domainTag = DomainIndex.findDomainDefinition(project, domainKey) ?: return@runWriteCommandAction

                val controlType = domainTag.getAttributeValue("RefControlType") ?: "TextEditor"
                val caption = deTag.getAttributeValue("Caption") ?: columnKey

                val colCollection = gridTag.findFirstSubTag("GridColumnCollection") ?: gridTag.createChildTag("GridColumnCollection", null, null, false).also { gridTag.addSubTag(it, false) }
                val newColumn = colCollection.createChildTag("GridColumn", null, null, false)
                newColumn.setAttribute("Key", columnKey)
                newColumn.setAttribute("Caption", caption)
                colCollection.addSubTag(newColumn, false)

                val rowCollection = gridTag.findFirstSubTag("GridRowCollection")
                rowCollection?.findSubTags("GridRow")?.forEach { row ->
                    val newCell = row.createChildTag("GridCell", null, null, false)
                    newCell.setAttribute("Key", columnKey)
                    newCell.setAttribute("CellType", controlType)

                    val cellControl = newCell.createChildTag(controlType, null, null, false)
                    cellControl.setAttribute("Key", columnKey)
                    applyDomainAttributes(cellControl, domainTag, controlType)

                    val dataBinding = cellControl.createChildTag("DataBinding", null, null, false)
                    dataBinding.setAttribute("TableKey", tableKey)
                    dataBinding.setAttribute("ColumnKey", columnKey)
                    cellControl.addSubTag(dataBinding, false)

                    newCell.addSubTag(cellControl, false)
                    row.addSubTag(newCell, false)
                }
            }
        }
    }

    private fun applyDomainAttributes(controlTag: XmlTag, domainTag: XmlTag, controlType: String) {
        val attrList = listOf("DataType", "Caption", "RefControlType")
        for (attribute in domainTag.attributes) {
            var name1 = attribute.name
            if (controlType == "TextEditor" && name1 == "Length") {
                val length = domainTag.getAttributeValue("Length")
                controlTag.setAttribute("MaxLength", length)
            } else if (attrList.contains(name1)) {
                continue
            } else {
                val value = domainTag.getAttributeValue(name1)
                if (value != null) controlTag.setAttribute(name1, value)
            }
        }
    }
}