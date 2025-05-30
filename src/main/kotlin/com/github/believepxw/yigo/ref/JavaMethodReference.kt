package com.github.believepxw.yigo.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope

class JavaMethodReference(
    element: PsiElement,
    range: TextRange,
    private val fullMethod: String
) : PsiReferenceBase<PsiElement>(element, range), PsiPolyVariantReference {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val (className, methodName) = fullMethod.substringBeforeLast('.') to fullMethod.substringAfterLast('.')
        val project = myElement.project
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        val searchScope = GlobalSearchScope.projectScope(project)
        val psiClass = javaPsiFacade.findClass(className, searchScope) ?: return ResolveResult.EMPTY_ARRAY

        val methods = psiClass.findMethodsByName(methodName, true)
        return methods.map { PsiElementResolveResult(it) }.toTypedArray()
    }

    override fun resolve(): PsiElement? = multiResolve(false).firstOrNull()?.element
}