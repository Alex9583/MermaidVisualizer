/** Module `text`: string helpers — base64 UTF-8 for the Kotlin bridges, message trimming, same-diagram heuristic. No DOM. */
'use strict';

window.__mermaidCore.define('text', [], function () {

    function utf8ToBase64(str) {
        const bytes = new TextEncoder().encode(str);
        let binary = '';
        for (let i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
        return btoa(binary);
    }

    function base64ToUtf8(b64) {
        const binary = atob(b64);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        return new TextDecoder('utf-8').decode(bytes);
    }

    function firstLine(text) {
        if (typeof text !== 'string') return '';
        const lines = text.split(/\r\n?|\n/);
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i].trim();
            if (line) return line;
        }
        return '';
    }

    function truncate(text, max) {
        return text.length > max ? text.slice(0, max - 1) + '\u2026' : text;
    }

    function truncateStart(text, max) {
        return text.length > max ? '\u2026' + text.slice(text.length - max + 1) : text;
    }

    // Share of the shorter source that must match at start + end to count as the same diagram being edited:
    // typing changes one spot, another diagram shares a header line at best.
    const SAME_DIAGRAM_MIN_SHARED = 0.5;

    function looksLikeSameDiagram(previousSource, source) {
        if (typeof previousSource !== 'string' || typeof source !== 'string') return false;
        const shorter = Math.min(previousSource.length, source.length);
        if (shorter === 0) return false;
        let prefix = 0;
        while (prefix < shorter && previousSource.charCodeAt(prefix) === source.charCodeAt(prefix)) prefix++;
        let suffix = 0;
        while (suffix < shorter - prefix &&
            previousSource.charCodeAt(previousSource.length - 1 - suffix) === source.charCodeAt(source.length - 1 - suffix)) {
            suffix++;
        }
        return prefix + suffix >= shorter * SAME_DIAGRAM_MIN_SHARED;
    }

    return {
        utf8ToBase64: utf8ToBase64,
        base64ToUtf8: base64ToUtf8,
        firstLine: firstLine,
        truncate: truncate,
        truncateStart: truncateStart,
        looksLikeSameDiagram: looksLikeSameDiagram
    };
});
