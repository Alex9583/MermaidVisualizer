package com.alextdev.mermaidvisualizer.welcome

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MermaidWelcomeStateTest {

    @Test
    fun `default state has no version`() {
        assertNull(MermaidWelcomeState().lastShownVersion)
        assertNull(MermaidWelcomeState().state.lastShownVersion)
    }

    @Test
    fun `markShown from nothing transitions`() {
        val state = MermaidWelcomeState()
        assertTrue(state.markShown("1.14.0"))
        assertEquals("1.14.0", state.lastShownVersion)
    }

    @Test
    fun `markShown twice with the same version returns false the second time`() {
        val state = MermaidWelcomeState()
        assertTrue(state.markShown("1.14.0"))
        assertFalse(state.markShown("1.14.0"))
        assertEquals("1.14.0", state.lastShownVersion)
    }

    @Test
    fun `markShown with another version transitions again`() {
        val state = MermaidWelcomeState()
        state.markShown("1.13.1")
        assertTrue(state.markShown("1.14.0"))
        assertEquals("1.14.0", state.lastShownVersion)
    }

    @Test
    fun `loadState restores the stored version and getState exposes it`() {
        val state = MermaidWelcomeState()
        state.loadState(MermaidWelcomeState.State(lastShownVersion = "1.12.0"))
        assertEquals("1.12.0", state.lastShownVersion)
        assertFalse(state.markShown("1.12.0"))
        state.markShown("1.14.0")
        assertEquals("1.14.0", state.state.lastShownVersion, "Persisted state follows markShown")
    }

    @Test
    fun `loadState copies the incoming state`() {
        val incoming = MermaidWelcomeState.State(lastShownVersion = "1.12.0")
        val state = MermaidWelcomeState()
        state.loadState(incoming)
        state.markShown("1.14.0")
        assertEquals("1.12.0", incoming.lastShownVersion)
    }

    @Test
    fun `concurrent markShown lets exactly one caller win`() {
        val state = MermaidWelcomeState()
        val threads = 16
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val winners = AtomicInteger()
        try {
            repeat(threads) {
                pool.submit {
                    start.await()
                    if (state.markShown("1.14.0")) winners.incrementAndGet()
                }
            }
            start.countDown()
        } finally {
            pool.shutdown()
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS))
        }
        assertEquals(1, winners.get())
    }
}
