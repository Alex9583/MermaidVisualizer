/**
 * Module registry of the rendering core shared by mermaid-render.js (Markdown preview) and
 * mermaid-standalone.js (.mmd editor). Classic scripts only — the standalone page inlines them, the Markdown
 * preview serves them — so each mermaid-core-*.js file calls define(name, deps, factory): deps are passed to
 * the factory, a missing one throws, every export is copied onto the flat facade window.__mermaidCore.
 * Load order: MermaidWebResources.kt and src/test/js/mermaid-core-sandbox.mjs.
 */
(function () {
    'use strict';

    const modules = Object.create(null);
    const RESERVED = ['modules', 'define', 'require'];

    function fail(message) {
        throw new Error('[MermaidVisualizer] mermaid-core: ' + message);
    }

    function define(name, deps, factory) {
        if (typeof name !== 'string' || !name) fail('a module needs a name');
        if (modules[name]) fail('module "' + name + '" is defined twice');
        if (!Array.isArray(deps) || typeof factory !== 'function') fail('define("' + name + '") expects (deps[], factory)');
        const resolved = deps.map(function (dep) {
            if (!modules[dep]) fail('module "' + name + '" needs "' + dep + '", which is not loaded yet: check the script order');
            return modules[dep];
        });
        const api = factory.apply(null, resolved);
        if (!api || typeof api !== 'object') fail('module "' + name + '" returned no API object');
        Object.keys(api).forEach(function (key) {
            if (RESERVED.indexOf(key) >= 0 || Object.prototype.hasOwnProperty.call(core, key)) {
                fail('module "' + name + '" exports "' + key + '", already defined on the core');
            }
            core[key] = api[key];
        });
        modules[name] = api;
        return api;
    }

    function require(name) {
        return modules[name] || fail('module "' + name + '" is not loaded');
    }

    const core = {
        modules: modules,
        define: define,
        require: require
    };

    window.__mermaidCore = core;
})();
