export type BuildStrategy = 'java-first' | 'maven-first';

export interface RuntimeSettings {
    buildStrategy: BuildStrategy;
    debugLogs: boolean;
}

export interface ProjectStateLike {
    needsCompile: boolean;
    needsClasspathRefresh: boolean;
}

export type ResolverKind = 'java' | 'maven';

export function normalizeBuildStrategy(value: string | undefined): BuildStrategy {
    return value === 'java-first' ? 'java-first' : 'maven-first';
}

export function resolverOrder(strategy: BuildStrategy): ResolverKind[] {
    return strategy === 'maven-first' ? ['maven', 'java'] : ['java', 'maven'];
}

export function applyPomInvalidation(state: ProjectStateLike): void {
    state.needsCompile = true;
    state.needsClasspathRefresh = true;
}

export function applyJavaInvalidation(state: ProjectStateLike): void {
    state.needsCompile = true;
}