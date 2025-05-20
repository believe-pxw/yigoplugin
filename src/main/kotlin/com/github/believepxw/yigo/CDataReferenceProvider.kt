package com.github.believepxw.yigo

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext

class CDataReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        println("✅ getReferencesByElement() triggered on: ${element.text}")
        val text = element.text

        val references = mutableListOf<PsiReference>()

        // 正则提取 Java 方法全限定名
        val methodRegex = Regex("""([\w.]+\.\w+)\(""")
        methodRegex.findAll(text).forEach { match ->
            val fullMethod = match.groupValues[1]
            val startOffset = text.indexOf(fullMethod)
            references += JavaMethodReference(element,
                TextRange(startOffset, startOffset + fullMethod.length), fullMethod)
        }

        // 宏名（以Macro_开头的变量或方法）
        val macroRegex = Regex("""\b(Macro_\w+)\b""")
        macroRegex.findAll(text).forEach { match ->
            val macroName = match.value
            val startOffset = text.indexOf(macroName)
            references += MacroReference(element, TextRange(startOffset, startOffset + macroName.length), macroName)
        }

        return references.toTypedArray()
    }
}