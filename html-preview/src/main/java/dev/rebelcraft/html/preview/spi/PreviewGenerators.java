package dev.rebelcraft.html.preview.spi;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import dev.rebelcraft.html.preview.Preview;
import dev.rebelcraft.html.preview.PreviewRunner;

/**
 * Discovers and selects {@link PreviewGenerator} implementations.
 */
public final class PreviewGenerators {

    private PreviewGenerators() {
    }

    /**
     * Generates preview HTML using the highest-priority matching generator.
     */
    public static String generate(Class<?> clazz, Method method, Preview preview) throws Exception {
        List<PreviewGenerator> generators = resolveCandidates(clazz, method);
        if (generators.isEmpty()) {
            throw new IllegalStateException(
                    "No preview generator supports method '"
                            + clazz.getName()
                            + "#"
                            + method.getName()
                            + "'. Add an implementation module that provides "
                            + PreviewGenerator.class.getName()
                            + "."
            );
        }

        Exception firstFailure = null;
        for (PreviewGenerator generator : generators) {
            try {
                return generator.generate(clazz, method, preview);
            } catch (Exception ex) {
                if (firstFailure == null) {
                    firstFailure = ex;
                }

                if (!isAccessFailure(ex)) {
                    throw ex;
                }
            }
        }

        throw firstFailure == null
                ? new IllegalStateException("No preview generator was able to render output.")
                : firstFailure;
    }

    /**
     * Resolves the highest-priority generator that supports this method.
     */
    public static Optional<PreviewGenerator> resolve(Class<?> clazz, Method method) {
        return resolveCandidates(clazz, method)
                .stream()
                .findFirst();
    }

    private static List<PreviewGenerator> resolveCandidates(Class<?> clazz, Method method) {
        Stream<PreviewGenerator> discovered = ServiceLoader.load(PreviewGenerator.class)
                .stream()
                .map(ServiceLoader.Provider::get);

        Stream<PreviewGenerator> defaults = Stream.of(
            new ObjectReturnPreviewGenerator(),
                new StringReturnPreviewGenerator()
        );

        return Stream.concat(discovered, defaults)
                .filter(generator -> generator.supports(clazz, method))
                .sorted(Comparator.comparingInt(PreviewGenerator::priority).reversed())
                .toList();
    }

    private static boolean isAccessFailure(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof IllegalAccessException) {
                return true;
            }

            String message = current.getMessage();
            if (message != null && message.contains("cannot access a member of class")) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    /**
     * Built-in strategy for String-returning preview methods.
     */
    private static final class StringReturnPreviewGenerator implements PreviewGenerator {

        @Override
        public String id() {
            return "string-return-default";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public boolean supports(Class<?> clazz, Method method) {
            return method.getParameterCount() == 0 && method.getReturnType() == String.class;
        }

        @Override
        public String generate(Class<?> clazz, Method method, Preview preview) throws Exception {
            Object instance = PreviewRunner.newAccessibleInstance(clazz);
            Object result = method.invoke(instance);
            return result == null ? "" : (String) result;
        }
    }

    /**
     * Built-in generic strategy for non-String object return values.
     */
    private static final class ObjectReturnPreviewGenerator implements PreviewGenerator {

        @Override
        public String id() {
            return "object-return-default";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public boolean supports(Class<?> clazz, Method method) {
            return method.getParameterCount() == 0
                    && method.getReturnType() != String.class
                    && method.getReturnType() != Void.TYPE;
        }

        @Override
        public String generate(Class<?> clazz, Method method, Preview preview) throws Exception {
            Object instance = PreviewRunner.newAccessibleInstance(clazz);
            Object result = method.invoke(instance);
            return result == null ? "" : result.toString();
        }
    }
}
