package com.alextdev.mermaidvisualizer

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import java.net.URI
import java.net.URISyntaxException

private val LOG = Logger.getInstance("MermaidLinkOpener")

private const val MAX_URL_LENGTH = 2000

private val ALLOWED_SCHEMES = setOf("http", "https")

/**
 * Only absolute http(s) URLs with a host may be opened: the JS→Kotlin channels are
 * callable by any script in the preview page, so `file:`, `javascript:`, `jetbrains:`
 * (IDE protocol handler) and other OS schemes must never reach the OS.
 * [maxLength] may only be raised for URLs built by the plugin itself (e.g. the prefilled GitHub issue).
 */
internal fun isSafeExternalUrl(url: String, maxLength: Int = MAX_URL_LENGTH): Boolean {
    val trimmed = url.trim()
    if (trimmed.isEmpty() || trimmed.length > maxLength) return false
    val uri = try {
        URI(trimmed)
    } catch (e: URISyntaxException) {
        return false
    }
    val scheme = uri.scheme?.lowercase() ?: return false
    return scheme in ALLOWED_SCHEMES && uri.host != null
}

/** Opens a validated link in the system browser. Safe to call from any thread. */
internal fun openExternalLink(url: String, maxLength: Int = MAX_URL_LENGTH) {
    if (!isSafeExternalUrl(url, maxLength)) {
        LOG.warn("Blocked opening non-http(s) link from Mermaid preview: '${url.take(200)}'")
        return
    }
    ApplicationManager.getApplication().invokeLater {
        BrowserUtil.browse(url.trim())
    }
}
