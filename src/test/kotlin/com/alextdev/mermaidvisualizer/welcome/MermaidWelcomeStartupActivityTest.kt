package com.alextdev.mermaidvisualizer.welcome

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking

private const val ACTION_ID = "MermaidVisualizer.ShowWhatsNew"

class MermaidWelcomeStartupActivityTest : BasePlatformTestCase() {

    private lateinit var savedState: MermaidWelcomeState.State

    override fun setUp() {
        super.setUp()
        savedState = service<MermaidWelcomeState>().state.copy()
    }

    override fun tearDown() {
        try {
            service<MermaidWelcomeState>().loadState(savedState)
        } finally {
            super.tearDown()
        }
    }

    fun testActivityIsInertInUnitTestMode() {
        val state = service<MermaidWelcomeState>()
        state.loadState(MermaidWelcomeState.State(lastShownVersion = "0.0.1"))
        // Returns before any suspension point in unit-test mode, so runBlocking completes at once on the EDT.
        runBlocking { MermaidWelcomeStartupActivity().execute(project) }
        assertEquals("0.0.1", state.lastShownVersion)
        assertEquals(0, FileEditorManager.getInstance(project).openFiles.size)
    }

    fun testActivityIsRegisteredAsPostStartupActivity() {
        val activities = ExtensionPointName<ProjectActivity>("com.intellij.postStartupActivity").extensionList
        assertTrue(activities.any { it is MermaidWelcomeStartupActivity })
    }

    fun testStateServiceIsRegistered() {
        assertNotNull(service<MermaidWelcomeState>())
    }

    fun testActionTextResolvesFromBundle() {
        val action = ActionManager.getInstance().getAction(ACTION_ID)
        assertInstanceOf(action, MermaidShowWelcomePageAction::class.java)
        assertEquals("Mermaid Visualizer: What's New", action.templatePresentation.text)
        assertFalse(action.templatePresentation.description.isNullOrBlank())
    }

    fun testActionIsInHelpMenu() {
        val actionManager = ActionManager.getInstance()
        val helpMenu = actionManager.getAction("HelpMenu") as DefaultActionGroup
        assertTrue(helpMenu.childActionsOrStubs.any { actionManager.getId(it) == ACTION_ID })
    }

    fun testActionEnabledOnlyWithProject() {
        val action = MermaidShowWelcomePageAction()
        val withProject = TestActionEvent.createTestEvent(action, DataContext { id ->
            if (id == CommonDataKeys.PROJECT.name) project else null
        })
        action.update(withProject)
        assertTrue(withProject.presentation.isEnabledAndVisible)

        val withoutProject = TestActionEvent.createTestEvent(action, DataContext { null })
        action.update(withoutProject)
        assertFalse(withoutProject.presentation.isEnabledAndVisible)
    }

    fun testCurrentPluginVersionIsKnownInTests() {
        assertFalse(currentPluginVersion().isNullOrBlank())
    }
}
