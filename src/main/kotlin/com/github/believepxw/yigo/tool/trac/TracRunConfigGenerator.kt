package com.github.believepxw.yigo.tool.trac

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.spring.boot.run.SpringBootApplicationConfigurationType
import com.intellij.spring.boot.run.SpringBootApplicationRunConfiguration
import kotlin.jvm.java

object TracRunConfigGenerator {
    fun generate(project: Project, data: TracTicketData) {
        val runManager = RunManager.getInstance(project)
        val configType = ConfigurationTypeUtil.findConfigurationType(SpringBootApplicationConfigurationType::class.java)
        
        if (configType == null) {
            javax.swing.SwingUtilities.invokeLater {
                Messages.showErrorDialog("Spring Boot plugin is not available.", "Generate Failed")
            }
            return
        }
        
        val factory = configType.configurationFactories.firstOrNull()
        if (factory == null) return
        
        val settingsState = TracSettingsState.getInstance(project)
        val titleSafe = data.title.replace("[^a-zA-Z0-9.\\u4e00-\\u9fa5 -_]".toRegex(), "")
        val configName = "Ticket ${data.ticketId}: $titleSafe"
        
        var finalName = configName
        var counter = 1
        while (runManager.findConfigurationByName(finalName) != null) {
            finalName = "$configName ($counter)"
            counter++
        }

        val settings: RunnerAndConfigurationSettings = runManager.createConfiguration(finalName, factory)
//        settings.storeInLocalWorkspace()
        val config = settings.configuration as? SpringBootApplicationRunConfiguration ?: return

        if (settingsState.defaultMainClass.isNotBlank()) {
            config.setMainClassName(settingsState.defaultMainClass)
            com.intellij.openapi.application.ReadAction.run<Exception> {
                val javaPsiFacade = JavaPsiFacade.getInstance(project)
                val searchScope = GlobalSearchScope.projectScope(project)
                val mainClass = javaPsiFacade.findClass(settingsState.defaultMainClass, searchScope)
                if (mainClass != null) {
                    val module = ModuleUtilCore.findModuleForFile(mainClass.containingFile)
                    if (module != null) {
                        config.setModuleName(module.name)
                    }
                }
            }
        }
        
        val envs = mutableMapOf<String, String>()
        if (data.envVars != null) {
            envs.putAll(data.envVars!!)
        }
        
        if (settingsState.defaultEnvVars.isNotBlank()) {
            val parts = settingsState.defaultEnvVars.split(";")
            for (p in parts) {
                if (p.contains("=")) {
                    val kv = p.split("=", limit = 2)
                    envs[kv[0].trim()] = kv[1].trim()
                }
            }
        }
        
        config.envs = envs
        config.isPassParentEnvs = true


        javax.swing.SwingUtilities.invokeLater {
            runManager.addConfiguration(settings)
            runManager.selectedConfiguration = settings
            Messages.showInfoMessage("Successfully created Run Configuration: $finalName", "Success")
        }
    }
}
