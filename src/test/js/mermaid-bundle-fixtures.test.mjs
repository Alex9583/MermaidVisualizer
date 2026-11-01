// Replays the real error fixtures against the bundled Mermaid: a Mermaid upgrade that changes an error
// message, an err.hash, or the preprocessing that mermaid-core.js mirrors (front matter, %%{init}%%
// directives, %% comment lines) fails here first, before the classification tests argue about stale data.
// Run with `node --test src/test/js/*.test.mjs`; `node src/test/js/mermaid-bundle.mjs` prints the current errors.
import { before, describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { loadBundledMermaid, parseErrorOf } from './mermaid-bundle.mjs';
import { BUNDLE_ERROR_FIXTURES } from './mermaid-error-fixtures.mjs';
import { loadCore, plain } from './mermaid-core-sandbox.mjs';

describe('error fixtures match the bundled Mermaid', () => {
    let mermaid;
    const core = loadCore();

    before(() => {
        mermaid = loadBundledMermaid();
    });

    for (const entry of BUNDLE_ERROR_FIXTURES) {
        test(entry.name, async () => {
            const { fixture } = entry;
            const err = await parseErrorOf(mermaid, fixture.source);
            assert.ok(err, 'the bundle should reject this source');
            assert.equal(err.message, fixture.message, 'message changed: refresh the fixture (node src/test/js/mermaid-bundle.mjs)');
            assert.deepEqual(plain(err.hash), plain(fixture.hash), 'err.hash changed: refresh the fixture');
            if (fixture.errorName) assert.equal(err.name, fixture.errorName);

            // End-to-end check of the preprocessing mirror: the position reported for the original source.
            const info = plain(core.classifyRenderError(err, core.prepareSource(fixture.source)));
            if (entry.sourceLine !== undefined) {
                assert.equal(info.line, entry.sourceLine, 'line remapped to the source (preprocessing regexes of mermaid-core.js)');
            }
            if (entry.sourceColumn !== undefined) assert.equal(info.column, entry.sourceColumn);
        });
    }

    test('a valid diagram parses (the sandbox stubs are sufficient)', async () => {
        assert.equal(await parseErrorOf(mermaid, 'sequenceDiagram\n  Alice->>Bob: hi'), null);
        assert.equal(await parseErrorOf(mermaid, '---\ntitle: t\n---\n%% note\nflowchart TD\n  A --> B'), null);
    });
});
