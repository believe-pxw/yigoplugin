package com.github.believepxw.yigo.inlay
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
class YigoInlayProvider : InlayHintsProvider<NoSettings> {

    override val key: SettingsKey<NoSettings> = SettingsKey("YigoInlayProvider")
    override val name: String = "Yigo Inlay Hints"
    override val previewText: String = "<DataBinding TableKey=\"Head\" ColumnKey=\"TotalAmount\" />\n<Column Key=\"NetMoney\" Caption=\"Total\" DataElementKey=\"NetMoney\"/>"

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
                ProgressManager.checkCanceled()
                
                if (element !is XmlTag) return true
                
                if (element.name == "DataBinding") {
                    handleDataBinding(element, sink)
                } else if (element.name == "Column") {
                    handleColumn(element, sink)
                }
                
                return true
            }
            
            private fun handleDataBinding(tag: XmlTag, sink: InlayHintsSink) {
                // TableKey hint removed as per requirements

                // Handle ColumnKey
                val columnKeyAttr = tag.getAttribute("ColumnKey")
                val columnKey = columnKeyAttr?.value
                
                // Need TableKey to resolve Column
                val tableKeyAttr = tag.getAttribute("TableKey")
                var tableKey = tableKeyAttr?.value
                if (tableKey == null) {
                    tableKey = YigoUtils.findTableKeyFromGridRow(tag)
                }
                
                if (tableKey != null && columnKey != null) {
                    val columnTag = YigoUtils.findColumnInTable(tag, tableKey, columnKey)
                    if (columnTag != null) {
                        val dataElementKey = columnTag.getAttributeValue("DataElementKey")
                        if (dataElementKey != null) {
                             resolveAndAddDomainHint(columnKeyAttr!!, dataElementKey, tag.project, sink)
                        }
                    }
                }
            }
            
            private fun handleColumn(tag: XmlTag, sink: InlayHintsSink) {
                val dataElementKeyAttr = tag.getAttribute("DataElementKey")
                val dataElementKey = dataElementKeyAttr?.value ?: return
                
                resolveAndAddDomainHint(dataElementKeyAttr, dataElementKey, tag.project, sink)
            }
            
            private fun resolveAndAddDomainHint(sourceAttr: com.intellij.psi.xml.XmlAttribute, dataElementKey: String, project: com.intellij.openapi.project.Project, sink: InlayHintsSink) {
                val dataElementTag = YigoUtils.findDataElementDefinition(project, dataElementKey) ?: return
                
                // In DataElement, DomainKey is usually the attribute pointing to Domain
                // Sometimes DataElementKey itself is the DomainKey if implied? 
                // Let's check 'DomainKey' attribute first.
                var domainKey = dataElementTag.getAttributeValue("DomainKey")
                
                // If DomainKey is missing, try using DataElementKey itself? 
                // Based on existing documentation logic (step 38), it strictly looks for DomainKey.
                if (domainKey == null) return
                
                val domainTag = example.index.DomainIndex.findDomainDefinition(project, domainKey) ?: return
                
                addDomainAttributesHint(sourceAttr, domainTag, sink)
            }
            
            private fun addDomainAttributesHint(sourceAttr: com.intellij.psi.xml.XmlAttribute, domainTag: XmlTag, sink: InlayHintsSink) {
                val attributes = domainTag.attributes
                val sb = StringBuilder()
                var first = true
                
                for (attr in attributes) {
                    val name = attr.name
                    val value = attr.value
                    if (name != "Key" && name != "Caption" && !value.isNullOrEmpty()) {
                        if (!first) {
                            sb.append(",")
                        }
                        if (name == "RefControlType") {
                            sb.append(value)
                        }else{
                            sb.append(name).append("=").append(value)
                        }
                        first = false
                    }
                }
                
                if (sb.isNotEmpty()) {
                    val valueElement = sourceAttr.valueElement
                    if (valueElement != null) {
                        val offset = valueElement.textRange.endOffset
                        sink.addInlineElement(offset, true, factory.text(":$sb"), false)
                    }
                }
            }
        }
    }

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent = panel {}
        }
    }
}