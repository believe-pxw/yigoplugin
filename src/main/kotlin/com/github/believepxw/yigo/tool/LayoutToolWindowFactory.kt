package com.github.believepxw.yigo.tool

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class LayoutToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = YigoLayoutPanel(project, toolWindow)
        val content = ContentFactory.getInstance().createContent(toolWindowContent, "", false)
        toolWindow.contentManager.addContent(content)
        toolWindow.setType(com.intellij.openapi.wm.ToolWindowType.DOCKED, null)
        // Focus Mode Logic: Hide other tool windows when this one is active
        project.messageBus.connect().subscribe(com.intellij.openapi.wm.ex.ToolWindowManagerListener.TOPIC, object : com.intellij.openapi.wm.ex.ToolWindowManagerListener {
            var previousVisibleIds: Set<String>? = null

            override fun stateChanged(toolWindowManager: com.intellij.openapi.wm.ToolWindowManager) {
                // Check if our window is visible
                val myWindow = toolWindowManager.getToolWindow(toolWindow.id) ?: return

                if (myWindow.isVisible) {
                    // If we haven't saved state yet, save and hide others
                    if (previousVisibleIds == null) {
                        try {
                            // Hack: Force width to half screen when opening
                             val width = java.awt.Toolkit.getDefaultToolkit().screenSize.width / 2
                             val current = myWindow.component.width
                             if (current < width) {
                                 (myWindow as? com.intellij.openapi.wm.ex.ToolWindowEx)?.stretchWidth(width - current)
                             }
                        } catch (e: Exception) { /* Ignore resize errors */ }

                        val idsToHide = toolWindowManager.toolWindowIds
                            .filter { it != toolWindow.id && toolWindowManager.getToolWindow(it)?.isVisible == true }
                            .toSet()

                        if (idsToHide.isNotEmpty()) {
                            previousVisibleIds = idsToHide
                            idsToHide.forEach { id ->
                                toolWindowManager.getToolWindow(id)?.hide(null)
                            }
                        } else {
                            // Mark as active even if nothing to hide, to ensure we catch the hide event later
                            previousVisibleIds = emptySet()
                        }
                    }
                } else {
                    // If my window is hidden (closed/minimized) and we have state, restore
                    if (previousVisibleIds != null) {
                         try {
                            // Hack: Restore smaller width (e.g. 300) so other windows don't look huge
                             val current = myWindow.component.width
                             if (current > 350) {
                                 (myWindow as? com.intellij.openapi.wm.ex.ToolWindowEx)?.stretchWidth(300 - current)
                             }
                        } catch (e: Exception) { /* Ignore resize errors */ }

                        previousVisibleIds?.forEach { id ->
                             toolWindowManager.getToolWindow(id)?.show(null)
                        }
                        previousVisibleIds = null
                    }
                }
            }
        })
    }
}
