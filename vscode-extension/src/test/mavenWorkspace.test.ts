import test from 'node:test';
import assert from 'node:assert/strict';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';

import { buildMavenArgs, findMavenProjectContext } from '../mavenWorkspace';

test('uses the current Maven project when there is no reactor parent', () => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), 'java-html-single-'));
    try {
        const projectRoot = path.join(root, 'sample');
        const sourceFile = path.join(projectRoot, 'src', 'main', 'java', 'Preview.java');
        fs.mkdirSync(path.dirname(sourceFile), { recursive: true });
        fs.writeFileSync(path.join(projectRoot, 'pom.xml'), '<project/>');

        const context = findMavenProjectContext(sourceFile);
        assert.deepEqual(context, { projectRoot, reactorRoot: projectRoot });
        assert.deepEqual(buildMavenArgs(context!, ['test-compile', '-q']), ['test-compile', '-q']);
    } finally {
        fs.rmSync(root, { recursive: true, force: true });
    }
});

test('selects the outer reactor and builds the requested module with its dependencies', () => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), 'java-html-reactor-'));
    try {
        const reactorRoot = path.join(root, 'multi');
        const moduleRoot = path.join(reactorRoot, 'modules', 'app');
        const sourceFile = path.join(moduleRoot, 'src', 'test', 'java', 'Preview.java');
        fs.mkdirSync(path.dirname(sourceFile), { recursive: true });
        fs.writeFileSync(
            path.join(reactorRoot, 'pom.xml'),
            '<project><modules><module>modules</module></modules></project>',
        );
        fs.writeFileSync(
            path.join(reactorRoot, 'modules', 'pom.xml'),
            '<project><modules><module>app</module></modules></project>',
        );
        fs.writeFileSync(path.join(moduleRoot, 'pom.xml'), '<project/>');

        const context = findMavenProjectContext(sourceFile);
        assert.deepEqual(context, { projectRoot: moduleRoot, reactorRoot });
        assert.deepEqual(
            buildMavenArgs(context!, ['test-compile', '-q']),
            ['-pl', path.join('modules', 'app'), '-am', 'test-compile', '-q'],
        );
    } finally {
        fs.rmSync(root, { recursive: true, force: true });
    }
});

test('returns no Maven context when the source is outside a Maven project', () => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), 'java-html-no-maven-'));
    try {
        const sourceFile = path.join(root, 'src', 'Preview.java');
        fs.mkdirSync(path.dirname(sourceFile), { recursive: true });
        assert.equal(findMavenProjectContext(sourceFile), undefined);
    } finally {
        fs.rmSync(root, { recursive: true, force: true });
    }
});
