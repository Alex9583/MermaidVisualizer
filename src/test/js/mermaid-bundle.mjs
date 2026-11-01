// Loads the bundled mermaid.min.js in a node:vm sandbox with the minimum DOM stubs for mermaid.parse() (no render).
// `node src/test/js/mermaid-bundle.mjs` prints what the bundle throws for each fixture source (refresh after an upgrade).
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import vm from 'node:vm';
import { BUNDLE_ERROR_FIXTURES } from './mermaid-error-fixtures.mjs';

const BUNDLE_PATH = fileURLToPath(new URL('../../main/resources/web/mermaid.min.js', import.meta.url));
const VERSION_PATH = fileURLToPath(new URL('../../main/resources/web/mermaid.version', import.meta.url));

/** The mermaid.initialize() options the plugin uses for the limits (see mermaid-core.js initMermaid). */
export const PLUGIN_PARSE_CONFIG = { startOnLoad: false, suppressErrorRendering: true, maxTextSize: 500000, maxEdges: 2000 };

export function bundledMermaidVersion() {
    return readFileSync(VERSION_PATH, 'utf8').trim();
}

function stubElement() {
    const noop = () => {};
    return {
        style: {}, children: [], childNodes: [], nodeType: 1, ownerDocument: null,
        setAttribute: noop, getAttribute: () => null, appendChild: noop, removeChild: noop, insertBefore: noop,
        querySelector: () => null, querySelectorAll: () => [], addEventListener: noop,
        classList: { add: noop, remove: noop },
        getBBox: () => ({ x: 0, y: 0, width: 0, height: 0 }), getComputedTextLength: () => 0
    };
}

/** Loads the bundle in a fresh sandbox and returns its `mermaid` global, initialized with [config]. */
export function loadBundledMermaid(config) {
    const noop = () => {};
    const document = {
        createElement: stubElement, createElementNS: stubElement, createTextNode: () => ({}),
        querySelector: () => null, querySelectorAll: () => [], getElementById: () => null,
        body: stubElement(), head: stubElement(), documentElement: stubElement(),
        addEventListener: noop, createRange: () => ({ selectNode: noop, getBoundingClientRect: () => ({}) })
    };
    const window = {
        document, console, setTimeout, clearTimeout,
        addEventListener: noop, location: { href: 'http://localhost/' }, navigator: { userAgent: 'node' },
        getComputedStyle: () => ({ getPropertyValue: () => '' }),
        requestAnimationFrame: (cb) => setTimeout(cb, 0),
        matchMedia: () => ({ matches: false, addEventListener: noop })
    };
    window.window = window;
    window.self = window;
    const sandbox = {
        window, document, self: window, navigator: window.navigator, console, setTimeout, clearTimeout,
        URL, TextEncoder, TextDecoder, performance, structuredClone, queueMicrotask, Promise
    };
    sandbox.globalThis = sandbox; // the bundle exports itself as globalThis["mermaid"]
    vm.createContext(sandbox);
    vm.runInContext(readFileSync(BUNDLE_PATH, 'utf8'), sandbox, { filename: 'mermaid.min.js' });
    const mermaid = sandbox.mermaid;
    if (!mermaid || typeof mermaid.parse !== 'function') throw new Error('mermaid.min.js did not export a mermaid global');
    mermaid.initialize(config || PLUGIN_PARSE_CONFIG);
    return mermaid;
}

/** The error thrown by `mermaid.parse(source)`, or null when the source parses. */
export async function parseErrorOf(mermaid, source) {
    try {
        await mermaid.parse(source);
        return null;
    } catch (err) {
        return err;
    }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
    const mermaid = loadBundledMermaid();
    console.log('Mermaid ' + bundledMermaidVersion() + ' (' + BUNDLE_PATH + ')');
    for (const entry of BUNDLE_ERROR_FIXTURES) {
        const err = await parseErrorOf(mermaid, entry.fixture.source);
        console.log('\n== ' + entry.name);
        if (!err) {
            console.log('   parses without error');
            continue;
        }
        console.log('   name:    ' + err.name);
        console.log('   message: ' + JSON.stringify(err.message));
        console.log('   hash:    ' + JSON.stringify(err.hash));
    }
}
