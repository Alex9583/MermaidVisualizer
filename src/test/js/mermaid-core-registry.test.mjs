// Runtime tests of the module registry (mermaid-core.js) and of the module graph of the core scripts.
import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { CORE_SCRIPTS, loadCore, loadScripts, readCoreScript } from './mermaid-core-sandbox.mjs';

const DEFINE = /window\.__mermaidCore\.define\('([^']+)',\s*\[([^\]]*)\]/;

function declaredModule(script) {
    const match = DEFINE.exec(readCoreScript(script));
    assert.ok(match, script + ' should register exactly one module with window.__mermaidCore.define');
    const deps = match[2].split(',').map((s) => s.trim().replace(/^'|'$/g, '')).filter(Boolean);
    return { name: match[1], deps };
}

describe('module registry', () => {
    test('define passes the dependencies to the factory and flattens the API onto the facade', () => {
        const core = loadScripts(['mermaid-core.js']).__mermaidCore;
        core.define('a', [], () => ({ one: () => 1 }));
        core.define('b', ['a'], (a) => ({ two: () => a.one() + 1 }));
        assert.equal(core.two(), 2);
        assert.equal(core.require('b').two(), 2);
        assert.deepEqual(Object.keys(core.modules), ['a', 'b']);
    });

    test('a module loaded before its dependency fails with a message naming both', () => {
        const core = loadScripts(['mermaid-core.js']).__mermaidCore;
        assert.throws(() => core.define('late', ['missing'], () => ({})), /module "late" needs "missing"/);
        assert.throws(() => core.require('missing'), /module "missing" is not loaded/);
    });

    test('duplicate modules and colliding exports are rejected', () => {
        const core = loadScripts(['mermaid-core.js']).__mermaidCore;
        core.define('a', [], () => ({ shared: 1 }));
        assert.throws(() => core.define('a', [], () => ({})), /defined twice/);
        assert.throws(() => core.define('b', [], () => ({ shared: 2 })), /exports "shared", already defined/);
        assert.throws(() => core.define('c', [], () => ({ define: 2 })), /exports "define", already defined/);
        assert.throws(() => core.define('d', [], () => undefined), /returned no API object/);
    });
});

describe('core module graph', () => {
    test('the registry comes first and every script registers one module whose dependencies are loaded earlier', () => {
        assert.equal(CORE_SCRIPTS[0], 'mermaid-core.js');
        const loaded = [];
        for (const script of CORE_SCRIPTS.slice(1)) {
            const { name, deps } = declaredModule(script);
            for (const dep of deps) {
                assert.ok(loaded.includes(dep), script + ' depends on "' + dep + '", which must be loaded before it');
            }
            loaded.push(name);
        }
        assert.deepEqual(loaded, ['text', 'shadow', 'runtime', 'export', 'errorPositions', 'errorLabels', 'errors', 'banner', 'pipeline']);
    });

    test('loading every core script registers the modules and the facade the adapters rely on', () => {
        const core = loadCore();
        assert.deepEqual(Object.keys(core.modules), ['text', 'shadow', 'runtime', 'export', 'errorPositions', 'errorLabels', 'errors', 'banner', 'pipeline']);
        for (const name of ['utf8ToBase64', 'base64ToUtf8', 'injectSvg', 'clearShadowContent', 'showError', 'installLinkHandler', 'createSvgIcon',
            'initMermaid', 'getCurrentTheme', 'nextRenderId', 'detectTextSizeExceeded', 'describeRenderError',
            'extractSvg', 'extractPng', 'createExportToolbar', 'prepareSource', 'classifyRenderError', 'toRenderErrorPayload',
            'parseErrorInfo', 'looksLikeSameDiagram', 'showErrorBanner', 'renderInto', 'showRenderError', 'mountDiagram']) {
            assert.equal(typeof core[name], 'function', name + ' should be on the facade');
        }
        assert.equal(typeof core.JISON_TOKEN_LABELS, 'object');
        assert.equal(core.getCurrentTheme(), 'default');
        assert.match(core.nextRenderId(), /^mermaid-render-\d+$/);
    });
});
