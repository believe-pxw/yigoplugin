package com.github.believepxw.yigo.util

import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlElement
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import example.index.DataObjectIndex
import example.index.DataElementIndex
import com.intellij.openapi.project.Project

object YigoUtils {

    fun getParentDataBindingTag(element: XmlElement): XmlTag? {
        var parent = element.parent
        while (parent != null) {
            if (parent is XmlTag) {
                if ("DataBinding" == parent.name || "Condition" == parent.name) {
                    return parent
                }
            }
            parent = parent.parent
        }
        return null
    }

    fun findTableKeyFromGridRow(dataBindingTag: XmlTag): String? {
        var current = dataBindingTag.parent
        while (current != null) {
            if (current is XmlTag) {
                if ("GridRow" == current.name) {
                    val tableKeyAttr = current.getAttribute("TableKey")
                    if (tableKeyAttr != null) {
                        return tableKeyAttr.value
                    }
                }
            }
            current = current.parent
        }
        return null
    }

    fun findColumnInTable(startElement: XmlElement, tableKey: String, columnKey: String): XmlTag? {
        val rootTag = getRootFormTag(startElement) ?: return null
        val dataSourceTag = findChildTagByName(rootTag, "DataSource") ?: return null

        var dataObjectTag: XmlTag? = null
        val refKey = dataSourceTag.getAttributeValue("RefObjectKey")
        if (refKey != null) {
            val dataObjectDefinition = DataObjectIndex.findDataObjectDefinition(startElement.project, refKey)
            if (dataObjectDefinition != null) {
                dataObjectTag = dataObjectDefinition.parent.parent as? XmlTag
            }
        } else {
            dataObjectTag = findChildTagByName(dataSourceTag, "DataObject")
        }

        if (dataObjectTag == null) return null

        val tableCollectionTag = findChildTagByName(dataObjectTag, "TableCollection") ?: return null

        var targetTable: XmlTag? = null
        val tables = tableCollectionTag.findSubTags("Table")
        for (table in tables) {
            val keyAttr = table.getAttribute("Key")
            if (keyAttr != null && tableKey == keyAttr.value) {
                targetTable = table
                break
            }
        }

        if (targetTable == null) {
             val embedTableCollection = findChildTagByName(dataObjectTag, "EmbedTableCollection")
             if (embedTableCollection != null) {
                 val subTags = embedTableCollection.findSubTags("EmbedTable")
                 for (subTag in subTags) {
                     if (subTag.getAttributeValue("TableKeys") == tableKey) {
                         val objectKey = subTag.getAttributeValue("ObjectKey")
                         if (objectKey != null) {
                             val dataObjectDefinition = DataObjectIndex.findDataObjectDefinition(startElement.project, objectKey)
                             if (dataObjectDefinition != null) {
                                  val linkedDataObjectTag = dataObjectDefinition.parent.parent as? XmlTag
                                  if (linkedDataObjectTag != null) {
                                      val linkedTableCollection = linkedDataObjectTag.findSubTags("TableCollection").firstOrNull()
                                      if (linkedTableCollection != null) {
                                          val linkedTables = linkedTableCollection.findSubTags("Table")
                                          for (tag in linkedTables) {
                                              if (tag.getAttributeValue("Key") == tableKey) {
                                                  targetTable = tag
                                                  break
                                              }
                                          }
                                      }
                                  }
                             }
                         }
                     }
                 }
             }
        }

        if (targetTable == null) return null

        val columns = targetTable.findSubTags("Column")
        for (column in columns) {
            val keyAttr = column.getAttribute("Key")
            if (keyAttr != null && columnKey == keyAttr.value) {
                return column
            }
        }
        return null
    }
    
    // Find Column definition but searching recursively if TableKey is not known (implied from context or just simple search)
    // However, the original logic requires TableKey. If TableKey is missing, we might need to search all tables?
    // For now, let's assume we can get TableKey.

    fun getRootFormTag(element: XmlElement): XmlTag? {
        var current: PsiElement? = element
        while (current != null) {
            if (current is XmlTag) {
                if ("Form" == current.name) {
                    return current
                }
            }
            if (current is XmlFile) return current.rootTag
            current = current.parent
        }
        return null
    }

    fun findChildTagByName(parent: XmlTag, tagName: String): XmlTag? {
        val children = parent.findSubTags(tagName)
        return if (children.isNotEmpty()) children[0] else null
    }
    
    fun findDataElementDefinition(project: Project, dataElementKey: String): XmlTag? {
         return DataElementIndex.findDEDefinition(project, dataElementKey)
    }

    fun findGridCell(gridTag: XmlTag, key: String): XmlTag? {
        // Search for GridCell with matching Key inside the Grid tag
        // Recursion might be needed if GridCell is nested (e.g. inside Reference info or something, but usually it's direct or simple structure)
        // Usually Grid -> GridRow -> GridCell? Or Grid -> GridDefine -> GridRow?
        // Let's do a recursive search for "GridCell" with attribute Key = key under the Grid tag.
        
        // Actually, the structure usually is Grid -> GenericGrid -> GridRowCollection -> GridRow -> GridCell
        // Or similar. Safer to search recursively.
        return findTagRecursive(gridTag, "GridCell", key)
    }
    
    fun findTagRecursive(parent: XmlTag, tagName: String, key: String): XmlTag? {
        if (parent.name == tagName && parent.getAttributeValue("Key") == key) {
            return parent
        }
        for (subTag in parent.subTags) {
            val found = findTagRecursive(subTag, tagName, key)
            if (found != null) return found
        }
        return null
    }
}
