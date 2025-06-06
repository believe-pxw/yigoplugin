package com.github.believepxw.yigo.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

class JavaMethodReference(
    element: PsiElement,
    range: TextRange,
    private val fullMethod: String
) : PsiReferenceBase<PsiElement>(element, range), PsiPolyVariantReference {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val project = myElement.project
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        val searchScope = GlobalSearchScope.projectScope(project)
        if (!fullMethod.contains(".")) {
            val shortNameClass = javaPsiFacade.findClass("com.bokesoft.erp.ShortNameFunction", searchScope)
                ?: return ResolveResult.EMPTY_ARRAY
            val methods = shortNameClass.findMethodsByName(fullMethod, true)
            return methods.map { PsiElementResolveResult(it) }.toTypedArray()
        } else {
            val (className, methodName) = fullMethod.substringBeforeLast('.') to fullMethod.substringAfterLast('.')
            val psiClass = javaPsiFacade.findClass(className, searchScope) ?: return ResolveResult.EMPTY_ARRAY
            val methods = psiClass.findMethodsByName(methodName, true)
            return methods.map { PsiElementResolveResult(it) }.toTypedArray()
        }
    }

    override fun resolve(): PsiElement? = multiResolve(false).firstOrNull()?.element

    override fun isSoft(): Boolean {
        return true
    }
}