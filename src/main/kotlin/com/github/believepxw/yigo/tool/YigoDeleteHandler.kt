package com.github.believepxw.yigo.tool

import com.github.believepxw.yigo.ref.VariableReference
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.intellij.ui.components.JBScrollPane
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.table.DefaultTableModel

class YigoDeleteHandler(private val panel: YigoLayoutPanel) {

    fun showLeafDeleteMenu(e: MouseEvent, tag: XmlTag) {
        val menu = JPopupMenu()
        val deleteNormal = JMenuItem("Delete")
        deleteNormal.icon = com.intellij.icons.AllIcons.Actions.GC
        deleteNormal.addActionListener {
            deleteTagsWithCascade(listOf(tag), cascade = false)
        }
        
        val deleteCascade = JMenuItem("Delete with Cascade")
        deleteCascade.icon = com.intellij.icons.AllIcons.Actions.GC
        deleteCascade.addActionListener {
            deleteTagsWithCascade(listOf(tag), cascade = true)
        }
        
        menu.add(deleteNormal)
        menu.add(deleteCascade)
        menu.show(e.component, e.x, e.y)
    }

    fun showBatchDeleteDialog(containerTag: XmlTag) {
        val childTags = mutableListOf<Pair<XmlTag, String>>()

        ApplicationManager.getApplication().runReadAction {
            if (!containerTag.isValid) return@runReadAction

            when (containerTag.name) {
                "Grid" -> {
                    // Only list GridColumns, not GridCells
                    val colCollection = containerTag.findFirstSubTag("GridColumnCollection")
                    colCollection?.findSubTags("GridColumn")?.forEach { col ->
                        val key = col.getAttributeValue("Key") ?: col.name
                        val caption = col.getAttributeValue("Caption") ?: ""
                        val display = if (caption.isNotEmpty()) "$key ($caption)" else key
                        childTags.add(col to display)
                    }
                }
                else -> {
                    // GridLayoutPanel, FlexGridLayoutPanel, FlexFlowLayoutPanel, etc.
                    containerTag.subTags.filter { panel.isValidGridChild(it.name) }.forEach { child ->
                        val key = child.getAttributeValue("Key") ?: child.name
                        val caption = child.getAttributeValue("Caption") ?: ""
                        val display = if (caption.isNotEmpty()) "$key ($caption)" else "$key [${child.name}]"
                        childTags.add(child to display)
                    }
                }
            }
        }

        if (childTags.isEmpty()) {
            JOptionPane.showMessageDialog(panel, "No deletable items found.", "Batch Delete", JOptionPane.INFORMATION_MESSAGE)
            return
        }

        // Build dialog with JTable checkboxes
        val dialog = JDialog(SwingUtilities.getWindowAncestor(panel), "Batch Delete", Dialog.ModalityType.APPLICATION_MODAL)
        dialog.layout = BorderLayout()

        val columnNames = arrayOf("", "Name")
        val data = Array(childTags.size) { arrayOf(false as Any, childTags[it].second as Any) }
        val tableModel = object : DefaultTableModel(data, columnNames) {
            override fun getColumnClass(columnIndex: Int): Class<*> {
                return if (columnIndex == 0) java.lang.Boolean::class.java else String::class.java
            }
            override fun isCellEditable(row: Int, column: Int): Boolean = column == 0
        }
        val table = javax.swing.JTable(tableModel)
        table.columnModel.getColumn(0).preferredWidth = 30
        table.columnModel.getColumn(0).maxWidth = 40
        table.columnModel.getColumn(1).preferredWidth = 350
        table.rowHeight = 24

        val tableScrollPane = JBScrollPane(table)
        tableScrollPane.preferredSize = Dimension(420, Math.min(childTags.size * 24 + 30, 400))
        dialog.add(tableScrollPane, BorderLayout.CENTER)

        // Buttons panel
        val buttonsPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        val selectAllBtn = JButton("Select All")
        selectAllBtn.addActionListener {
            for (i in 0 until tableModel.rowCount) tableModel.setValueAt(true, i, 0)
        }
        val deselectAllBtn = JButton("Deselect All")
        deselectAllBtn.addActionListener {
            for (i in 0 until tableModel.rowCount) tableModel.setValueAt(false, i, 0)
        }
        val cascadeCheckbox = JCheckBox("Cascade Delete", true)
        val deleteBtn = JButton("Delete Selected")
        deleteBtn.addActionListener {
            val selectedTags = mutableListOf<XmlTag>()
            for (i in 0 until tableModel.rowCount) {
                if (tableModel.getValueAt(i, 0) == true) {
                    selectedTags.add(childTags[i].first)
                }
            }
            if (selectedTags.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "No items selected.", "Batch Delete", JOptionPane.WARNING_MESSAGE)
                return@addActionListener
            }
            dialog.dispose()
            deleteTagsWithCascade(selectedTags, cascade = cascadeCheckbox.isSelected)
        }
        val cancelBtn = JButton("Cancel")
        cancelBtn.addActionListener { dialog.dispose() }

        buttonsPanel.add(cascadeCheckbox)
        buttonsPanel.add(selectAllBtn)
        buttonsPanel.add(deselectAllBtn)
        buttonsPanel.add(deleteBtn)
        buttonsPanel.add(cancelBtn)
        dialog.add(buttonsPanel, BorderLayout.SOUTH)

        dialog.pack()
        dialog.setLocationRelativeTo(panel)
        dialog.isVisible = true
    }

    /**
     * Delete the given tags, optionally with cascade.
     */
    fun deleteTagsWithCascade(tags: List<XmlTag>, cascade: Boolean = true) {
        val allTagsToDelete = mutableListOf<XmlTag>()
        val warningMessages = mutableListOf<String>()

        ApplicationManager.getApplication().runReadAction {
            for (tag in tags) {
                if (!tag.isValid) continue
                allTagsToDelete.add(tag)
                if (cascade) {
                    collectCascadeTargets(tag, allTagsToDelete, warningMessages)
                }
            }
        }

        ApplicationManager.getApplication().invokeLater {
            // Remove Swing components before deleting XML to avoid full rerender
            panel.isDeletingControls = true
            try {
                // Remove UI components for all tags being deleted
                for (tag in allTagsToDelete) {
                    val comp = panel.tagToComponent[tag]
                    if (comp != null) {
                        val parent = comp.parent
                        val toRemove = if (parent is javax.swing.JViewport && parent.parent is JBScrollPane) {
                            parent.parent
                        } else {
                            comp
                        }
                        toRemove.parent?.remove(toRemove)
                        toRemove.parent?.let { it.revalidate(); it.repaint() }
                        panel.tagToComponent.remove(tag)
                    }
                }
                panel.rootPanel.revalidate()
                panel.rootPanel.repaint()

                // Collect parent Grids BEFORE deleting XML
                val gridsToRefresh = mutableSetOf<XmlTag>()
                for (tag in tags) {
                    if (tag.name == "GridColumn" && tag.isValid) {
                        val gridTag = tag.parentTag?.parentTag // GridColumn -> GridColumnCollection -> Grid
                        if (gridTag != null && gridTag.name == "Grid") {
                            gridsToRefresh.add(gridTag)
                        }
                    }
                }

                WriteCommandAction.runWriteCommandAction(panel.project, "Delete Controls", null, Runnable {
                    for (t in allTagsToDelete) {
                        if (t.isValid) {
                            t.delete()
                        }
                    }
                })

                // Refresh parent Grid components
                ApplicationManager.getApplication().runReadAction {
                    for (gridTag in gridsToRefresh) {
                        if (gridTag.isValid) {
                            panel.refreshGridComponent(gridTag)
                        }
                    }
                }
            } finally {
                panel.isDeletingControls = false
            }
        }
    }

    private fun collectCascadeTargets(tag: XmlTag, result: MutableList<XmlTag>, warnings: MutableList<String>) {
        when (tag.name) {
            "GridColumn" -> {
                val colKey = tag.getAttributeValue("Key") ?: return
                val gridTag = tag.parentTag?.parentTag ?: return
                var gridCell = findGridCellsForColumn(gridTag, colKey)
                var refColumn: XmlAttributeValue? = null
                if (gridCell.size > 0) {
                    result.addAll(gridCell)
                    for (xmlTag in gridCell.first().subTags) {
                        if (xmlTag.name == "DataBinding") {
                            refColumn = xmlTag.getAttribute("ColumnKey")?.valueElement
                        }
                    }
                }

                if (refColumn != null) {
                    var def = refColumn.references.firstOrNull()?.resolve()
                    if (def != null) {
                        result.add(def.parent.parent as XmlTag)
                    }
                }
            }
            "Grid" -> {
                val colCollection = tag.findFirstSubTag("GridColumnCollection")
                colCollection?.findSubTags("GridColumn")?.forEach { col ->
                    collectCascadeTargets(col, result, warnings)
                }
                val rowCollection = tag.findFirstSubTag("GridRowCollection")
                val firstRow = rowCollection?.findSubTags("GridRow")?.firstOrNull()
                val tableKey = firstRow?.getAttributeValue("TableKey")
                if (tableKey != null) {
                    var def = firstRow.getAttribute("TableKey")?.valueElement?.references?.firstOrNull()?.resolve()
                    if (def != null) {
                        result.add(def.parent.parent as XmlTag)
                    }
                }
            }
            in VariableReference.variableDefinitionTagNames -> {
                var refColumn: XmlAttributeValue? = null
                for (xmlTag in tag.subTags) {
                    if (xmlTag.name == "DataBinding") {
                        refColumn = xmlTag.getAttribute("ColumnKey")?.valueElement
                    }
                }
                var defPsi = refColumn?.references?.firstOrNull()?.resolve()
                if (defPsi != null) {
                    result.add(defPsi.parent.parent as XmlTag)
                }
            }
            else -> {
                for (child in tag.subTags) {
                    if (child.name == "Grid") {
                        collectCascadeTargets(child, result, warnings)
                    } else if (child.name == "GridColumn") {
                        collectCascadeTargets(child, result, warnings)
                    } else if (child.name in VariableReference.variableDefinitionTagNames) {
                        collectCascadeTargets(child, result, warnings)
                    } else if (panel.isValidGridChild(child.name)) {
                        collectCascadeTargets(child, result, warnings)
                    }
                }
            }
        }
    }

    private fun findGridCellsForColumn(gridTag: XmlTag, columnKey: String): List<XmlTag> {
        val result = mutableListOf<XmlTag>()
        val rowCollection = gridTag.findFirstSubTag("GridRowCollection") ?: return result
        for (row in rowCollection.findSubTags("GridRow")) {
            for (cell in row.findSubTags("GridCell")) {
                if (cell.getAttributeValue("Key") == columnKey) {
                    result.add(cell)
                }
            }
        }
        return result
    }
}
