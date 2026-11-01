package com.alextdev.mermaidvisualizer.welcome

import com.alextdev.mermaidvisualizer.settings.MermaidSettings
import com.intellij.ide.lightEdit.LightEdit
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginAware
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val LOG = Logger.getInstance("MermaidWelcomeStartupActivity")

/**
 * Opens the welcome / what's new page once per plugin version (first install included), at the first project
 * opening that follows the install or update. The version is recorded before anything is opened, atomically,
 * so several projects opening at once show the page only once; it is recorded even when the page is disabled.
 */
internal class MermaidWelcomeStartupActivity : ProjectActivity, PluginAware {

    /** Set by the platform when it instantiates this extension (see [currentPluginVersion]). */
    @Volatile
    var pluginVersion: String? = null
        private set

    override fun setPluginDescriptor(pluginDescriptor: PluginDescriptor) {
        pluginVersion = pluginDescriptor.version?.trim()?.ifEmpty { null }
    }

    override suspend fun execute(project: Project) {
        val application = ApplicationManager.getApplication()
        if (application.isUnitTestMode || application.isHeadlessEnvironment) return
        if (project.isDisposed || LightEdit.owns(project)) return

        val version = pluginVersion
        val enabled = service<MermaidSettings>().state.showWelcomePage
        val outcome = recordWelcomeForVersion(service<MermaidWelcomeState>(), version, enabled)
        LOG.debug("Welcome page for plugin version $version (enabled=$enabled): $outcome")
        if (outcome !is WelcomeOutcome.Show) return

        // Non-modal: never pop the tab over a dialog (e.g. the trust-project prompt); wait until it is closed.
        withContext(Dispatchers.EDT + ModalityState.nonModal().asContextElement()) {
            if (!project.isDisposed) {
                openWelcomePage(project, outcome.version, outcome.previousShown)
            }
        }
    }
}
