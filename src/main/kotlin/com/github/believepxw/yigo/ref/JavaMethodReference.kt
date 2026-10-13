package com.github.believepxw.yigo.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

class JavaMethodReference(
    element: PsiElement,
    range: TextRange,
    private val fullMethod: String
) : PsiReferenceBase<PsiElement>(element, range) {

    override fun resolve(): PsiElement? {
        val project = myElement.project
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        val projectScope = GlobalSearchScope.projectScope(project)
        val allScope = GlobalSearchScope.allScope(project)

        fun findPsiClass(fqn: String): PsiClass? {
            return javaPsiFacade.findClass(fqn, projectScope)
                ?: javaPsiFacade.findClass(fqn, allScope)
        }

        if (!fullMethod.contains(".")) {
            val shortNameClass = findPsiClass("com.bokesoft.erp.ShortNameFunction")
            var methods = shortNameClass?.findMethodsByName(fullMethod, true) ?: emptyArray()
            if (methods.isEmpty()) {
                val onlyUIClass = findPsiClass("com.bokesoft.erp.OnlyInUIFunction")
                if (onlyUIClass != null) {
                    methods = onlyUIClass.findMethodsByName(fullMethod, true)
                }
            }
            return methods.firstOrNull()
        } else {
            val className = fullMethod.substringBeforeLast('.')
            val methodName = fullMethod.substringAfterLast('.')
            val psiClass = findPsiClass(className) ?: return null
            val methods = psiClass.findMethodsByName(methodName, true)
            return methods.firstOrNull()
        }
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return myElement
    }

    override fun getVariants(): Array<Any> {
        return emptyArray()
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        val resolved = resolve()
        return element.manager.areElementsEquivalent(resolved, element)
    }

    override fun isSoft(): Boolean {
        return true
    }
}