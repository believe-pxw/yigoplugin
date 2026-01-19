package com.github.believepxw.yigo.linemarker

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator
import com.intellij.codeInsight.hint.HintManager
import com.intellij.icons.AllIcons
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.xml.XmlTag
import example.ref.DataBindingColumnReference

class DataBindingColumnLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element !is XmlTag) {
            return
        }

        if (element.name != "Column") {
            return
        }

        val keyAttr = element.getAttribute("Key") ?: return
        val key = keyAttr.value
        if (key.isNullOrEmpty()) {
            return
        }
        
        // Find usages of the Column definition
        // The element refers to the tag itself, but refs usually target the attribute value or the tag.
        // In DataBindingColumnReference, resolve() returns the element itself (if isDefinition is true)
        // ReferencesSearch searches for references TO the element.
        // We want to find references TO the Column tag (or its Key attribute value).
        // Let's search for references to the 'Key' attribute value element if possible, or the tag.
        
        // Actually, DataBindingColumnReference logic:
        // resolve() returns getElement() if isDefinition=true. 
        // Wait, resolve() should return the TARGET. 
        // If I am a reference (usage), resolve() returns the definition (Column).
        // So passed `element` here is the DEFINITION (Column tag).
        // We want to find who REFERS to this `element`.
        
        // However, usually references target the XmlAttributeValue of the 'Key' attribute, not the XmlTag itself.
        // Let's check DataBindingColumnReference.resolve(). 
        // It calls findColumnInTable -> returns column.getAttribute("Key").getValueElement().
        // So the reference target is XmlAttributeValue of the Key attribute.
        
        val valueElement = keyAttr.valueElement ?: return
        
        val navigationHandler = GutterIconNavigationHandler<PsiElement> { e, elt ->
            val scope = GlobalSearchScope.getScopeRestrictedByFileTypes(
                GlobalSearchScope.projectScope(elt.project),
                XmlFileType.INSTANCE
            )
            val references = ReferencesSearch.search(valueElement, scope).findAll()
            val targets = references.filter { it is DataBindingColumnReference && !it.isDefinition }
                .map { it.element }
                .filterIsInstance<com.intellij.psi.NavigatablePsiElement>()

            if (targets.isEmpty()) {
                val editor = FileEditorManager.getInstance(elt.project).selectedTextEditor
                if (editor != null) {
                    HintManager.getInstance().showInformationHint(editor, "No DataBinding usages found")
                }
            } else {
                PsiElementListNavigator.openTargets(
                    e,
                    targets.toTypedArray(),
                    "DataBinding Usages",
                    null,
                    DefaultPsiElementCellRenderer()
                )
            }
        }

        result.add(RelatedItemLineMarkerInfo(
            element.firstChild,
            element.firstChild.textRange,
            AllIcons.Gutter.ImplementedMethod,
            { "Navigate to DataBinding Usage" },
            navigationHandler,
            com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.RIGHT,
            { emptyList() }
        ))
    }
}
