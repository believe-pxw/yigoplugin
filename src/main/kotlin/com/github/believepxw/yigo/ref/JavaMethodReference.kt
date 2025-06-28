package com.github.believepxw.yigo.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

class JavaMethodReference(
    element: PsiElement,
    range: TextRange,
    private val fullMethod: String
) : PsiReferenceBase<PsiElement>(element, range) {

    override fun resolve(): PsiElement?{
        val project = myElement.project
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        val searchScope = GlobalSearchScope.projectScope(project)
        if (!fullMethod.contains(".")) {
            val shortNameClass = javaPsiFacade.findClass("com.bokesoft.erp.ShortNameFunction", searchScope)
                ?: return null
            var methods = shortNameClass.findMethodsByName(fullMethod, true)
            if (methods.isEmpty()) {
                val onlyUIClass = javaPsiFacade.findClass("com.bokesoft.erp.OnlyInUIFunction", searchScope)
                    ?: return null
                methods = onlyUIClass.findMethodsByName(fullMethod, true)
            }
            if (methods.isEmpty()) {
                return null
            }
            return methods.first()
        } else {
            val (className, methodName) = fullMethod.substringBeforeLast('.') to fullMethod.substringAfterLast('.')
            val psiClass = javaPsiFacade.findClass(className, searchScope) ?: return null
            val methods = psiClass.findMethodsByName(methodName, true)
            if (methods.isEmpty()) {
                return null
            }
            return methods.first()
        }
    }

    override fun isSoft(): Boolean {
        return true
    }
}