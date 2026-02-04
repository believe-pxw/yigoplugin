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

        project.messageBus.connect().subscribe(com.intellij.openapi.wm.ex.ToolWindowManagerListener.TOPIC, object : com.intellij.openapi.wm.ex.ToolWindowManagerListener {
            var expanded = false
            var originalWidth = 400
            var previousVisibleIds: Set<String>? = null

            override fun stateChanged(toolWindowManager: com.intellij.openapi.wm.ToolWindowManager) {
                val myWindow = toolWindowManager.getToolWindow(toolWindow.id) ?: return

                if (myWindow.isVisible) {
                    if (!expanded) {
                        try {
                            val screenSize = java.awt.Toolkit.getDefaultToolkit().screenSize
                            val targetWidth = screenSize.width / 2
                            val currentWidth = myWindow.component.width

                            if (currentWidth < targetWidth - 20) {
                                originalWidth = currentWidth
                            }

                            if (currentWidth < targetWidth) {
                                (myWindow as? com.intellij.openapi.wm.ex.ToolWindowEx)?.stretchWidth(targetWidth - currentWidth)
                            }
                            expanded = true
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    if (previousVisibleIds == null) {
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
                    if (expanded) {
                        try {
                            // Find any visible tool window on the same anchor
                            val sibling = toolWindowManager.toolWindowIds
                                .mapNotNull { toolWindowManager.getToolWindow(it) }
                                .firstOrNull { it.isVisible && it.anchor == myWindow.anchor && it.id != myWindow.id }

                            if (sibling != null) {
                                val current = sibling.component.width
                                val target = if (originalWidth > 100) originalWidth else 400
                                (sibling as? com.intellij.openapi.wm.ex.ToolWindowEx)?.stretchWidth(target - current)
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                        expanded = false
                        // If my window is hidden (closed/minimized) and we have state, restore
                        if (previousVisibleIds != null) {
                            previousVisibleIds?.forEach { id ->
                                toolWindowManager.getToolWindow(id)?.show(null)
                            }
                            previousVisibleIds = null
                        }
                    }
                }
            }
        })
    }
}
