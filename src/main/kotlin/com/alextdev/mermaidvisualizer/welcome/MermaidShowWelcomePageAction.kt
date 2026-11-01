package com.alextdev.mermaidvisualizer.welcome

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

/**
 * Help > Mermaid Visualizer: What's New. Opens the page on demand, whatever the setting and the stored version
 * (it does not record anything), with the release notes of the installed version only.
 */
internal class MermaidShowWelcomePageAction : DumbAwareAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val version = currentPluginVersion() ?: MermaidChangelog.loadBundled().latest?.version.orEmpty()
        openWelcomePage(project, version, previousShown = null)
    }
}
