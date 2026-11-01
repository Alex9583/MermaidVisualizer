// Shared helpers of the Node runtime tests: load mermaid-core.js in a node:vm sandbox.
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import vm from 'node:vm';

const WEB_DIR = new URL('../../main/resources/web/', import.meta.url);

// Load order of the core; must match MermaidWebResources.CORE_SCRIPTS (checked by MermaidWebResourcesTest).
export const CORE_SCRIPTS = [
    'mermaid-core.js',
    'mermaid-core-text.js',
    'mermaid-core-shadow.js',
    'mermaid-core-runtime.js',
    'mermaid-core-export.js',
    'mermaid-core-error-positions.js',
    'mermaid-core-error-labels.js',
    'mermaid-core-errors.js',
    'mermaid-core-banner.js',
    'mermaid-core-pipeline.js'
];

export function readCoreScript(name) {
    return readFileSync(fileURLToPath(new URL(name, WEB_DIR)), 'utf8');
}

/** Runs the given scripts, in order, in a fresh sandbox holding only `window` (with __MERMAID_CONFIG) and `console`. */
export function loadScripts(names, config) {
    const window = {};
    if (config) window.__MERMAID_CONFIG = config;
    const sandbox = { window, console };
    vm.createContext(sandbox);
    for (const name of names) {
        vm.runInContext(readCoreScript(name), sandbox, { filename: name });
    }
    return window;
}

/** The core facade with every module loaded (pure functions: no DOM, no mermaid global needed). */
export function loadCore(config) {
    return loadScripts(CORE_SCRIPTS, config).__mermaidCore;
}

/** Objects created inside a vm context have another realm's prototypes: compare plain copies. */
export const plain = (value) => (value === undefined ? undefined : JSON.parse(JSON.stringify(value)));

/** An Error shaped like the ones thrown by Mermaid's Jison parsers (`err.hash` carries the position). */
export function jisonError(message, hash) {
    const err = new Error(message);
    if (hash) err.hash = hash;
    return err;
}
