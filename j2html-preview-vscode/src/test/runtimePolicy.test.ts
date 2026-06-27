import test from 'node:test';
import assert from 'node:assert/strict';

import {
    applyJavaInvalidation,
    applyPomInvalidation,
    normalizeBuildStrategy,
    resolverOrder,
} from '../runtimePolicy';

test('defaults to java-first strategy when unset or unknown', () => {
    assert.equal(normalizeBuildStrategy(undefined), 'java-first');
    assert.equal(normalizeBuildStrategy('unknown'), 'java-first');
});

test('uses maven-first strategy when configured', () => {
    assert.equal(normalizeBuildStrategy('maven-first'), 'maven-first');
});

test('returns resolver order for java-first', () => {
    assert.deepEqual(resolverOrder('java-first'), ['java', 'maven']);
});

test('returns resolver order for maven-first', () => {
    assert.deepEqual(resolverOrder('maven-first'), ['maven', 'java']);
});

test('pom invalidation marks both compile and classpath as stale', () => {
    const state = { needsCompile: false, needsClasspathRefresh: false };
    applyPomInvalidation(state);
    assert.deepEqual(state, { needsCompile: true, needsClasspathRefresh: true });
});

test('java invalidation marks compile stale and preserves classpath staleness', () => {
    const state = { needsCompile: false, needsClasspathRefresh: false };
    applyJavaInvalidation(state);
    assert.deepEqual(state, { needsCompile: true, needsClasspathRefresh: false });
});