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
import com.intellij.remoteDev.util.UrlParameterKeys.Companion.host
import com.intellij.util.ProcessingContext
import com.intellij.util.containers.toArray
import example.psi.MyLanguageTypes
import example.ref.DataElementReference
import example.ref.DataObjectReference
import example.ref.DomainReference
import example.ref.MacroReference

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
                        val host = element.getParent().getParent() as PsiLanguageInjectionHost
                        // 获取所有注入片段
                        injectedFragments = InjectedLanguageManager.getInstance(element.getProject())
                            .getInjectedPsiFiles(host)
                        if (injectedFragments == null || injectedFragments.isEmpty()) {
                            return PsiReference.EMPTY_ARRAY
                        }
                    }else if (element is XmlAttributeValue) {
                        // 获取所有注入片段
                        injectedFragments = InjectedLanguageManager.getInstance(element.getProject())
                            .getInjectedPsiFiles(element)
                        if (injectedFragments == null || injectedFragments.isEmpty()) {
                            var tagName = element.parent.firstChild.text
                            if (tagName == "ItemKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(DataObjectReference(element, TextRange(0, element.text.length)))
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            }else if (tagName == "DataElementKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(DataElementReference(element, TextRange(0, element.text.length)))
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            }else if (tagName == "DomainKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(DomainReference(element, TextRange(0, element.text.length)))
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                            }else if (tagName == "RefObjectKey") {
                                val references: MutableList<PsiReference?> = ArrayList<PsiReference?>()
                                references.add(DataObjectReference(element, TextRange(0, element.text.length)))
                                return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
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
                            }else if (elementType == MyLanguageTypes.JAVA_PATH_IDENTIFIER) {
                                references.add(JavaMethodReference(element, rangeInInjectedFragment, referencedName))
                            }
                            true // 继续遍历
                        }
                    }
                    return references.toArray<PsiReference?>(PsiReference.EMPTY_ARRAY)
                }
            })
    }
}