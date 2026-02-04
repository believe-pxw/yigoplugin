package com.github.believepxw.yigo

import com.github.believepxw.yigo.ref.*
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext
import example.index.FormIndex
import example.psi.MyLanguageTypes
import example.ref.DataBindingColumnReference
import example.ref.DataElementDefinitionReference
import example.ref.DataElementReference
import example.ref.DataObjectDefinitionReference
import example.ref.DataObjectReference
import example.ref.DomainDefinitionReference
import example.ref.DomainReference
import example.ref.FormDefinitionReference
import example.ref.FormReference
import example.ref.MacroReference
import example.ref.OperationRefKeyReference
import example.ref.ParaGroupDefinitionReference
import example.ref.ParaGroupReference
import example.ref.TableReference

class MyLanguageReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // 注册对 MACRO_IDENTIFIER 元素的引用解析
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(),  // 匹配宏标识符的 PSI 元素
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    val project = element.project
                    val injectedFragments = if (element.node.elementType.toString() == "XML_DATA_CHARACTERS") {
                        val host = element.parent.parent
                        if (host is PsiLanguageInjectionHost) {
                            InjectedLanguageManager.getInstance(project).getInjectedPsiFiles(host)
                        } else null
                    } else if (element is XmlAttributeValue) {
                        InjectedLanguageManager.getInstance(project).getInjectedPsiFiles(element)
                    } else null

                    if (!injectedFragments.isNullOrEmpty()) {
                        return getInjectedReferences(element, injectedFragments).toTypedArray()
                    }

                    if (element is XmlAttributeValue && element.node.elementType.toString() != "XML_DATA_CHARACTERS") {
                        // XML_DATA_CHARACTERS logic above defaults to empty if no injection.
                        // But original code: if XML_DATA_CHARACTERS, if empty -> return empty (lines 39-40).
                        // If XmlAttributeValue, if empty -> check attributes (line 46).
                        // Note: XmlAttributeValue is NOT XML_DATA_CHARACTERS.
                        return getAttributeReferences(element).toTypedArray()
                    }

                    return PsiReference.EMPTY_ARRAY
                }
            })
    }

    private fun getAttributeReferences(element: XmlAttributeValue): List<PsiReference> {
        val attr = element.parent as? XmlAttribute ?: return emptyList()
        val attrKey = attr.name
        val references = mutableListOf<PsiReference>()
        val range = TextRange(1, element.textLength - 1)
        val value = element.value
        val project = element.project

        when (attrKey) {
            "SrcFormKey", "TgtFormKey" -> {
                val tag = attr.parent
                if (tag.localName == "Map") {
                    references.add(FormReference(element, range, value))
                }
            }
            "TgtDataObjectKey", "SrcDataObjectKey" -> {
                val tag = attr.parent
                if (tag.localName == "Map") {
                    references.add(DataObjectReference(element, range, value))
                }
            }
            "TargetFieldKey" -> {
                val tag = attr.parent
                if (tag.localName == "SourceField") {
                    // Original code redundancy check: tag.localName == "SourceField" || tag.localName == "SourceTable"
                    // Since we checked SourceField, this is guaranteed.
                    val mapTag = findMapTag(element)
                    val tgtFormKey = mapTag?.getAttributeValue("TgtFormKey")
                    if (tgtFormKey != null) {
                        val formDef = FormIndex.findFormDefinition(project, tgtFormKey)
                        val formTag = formDef?.parent?.parent as? XmlTag
                        if (formTag != null) {
                            references.add(VariableReference(element, range, value, formTag))
                        }
                    }
                }
            }
            "Definition" -> {
                val tag = attr.parent
                if (tag.localName == "SourceField") {
                    val type = tag.getAttributeValue("Type")
                    if (type != "Formula") {
                        val mapTag = findMapTag(element)
                        val srcFormKey = mapTag?.getAttributeValue("SrcFormKey")
                        if (srcFormKey != null) {
                            val formDef = FormIndex.findFormDefinition(project, srcFormKey)
                            val formTag = formDef?.parent?.parent as? XmlTag
                            if (formTag != null) {
                                references.add(VariableReference(element, range, value, formTag))
                            }
                        }
                    }
                }
            }
            "ItemKey" -> references.add(DataObjectReference(element, range, value))
            "DataElementKey" -> references.add(DataElementReference(element, range))
            "FormKey" -> references.add(FormReference(element, range, value))
            "Parameters" -> {
                var currentValue = value
                if (currentValue.contains("FormKey=")) {
                    val split = currentValue.split(";")
                    split.forEach { part ->
                         if (part.contains("FormKey=")) {
                             val split1 = part.split("=")
                             // Logic from original code: value var reused
                             currentValue = split1[1]
                             val startOffset = part.indexOf(currentValue)
                             references.add(
                                 FormReference(
                                     element,
                                     TextRange(startOffset + 1, startOffset + currentValue.length - 1),
                                     currentValue
                                 )
                             )
                         }
                    }
                }
            }
            "DomainKey" -> references.add(DomainReference(element, range))
            "TableKey" -> references.add(TableReference(element, range, false))
            "TableKeys" -> {
                val tag = attr.parent
                if (tag.localName == "EmbedTable") {
                    references.add(TableReference(element, range, false))
                }
            }
            "RefObjectKey" -> references.add(DataObjectReference(element, range, value))
            "ColumnKey" -> references.add(DataBindingColumnReference(element, range, false))
            "BindingCellKey" -> references.add(VariableReference(element, range, value))
            "RefKey" -> references.add(OperationRefKeyReference(element, range))
            "GroupKey" -> references.add(ParaGroupReference(element, range, value))
            "Key" -> {
                 val tag = attr.parent
                 val tagName = tag.localName
                 if (tagName in VariableReference.variableDefinitionTagNames) {
                     references.add(VariableReference(element, range, value))
                 } else {
                     when (tagName) {
                         "Macro" -> references.add(MacroReference(element, range, value))
                         "DataObject" -> references.add(DataObjectDefinitionReference(element, range, value))
                         "Form" -> references.add(FormDefinitionReference(element, range, value))
                         "DataElement" -> references.add(DataElementDefinitionReference(element, range, value))
                         "Domain" -> references.add(DomainDefinitionReference(element, range, value))
                         "ParaGroup" -> references.add(ParaGroupDefinitionReference(element, range, value))
                         "Column" -> references.add(DataBindingColumnReference(element, range, true))
                         "Table" -> references.add(TableReference(element, range, true))
                         "Operation" -> {
                             if (tag.containingFile.name == "CommonDef.xml") {
                                 references.add(OperationRefKeyReference(element, range))
                             }
                         }
                         "SourceField" -> {
                             if (tag.getAttribute("Definition") == null) {
                                 val mapTag = findMapTag(element)
                                 val srcFormKey = mapTag?.getAttributeValue("SrcFormKey")
                                 if (srcFormKey != null) {
                                     val formDef = FormIndex.findFormDefinition(project, srcFormKey)
                                     val formTag = formDef?.parent?.parent as? XmlTag
                                     if (formTag != null) {
                                         references.add(VariableReference(element, range, value, formTag))
                                     }
                                 }
                             }
                         }
                     }
                 }
            }
            "ObjectKey" -> {
                 val tag = attr.parent
                 if (tag.localName == "EmbedTable") {
                     references.add(DataObjectReference(element, range, value))
                 }
            }
        }
        return references
    }

    private fun getInjectedReferences(
        element: PsiElement,
        injectedFragments: List<Pair<PsiElement, TextRange>>
    ): List<PsiReference> {
        val references = mutableListOf<PsiReference>()
        val project = element.project
        
        for (fragment in injectedFragments) {
            val injectedPsiRoot = fragment.first ?: continue // Unsafe !! in original, ?: continue safer
            
            PsiTreeUtil.processElements(injectedPsiRoot) { injectedElement ->
                val elementType = injectedElement.node.elementType
                val referencedName = injectedElement.text
                val injectedRange = injectedElement.textRange
                val startOffsetInHost = injectedRange.startOffset - injectedPsiRoot.textRange.startOffset
                val endOffsetInHost = injectedRange.endOffset - injectedPsiRoot.textRange.startOffset
                val rangeInInjectedFragment = TextRange(startOffsetInHost, endOffsetInHost)
                
                val rootTagOriginal = (element.containingFile as? XmlFile)?.document?.rootTag
                
                if (rootTagOriginal?.localName == "Map" &&
                    (elementType == MyLanguageTypes.IDENTIFIER || elementType == MyLanguageTypes.VARIABLE_REFERENCE)
                ) {
                    val mapTag = findMapTag(element)
                    val srcFormKey = mapTag?.getAttributeValue("SrcFormKey")
                    if (srcFormKey != null) {
                        val formDef = FormIndex.findFormDefinition(project, srcFormKey)
                        val formTag = formDef?.parent?.parent as? XmlTag
                        if (formTag != null) {
                            references.add(VariableReference(element, rangeInInjectedFragment, referencedName, formTag))
                        }
                    }
                }
                
                if (elementType == MyLanguageTypes.MACRO_IDENTIFIER) {
                    references.add(MacroReference(element, rangeInInjectedFragment, referencedName))
                } else if (elementType == MyLanguageTypes.VARIABLE_REFERENCE) {
                    val formTag = getExternalFile(element)
                    references.add(VariableReference(element, rangeInInjectedFragment, referencedName, formTag))
                } else if (elementType == MyLanguageTypes.JAVA_PATH_IDENTIFIER) {
                    references.add(JavaMethodReference(element, rangeInInjectedFragment, referencedName))
                } else if (elementType == MyLanguageTypes.IDENTIFIER) {
                    val parentElementType = injectedElement.parent.node.elementType
                    if (parentElementType == MyLanguageTypes.REGULAR_FUNCTION_CALL) {
                        references.add(JavaMethodReference(element, rangeInInjectedFragment, referencedName))
                    }
                } else if (elementType == MyLanguageTypes.CONSTANT) {
                    // Deep parent access check
                    val callNode = injectedElement.parent?.parent?.parent?.parent?.node
                    if (callNode?.elementType == MyLanguageTypes.REGULAR_FUNCTION_CALL) {
                        val regularFuncCall = injectedElement.parent.parent.parent.parent
                        val funcName = regularFuncCall.firstChild
                        if (funcName.node.elementType == MyLanguageTypes.IDENTIFIER) {
                            val argumentList = injectedElement.parent.parent.parent
                            val firstExpression = injectedElement.parent.parent
                            if (argumentList.firstChild == firstExpression) {
                                val name = funcName.text
                                when (name) {
                                    "ERPShowModal", "Open", "OpenDict", "New" -> {
                                        references.add(FormReference(element, rangeRemoveQuotation(rangeInInjectedFragment), removeQuotation(referencedName)))
                                    }
                                    "SetValue", "GetValue", "Sum" -> {
                                        val formTag = getExternalFile(element)
                                        references.add(VariableReference(element, rangeRemoveQuotation(rangeInInjectedFragment), removeQuotation(referencedName), formTag))
                                    }
                                    "GetDictValue" -> {
                                        references.add(DataObjectReference(element, rangeRemoveQuotation(rangeInInjectedFragment), removeQuotation(referencedName)))
                                    }
                                }
                            }
                        }
                    }
                }
                true
            }
        }
        return references
    }

    private fun removeQuotation(referencedName: String): String {
        return referencedName.substring(1, referencedName.length - 1)
    }

    private fun rangeRemoveQuotation(rangeInInjectedFragment: TextRange): TextRange {
        return TextRange(rangeInInjectedFragment.startOffset + 1, rangeInInjectedFragment.endOffset - 1)
    }

    private fun findMapTag(element: PsiElement): XmlTag? {
        var current: PsiElement? = element
        while (current != null) {
            if (current is XmlTag && current.localName == "Map") {
                return current
            }
            if (current is PsiFile) {
                if (current is XmlFile) {
                    val root = current.document?.rootTag
                    if (root?.localName == "Map") {
                        return root
                    }
                }
                return null
            }
            current = current.parent
        }
        return null
    }

    private fun getExternalFile(element: PsiElement): XmlTag? {
        val xmlFile = element.containingFile as? XmlFile ?: return null
        var formTag: XmlTag? = null
        if (xmlFile.rootTag != null && xmlFile.rootTag?.localName == "Map") {
            val mapTag = findMapTag(element)
            // Original code: element.getParent().getParent().getParent() as XmlTag (Assuming PostProcess/other structure)
            val postProcess = element.parent?.parent?.parent as? XmlTag
            if (postProcess?.name == "PostProcess") {
                val tgtFormKey = mapTag?.getAttributeValue("TgtFormKey")
                if (tgtFormKey != null) {
                    val formDef = FormIndex.findFormDefinition(element.project, tgtFormKey)
                    formTag = formDef?.parent?.parent as? XmlTag
                }
            } else {
                val srcFormKey = mapTag?.getAttributeValue("SrcFormKey")
                if (srcFormKey != null) {
                    val formDef = FormIndex.findFormDefinition(element.project, srcFormKey)
                    formTag = formDef?.parent?.parent as? XmlTag
                }
            }
        }
        return formTag
    }
}