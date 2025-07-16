package com.github.believepxw.yigo

import com.github.believepxw.yigo.ref.JavaMethodReference
import com.github.believepxw.yigo.ref.VariableReference
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext
import com.intellij.util.containers.toArray
import example.psi.MyLanguageTypes
import example.ref.*

class MyLanguageReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // 注册对 MACRO_IDENTIFIER 元素的引用解析
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(),  // 匹配宏标识符的 PSI 元素
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference?> {
                    var injectedFragments: MutableList<Pair<PsiElement?, TextRange?>>? = null
                    if (element.getNode().getElementType().toString() == "XML_DATA_CHARACTERS") {
                        val ancestor = element.parent.parent
                        if (ancestor !is PsiLanguageInjectionHost) {
                            return PsiReference.EMPTY_ARRAY
                        }
                        val host = ancestor
                        // 获取所有注入片段
                        injectedFragments = InjectedLanguageManager.getInstance(element.getProject())
                            .getInjectedPsiFiles(host)
                        if (injectedFragments == null || injectedFragments.isEmpty()) {
                            return PsiReference.EMPTY_ARRAY
                        }
                    } else if (element is XmlAttributeValue) {
                        // 获取所有注入片段
                        injectedFragments = InjectedLanguageManager.getInstance(element.getProject())
                            .getInjectedPsiFiles(element)
                        if (injectedFragments == null || injectedFragments.isEmpty()) {
                            var attrKey = element.parent.firstChild.text
                            if (attrKey == "ItemKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(
                                    DataObjectReference(
                                        element,
                                        TextRange(0 + 1, element.text.length - 1),
                                        element.value
                                    )
                                )
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            } else if (attrKey == "DataElementKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(DataElementReference(element, TextRange(0 + 1, element.text.length - 1)))
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            } else if (attrKey == "FormKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(FormReference(element, TextRange(0 + 1, element.text.length - 1), element.value))
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            } else if (attrKey == "Parameters") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                var value = element.value
                                if (value.contains("FormKey=")) {
                                    var split = value.split(";")
                                    split.forEach {
                                        if (it.contains("FormKey=")) {
                                            var split1 = it.split("=")
                                            value = split1[1]
                                            //找出FormKey在value中的offset
                                            var startOffset = it.indexOf(value)
                                            references.add(
                                                FormReference(
                                                    element,
                                                    TextRange(startOffset + 1, startOffset + value.length - 1),
                                                    value
                                                )
                                            )
                                        }
                                    }
                                }
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            } else if (attrKey == "DomainKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(DomainReference(element, TextRange(0 + 1, element.text.length - 1)))
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            } else if (attrKey == "RefObjectKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(
                                    DataObjectReference(
                                        element,
                                        TextRange(0 + 1, element.text.length - 1),
                                        element.value
                                    )
                                )
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            } else if (attrKey == "ColumnKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(
                                    DataBindingColumnReference(
                                        element,
                                        TextRange(0 + 1, element.text.length - 1),
                                        false
                                    )
                                )
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            } else if (attrKey == "RefKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(OperationRefKeyReference(element, TextRange(0 + 1, element.text.length - 1)))
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            } else if (attrKey == "GroupKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(
                                    ParaGroupReference(
                                        element,
                                        TextRange(0 + 1, element.text.length - 1),
                                        element.value
                                    )
                                )
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            } else if (attrKey == "Key") {
                                var tag = element.parent.parent as XmlTag
                                if (tag.localName in VariableReference.variableDefinitionTagNames) {
                                    val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                    references.add(
                                        VariableReference(
                                            element,
                                            TextRange(0 + 1, element.text.length - 1),
                                            element.value
                                        )
                                    )
                                    return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                                }
                                if (tag.localName == "Macro") {
                                    val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                    references.add(
                                        MacroReference(
                                            element,
                                            TextRange(0 + 1, element.text.length - 1),
                                            element.value
                                        )
                                    )
                                    return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                                }
                                if (tag.localName == "DataObject") {
                                    val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                    references.add(
                                        DataObjectDefinitionReference(
                                            element,
                                            TextRange(0 + 1, element.text.length - 1),
                                            element.value
                                        )
                                    )
                                    return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                                }
                                if (tag.localName == "Form") {
                                    val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                    references.add(
                                        FormDefinitionReference(
                                            element,
                                            TextRange(0 + 1, element.text.length - 1),
                                            element.value
                                        )
                                    )
                                    return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                                }
                                if (tag.localName == "DataElement") {
                                    val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                    references.add(
                                        DataElementDefinitionReference(
                                            element,
                                            TextRange(0 + 1, element.text.length - 1),
                                            element.value
                                        )
                                    )
                                    return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                                }
                                if (tag.localName == "Domain") {
                                    val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                    references.add(
                                        DomainDefinitionReference(
                                            element,
                                            TextRange(0 + 1, element.text.length - 1),
                                            element.value
                                        )
                                    )
                                    return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                                }
                                if (tag.localName == "ParaGroup") {
                                    val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                    references.add(
                                        ParaGroupDefinitionReference(
                                            element,
                                            TextRange(0 + 1, element.text.length - 1),
                                            element.value
                                        )
                                    )
                                    return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                                }
                                if (tag.localName == "Column") {
                                    val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                    references.add(
                                        DataBindingColumnReference(
                                            element,
                                            TextRange(0 + 1, element.text.length - 1), true
                                        )
                                    )
                                    return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                                }
                                if (tag.localName == "Operation") {
                                    if (tag.containingFile.name == "CommonDef.xml") {
                                        val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                        references.add(
                                            OperationRefKeyReference(
                                                element,
                                                TextRange(0 + 1, element.text.length - 1)
                                            )
                                        )
                                        return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                                    }
                                }
                            }else if (attrKey == "ObjectKey") {
                                var tag = element.parent.parent as XmlTag
                                if (tag.localName == "EmbedTable") {
                                    val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                    references.add(
                                        DataObjectReference(
                                            element,
                                            TextRange(0 + 1, element.text.length - 1),
                                            element.value
                                        )
                                    )
                                    return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                                }
                            }
                            return PsiReference.EMPTY_ARRAY
                        }
                    } else {
                        return PsiReference.EMPTY_ARRAY
                    }
                    val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                    for (fragment in injectedFragments) {
                        val injectedPsiRoot: PsiElement = fragment.first!! // 这是注入语言的 PSI 根节点

                        // 遍历注入语言的 PSI 树，查找需要添加引用的元素
                        // 这里你需要根据你的注入语言的PSI结构来查找
                        // 示例：查找 MyInjectedLanguageReferenceTarget 类型的元素
                        PsiTreeUtil.processElements(
                            injectedPsiRoot
                        ) { injectedElement: PsiElement? ->
                            val elementType = injectedElement!!.node.elementType
                            val referencedName = injectedElement.text
                            val injectedRange = injectedElement.textRange
                            // 计算引用在宿主元素中的真实范围
                            val rangeInInjectedFragment = TextRange(
                                injectedRange.startOffset - injectedPsiRoot.textRange
                                    .startOffset,
                                injectedRange.endOffset - injectedPsiRoot.textRange.startOffset
                            )
                            if (elementType == MyLanguageTypes.MACRO_IDENTIFIER) {
                                references.add(MacroReference(element, rangeInInjectedFragment, referencedName))
                            } else if (elementType == MyLanguageTypes.VARIABLE_REFERENCE) {
                                references.add(VariableReference(element, rangeInInjectedFragment, referencedName))
                            } else if (elementType == MyLanguageTypes.JAVA_PATH_IDENTIFIER) {
                                references.add(JavaMethodReference(element, rangeInInjectedFragment, referencedName))
                            } else if (elementType == MyLanguageTypes.IDENTIFIER) {
                                val parentElementType = injectedElement.parent.node.elementType
                                if (parentElementType == MyLanguageTypes.REGULAR_FUNCTION_CALL) {
                                    references.add(
                                        JavaMethodReference(
                                            element,
                                            rangeInInjectedFragment,
                                            referencedName
                                        )
                                    )
                                }
                            } else if (elementType == MyLanguageTypes.CONSTANT) {
                                val parentElementType =
                                    injectedElement.parent.parent.parent.parent.node.elementType
                                if (parentElementType == MyLanguageTypes.REGULAR_FUNCTION_CALL) {
                                    var funcName = injectedElement.parent.parent.parent.parent.firstChild
                                    if (funcName.node.elementType == MyLanguageTypes.IDENTIFIER) {
                                        var argumentList = injectedElement.parent.parent.parent
                                        var firstExpression = injectedElement.parent.parent
                                        if (argumentList.firstChild == firstExpression) {
                                            if (funcName.text == "ERPShowModal" || funcName.text == "Open" || funcName.text == "OpenDict" || funcName.text == "New") {
                                                references.add(
                                                    FormReference(
                                                        element,
                                                        TextRange(
                                                            rangeInInjectedFragment.startOffset + 1,
                                                            rangeInInjectedFragment.endOffset - 1
                                                        ),
                                                        referencedName.substring(1, referencedName.length - 1)
                                                    )
                                                )
                                            } else if (funcName.text == "SetValue" || funcName.text == "GetValue") {
                                                references.add(
                                                    VariableReference(
                                                        element,
                                                        TextRange(
                                                            rangeInInjectedFragment.startOffset + 1,
                                                            rangeInInjectedFragment.endOffset - 1
                                                        ),
                                                        referencedName.substring(1, referencedName.length - 1)
                                                    )
                                                )
                                            } else if (funcName.text == "GetDictValue") {
                                                references.add(
                                                    DataObjectReference(
                                                        element,
                                                        TextRange(
                                                            rangeInInjectedFragment.startOffset + 1,
                                                            rangeInInjectedFragment.endOffset - 1
                                                        ),
                                                        referencedName.substring(1, referencedName.length - 1)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            true // 继续遍历
                        }
                    }
                    return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                }
            })
    }
}