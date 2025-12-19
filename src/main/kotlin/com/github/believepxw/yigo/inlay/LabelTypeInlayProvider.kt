package com.github.believepxw.yigo.inlay

import com.github.believepxw.yigo.ref.VariableReference
import com.github.believepxw.yigo.util.YigoUtils
import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.ui.layout.panel
import javax.swing.JComponent

@Suppress("UnstableApiUsage")
class LabelTypeInlayProvider : InlayHintsProvider<NoSettings> {

    override val key: SettingsKey<NoSettings> = SettingsKey("YigoLabelTypeInlayProvider")
    override val name: String = "Yigo LabelType Inlay"
    override val previewText: String = "<Dict LabelType=\"M\" />"

    override fun createSettings(): NoSettings = NoSettings()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        if (file !is XmlFile) return null
        
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                 // Check process canceled
                ProgressManager.checkCanceled()
                
                if (element !is XmlTag) return true
                
                val labelTypeAttr = element.getAttribute("LabelType") ?: return true
                val labelTypeValue = labelTypeAttr.value
                if (labelTypeValue.isNullOrEmpty()) return true
                
                // Check if element is a valid component (Dict, GridColumn, etc)
                // GridColumn is not in VariableReference.variableDefinitionTagNames, but required.
                val allowedTags = VariableReference.variableDefinitionTagNames + "GridColumn"
                if (element.name !in allowedTags) return true
                
                var dataElement: XmlTag? = null
                
                if (element.name == "GridColumn") {
                    val key = element.getAttributeValue("Key") ?: return true
                    // Find generic GridCell with same Key in the same file (assuming same file for now)
                    // Or actually, GridColumn doesn't have DataBinding directly sometimes, but GridCell does.
                    // The GridColumn Key usually matches GridCell Key.
                    // We need to find the parent Grid -> find GridCell -> find DataBinding
                    
                    // Simple approach: Search entire file for GridCell with same Key? 
                    // Or look up? 
                    // Let's assume the GridCell is in the same file as GridColumn.
                    // We can use the root tag to search.
                    val root = YigoUtils.getRootFormTag(element)
                    if (root != null) {
                        val gridCell = YigoUtils.findTagRecursive(root, "GridCell", key)
                        if (gridCell != null) {
                            dataElement = resolveDataElementFromTag(gridCell)
                        }
                    }
                } else {
                    dataElement = resolveDataElementFromTag(element)
                }

                if (dataElement != null) {
                    val labelText = resolveLabelText(dataElement, labelTypeValue)
                    if (labelText != null) {
                         // Add inlay hint after the value of LabelType
                        val valueElement = labelTypeAttr.valueElement
                        if (valueElement != null) {
                            val offset = valueElement.textRange.endOffset
                            sink.addInlineElement(offset, true, factory.text(":$labelText"), false)
                        }
                    }
                }
                
                return true
            }
        }
    }
    
    private fun resolveDataElementFromTag(tag: XmlTag): XmlTag? {
        val dataBindingTag = YigoUtils.findChildTagByName(tag, "DataBinding") ?: return null
        
        var tableKey = dataBindingTag.getAttributeValue("TableKey")
        if (tableKey == null) {
            tableKey = YigoUtils.findTableKeyFromGridRow(dataBindingTag)
        }
        
        // If TableKey is still null, maybe we can try to find it from implicit context if possible?
        // But original logic required TableKey. 
        // Let's assume we can't do much without TableKey unless we scan all tables for the column?
        // Let's stick strictly to strict resolution first.
        if (tableKey == null) return null // Strict mode
        
        val columnKey = dataBindingTag.getAttributeValue("ColumnKey") ?: return null
        
        val columnTag = YigoUtils.findColumnInTable(tag, tableKey, columnKey) ?: return null
        
        val dataElementKey = columnTag.getAttributeValue("DataElementKey") ?: return null
        
        return YigoUtils.findDataElementDefinition(tag.project, dataElementKey)
    }

    private fun resolveLabelText(dataElement: XmlTag, labelType: String): String? {
        val collection = YigoUtils.findChildTagByName(dataElement, "FieldLabelCollection") ?: return null
        val labels = collection.findSubTags("FieldLabel")
        
        val targetKey = when(labelType.uppercase()) {
            "S" -> "Short"
            "M" -> "Medium"
            "L" -> "Long"
            "H" -> "Header"
            else -> labelType // Fallback or maybe exact match? Requirement said S, M, L, H.
        }
        
        for (label in labels) {
            if (label.getAttributeValue("Key") == targetKey) {
                return label.getAttributeValue("Text")
            }
        }
        return null
    }

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent = panel {}
        }
    }
}
