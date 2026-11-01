package com.alextdev.mermaidvisualizer

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MermaidLinkOpenerTest {

    @Test
    fun `accepts http url with host`() {
        assertTrue(isSafeExternalUrl("http://example.com/path?q=1"))
    }

    @Test
    fun `accepts https url with host`() {
        assertTrue(isSafeExternalUrl("https://google.com"))
    }

    @Test
    fun `accepts mixed case scheme`() {
        assertTrue(isSafeExternalUrl("HTTPS://Example.COM/Page"))
    }

    @Test
    fun `accepts url with surrounding whitespace`() {
        assertTrue(isSafeExternalUrl("  https://example.com  "))
    }

    @Test
    fun `rejects file scheme`() {
        assertFalse(isSafeExternalUrl("file:///etc/hosts"))
    }

    @Test
    fun `rejects javascript scheme`() {
        assertFalse(isSafeExternalUrl("javascript:alert(1)"))
    }

    @Test
    fun `rejects data scheme`() {
        assertFalse(isSafeExternalUrl("data:text/html,<script>alert(1)</script>"))
    }

    @Test
    fun `rejects jetbrains ide protocol scheme`() {
        assertFalse(isSafeExternalUrl("jetbrains://idea/settings"))
    }

    @Test
    fun `rejects mailto scheme`() {
        assertFalse(isSafeExternalUrl("mailto:someone@example.com?bcc=evil@example.com"))
    }

    @Test
    fun `rejects scheme-relative url`() {
        assertFalse(isSafeExternalUrl("//evil.com/path"))
    }

    @Test
    fun `rejects opaque https url without host`() {
        assertFalse(isSafeExternalUrl("https:opaque-junk"))
    }

    @Test
    fun `rejects http url without host`() {
        assertFalse(isSafeExternalUrl("http://"))
    }

    @Test
    fun `rejects relative url`() {
        assertFalse(isSafeExternalUrl("some/relative/path"))
    }

    @Test
    fun `rejects empty and blank urls`() {
        assertFalse(isSafeExternalUrl(""))
        assertFalse(isSafeExternalUrl("   "))
    }

    @Test
    fun `rejects overlong url`() {
        assertFalse(isSafeExternalUrl("https://example.com/" + "a".repeat(2000)))
    }

    @Test
    fun `raised length limit still validates the scheme`() {
        val longUrl = "https://example.com/" + "a".repeat(3000)
        assertTrue(isSafeExternalUrl(longUrl, maxLength = 6000))
        assertFalse(isSafeExternalUrl(longUrl + "a".repeat(3000), maxLength = 6000))
        assertFalse(isSafeExternalUrl("file:///" + "a".repeat(3000), maxLength = 6000))
    }

    @Test
    fun `rejects unparseable url`() {
        assertFalse(isSafeExternalUrl("https://exa mple.com"))
    }
}
