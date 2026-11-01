package com.alextdev.mermaidvisualizer.welcome

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Remembers the last plugin version for which the welcome / what's new page was handled (shown, or skipped
 * because the page is disabled), so that it appears once per version. Application-level: the page is shown
 * in the first project opened after an update, not once per project. Same storage file as the settings,
 * distinct component name.
 */
@State(name = "MermaidVisualizerWelcome", storages = [Storage("MermaidVisualizer.xml")])
@Service(Service.Level.APP)
internal class MermaidWelcomeState : PersistentStateComponent<MermaidWelcomeState.State> {

    data class State(
        var lastShownVersion: String? = null,
    )

    private var myState = State()

    @Synchronized
    override fun getState(): State = myState

    @Synchronized
    override fun loadState(state: State) {
        myState = state.copy()
    }

    val lastShownVersion: String?
        @Synchronized get() = myState.lastShownVersion

    /**
     * Compare-and-set: records [version] and returns true only for the caller that actually changed the stored
     * value. When several projects open at once, exactly one of them gets true and shows the page.
     */
    @Synchronized
    fun markShown(version: String): Boolean {
        if (myState.lastShownVersion == version) return false
        myState.lastShownVersion = version
        return true
    }
}
