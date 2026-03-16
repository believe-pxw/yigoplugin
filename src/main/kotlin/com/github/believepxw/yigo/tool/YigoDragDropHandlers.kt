package com.github.believepxw.yigo.tool

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import java.awt.Component
import java.awt.Container
import javax.swing.JComponent
import javax.swing.TransferHandler

object DragContext {
    var draggedTag: XmlTag? = null
}

class FlexDropHandler(private val panel: YigoLayoutPanel, private val parentTag: XmlTag, private val project: Project) : TransferHandler() {
    override fun canImport(support: TransferSupport): Boolean {
        return DragContext.draggedTag != null
    }

    override fun importData(support: TransferSupport): Boolean {
        val draggedTag = DragContext.draggedTag ?: return false
        if (PsiTreeUtil.isAncestor(draggedTag, parentTag, false)) return false

        val dropLocation = support.dropLocation as? DropLocation ?: return false
        val panelComp = support.component as? Container ?: return false

        var closestDist = Double.MAX_VALUE
        var closestComp: Component? = null
        val p = dropLocation.dropPoint

        for (comp in panelComp.components) {
            if (comp !is JComponent || comp.getClientProperty("YigoXmlTag") == null) continue
            val cx = comp.x + comp.width / 2
            val cy = comp.y + comp.height / 2
            val dist = Math.pow((p.x - cx).toDouble(), 2.0) + Math.pow((p.y - cy).toDouble(), 2.0)
            if (dist < closestDist) {
                closestDist = dist
                closestComp = comp
            }
        }

        if (closestComp != null) {
            val refTag = (closestComp as JComponent).getClientProperty("YigoXmlTag") as XmlTag
            val center = closestComp.x + closestComp.width / 2
            val insertBefore = p.x < center

            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(project) {
                    val newCopy = draggedTag.copy()
                    draggedTag.delete()
                    if (insertBefore) {
                        parentTag.addBefore(newCopy, refTag)
                    } else {
                        parentTag.addAfter(newCopy, refTag)
                    }
                }
            }
            return true
        } else {
            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(project) {
                    val newCopy = draggedTag.copy()
                    draggedTag.delete()
                    parentTag.add(newCopy)
                }
            }
            return true
        }
    }
}

class GridDropHandler(private val panel: YigoLayoutPanel, private val gridTag: XmlTag, private val project: Project) : TransferHandler() {
    override fun canImport(support: TransferSupport): Boolean {
        return DragContext.draggedTag != null
    }

    override fun importData(support: TransferSupport): Boolean {
        val draggedTag = DragContext.draggedTag ?: return false
        val dropLocation = support.dropLocation as? DropLocation ?: return false
        val component = support.component as? Container ?: return false

        var targetX = 0
        var targetY = 0
        var found = false

        for (comp in component.components) {
            if (comp.bounds.contains(dropLocation.dropPoint)) {
                if (comp.name?.startsWith("Placeholder") == true) {
                    val parts = comp.name.substringAfter(":").split(",")
                    targetX = parts[0].toInt()
                    targetY = parts[1].toInt()
                    found = true
                } else {
                    val layout = component.layout as? java.awt.GridBagLayout
                    if (layout != null) {
                        val c = layout.getConstraints(comp)
                        targetX = c.gridx
                        targetY = c.gridy
                        found = true
                    }
                }
                break
            }
        }

        if (!found) return false

        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project) {
                if (draggedTag.parentTag != gridTag) {
                    try {
                        val copy = draggedTag.copy() as XmlTag
                        copy.setAttribute("X", targetX.toString())
                        copy.setAttribute("Y", targetY.toString())
                        gridTag.add(copy)
                        draggedTag.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    draggedTag.setAttribute("X", targetX.toString())
                    draggedTag.setAttribute("Y", targetY.toString())
                }
            }
        }
        return true
    }
}

class TableDropHandler(private val gridTag: XmlTag, private val project: Project) : TransferHandler() {
    override fun canImport(support: TransferSupport): Boolean {
        val dragged = DragContext.draggedTag
        return dragged != null && dragged.name == "GridColumn"
    }

    override fun importData(support: TransferSupport): Boolean {
        val draggedTag = DragContext.draggedTag ?: return false
        if (draggedTag.name != "GridColumn") return false

        val dropLocation = support.dropLocation as? DropLocation ?: return false
        val component = support.component as? Container ?: return false

        var targetTag: XmlTag? = null
        var insertBefore = false

        ApplicationManager.getApplication().runReadAction {
            for (comp in component.components) {
                if (comp.bounds.contains(dropLocation.dropPoint)) {
                    val tag = (comp as? JComponent)?.getClientProperty("YigoXmlTag") as? XmlTag
                    if (tag != null && tag.isValid && tag.name == "GridColumn") {
                        targetTag = tag
                        val center = comp.x + comp.width / 2
                        insertBefore = dropLocation.dropPoint.x < center
                    }
                    break
                }
            }
        }

        if (targetTag == null) return false
        if (targetTag == draggedTag) return false

        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project) {
                val parent = targetTag!!.parentTag
                if (parent != null) {
                    moveColumnAndCells(gridTag, draggedTag, targetTag!!, insertBefore)
                }
            }
        }
        return true
    }

    private fun moveColumnAndCells(gridTag: XmlTag, draggedCol: XmlTag, targetCol: XmlTag, insertBefore: Boolean) {
        val colCollection = draggedCol.parentTag ?: return
        val rowCollection = gridTag.findFirstSubTag("GridRowCollection")

        val newCopy = draggedCol.copy() as XmlTag
        draggedCol.delete()
        if (insertBefore) {
            colCollection.addBefore(newCopy, targetCol)
        } else {
            colCollection.addAfter(newCopy, targetCol)
        }

        val colKey = newCopy.getAttributeValue("Key") ?: return
        val targetKey = targetCol.getAttributeValue("Key") ?: return

        if (rowCollection != null) {
            for (row in rowCollection.findSubTags("GridRow")) {
                val cells = row.findSubTags("GridCell")
                val cellToMove = cells.find { it.getAttributeValue("Key") == colKey }
                val targetCell = cells.find { it.getAttributeValue("Key") == targetKey }

                if (cellToMove != null && targetCell != null) {
                    val cellCopy = cellToMove.copy()
                    cellToMove.delete()
                    if (insertBefore) {
                        row.addBefore(cellCopy, targetCell)
                    } else {
                        row.addAfter(cellCopy, targetCell)
                    }
                }
            }
        }
    }
}
