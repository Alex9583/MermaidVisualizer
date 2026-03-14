package com.alextdev.mermaidvisualizer.settings

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidSettingsConfigurableTest : BasePlatformTestCase() {

    private lateinit var configurable: MermaidSettingsConfigurable

    override fun setUp() {
        super.setUp()
        configurable = MermaidSettingsConfigurable()
        service<MermaidSettings>().loadState(MermaidSettings.State())
    }

    override fun tearDown() {
        try {
            configurable.disposeUIResources()
        } finally {
            super.tearDown()
        }
    }

    fun testDisplayNameIsNotEmpty() {
        val name = configurable.displayName
        assertTrue("Display name should not be blank", name.isNotBlank())
    }

    fun testCreateComponentReturnsNonNull() {
        val component = configurable.createComponent()
        assertNotNull("createComponent should return a JComponent", component)
    }

    fun testIsModifiedReturnsFalseInitially() {
        configurable.createComponent()
        assertFalse("Should not be modified right after creation", configurable.isModified)
    }

    fun testApplyPersistsState() {
        configurable.createComponent()
        val settings = service<MermaidSettings>()
        settings.loadState(MermaidSettings.State(theme = MermaidTheme.FOREST, maxTextSize = 50_000))
        configurable.reset()
        configurable.apply()
        assertEquals(MermaidTheme.FOREST, settings.state.theme)
        assertEquals(50_000, settings.state.maxTextSize)
    }

    fun testResetRestoresPersistedState() {
        configurable.createComponent()
        val settings = service<MermaidSettings>()
        settings.loadState(MermaidSettings.State(theme = MermaidTheme.NEUTRAL))
        configurable.reset()
        configurable.apply()
        assertEquals(MermaidTheme.NEUTRAL, settings.state.theme)
    }

    fun testApplyClampsInvalidMaxTextSize() {
        configurable.createComponent()
        val settings = service<MermaidSettings>()
        settings.loadState(MermaidSettings.State(maxTextSize = 500))
        configurable.reset()
        configurable.apply()
        assertEquals(1000, settings.state.maxTextSize)
    }

    fun testApplyClampsInvalidDebounceMs() {
        configurable.createComponent()
        val settings = service<MermaidSettings>()
        settings.loadState(MermaidSettings.State(debounceMs = 10_000))
        configurable.reset()
        configurable.apply()
        assertEquals(5000, settings.state.debounceMs)
    }
}
