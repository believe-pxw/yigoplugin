package com.github.believepxw.yigo

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

class MacroReference(
    element: PsiElement,
    range: TextRange,
    private val macroName: String
) : PsiReferenceBase<PsiElement>(element, range) {

    override fun resolve(): PsiElement? {
        val file = element.containingFile as? XmlFile ?: return null
        val macros = PsiTreeUtil.findChildrenOfType(file, XmlTag::class.java)
            .filter { it.name == "Macro" && it.getAttributeValue("Key") == macroName }
        return macros.firstOrNull()
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
