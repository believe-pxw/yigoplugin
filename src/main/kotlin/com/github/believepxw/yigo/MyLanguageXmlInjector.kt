package com.github.believepxw.yigo

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.elementType
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlText
import example.MyLanguage

class MyLanguageXmlInjector : MultiHostInjector {
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        if (context is XmlText) {
            context.children.forEach {
                if (it.elementType.toString() == "XML_CDATA") {
                    registrar
                        .startInjecting(MyLanguage.INSTANCE)
                        .addPlace(
                            null,
                            null,
                            context as PsiLanguageInjectionHost,
                            TextRange.from(it.startOffsetInParent + 9, it.textLength - 3)
                        )
                        .doneInjecting()
                }
            }
        }else if (context is XmlAttributeValue) {
            var attrKey = context.parent.firstChild.text
            if (attrKey in listOf("Enable", "Visible","ValueChanged","DefaultFormulaValue","CheckRule")) {
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