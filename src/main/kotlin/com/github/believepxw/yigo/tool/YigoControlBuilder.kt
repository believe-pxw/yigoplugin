package com.github.believepxw.yigo.tool

import com.github.believepxw.yigo.util.YigoUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SearchTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import example.index.DataElementIndex
import example.index.DomainIndex
import example.index.FormIndex
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.datatransfer.DataFlavor
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.table.AbstractTableModel

class YigoControlBuilder(private val project: Project) {

    data class ColumnSelection(val key: String, val caption: String, var customKey: String, var selected: Boolean = false, var isDuplicate: Boolean = false)
    data class DEColumnSelection(var deKey: String = "", var columnKey: String = "", var fieldKey: String = "", var deExists: Boolean = true, var columnDuplicate: Boolean = false, var fieldDuplicate: Boolean = false)

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

    private class DEColumnTableModel(var data: MutableList<DEColumnSelection>) : AbstractTableModel() {
        private val columnNames = arrayOf("DataElementKey", "ColumnKey", "FieldKey")

        override fun getRowCount(): Int = data.size
        override fun getColumnCount(): Int = columnNames.size
        override fun getColumnName(column: Int): String = columnNames[column]
        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = true

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
            val item = data[rowIndex]
            return when (columnIndex) {
                0 -> item.deKey
                1 -> item.columnKey
                2 -> item.fieldKey
                else -> null
            }
        }

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            val item = data[rowIndex]
            val value = (aValue as? String ?: "").trim()
            when (columnIndex) {
                0 -> {
                    item.deKey = value
                    if (item.columnKey.isEmpty()) item.columnKey = value
                    if (item.fieldKey.isEmpty()) item.fieldKey = value
                }
                1 -> item.columnKey = value
                2 -> item.fieldKey = value
            }
            fireTableRowsUpdated(rowIndex, rowIndex)
            // Add a new row if we just edited the last one
            if (rowIndex == data.size - 1 && item.deKey.isNotEmpty()) {
                data.add(DEColumnSelection())
                fireTableRowsInserted(data.size - 1, data.size - 1)
            }
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
            WriteCommandAction.runWriteCommandAction(project) {
                selectedColumns.forEachIndexed { index, col ->
                    createControlFromDomain(gridTag, tableKey, col.key, col.customKey, clickX, clickY, containerComponent)
                }
            }
        }
    }

    fun showAddGridColumnDialog(parent: Component, gridTag: XmlTag, afterColumnKey: String? = null) {
        showCommonAddDialog(parent, gridTag, "Add Grid Column") { tableKey, selectedColumns ->
            WriteCommandAction.runWriteCommandAction(project) {
                var currentAfter = afterColumnKey
                selectedColumns.forEach { col ->
                    createGridColumnFromDomain(gridTag, tableKey, col.key, col.customKey, currentAfter)
                    currentAfter = col.customKey
                }
            }
        }
    }

    fun showAddByDEDialog(
        parent: Component,
        gridTag: XmlTag,
        clickX: Int = 0,
        clickY: Int = 0,
        containerComponent: JComponent? = null,
        afterColumnKey: String? = null
    ) {
        val dialog = JDialog(SwingUtilities.getWindowAncestor(parent), "Add by DataElementKey", Dialog.ModalityType.APPLICATION_MODAL)
        dialog.layout = BorderLayout()
        dialog.setSize(600, 400)

        val panel = JPanel(GridBagLayout())
        panel.border = JBUI.Borders.empty(10)

        val tableKeyCombo = ComboBox<String>()
        val errorLabel = JLabel(" ").apply { foreground = Color.RED }

        val data = MutableList(1) { DEColumnSelection() }
        val tableModel = DEColumnTableModel(data)
        val table = JBTable(tableModel)
        table.preferredScrollableViewportSize = JBUI.size(600, 400)
        table.rowHeight = 25
        table.columnModel.getColumn(0).preferredWidth = 250
        table.columnModel.getColumn(1).preferredWidth = 200
        table.columnModel.getColumn(2).preferredWidth = 200

        var existingKeys = emptySet<String>()

        // Validation logic
        val validate = {
            val tableKey = tableKeyCombo.selectedItem as? String ?: ""
            ApplicationManager.getApplication().executeOnPooledThread {
                val results = ApplicationManager.getApplication().runReadAction<Set<String>> {
                    val root = YigoUtils.getRootFormTag(gridTag)
                    val keys = root?.let { getExistingKeys(it) } ?: emptySet()
                    val targetTable = YigoUtils.findTable(gridTag, tableKey)
                    val tableColumns = targetTable?.findSubTags("Column")?.mapNotNull { it.getAttributeValue("Key") }?.toSet() ?: emptySet()

                    data.filter { it.deKey.isNotEmpty() }.forEach { item ->
                        item.deExists = DataElementIndex.findDEDefinition(project, item.deKey) != null
                        item.columnDuplicate = tableColumns.contains(item.columnKey)
                        item.fieldDuplicate = keys.contains(item.fieldKey)
                    }
                    keys
                }
                SwingUtilities.invokeLater {
                    existingKeys = results
                    table.repaint()
                    val invalidItems = data.filter { it.deKey.isNotEmpty() }
                    val errorMsg = StringBuilder()
                    invalidItems.forEachIndexed { index, item ->
                        val rowNum = index + 1
                        if (!item.deExists) errorMsg.append("Row $rowNum: DE '${item.deKey}' not found. ")
                        if (item.columnDuplicate) errorMsg.append("Row $rowNum: Column '${item.columnKey}' exists in table. ")
                        if (item.fieldDuplicate) errorMsg.append("Row $rowNum: Field '${item.fieldKey}' exists in form. ")
                    }
                    
                    if (errorMsg.isNotEmpty()) {
                        errorLabel.text = errorMsg.toString()
                        errorLabel.toolTipText = errorMsg.toString()
                    } else {
                        errorLabel.text = " "
                    }
                }
            }
        }

        tableModel.addTableModelListener { validate() }
        tableKeyCombo.addActionListener { validate() }

        // Paste support
        table.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.isControlDown && e.keyCode == KeyEvent.VK_V) {
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    val content = clipboard.getContents(null)
                    if (content != null && content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        val text = content.getTransferData(DataFlavor.stringFlavor) as String
                        val lines = text.split("\n", "\r\n")
                        val startRow = table.selectedRow.coerceAtLeast(0)
                        val startCol = table.selectedColumn.coerceAtLeast(0)

                        lines.filter { it.isNotBlank() }.forEachIndexed { rIdx, line ->
                            val targetRow = startRow + rIdx
                            // Ensure data list is large enough
                            while (data.size <= targetRow) {
                                data.add(DEColumnSelection())
                            }
                            
                            val cells = line.split("\t")
                            cells.forEachIndexed { cIdx, cell ->
                                val targetCol = startCol + cIdx
                                if (targetCol < tableModel.columnCount) {
                                    tableModel.setValueAt(cell.trim(), targetRow, targetCol)
                                }
                            }
                        }
                        tableModel.fireTableDataChanged()
                        validate()
                    }
                }
            }
        })

        // Renderer for highlighting errors
        table.setDefaultRenderer(java.lang.Object::class.java, object : javax.swing.table.DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(t: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
                val c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column)
                val item = (t.model as DEColumnTableModel).data[row]
                if (item.deKey.isNotEmpty()) {
                    val hasError = when (column) {
                        0 -> !item.deExists
                        1 -> item.columnDuplicate
                        2 -> item.fieldDuplicate
                        else -> false
                    }
                    if (hasError) {
                        c.background = if (isSelected) Color(255, 100, 100) else Color(255, 200, 200)
                        c.foreground = Color.BLACK
                    } else {
                        c.background = if (isSelected) t.selectionBackground else t.background
                        c.foreground = if (isSelected) t.selectionForeground else t.foreground
                    }
                }
                return c
            }
        })

        // Initial table load
        ApplicationManager.getApplication().executeOnPooledThread {
            val initData = ApplicationManager.getApplication().runReadAction<Triple<List<String>, String?, Boolean>> {
                val availableTables = YigoUtils.getTables(gridTag)?.mapNotNull { it.getAttributeValue("Key") } ?: emptyList()
                val suggestedKey = getSuggestTableKey(gridTag)
                Triple(availableTables, suggestedKey, gridTag.name == "Grid")
            }
            SwingUtilities.invokeLater {
                val (tableList, suggested, isGrid) = initData
                tableList.forEach { tableKeyCombo.addItem(it) }
                if (isGrid) {
                    tableKeyCombo.isEnabled = false
                }
                if (suggested != null) {
                    tableKeyCombo.selectedItem = suggested
                }
                validate()
            }
        }

        val c = GridBagConstraints()
        c.fill = GridBagConstraints.HORIZONTAL; c.insets = JBUI.insets(5)
        c.gridx = 0; c.gridy = 0; c.weightx = 0.0; panel.add(JLabel("TableKey:"), c)
        c.gridx = 1; c.weightx = 1.0; panel.add(tableKeyCombo, c)
        c.gridx = 0; c.gridy = 1; c.gridwidth = 2; c.weighty = 1.0; c.fill = GridBagConstraints.BOTH
        panel.add(ScrollPaneFactory.createScrollPane(table), c)
        c.gridy = 2; c.weighty = 0.0; c.fill = GridBagConstraints.HORIZONTAL
        panel.add(errorLabel, c)

        val btnPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        val okBtn = JButton("OK")
        okBtn.addActionListener {
            val tableKey = tableKeyCombo.selectedItem as? String ?: ""
            val items = data.filter { it.deKey.isNotEmpty() }
            if (tableKey.isEmpty() || items.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please select a Table and input at least one DataElementKey.")
                return@addActionListener
            }
            if (items.any { !it.deExists || it.fieldDuplicate || it.columnDuplicate }) {
                JOptionPane.showMessageDialog(dialog, "Please fix errors before proceeding.")
                return@addActionListener
            }

            dialog.dispose()
            WriteCommandAction.runWriteCommandAction(project) {
                var currentAfter = afterColumnKey
                items.forEach { item ->
                    ensureColumnExists(gridTag, tableKey, item.columnKey, item.deKey)
                    if (gridTag.name == "Grid") {
                        createGridColumnFromDomain(gridTag, tableKey, item.columnKey, item.fieldKey, currentAfter)
                        currentAfter = item.fieldKey
                    } else {
                        createControlFromDomain(gridTag, tableKey, item.columnKey, item.fieldKey, clickX, clickY, containerComponent)
                    }
                }
            }
        }
        // Close on ESC
        val escapeStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
        val dispatchAction = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                dialog.dispose()
            }
        }
        dialog.rootPane.registerKeyboardAction(dispatchAction, escapeStroke, JComponent.WHEN_IN_FOCUSED_WINDOW)
        btnPanel.add(okBtn)
        val cancelBtn = JButton("Cancel"); cancelBtn.addActionListener { dialog.dispose() }; btnPanel.add(cancelBtn)
        
        dialog.add(panel, BorderLayout.CENTER)
        dialog.add(btnPanel, BorderLayout.SOUTH)
        dialog.setLocationRelativeTo(parent); dialog.isVisible = true
    }

    private fun ensureColumnExists(gridTag: XmlTag, tableKey: String, columnKey: String, deKey: String) {
        val table = YigoUtils.findTable(gridTag, tableKey) ?: return
        val existing = table.findSubTags("Column").find { it.getAttributeValue("Key") == columnKey }
        if (existing == null) {
            val newCol = table.createChildTag("Column", null, null, false)
            newCol.setAttribute("Key", columnKey)
            val deTag = DataElementIndex.findDEDefinition(project, deKey) ?: return@ensureColumnExists
            newCol.setAttribute("Caption", deTag.getAttributeValue("Caption") ?: deKey)
            newCol.setAttribute("DataElementKey", deKey)
            table.addSubTag(newCol, false)
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
        "SystemVestKey","SequenceValue","ResetPattern","ExternalSystemID","ExternalSystemPrimaryKey","ClientID"
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
        iterAllFields(rootTag, { tag ->
            if (tag.name == "GridCell") {
                val columnKey =
                    tag.findFirstSubTag("DataBinding")?.getAttributeValue("ColumnKey") ?: tag.getAttributeValue("Key")
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
        })
        return used
    }

    fun iterAllFields(rootTag: XmlTag, callback: (XmlTag) -> Unit) {
        fun iter(tag: XmlTag) {
            if (tag.name in variableDefinitionTagNames) {
                callback(tag)
                return
            }
            for (subTag in tag.subTags) {
                iter(subTag)
            }
            if (tag.name == "Embed") {
                val formKey = tag.getAttributeValue("FormKey")
                val defAttr = FormIndex.findFormDefinition(project, formKey)
                if (defAttr != null) {
                    (defAttr.containingFile as XmlFile).rootTag?.let { iter(it) }
                }
            }
        }
        iter(rootTag)
    }

    private fun getExistingKeys(rootTag: XmlTag): Set<String> {
        val keys = mutableSetOf<String>()
        iterAllFields(rootTag,{ tag ->
            val key = tag.getAttributeValue("Key")
            if (key != null) keys.add(key)
        })
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

    fun getSuggestTableKey(xmlTag: XmlTag): String?{
        var suggestedKey: String? = null
        if (xmlTag.isValid) {
            if (xmlTag.name == "Grid") {
                val rowCollection = xmlTag.findFirstSubTag("GridRowCollection")
                suggestedKey = rowCollection?.findSubTags("GridRow")?.firstOrNull()?.getAttributeValue("TableKey")
            } else {
                for (control in xmlTag.subTags) {
                    for (subTag in control.subTags) {
                        if (subTag.name == "DataBinding") {
                            val tk = subTag.getAttributeValue("TableKey")
                            if (tk != null) {
                                suggestedKey = tk
                                break
                            }
                        }
                    }
                    if (suggestedKey != null) break
                }
            }
        }
        return suggestedKey
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
        var usedColumns = emptySet<Pair<String, String>>()
        var existingKeys = emptySet<String>()

        val updateList: () -> Unit = {
            val filter = columnKeyField.text.lowercase()
            val currentTableKey = tableKeyCombo.selectedItem as? String ?: ""
            
            // Check duplicates in real-time
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

        val loadColumns: () -> Unit = {
            val tableKey = tableKeyCombo.selectedItem as? String ?: ""
            if (tableKey.isNotEmpty()) {
                // Show loading state without clearing the table to avoid flicker
                errorLabel.text = "Loading columns for $tableKey..."
                errorLabel.foreground = Color.GRAY
                
                ApplicationManager.getApplication().executeOnPooledThread {
                    val result = ApplicationManager.getApplication().runReadAction<List<ColumnSelection>?> {
                        val table = YigoUtils.findTable(gridTag, tableKey)
                        table?.let {
                            getAllColumn(it, tableKey, emptySet()).map {
                                ColumnSelection(it.first, it.second, it.first)
                            }
                        }
                    }
                    SwingUtilities.invokeLater {
                        // VERIFY current selection still matches the table we just loaded
                        if (tableKeyCombo.selectedItem == tableKey) {
                            if (result != null) {
                                allColumns = result
                                updateList()
                                errorLabel.text = " "
                                errorLabel.foreground = Color.RED
                            }
                        }
                    }
                }
            }
        }

        // Fetch initial data in background
        ApplicationManager.getApplication().executeOnPooledThread {
            val initData = ApplicationManager.getApplication().runReadAction<Triple<Set<Pair<String, String>>, Set<String>, Triple<List<String>, String?, Boolean>>> {
                val root = YigoUtils.getRootFormTag(gridTag)
                val used = root?.let { getUsedColumns(it) } ?: emptySet()
                val keys = root?.let { getExistingKeys(it) } ?: emptySet()
                val availableTables = YigoUtils.getTables(gridTag)?.mapNotNull { it.getAttributeValue("Key") } ?: emptyList()
                val suggestedKey = getSuggestTableKey(gridTag)
                Triple(used, keys, Triple(availableTables, suggestedKey, gridTag.name == "Grid"))
            }

            SwingUtilities.invokeLater {
                usedColumns = initData.first
                existingKeys = initData.second
                val (tableList, suggested, isGrid) = initData.third
                
                tableList.forEach { tableKeyCombo.addItem(it) }
                if (isGrid) {
                    tableKeyCombo.isEnabled = false
                }
                if (suggested != null) {
                    tableKeyCombo.selectedItem = suggested
                }
                
                loadColumns()
            }
        }

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

        val toggleBtn = JCheckBox("Show All", showAll)
        val selectAllBtn = JButton("Select All").apply {  }
        val deselectAllBtn = JButton("Deselect All").apply {  }
        
        val filterPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        filterPanel.add(toggleBtn)
        filterPanel.add(selectAllBtn)
        filterPanel.add(deselectAllBtn)
        
        c.gridx = 1; c.gridy = 2; c.weightx = 1.0
        panel.add(filterPanel, c)


        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; c.weighty = 1.0; c.fill = GridBagConstraints.BOTH
        val scrollPane = ScrollPaneFactory.createScrollPane(columnTable)
        scrollPane.preferredSize = Dimension(500, 300)
        panel.add(scrollPane, c)

        c.gridx = 0; c.gridy = 4; c.gridwidth = 2; c.weighty = 0.0; c.fill = GridBagConstraints.HORIZONTAL
        panel.add(errorLabel, c)



        selectAllBtn.addActionListener {
            tableModel.data.forEach { it.selected = true }
            updateList()
        }
        deselectAllBtn.addActionListener {
            tableModel.data.forEach { it.selected = false }
            updateList()
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
        // loadColumns() // Called in background finish

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
        val column = YigoUtils.findColumnInTable(gridTag, tableKey, columnKey) ?: return
        val deKey = column.getAttributeValue("DataElementKey") ?: return
        val deTag = DataElementIndex.findDEDefinition(project, deKey) ?: return
        val domainKey = deTag.getAttributeValue("DomainKey") ?: return
        val domainTag = DomainIndex.findDomainDefinition(project, domainKey) ?: return

        val controlType = domainTag.getAttributeValue("RefControlType") ?: "TextEditor"
        val caption = column.getAttributeValue("Caption") ?: deTag.getAttributeValue("Caption")

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
        val fieldLabelCollection = deTag.findFirstSubTag("FieldLabelCollection")
        if (fieldLabelCollection != null) {
            newControl.setAttribute("LabelType", "M")
            for (tag in fieldLabelCollection.subTags) {
                // 处理每个 FieldLabel 标签//
                val labelType = tag.getAttributeValue("Key")
                if (labelType == "Medium") {
                    newControl.setAttribute("Caption", tag.getAttributeValue("Text"))
                    break
                }
            }
        }

        applyDomainAttributes(newControl, domainTag, controlType)

        val dataBinding = newControl.createChildTag("DataBinding", null, null, false)
        dataBinding.setAttribute("TableKey", tableKey)
        dataBinding.setAttribute("ColumnKey", columnKey)
        newControl.addSubTag(dataBinding, false)

        gridTag.addSubTag(newControl, false)
    }

    private fun createGridColumnFromDomain(
        gridTag: XmlTag,
        tableKey: String,
        columnKey: String,
        customKey: String,
        afterColumnKey: String? = null
    ) {
        val columnDef =
            YigoUtils.findColumnInTable(gridTag, tableKey, columnKey) ?: return
        val deKey = columnDef.getAttributeValue("DataElementKey") ?: return
        val deTag = DataElementIndex.findDEDefinition(project, deKey) ?: return
        val domainKey = deTag.getAttributeValue("DomainKey") ?: return
        val domainTag = DomainIndex.findDomainDefinition(project, domainKey) ?: return

        val controlType = domainTag.getAttributeValue("RefControlType") ?: "TextEditor"
        val caption = columnDef.getAttributeValue("Caption") ?: deTag.getAttributeValue("Caption")

        val colCollection = gridTag.findFirstSubTag("GridColumnCollection") ?: gridTag.createChildTag(
            "GridColumnCollection",
            null,
            null,
            false
        ).also { gridTag.addSubTag(it, false) }
        val newColumn = colCollection.createChildTag("GridColumn", null, null, false)
        newColumn.setAttribute("Key", customKey)
        newColumn.setAttribute("Caption", caption)
        newColumn.setAttribute("Width", "80px")
        val fieldLabelCollection = deTag.findFirstSubTag("FieldLabelCollection")
        if (fieldLabelCollection != null) {
            newColumn.setAttribute("LabelType", "M")
            for (tag in fieldLabelCollection.subTags) {
                // 处理每个 FieldLabel 标签//
                val labelType = tag.getAttributeValue("Key")
                if (labelType == "Medium") {
                    newColumn.setAttribute("Caption", tag.getAttributeValue("Text"))
                    break
                }
            }
        }


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

    private fun applyDomainAttributes(controlTag: XmlTag, domainTag: XmlTag, controlType: String) {
        val attrList = listOf("Key", "DataType", "Length", "Caption", "RefControlType")
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
