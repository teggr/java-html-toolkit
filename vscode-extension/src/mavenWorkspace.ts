import * as fs from 'fs';
import * as path from 'path';

export interface MavenProjectContext {
    projectRoot: string;
    reactorRoot: string;
}

export function findMavenProjectContext(filePath: string): MavenProjectContext | undefined {
    const projectRoot = findNearestPom(path.dirname(filePath));
    if (!projectRoot) {
        return undefined;
    }

    let reactorRoot = projectRoot;
    let candidate = path.dirname(projectRoot);
    const filesystemRoot = path.parse(candidate).root;

    while (candidate !== filesystemRoot) {
        if (isReactorFor(candidate, projectRoot)) {
            reactorRoot = candidate;
        }
        candidate = path.dirname(candidate);
    }

    return { projectRoot, reactorRoot };
}

export function buildMavenArgs(context: MavenProjectContext, args: string[]): string[] {
    if (context.reactorRoot === context.projectRoot) {
        return args;
    }

    const modulePath = path.relative(context.reactorRoot, context.projectRoot);
    return ['-pl', modulePath, '-am', ...args];
}

function findNearestPom(startDirectory: string): string | undefined {
    let directory = startDirectory;
    const filesystemRoot = path.parse(directory).root;

    while (directory !== filesystemRoot) {
        if (fs.existsSync(path.join(directory, 'pom.xml'))) {
            return directory;
        }
        directory = path.dirname(directory);
    }

    return undefined;
}

function isReactorFor(reactorRoot: string, projectRoot: string): boolean {
    const pomPath = path.join(reactorRoot, 'pom.xml');
    if (!fs.existsSync(pomPath)) {
        return false;
    }

    let pom: string;
    try {
        pom = fs.readFileSync(pomPath, 'utf8').replace(/<!--[\s\S]*?-->/g, '');
    } catch (error: unknown) {
        const message = error instanceof Error ? error.message : String(error);
        throw new Error(`Cannot read Maven reactor POM ${pomPath}: ${message}`);
    }

    const modulesBlock = pom.match(/<modules\b[^>]*>([\s\S]*?)<\/modules>/i)?.[1];
    if (!modulesBlock) {
        return false;
    }

    const modules = [...modulesBlock.matchAll(/<module\b[^>]*>\s*([^<]+?)\s*<\/module>/gi)];
    return modules.some((match) => {
        const modulePath = match[1].trim();
        if (!modulePath || modulePath.includes('${')) {
            return false;
        }

        const moduleRoot = path.resolve(reactorRoot, modulePath);
        const relativeProjectPath = path.relative(moduleRoot, projectRoot);
        return relativeProjectPath === ''
            || (!relativeProjectPath.startsWith('..') && !path.isAbsolute(relativeProjectPath));
    });
}
