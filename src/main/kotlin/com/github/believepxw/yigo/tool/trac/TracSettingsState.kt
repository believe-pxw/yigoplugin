package com.github.believepxw.yigo.tool.trac

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.github.believepxw.yigo.tool.trac.TracSettingsState",
    storages = [Storage("TracIntegrationSettings.xml")]
)
class TracSettingsState : PersistentStateComponent<TracSettingsState> {
    var defaultMainClass: String = "com.bokesoft.erp.all.StartYigoERP"
    var defaultEnvVars: String = "CALCSCOPE_IMMEDIATELY=false;AUTHORITY_ENABLE=true;PASSWORD_DURATION=10000" // key=value;key2=value2 format
    var tracUsername: String = ""
    var tracPassword: String = ""
    var serializedCookies: String = ""

    override fun getState(): TracSettingsState = this
    override fun loadState(state: TracSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(project: Project): TracSettingsState {
            return project.getService(TracSettingsState::class.java)
        }
    }
}
