package io.github.teggr.html.catalog;

import io.github.teggr.html.preview.Preview;

import java.io.File;
import java.lang.annotation.AnnotationFormatError;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scans classpath directories for classes that contain methods annotated with
 * {@link Preview}, and returns a sorted list of {@link CatalogEntry} descriptors.
 *
 * <p>Only file-system directories on the classpath are scanned; JAR files are
 * intentionally skipped so that only the project's own compiled classes are
 * discovered rather than every class in every dependency.
 */
public class CatalogScanner {

    /**
     * Scans using the current thread's context {@link ClassLoader}.
     */
    public List<CatalogEntry> scan() {
        return scan(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Scans all file-system directory entries reachable from {@code classLoader}
     * (and its parents) for {@link Preview}-annotated methods.
     *
     * @param classLoader the class loader used both to locate classpath roots and
     *                    to load discovered classes
     * @return sorted, de-duplicated list of catalog entries
     */
    public List<CatalogEntry> scan(ClassLoader classLoader) {
        List<URL> urls = resolveClasspathUrls(classLoader);
        Map<String, CatalogEntry> entries = new LinkedHashMap<>();

        for (URL url : urls) {
            if (!"file".equals(url.getProtocol())) {
                continue;
            }
            try {
                File file = new File(url.toURI());
                if (file.isDirectory()) {
                    for (CatalogEntry entry : scanDirectory(file, "", classLoader)) {
                        entries.putIfAbsent(entry.className() + "#" + entry.methodSignature(), entry);
                    }
                }
                // JARs are intentionally skipped
            } catch (Exception e) {
                warn("Could not inspect classpath entry " + url, e);
            }
        }

        return entries.values().stream()
                .sorted(Comparator.comparing(CatalogEntry::group)
                        .thenComparingInt(CatalogEntry::order)
                        .thenComparing(CatalogEntry::name)
                        .thenComparing(CatalogEntry::className)
                        .thenComparing(CatalogEntry::methodSignature))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Collects all {@link URL}s from the classloader hierarchy, falling back to
     * the {@code java.class.path} system property when no {@link URLClassLoader}
     * ancestor is found.
     */
    private List<URL> resolveClasspathUrls(ClassLoader classLoader) {
        List<URL> urls = new ArrayList<>();
        ClassLoader cl = classLoader;
        while (cl != null) {
            if (cl instanceof URLClassLoader urlCl) {
                urls.addAll(Arrays.asList(urlCl.getURLs()));
            }
            cl = cl.getParent();
        }
        if (!urls.isEmpty()) {
            return urls;
        }

        // Fallback: parse java.class.path (covers non-URLClassLoader app loaders in Java 9+)
        String classpath = System.getProperty("java.class.path", "");
        for (String entry : classpath.split(File.pathSeparator)) {
            if (entry.isEmpty()) {
                continue;
            }
            try {
                urls.add(new File(entry).toURI().toURL());
            } catch (Exception e) {
                warn("Could not parse classpath entry " + entry, e);
            }
        }
        return urls;
    }

    /**
     * Recursively walks a directory, converting {@code .class} files to class
     * names and checking each loaded class for {@link Preview} annotations.
     *
     * @param dir         the directory to walk
     * @param packageName the Java package prefix accumulated so far (dot-separated)
     * @param classLoader the class loader used to load discovered classes
     */
    private List<CatalogEntry> scanDirectory(File dir, String packageName, ClassLoader classLoader) {
        List<CatalogEntry> entries = new ArrayList<>();
        File[] children = dir.listFiles();
        if (children == null) {
            warn("Could not list classpath directory " + dir, null);
            return entries;
        }

        for (File child : children) {
            if (child.isDirectory()) {
                String subPackage = packageName.isEmpty()
                        ? child.getName()
                        : packageName + "." + child.getName();
                entries.addAll(scanDirectory(child, subPackage, classLoader));
            } else if (child.getName().endsWith(".class")) {
                String simpleName = child.getName().substring(0, child.getName().length() - 6);
                // Skip synthetic/metadata class files
                if ("module-info".equals(simpleName) || "package-info".equals(simpleName)) {
                    continue;
                }
                String className = packageName.isEmpty()
                        ? simpleName
                        : packageName + "." + simpleName;
                try {
                    // initialize=false avoids running static initialisers during scanning
                    Class<?> clazz = Class.forName(className, false, classLoader);
                    entries.addAll(entriesForClass(clazz));
                } catch (ClassNotFoundException | LinkageError | SecurityException e) {
                    warn("Could not load class " + className, e);
                }
            }
        }
        return entries;
    }

    /**
     * Returns a {@link CatalogEntry} for each method on {@code clazz} that is
     * annotated with {@link Preview}.
     */
    private List<CatalogEntry> entriesForClass(Class<?> clazz) {
        List<CatalogEntry> entries = new ArrayList<>();
        Method[] methods;
        try {
            methods = clazz.getDeclaredMethods();
        } catch (LinkageError | RuntimeException | AnnotationFormatError e) {
            warn("Could not inspect methods on " + clazz.getName(), e);
            return entries;
        }

        for (Method method : methods) {
            Preview preview;
            try {
                preview = method.getAnnotation(Preview.class);
            } catch (LinkageError | RuntimeException | AnnotationFormatError e) {
                warn("Could not read @Preview metadata from " + method, e);
                continue;
            }
            if (preview == null) {
                continue;
            }

            String name = preview.value().isEmpty() ? method.getName() : preview.value();
            String group = preview.group().isEmpty() ? clazz.getSimpleName() : preview.group();
            entries.add(new CatalogEntry(
                    group,
                    name,
                    preview.description(),
                    preview.order(),
                    clazz.getName(),
                    method.getName(),
                    method.toGenericString()
            ));
        }
        return entries;
    }

    private void warn(String message, Throwable error) {
        String detail = error == null
                ? ""
                : ": " + (error.getMessage() == null ? error.getClass().getName() : error.getMessage());
        System.err.println("[html-catalog] Warning: " + message + detail);
    }
}
