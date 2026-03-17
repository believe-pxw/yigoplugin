package com.github.believepxw.yigo.tool

import com.github.believepxw.yigo.util.YigoUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.psi.xml.XmlTag
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SearchTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import example.index.DataElementIndex
import example.index.DomainIndex
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.table.AbstractTableModel

class YigoControlBuilder(private val project: Project) {

    data class ColumnSelection(val key: String, val caption: String, var customKey: String, var selected: Boolean = false, var isDuplicate: Boolean = false)

    private class ColumnTableModel(var data: List<ColumnSelection>) : AbstractTableModel() {
        private val columnNames = arrayOf("Selected", "Key", "Caption", "Custom Key")

        override fun getRowCount(): Int = data.size
        override fun getColumnCount(): Int = columnNames.size
        override fun getColumnName(column: Int): String = columnNames[column]
        override fun getColumnClass(columnIndex: Int): Class<*> {
            return when (columnIndex) {
                0 -> java.lang.Boolean::class.java
                else -> String::class.java
            }
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return columnIndex == 0 || columnIndex == 3
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
            val item = data[rowIndex]
            return when (columnIndex) {
                0 -> item.selected
                1 -> item.key
                2 -> item.caption
                3 -> item.customKey
                else -> null
            }
        }

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            val item = data[rowIndex]
            when (columnIndex) {
                0 -> item.selected = aValue as? Boolean ?: false
                3 -> item.customKey = aValue as? String ?: ""
            }
            fireTableCellUpdated(rowIndex, columnIndex)
        }
    }

    fun showAddControlDialog(
        parent: Component,
        gridTag: XmlTag,
        clickX: Int,
        clickY: Int,
        containerComponent: JComponent? = null
    ) {
        showCommonAddDialog(parent, gridTag, "Add Control") { tableKey, selectedColumns ->
            selectedColumns.forEachIndexed { index, col ->
                // Offset Y slightly if adding multiple at same position?
                // Or just stack them. Yigo XML supports overlapping.
                createControlFromDomain(gridTag, tableKey, col.key, col.customKey, clickX, clickY, containerComponent)
            }
        }
    }

    fun showAddGridColumnDialog(parent: Component, gridTag: XmlTag, afterColumnKey: String? = null) {
        showCommonAddDialog(parent, gridTag, "Add Grid Column") { tableKey, selectedColumns ->
            var currentAfter = afterColumnKey
            selectedColumns.forEach { col ->
                createGridColumnFromDomain(gridTag, tableKey, col.key, col.customKey, currentAfter)
                currentAfter = col.customKey
            }
        }
    }

    val ignoreList = setOf(
        "OID"
        ,"SOID"
        ,"POID"
        ,"VERID"
        ,"DVERID"
        ,"Status"
        ,"InstanceID"
        ,"ClusterID"
        ,"BillDate"
        ,"CreateTime"
        ,"ModifyTime"
        ,"TransactTime"
        ,"Creator"
        ,"Modifier"
        ,"Checker"
        ,"CheckerTime"
        ,"NO"
        ,"MapKey"
        ,"SrcOID"
        ,"SrcSOID"
        ,"MapCount"
        ,"Layer"
        ,"Hidden"
        ,"Slock"
        ,"Sequence"
        ,"Code"
        ,"Name"
        ,"ParentID"
        ,"TLeft"
        ,"TRight"
        ,"Enable"
        ,"NodeType"
        ,"HVER"
        ,"HVERM"
        ,"SVERID"
        ,"Submitter"
        ,"Name"
        ,"UploadOperator"
        ,"Path"
        ,"UploadTime"
        ,"LastModified"
        ,"CreateDate",
        "SystemVestKey","SequenceValue","ResetPattern","ExternalSystemID","ExternalSystemPrimaryKey"
    )

    private val variableDefinitionTagNames: Set<String> = setOf(
        "Dict", "DynamicDict", "TextEditor", "TextArea", "CheckBox", "ComboBox",
        "CheckListBox", "DatePicker", "UTCDatePicker", "MonthPicker", "TimePicker",
        "Button", "NumberEditor", "Label", "TextButton", "RadioButton", "PasswordEditor",
        "Image", "WebBrowser", "RichEditor", "HyperLink", "Separator", "DropdownButton",
        "Icon", "Custom", "BPMGraph", "Dynamic", "Carousel", "EditView", "Gantt",
        "Variable", "VarDef", "GridCell"
    )

    private fun getUsedColumns(rootTag: XmlTag): Set<Pair<String, String>> {
        val used = mutableSetOf<Pair<String, String>>()
        fun scan(tag: XmlTag) {
            if (tag.name in variableDefinitionTagNames) {
                if (tag.name == "GridCell") {
                    val columnKey = tag.findFirstSubTag("DataBinding")?.getAttributeValue("ColumnKey") ?: tag.getAttributeValue("Key")
                    var gridRow = tag.parentTag
                    while (gridRow != null && gridRow.name != "GridRow") {
                        gridRow = gridRow.parentTag
                    }
                    val tableKey = gridRow?.getAttributeValue("TableKey")
                    if (columnKey != null && tableKey != null) {
                        used.add(tableKey to columnKey)
                    }
                } else {
                    val dataBinding = tag.findFirstSubTag("DataBinding")
                    val tableKey = dataBinding?.getAttributeValue("TableKey")
                    val columnKey = dataBinding?.getAttributeValue("ColumnKey")
                    if (tableKey != null && columnKey != null) {
                        used.add(tableKey to columnKey)
                    }
                }
            }
            for (subTag in tag.subTags) {
                scan(subTag)
            }
        }
        scan(rootTag)
        return used
    }

    private fun getExistingKeys(rootTag: XmlTag): Set<String> {
        val keys = mutableSetOf<String>()
        fun scan(tag: XmlTag) {
            if (tag.name in variableDefinitionTagNames) {
                val key = tag.getAttributeValue("Key")
                if (key != null) keys.add(key)
            }
            for (subTag in tag.subTags) scan(subTag)
        }
        scan(rootTag)
        return keys
    }

    fun getAllColumn(table: XmlTag, tableKey: String, usedColumns: Set<Pair<String, String>>): List<Pair<String, String>> {
        val codeColumnKey = mutableListOf<String>()
        table.findSubTags("Column")?.mapNotNull {
            if (it.getAttributeValue("CodeColumnKey")?.isNotEmpty() == true) {
                codeColumnKey.add(it.getAttributeValue("CodeColumnKey").orEmpty())
            }
        } ?: emptyList()
        var allColumns: List<Pair<String, String>>
        allColumns = table.findSubTags("Column")?.mapNotNull {
            val key = it.getAttributeValue("Key")
            if (key == null || ignoreList.contains(key) || codeColumnKey.contains(key)) {
                return@mapNotNull null
            }

            // Filter out columns already used in the form
            if (usedColumns.contains(tableKey to key)) {
                return@mapNotNull null
            }

            if (it.getAttributeValue("CodeColumnKey")?.isNotEmpty() == true) {
                codeColumnKey.add(it.getAttributeValue("CodeColumnKey").orEmpty())
            }
            val caption = it.getAttributeValue("Caption") ?: ""
            key to caption
        } ?: emptyList()
        return allColumns
    }

    private fun showCommonAddDialog(parent: Component, gridTag: XmlTag, title: String, onOk: (String, List<ColumnSelection>) -> Unit) {
        val dialog = JDialog(SwingUtilities.getWindowAncestor(parent), title, Dialog.ModalityType.APPLICATION_MODAL)
        dialog.layout = BorderLayout()

        val panel = JPanel(GridBagLayout())
        panel.border = JBUI.Borders.empty(10)

        val tableKeyCombo = ComboBox<String>()
        val columnKeyField = SearchTextField()
        val errorLabel = JLabel(" ").apply { foreground = Color.RED }
        
        val tableModel = ColumnTableModel(emptyList())
        val columnTable = JBTable(tableModel)
        columnTable.selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        
        // Custom renderer for highlighting duplicates
        columnTable.setDefaultRenderer(java.lang.Object::class.java, object : javax.swing.table.DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
                val c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                val item = (table.model as ColumnTableModel).data[row]
                if (column == 3 && item.isDuplicate) {
                    c.background = if (isSelected) Color(255, 100, 100) else Color(255, 200, 200)
                    c.foreground = Color.BLACK
                } else {
                    c.background = if (isSelected) table.selectionBackground else table.background
                    c.foreground = if (isSelected) table.selectionForeground else table.foreground
                }
                return c
            }
        })

        // Adjust column widths
        columnTable.columnModel.getColumn(0).preferredWidth = 60
        columnTable.columnModel.getColumn(0).maxWidth = 60
        columnTable.columnModel.getColumn(1).preferredWidth = 120
        columnTable.columnModel.getColumn(2).preferredWidth = 120
        columnTable.columnModel.getColumn(3).preferredWidth = 120

        var allColumns = listOf<ColumnSelection>()
        var showAll = false

        val rootTag = ApplicationManager.getApplication().runReadAction<XmlTag?> {
            YigoUtils.getRootFormTag(gridTag)
        }
        val usedColumns = rootTag?.let { getUsedColumns(it) } ?: emptySet()
        val existingKeys = rootTag?.let { getExistingKeys(it) } ?: emptySet()

        val tables = ApplicationManager.getApplication().runReadAction<List<String>> {
            YigoUtils.getTables(gridTag)?.mapNotNull { it.getAttributeValue("Key") } ?: emptyList()
        }
        tables.forEach { tableKeyCombo.addItem(it) }

        val suggestedTableKey = ApplicationManager.getApplication().runReadAction<String?> {
            if (!gridTag.isValid) return@runReadAction null
            if (gridTag.name == "Grid") {
                tableKeyCombo.isEnabled = false // Disable for Grid
                val rowCollection = gridTag.findFirstSubTag("GridRowCollection")
                rowCollection?.findSubTags("GridRow")?.firstOrNull()?.getAttributeValue("TableKey")
            } else {
                tableKeyCombo.isEnabled = true // Enable for others (like GridLayoutPanel)
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
        }
        if (suggestedTableKey != null) tableKeyCombo.selectedItem = suggestedTableKey

        val c = GridBagConstraints()
        c.fill = GridBagConstraints.HORIZONTAL
        c.insets = JBUI.insets(5)

        c.gridx = 0; c.gridy = 0; c.weightx = 0.0
        panel.add(JLabel("TableKey:"), c)
        c.gridx = 1; c.weightx = 1.0
        panel.add(tableKeyCombo, c)

        c.gridx = 0; c.gridy = 1; c.weightx = 0.0
        panel.add(JLabel("Search:"), c)
        c.gridx = 1; c.weightx = 1.0
        panel.add(columnKeyField, c)

        val toggleBtn = JCheckBox("Show All Columns", showAll)
        c.gridx = 1; c.gridy = 2; c.weightx = 1.0
        panel.add(toggleBtn, c)

        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; c.weighty = 1.0; c.fill = GridBagConstraints.BOTH
        val scrollPane = ScrollPaneFactory.createScrollPane(columnTable)
        scrollPane.preferredSize = Dimension(500, 300)
        panel.add(scrollPane, c)

        c.gridx = 0; c.gridy = 4; c.gridwidth = 2; c.weighty = 0.0; c.fill = GridBagConstraints.HORIZONTAL
        panel.add(errorLabel, c)

        val updateList = {
            val filter = columnKeyField.text.lowercase()
            val currentTableKey = tableKeyCombo.selectedItem as? String ?: ""
            
            // Check duplicates in real-time
            val localSelections = allColumns.filter { it.selected }.map { it.customKey }.toSet()
            allColumns.forEach { col ->
                col.isDuplicate = existingKeys.contains(col.customKey)
            }

            val filtered = allColumns.filter { col ->
                val matchesSearch = col.key.lowercase().contains(filter) || col.caption.lowercase().contains(filter)
                val isVisible = showAll || !usedColumns.contains(currentTableKey to col.key)
                matchesSearch && isVisible
            }
            
            val selectedWithDuplicates = allColumns.filter { it.selected && it.isDuplicate }
            if (selectedWithDuplicates.isNotEmpty()) {
                errorLabel.text = "Duplicate Keys: " + selectedWithDuplicates.joinToString { it.customKey }
            } else {
                errorLabel.text = " "
            }

            tableModel.data = filtered
            tableModel.fireTableDataChanged()
        }

        val loadColumns = {
            val tableKey = tableKeyCombo.selectedItem as? String ?: ""
            if (tableKey.isNotEmpty()) {
                ApplicationManager.getApplication().runReadAction {
                    val table = YigoUtils.findTable(gridTag, tableKey)
                    if (table != null) {
                        allColumns = getAllColumn(table, tableKey, emptySet()).map { 
                            ColumnSelection(it.first, it.second, it.first) 
                        }
                        updateList()
                    }
                }
            }
        }

        tableModel.addTableModelListener { e ->
            if (e.column == 0 || e.column == 3) {
                updateList()
            }
        }

        toggleBtn.addActionListener {
            showAll = toggleBtn.isSelected
            updateList()
        }

        tableKeyCombo.addActionListener { loadColumns() }

        columnKeyField.addKeyboardListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                updateList()
            }
        })

        dialog.add(panel, BorderLayout.CENTER)

        val btnPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        val okBtn = JButton("OK")
        okBtn.addActionListener {
            val tableKey = tableKeyCombo.selectedItem as? String ?: ""
            val selected = allColumns.filter { it.selected }
            val selectedWithDuplicates = selected.filter { it.isDuplicate }
            
            if (selectedWithDuplicates.isNotEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Cannot proceed: Some selected keys already exist in the form.")
                return@addActionListener
            }

            if (tableKey.isNotEmpty() && selected.isNotEmpty()) {
                dialog.dispose()
                onOk(tableKey, selected)
            } else if (selected.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please select at least one column")
            }
        }
        val cancelBtn = JButton("Cancel")
        cancelBtn.addActionListener { dialog.dispose() }
        btnPanel.add(okBtn)
        btnPanel.add(cancelBtn)
        dialog.add(btnPanel, BorderLayout.SOUTH)

        // Close on ESC
        val escapeStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
        val dispatchAction = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                dialog.dispose()
            }
        }
        dialog.rootPane.registerKeyboardAction(dispatchAction, escapeStroke, JComponent.WHEN_IN_FOCUSED_WINDOW)

        dialog.pack()
        dialog.setLocationRelativeTo(parent)
        loadColumns()

        SwingUtilities.invokeLater {
            columnKeyField.requestFocusInWindow()
        }

        dialog.isVisible = true
    }


    private fun createControlFromDomain(
        gridTag: XmlTag,
        tableKey: String,
        columnKey: String,
        customKey: String,
        clickX: Int,
        clickY: Int,
        containerComponent: JComponent?
    ) {
        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project) {
                val column = YigoUtils.findColumnInTable(gridTag, tableKey, columnKey) ?: return@runWriteCommandAction
                val deKey = column.getAttributeValue("DataElementKey") ?: return@runWriteCommandAction
                val deTag = DataElementIndex.findDEDefinition(project, deKey) ?: return@runWriteCommandAction
                val domainKey = deTag.getAttributeValue("DomainKey") ?: return@runWriteCommandAction
                val domainTag = DomainIndex.findDomainDefinition(project, domainKey) ?: return@runWriteCommandAction

                val controlType = domainTag.getAttributeValue("RefControlType") ?: "TextEditor"
                val caption = deTag.getAttributeValue("Caption") ?: columnKey

                val layout = containerComponent?.layout as? GridBagLayout
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
                newControl.setAttribute("Key", customKey)
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

    private fun createGridColumnFromDomain(
        gridTag: XmlTag,
        tableKey: String,
        columnKey: String,
        customKey: String,
        afterColumnKey: String? = null
    ) {
        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project) {
                val columnDef =
                    YigoUtils.findColumnInTable(gridTag, tableKey, columnKey) ?: return@runWriteCommandAction
                val deKey = columnDef.getAttributeValue("DataElementKey") ?: return@runWriteCommandAction
                val deTag = DataElementIndex.findDEDefinition(project, deKey) ?: return@runWriteCommandAction
                val domainKey = deTag.getAttributeValue("DomainKey") ?: return@runWriteCommandAction
                val domainTag = DomainIndex.findDomainDefinition(project, domainKey) ?: return@runWriteCommandAction

                val controlType = domainTag.getAttributeValue("RefControlType") ?: "TextEditor"
                val caption = deTag.getAttributeValue("Caption") ?: columnKey

                val colCollection = gridTag.findFirstSubTag("GridColumnCollection") ?: gridTag.createChildTag(
                    "GridColumnCollection",
                    null,
                    null,
                    false
                ).also { gridTag.addSubTag(it, false) }
                val newColumn = colCollection.createChildTag("GridColumn", null, null, false)
                newColumn.setAttribute("Key", customKey)
                newColumn.setAttribute("Caption", caption)

                if (afterColumnKey != null) {
                    val targetCol =
                        colCollection.findSubTags("GridColumn").find { it.getAttributeValue("Key") == afterColumnKey }
                    if (targetCol != null) {
                        colCollection.addAfter(newColumn, targetCol)
                    } else {
                        colCollection.addSubTag(newColumn, false)
                    }
                } else {
                    colCollection.addSubTag(newColumn, false)
                }

                val rowCollection = gridTag.findFirstSubTag("GridRowCollection")
                rowCollection?.findSubTags("GridRow")?.forEach { row ->
                    val newCell = row.createChildTag("GridCell", null, null, false)
                    newCell.setAttribute("Key", customKey)
                    newCell.setAttribute("Caption", caption)
                    newCell.setAttribute("CellType", controlType)

                    applyDomainAttributes(newCell, domainTag, controlType)
                    
                    val dataBinding = newCell.createChildTag("DataBinding", null, null, false)
                    dataBinding.setAttribute("ColumnKey", columnKey)
                    newCell.addSubTag(dataBinding, false)

                    if (afterColumnKey != null) {
                        val targetCell = row.findSubTags("GridCell").find { it.getAttributeValue("Key") == afterColumnKey }
                        if (targetCell != null) {
                            row.addAfter(newCell, targetCell)
                        } else {
                            row.addSubTag(newCell, false)
                        }
                    } else {
                        row.addSubTag(newCell, false)
                    }
                }
            }
        }
    }

    private fun applyDomainAttributes(controlTag: XmlTag, domainTag: XmlTag, controlType: String) {
        val attrList = listOf("Key", "DataType", "Caption", "RefControlType")
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
            if (name1 == "SourceType" && domainTag.getAttributeValue("SourceType") == "Items") {
                for (tag in domainTag.subTags) {
                    controlTag.addSubTag(tag.copy() as XmlTag, false)
                }
            }
        }
    }
}
