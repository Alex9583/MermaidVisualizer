package com.alextdev.mermaidvisualizer.editor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Publishes render errors to the editor only once they have "settled": an error is published
 * [delayMs] milliseconds after it was reported, provided that its render generation is still current,
 * that no newer result arrived in between and that the publisher is not disposed. Avoids flashing
 * warnings on every keystroke while a line is being typed.
 *
 * - [onError] cancels any pending publication, then publishes after [delayMs] (read at scheduling time;
 *   `0` publishes immediately, synchronously).
 * - [onSuccess] cancels the pending publication and calls [clear] right away.
 * - [cancelPending] (new render started, document changed) and [dispose] drop the pending publication.
 *
 * Not thread-safe: call every method from the same thread (the EDT in [MermaidPreviewFileEditor]).
 * The delayed [publish] runs in [scope] and re-checks [isCurrent] before publishing.
 */
internal class RenderErrorPublisher(
    private val scope: CoroutineScope,
    private val delayMs: () -> Long,
    private val isCurrent: (Long) -> Boolean,
    private val publish: (MermaidRenderError) -> Unit,
    private val clear: () -> Unit,
) {
    private var pendingJob: Job? = null

    @Volatile
    private var disposed = false

    /**
     * Schedules the publication of [error], reported for render [generation].
     * Returns the pending job (exposed for tests), or null when the error was published immediately or dropped.
     */
    fun onError(generation: Long, error: MermaidRenderError): Job? {
        cancelPending()
        if (disposed || !isCurrent(generation)) return null
        val settleMs = delayMs().coerceAtLeast(0L)
        if (settleMs == 0L) {
            publish(error)
            return null
        }
        val job = scope.launch {
            delay(settleMs.milliseconds)
            if (isActive && !disposed && isCurrent(generation)) publish(error)
        }
        pendingJob = job
        return job
    }

    /** A render succeeded: drops the pending error and clears an already-published one. */
    fun onSuccess() {
        cancelPending()
        if (!disposed) clear()
    }

    fun cancelPending() {
        pendingJob?.cancel()
        pendingJob = null
    }

    fun dispose() {
        disposed = true
        cancelPending()
    }
}
