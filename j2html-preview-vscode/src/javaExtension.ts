import * as path from 'path';
import * as vscode from 'vscode';

interface JavaProjectClasspathResult {
    classpaths?: unknown;
    modulepaths?: unknown;
}

function toStringArray(value: unknown): string[] {
    if (!value) {
        return [];
    }

    if (Array.isArray(value)) {
        return value
            .map((item) => (typeof item === 'string' ? item : item?.toString()))
            .filter((item): item is string => Boolean(item));
    }

    const items: string[] = [];

    try {
        for (const item of value as Iterable<unknown>) {
            const stringValue = typeof item === 'string' ? item : item?.toString();
            if (stringValue) {
                items.push(stringValue);
            }
        }
    } catch {
        // Ignore non-iterables from the extension host bridge.
    }

    return items;
}

async function ensureJavaExtension(): Promise<void> {
    const extension = vscode.extensions.getExtension('redhat.java');
    if (!extension) {
        throw new Error(
            'Red Hat\'s Language Support for Java extension is required for classpath resolution.',
        );
    }

    if (!extension.isActive) {
        await extension.activate();
    }
}

export async function resolveJavaExtensionClasspath(
    documentUri: vscode.Uri,
    projectRoot: string,
): Promise<string[]> {
    await ensureJavaExtension();

    const result = await requestJavaProjectClasspaths(documentUri);

    if (!result) {
        throw new Error('Java extension did not return classpath information.');
    }

    const entries = [
        ...toStringArray(result.classpaths),
        ...toStringArray(result.modulepaths),
        path.join(projectRoot, 'target', 'classes'),
        path.join(projectRoot, 'target', 'test-classes'),
    ];

    return [...new Set(entries.filter(Boolean))];
}

async function requestJavaProjectClasspaths(
    documentUri: vscode.Uri,
): Promise<JavaProjectClasspathResult> {
    const uri = documentUri.toString();

    try {
        // The command bridge expects commandName followed by positional args.
        // Passing a single array can reach Java as java.util.ArrayList and fail casts.
        const result = await vscode.commands.executeCommand<JavaProjectClasspathResult>(
            'java.execute.workspaceCommand',
            'java.project.getClasspaths',
            uri,
            { scope: 'test' },
        );

        if (result) {
            return result;
        }
    } catch {
        // Fall through to alternate signatures below.
    }

    try {
        const result = await vscode.commands.executeCommand<JavaProjectClasspathResult>(
            'java.execute.workspaceCommand',
            'java.project.getClasspaths',
            uri,
            'test',
        );

        if (result) {
            return result;
        }
    } catch {
        // Fall through to legacy array signature.
    }

    const legacyResult = await vscode.commands.executeCommand<JavaProjectClasspathResult>(
        'java.execute.workspaceCommand',
        'java.project.getClasspaths',
        [uri, { scope: 'test' }],
    );

    if (!legacyResult) {
        throw new Error('Java extension did not return classpath information.');
    }

    return legacyResult;
}