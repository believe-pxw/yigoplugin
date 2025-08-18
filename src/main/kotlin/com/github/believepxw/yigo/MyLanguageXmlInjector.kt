package com.github.believepxw.yigo

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.elementType
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlText
import example.MyLanguage

class MyLanguageXmlInjector : MultiHostInjector {
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        if (context !is PsiLanguageInjectionHost) {
            return
        }
        if (context.containingFile?.virtualFile?.path?.contains("/initializeData/") == true) {
            return
        }
        if (context is XmlText) {
            (context as XmlText).children.forEach {
                if (it.elementType.toString() == "XML_CDATA") {
                    try {
                        val parent = (context as? PsiElement)?.parent
                        if (parent is XmlTag) {
                            var tag = parent
                            if (tag.localName == "Statement" || tag.localName == "TableFilter") {
                                val attributeValue = tag.getAttributeValue("Type")
                                if (attributeValue == null || attributeValue == "Sql") {
                                    return
                                }
                            }
                        }
                        registrar
                            .startInjecting(MyLanguage.INSTANCE)
                            .addPlace(
                                null,
                                null,
                                context as PsiLanguageInjectionHost,
                                TextRange.from(it.startOffsetInParent + 9, it.getTextLength() - 3 - 9)
                            )
                            .doneInjecting()
                    } catch (e: IllegalArgumentException) {
                        throw e
                    }

                }
            }
        }else if (context is XmlAttributeValue) {
            var attrKey = (context as XmlAttributeValue).parent.firstChild.text
            if (attrKey in listOf(
                    "Enable",
                    "Visible",
                    "ValueChanged",
                    "DefaultFormulaValue",
                    "CheckRule",
                    "ParaValue",
                    "RefValue",
                    "GroupBy",
                    "OrderBy",
                    "FormulaCaption",
                    "Formula"
                )
            ) {
                registrar
                    .startInjecting(MyLanguage.INSTANCE)
                    .addPlace(
                        null,
                        null,
                        context as PsiLanguageInjectionHost,
                        TextRange.from(0, context.getTextLength())
                    )
                    .doneInjecting()
            }
        }
    }

    override fun elementsToInjectIn(): MutableList<Class<out PsiElement?>?> {
        return mutableListOf(XmlText::class.java, XmlAttributeValue::class.java)
    }
}