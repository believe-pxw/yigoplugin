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

        // Focus Mode Logic: Hide other tool windows when this one is active
        project.messageBus.connect().subscribe(com.intellij.openapi.wm.ex.ToolWindowManagerListener.TOPIC, object : com.intellij.openapi.wm.ex.ToolWindowManagerListener {
            var previousVisibleIds: Set<String>? = null
            
            override fun stateChanged(toolWindowManager: com.intellij.openapi.wm.ToolWindowManager) {
                // Check if our window is visible
                val myWindow = toolWindowManager.getToolWindow(toolWindow.id) ?: return
                
                if (myWindow.isVisible) {
                    // If we haven't saved state yet, save and hide others
                    if (previousVisibleIds == null) {
                        val idsToHide = toolWindowManager.toolWindowIds
                            .filter { it != toolWindow.id && toolWindowManager.getToolWindow(it)?.isVisible == true }
                            .toSet()
                        
                        if (idsToHide.isNotEmpty()) {
                            previousVisibleIds = idsToHide
                            // Execute in strict dispatch thread if needed (stateChanged is usually on EDT)
                            idsToHide.forEach { id ->
                                toolWindowManager.getToolWindow(id)?.hide(null)
                            }
                        }
                    }
                } else {
                    // If my window is hidden (closed/minimized) and we have state, restore
                    previousVisibleIds?.let { ids ->
                        ids.forEach { id -> 
                             toolWindowManager.getToolWindow(id)?.show(null)
                        }
                        previousVisibleIds = null
                    }
                }
            }
        })
    }
}
