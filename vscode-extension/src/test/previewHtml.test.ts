import test from 'node:test';
import assert from 'node:assert/strict';

import { injectCssIntoHtml } from '../previewHtml';

test('wraps HTML fragments and injects CSS into the head', () => {
    const result = injectCssIntoHtml('<main>preview</main>', '<style>main { color: red; }</style>');
    assert.match(result, /<head>[\s\S]*<style>main \{ color: red; \}<\/style>[\s\S]*<\/head>/);
    assert.match(result, /<body>\s*<main>preview<\/main>\s*<\/body>/);
});

test('injects CSS after an existing head tag', () => {
    const result = injectCssIntoHtml(
        '<!doctype html><html><head><title>Title</title></head><body>preview</body></html>',
        '<link rel="stylesheet" href="styles.css">',
    );
    assert.match(result, /<head>\s*<link rel="stylesheet" href="styles.css">/);
    assert.match(result, /<title>Title<\/title>/);
});

test('creates a head for full documents that do not have one', () => {
    const result = injectCssIntoHtml(
        '<html><body>preview</body></html>',
        '<style>body { color: blue; }</style>',
    );
    assert.match(result, /<html>\s*<head>\s*<style>body \{ color: blue; \}<\/style>\s*<\/head><body>/);
});

test('leaves HTML unchanged when no CSS is configured', () => {
    const html = '<main>preview</main>';
    assert.equal(injectCssIntoHtml(html, ''), html);
});
