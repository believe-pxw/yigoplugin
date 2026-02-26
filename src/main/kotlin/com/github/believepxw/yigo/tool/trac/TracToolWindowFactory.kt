package com.github.believepxw.yigo.tool.trac

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class TracToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val tracBrowserPanel = TracBrowserPanel(project)
        val content = ContentFactory.getInstance().createContent(tracBrowserPanel.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}
