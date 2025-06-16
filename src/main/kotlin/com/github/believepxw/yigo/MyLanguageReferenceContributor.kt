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
                            var tagName = element.parent.firstChild.text
                            if (tagName == "ItemKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(
                                    DataObjectReference(
                                        element,
                                        TextRange(0, element.text.length),
                                        element.value
                                    )
                                )
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            } else if (tagName == "DataElementKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(DataElementReference(element, TextRange(0, element.text.length)))
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            } else if (tagName == "FormKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(FormReference(element, TextRange(0, element.text.length), element.value))
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            } else if (tagName == "Parameters") {
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
                                                    TextRange(startOffset, startOffset + value.length),
                                                    value
                                                )
                                            )
                                        }
                                    }
                                }
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            } else if (tagName == "DomainKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(DomainReference(element, TextRange(0, element.text.length)))
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            } else if (tagName == "RefObjectKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(
                                    DataObjectReference(
                                        element,
                                        TextRange(0, element.text.length),
                                        element.value
                                    )
                                )
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            } else if (tagName == "ColumnKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(DataBindingColumnReference(element, TextRange(0, element.text.length)))
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            } else if (tagName == "RefKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(OperationRefKeyReference(element, TextRange(0, element.text.length)))
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            } else if (tagName == "Key") {
                                var tag = element.parent.parent as XmlTag
                                if (tag.localName in VariableReference.variableDefinitionTagNames) {
                                    val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                    references.add(
                                        VariableReference(
                                            element,
                                            TextRange(0, element.text.length),
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
                                            TextRange(0, element.text.length),
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
                                    var firstChild = injectedElement.parent.parent.parent.parent.firstChild
                                    if (firstChild.node.elementType == MyLanguageTypes.IDENTIFIER)
                                        if (firstChild.text == "ERPShowModal" || firstChild.text == "Open" || firstChild.text == "OpenDict") {
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
                                        } else if (firstChild.text == "SetValue" || firstChild.text == "GetValue") {
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
                                        } else if (firstChild.text == "GetDictValue") {
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
                            true // 继续遍历
                        }
                    }
                    return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                }
            })
    }
}