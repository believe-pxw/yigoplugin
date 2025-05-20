package com.github.believepxw.yigo

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.xml.XmlToken
import com.intellij.psi.xml.XmlTokenType
import kotlin.jvm.java


class CDataReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(XmlToken::class.java)
                .withElementType(XmlTokenType.XML_DATA_CHARACTERS),
            CDataReferenceProvider()
        )
    }
}