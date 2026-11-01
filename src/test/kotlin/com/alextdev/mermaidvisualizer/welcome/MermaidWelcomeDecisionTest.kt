package com.alextdev.mermaidvisualizer.welcome

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MermaidWelcomeDecisionTest {

    @Test
    fun `first install shows the page`() {
        assertTrue(MermaidWelcomeDecision.shouldShow(lastShown = null, current = "1.14.0", enabled = true))
    }

    @Test
    fun `update shows the page`() {
        assertTrue(MermaidWelcomeDecision.shouldShow(lastShown = "1.13.1", current = "1.14.0", enabled = true))
    }

    @Test
    fun `downgrade shows the page`() {
        assertTrue(MermaidWelcomeDecision.shouldShow(lastShown = "1.14.0", current = "1.13.1", enabled = true))
    }

    @Test
    fun `same version does not show the page`() {
        assertFalse(MermaidWelcomeDecision.shouldShow(lastShown = "1.14.0", current = "1.14.0", enabled = true))
        assertFalse(MermaidWelcomeDecision.shouldShow(lastShown = " 1.14.0", current = "1.14.0 ", enabled = true))
    }

    @Test
    fun `unknown or blank current version never shows the page`() {
        assertFalse(MermaidWelcomeDecision.shouldShow(lastShown = null, current = null, enabled = true))
        assertFalse(MermaidWelcomeDecision.shouldShow(lastShown = null, current = "", enabled = true))
        assertFalse(MermaidWelcomeDecision.shouldShow(lastShown = "1.13.1", current = "   ", enabled = true))
    }

    @Test
    fun `disabled page is never shown`() {
        assertFalse(MermaidWelcomeDecision.shouldShow(lastShown = null, current = "1.14.0", enabled = false))
        assertFalse(MermaidWelcomeDecision.shouldShow(lastShown = "1.13.1", current = "1.14.0", enabled = false))
    }

    @Test
    fun `isNewVersion ignores the enabled flag`() {
        assertTrue(MermaidWelcomeDecision.isNewVersion(null, "1.14.0"))
        assertFalse(MermaidWelcomeDecision.isNewVersion("1.14.0", "1.14.0"))
    }

    // --- recordWelcomeForVersion: the startup activity flow ---

    @Test
    fun `first install records the version and shows the current version only`() {
        val state = MermaidWelcomeState()
        assertEquals(WelcomeOutcome.Show("1.14.0", null), recordWelcomeForVersion(state, "1.14.0", enabled = true))
        assertEquals("1.14.0", state.lastShownVersion)
    }

    @Test
    fun `update shows the changes since the previously shown version`() {
        val state = MermaidWelcomeState().apply { loadState(MermaidWelcomeState.State("1.12.0")) }
        assertEquals(WelcomeOutcome.Show("1.14.0", "1.12.0"), recordWelcomeForVersion(state, "1.14.0", enabled = true))
        assertEquals("1.14.0", state.lastShownVersion)
    }

    @Test
    fun `second project opening of the same version is skipped`() {
        val state = MermaidWelcomeState()
        recordWelcomeForVersion(state, "1.14.0", enabled = true)
        assertEquals(WelcomeOutcome.Skip, recordWelcomeForVersion(state, "1.14.0", enabled = true))
    }

    @Test
    fun `disabled page still records the version so enabling it later shows nothing stale`() {
        val state = MermaidWelcomeState().apply { loadState(MermaidWelcomeState.State("1.13.1")) }
        assertEquals(WelcomeOutcome.RecordOnly, recordWelcomeForVersion(state, "1.14.0", enabled = false))
        assertEquals("1.14.0", state.lastShownVersion)
        assertEquals(WelcomeOutcome.Skip, recordWelcomeForVersion(state, "1.14.0", enabled = true))
    }

    @Test
    fun `unknown version is skipped without touching the state`() {
        val state = MermaidWelcomeState().apply { loadState(MermaidWelcomeState.State("1.13.1")) }
        assertEquals(WelcomeOutcome.Skip, recordWelcomeForVersion(state, null, enabled = true))
        assertEquals(WelcomeOutcome.Skip, recordWelcomeForVersion(state, " ", enabled = true))
        assertEquals("1.13.1", state.lastShownVersion)
    }

    @Test
    fun `version is recorded trimmed`() {
        val state = MermaidWelcomeState()
        assertEquals(WelcomeOutcome.Show("1.14.0", null), recordWelcomeForVersion(state, " 1.14.0 ", enabled = true))
        assertEquals("1.14.0", state.lastShownVersion)
    }
}
