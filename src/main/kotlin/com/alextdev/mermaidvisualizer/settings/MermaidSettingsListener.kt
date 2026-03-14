package com.alextdev.mermaidvisualizer.settings

import com.intellij.util.messages.Topic

internal val MERMAID_SETTINGS_TOPIC: Topic<MermaidSettingsListener> =
    Topic.create("MermaidSettingsChanged", MermaidSettingsListener::class.java)

internal fun interface MermaidSettingsListener {
    fun settingsChanged()
}
