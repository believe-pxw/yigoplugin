package com.github.believepxw.yigo.tool

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager

class ActivateYigoLayoutAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("Yigo Layout") ?: return
        
        if (toolWindow.isVisible) {
            toolWindow.hide(null)
        } else {
            toolWindow.activate(null)
        }
    }
}
