package com.github.believepxw.yigo.tool.trac

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.github.believepxw.yigo.tool.trac.TracGlobalSettingsState",
    storages = [Storage("TracGlobalSettings.xml")]
)
class TracGlobalSettingsState : PersistentStateComponent<TracGlobalSettingsState> {
    var tracUsername: String = ""
    var tracPassword: String = ""
    var serializedCookies: String = ""

    override fun getState(): TracGlobalSettingsState = this
    override fun loadState(state: TracGlobalSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): TracGlobalSettingsState {
            return ApplicationManager.getApplication().getService(TracGlobalSettingsState::class.java)
        }
    }
}
