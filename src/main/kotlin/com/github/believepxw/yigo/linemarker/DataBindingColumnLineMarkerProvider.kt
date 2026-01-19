package com.github.believepxw.yigo.linemarker

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
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
        
        val builder = NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementedMethod)
            .setTargets(com.intellij.openapi.util.NotNullLazyValue.createValue {
                val references = ReferencesSearch.search(valueElement).findAll()
                references.filter { it is DataBindingColumnReference && !it.isDefinition }.map { it.element }
            })
            .setTooltipText("Navigate to DataBinding Usage")
            
        result.add(builder.createLineMarkerInfo(element.firstChild))
    }
}
