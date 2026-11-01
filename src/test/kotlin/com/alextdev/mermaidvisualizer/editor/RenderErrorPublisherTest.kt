package com.alextdev.mermaidvisualizer.editor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Deterministic timing: a publication is awaited with `job.join()` (never with a sleep), "not published
 * yet" is only asserted against a 60 s delay, and short delays only bound the elapsed time from below.
 */
class RenderErrorPublisherTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val published = CopyOnWriteArrayList<MermaidRenderError>()
    private val clears = AtomicInteger()
    private val currentGeneration = AtomicLong(1)
    private val delayMs = AtomicLong(0)

    private val publisher = RenderErrorPublisher(
        scope = scope,
        delayMs = { delayMs.get() },
        isCurrent = { generation -> generation == currentGeneration.get() },
        publish = { published += it },
        clear = { clears.incrementAndGet() },
    )

    private val syntaxError = MermaidRenderError(
        "Parse error on line 2:", 3, 5,
        summary = "Syntax error at line 3: unexpected \"-->\"",
        category = MermaidRenderErrorCategory.SYNTAX,
    )
    private val incompleteError = MermaidRenderError(
        "Parse error on line 2:", 3, null,
        summary = "Line 3 looks unfinished",
        category = MermaidRenderErrorCategory.INCOMPLETE,
    )

    @AfterEach
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `zero delay publishes immediately`() {
        delayMs.set(0)
        val job = publisher.onError(1, syntaxError)
        assertNull(job, "Nothing is scheduled at 0 ms")
        assertEquals(listOf(syntaxError), published)
    }

    @Test
    fun `negative delay is treated as zero`() {
        delayMs.set(-50)
        assertNull(publisher.onError(1, syntaxError))
        assertEquals(listOf(syntaxError), published)
    }

    @Test
    fun `error is not published before the delay`() = runBlocking {
        delayMs.set(60_000)
        val job = publisher.onError(1, syntaxError)
        assertNotNull(job)
        assertTrue(job!!.isActive)
        assertTrue(published.isEmpty(), "Must wait for the settle delay")
        publisher.cancelPending()
        job.join()
        assertTrue(published.isEmpty())
    }

    @Test
    fun `error is published once the delay has elapsed`() = runBlocking {
        delayMs.set(150)
        val start = System.nanoTime()
        val job = publisher.onError(1, syntaxError)
        assertNotNull(job)
        job!!.join()
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
        assertFalse(job.isCancelled)
        assertTrue(elapsedMs >= 150, "Published after $elapsedMs ms, before the 150 ms delay")
        assertEquals(listOf(syntaxError), published)
        assertEquals(0, clears.get())
    }

    @Test
    fun `delay is read at scheduling time`() = runBlocking {
        delayMs.set(60_000)
        val slow = publisher.onError(1, syntaxError)
        delayMs.set(0)
        assertNull(publisher.onError(1, incompleteError), "The new delay applies to the next error")
        slow!!.join()
        assertTrue(slow.isCancelled, "The newer error cancels the pending one")
        assertEquals(listOf(incompleteError), published)
    }

    @Test
    fun `newer error cancels the pending one`() = runBlocking {
        delayMs.set(150)
        val first = publisher.onError(1, incompleteError)
        val second = publisher.onError(1, syntaxError)
        first!!.join()
        second!!.join()
        assertTrue(first.isCancelled)
        assertFalse(second.isCancelled)
        assertEquals(listOf(syntaxError), published, "Only the newest error is published")
    }

    @Test
    fun `success cancels the pending error and clears immediately`() = runBlocking {
        delayMs.set(150)
        val job = publisher.onError(1, syntaxError)
        publisher.onSuccess()
        assertEquals(1, clears.get(), "Success clears without waiting")
        job!!.join()
        assertTrue(job.isCancelled)
        assertTrue(published.isEmpty())
    }

    @Test
    fun `generation change before the delay drops the publication`() = runBlocking {
        delayMs.set(150)
        val job = publisher.onError(1, syntaxError)
        currentGeneration.set(2) // a new render started without cancelling explicitly
        job!!.join()
        assertFalse(job.isCancelled, "The job runs to completion but re-checks the generation")
        assertTrue(published.isEmpty())
    }

    @Test
    fun `stale generation is dropped at scheduling time`() {
        currentGeneration.set(2)
        delayMs.set(0)
        assertNull(publisher.onError(1, syntaxError))
        assertTrue(published.isEmpty())
    }

    @Test
    fun `cancelPending drops the publication`() = runBlocking {
        delayMs.set(150)
        val job = publisher.onError(1, syntaxError)
        publisher.cancelPending()
        job!!.join()
        assertTrue(job.isCancelled)
        assertTrue(published.isEmpty())
        assertEquals(0, clears.get(), "Cancelling does not clear an already-published error")
    }

    @Test
    fun `nothing is published after dispose`() = runBlocking {
        delayMs.set(150)
        val job = publisher.onError(1, syntaxError)
        publisher.dispose()
        job!!.join()
        assertTrue(job.isCancelled)

        delayMs.set(0)
        assertNull(publisher.onError(1, incompleteError), "Results arriving after dispose are ignored")
        publisher.onSuccess()
        assertTrue(published.isEmpty())
        assertEquals(0, clears.get())
    }
}
