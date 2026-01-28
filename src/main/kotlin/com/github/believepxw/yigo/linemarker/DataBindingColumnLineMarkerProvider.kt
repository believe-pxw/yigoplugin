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
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.containers.ContainerUtil.filterIsInstance
import example.ref.DataBindingColumnReference

class DataBindingColumnLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element !is XmlTag) {
            return
        }

        when (element.name) {
            "Column" -> {
                val keyAttr = element.getAttribute("Key") ?: return
                val key = keyAttr.value
                if (key.isNullOrEmpty()) {
                    return
                }

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
                    AllIcons.Chooser.Left,
                    { "Navigate to DataBinding Usage" },
                    navigationHandler,
                    com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.RIGHT,
                    { emptyList() }
                ))
            }
            "GridCell" -> {
                val keyAttr = element.getAttribute("Key") ?: return
                val key = keyAttr.value
                if (key.isNullOrEmpty()) {
                    return
                }

                val navigationHandler = GutterIconNavigationHandler<PsiElement> { e, elt ->
                    val rootTag = (element.containingFile as? XmlFile)?.document?.rootTag
                    val targets = listOf(findTagByKeyRecursive(rootTag, "GridColumn", key)).filterIsInstance<com.intellij.psi.NavigatablePsiElement>()
                    if (targets.isEmpty()) {
                        val editor = FileEditorManager.getInstance(elt.project).selectedTextEditor
                        if (editor != null) {
                            HintManager.getInstance().showInformationHint(editor, "No GridColumn found")
                        }
                    } else {
                        PsiElementListNavigator.openTargets(
                            e,
                            targets.toTypedArray(),
                            "Jump to GridColumn",
                            null,
                            DefaultPsiElementCellRenderer()
                        )
                    }
                }

                result.add(RelatedItemLineMarkerInfo(
                    element.firstChild,
                    element.firstChild.textRange,
                    AllIcons.Chooser.Top,
                    { "Navigate to GridColumn" },
                    navigationHandler,
                    com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.RIGHT,
                    { emptyList() }
                ))
            }
            "GridColumn" -> {
                val keyAttr = element.getAttribute("Key") ?: return
                val key = keyAttr.value
                if (key.isNullOrEmpty()) {
                    return
                }

                val navigationHandler = GutterIconNavigationHandler<PsiElement> { e, elt ->
                    val rootTag = (element.containingFile as? XmlFile)?.document?.rootTag
                    val targets = listOf(findTagByKeyRecursive(rootTag, "GridCell", key)).filterIsInstance<com.intellij.psi.NavigatablePsiElement>()
                    if (targets.isEmpty()) {
                        val editor = FileEditorManager.getInstance(elt.project).selectedTextEditor
                        if (editor != null) {
                            HintManager.getInstance().showInformationHint(editor, "No GridCell found")
                        }
                    } else {
                        PsiElementListNavigator.openTargets(
                            e,
                            targets.toTypedArray(),
                            "Jump to GridCell",
                            null,
                            DefaultPsiElementCellRenderer()
                        )
                    }
                }

                result.add(RelatedItemLineMarkerInfo(
                    element.firstChild,
                    element.firstChild.textRange,
                    AllIcons.Chooser.Bottom,
                    { "Navigate to GridCell" },
                    navigationHandler,
                    com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.RIGHT,
                    { emptyList() }
                ))
            }
            else -> return
        }
    }


    private fun findTagByKeyRecursive(currentTag: XmlTag?, tagName: String, key: String): PsiElement? {
        if (currentTag == null) {
            return null
        }
        if (currentTag.localName == tagName && currentTag.getAttributeValue("Key") == key) {
            return currentTag.getAttribute("Key")?.lastChild
        }
        for (subTag in currentTag.subTags) {
            val found = findTagByKeyRecursive(subTag, tagName, key)
            if (found != null) return found
        }
        return null
    }
}
