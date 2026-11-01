// Runtime tests of looksLikeSameDiagram (mermaid-core.js): the Markdown preview reuses a remembered rendering
// under the error banner only for a source that looks like an edit of the one it came from.
import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { loadCore } from './mermaid-core-sandbox.mjs';

const FLOWCHART = 'flowchart TD\n    A[Start] --> B{Ok?}\n    B -->|yes| C[Done]\n    B -->|no| D[Retry]';

describe('looksLikeSameDiagram', () => {
    const core = loadCore();
    const same = (previous, current) => core.looksLikeSameDiagram(previous, current);

    test('identical sources', () => {
        assert.equal(same(FLOWCHART, FLOWCHART), true);
    });

    test('typing at the end, in the middle and at the start of the diagram', () => {
        assert.equal(same(FLOWCHART, FLOWCHART + '\n    D --> A['), true, 'a new line being typed');
        assert.equal(same(FLOWCHART, FLOWCHART.replace('B{Ok?}', 'B{Is it ok')), true, 'a label being edited');
        assert.equal(same(FLOWCHART, FLOWCHART.replace('C[Done]', 'C[')), true, 'a label being retyped');
        assert.equal(same(FLOWCHART, '%%{init: {"theme": "dark"}}%%\n' + FLOWCHART), true, 'a directive added above');
        assert.equal(same(FLOWCHART, FLOWCHART.replace('    B -->|no| D[Retry]', '')), true, 'a line removed');
    });

    test('a larger diagram grown from the remembered one', () => {
        const grown = FLOWCHART + '\n' + Array.from({ length: 30 }, (_, i) => '    N' + i + ' --> N' + (i + 1)).join('\n') + '\n    X --> Y[';
        assert.equal(same(FLOWCHART, grown), true, 'the whole remembered source is still its prefix');
    });

    test('another diagram at the same position is not the same, even with the same header', () => {
        assert.equal(same(FLOWCHART, 'sequenceDiagram\n    Alice->>Bob: hi\n    Bob-'), false);
        assert.equal(same(FLOWCHART, 'flowchart TD\n    Order --> Payment\n    Payment --> Shipping\n    Shipping --> Invoice['), false);
        // Known limit: in a tiny diagram the shared header line alone can reach the threshold. Accepted:
        // a stale two-node rendering under the banner is harmless, and real diagrams have longer bodies.
        assert.equal(same('graph LR\n    A --> B', 'graph LR\n    X --> Y --> Z --> W --> V['), true);
    });

    test('the threshold is half of the shorter source, start plus end', () => {
        assert.equal(same('abcdefghij', 'abcde12345'), true, '5 of 10 shared at the start');
        assert.equal(same('abcdefghij', 'abcd123456'), false, '4 of 10 shared');
        assert.equal(same('abcdefghij', 'ab12345hij'), true, '2 at the start + 3 at the end');
        assert.equal(same('abcdefghij', 'xxabcdefghijxx'), false, 'an insertion at both ends shares nothing');
    });

    test('empty and non-string sources never match', () => {
        assert.equal(same('', ''), false);
        assert.equal(same(FLOWCHART, ''), false);
        assert.equal(same(undefined, FLOWCHART), false);
        assert.equal(same(FLOWCHART, null), false);
        assert.equal(same(42, 42), false);
    });
});
