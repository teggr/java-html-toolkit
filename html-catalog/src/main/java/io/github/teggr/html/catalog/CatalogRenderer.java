package io.github.teggr.html.catalog;

import io.github.teggr.html.preview.Preview;
import io.github.teggr.html.preview.spi.PreviewGenerators;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders each {@link CatalogEntry} into HTML by delegating to
 * {@link PreviewGenerators}, and returns a matching list of
 * {@link RenderedEntry} results.
 *
 * <p>Rendering failures are captured as error messages rather than propagated,
 * so that a single broken preview does not abort the whole catalog build.
 */
public class CatalogRenderer {

    /**
     * Renders all entries using the current thread's context {@link ClassLoader}.
     */
    public List<RenderedEntry> render(List<CatalogEntry> entries) {
        return render(entries, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Renders all entries using the supplied {@code classLoader}.
     *
     * @param entries     the catalog entries to render
     * @param classLoader the class loader used to load the declaring classes
     * @return a list of {@link RenderedEntry} objects in the same order as {@code entries}
     */
    public List<RenderedEntry> render(List<CatalogEntry> entries, ClassLoader classLoader) {
        List<RenderedEntry> results = new ArrayList<>(entries.size());
        for (CatalogEntry entry : entries) {
            results.add(renderEntry(entry, classLoader));
        }
        return results;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private RenderedEntry renderEntry(CatalogEntry entry, ClassLoader classLoader) {
        try {
            Class<?> clazz = Class.forName(entry.className(), true, classLoader);
            Method method = findMethod(clazz, entry);
            method.setAccessible(true);
            Preview preview = method.getAnnotation(Preview.class);
            String html = PreviewGenerators.generate(clazz, method, preview);
            return new RenderedEntry(entry, html, null);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof VirtualMachineError fatal) {
                throw fatal;
            }
            if (cause instanceof ThreadDeath fatal) {
                throw fatal;
            }
            String msg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getName();
            return new RenderedEntry(entry, null, msg);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            return new RenderedEntry(entry, null, msg);
        }
    }

    private Method findMethod(Class<?> clazz, CatalogEntry entry) throws NoSuchMethodException {
        if (entry.methodSignature() == null || entry.methodSignature().isEmpty()) {
            return clazz.getDeclaredMethod(entry.methodName());
        }
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(entry.methodName())
                    && method.toGenericString().equals(entry.methodSignature())) {
                return method;
            }
        }
        throw new NoSuchMethodException(entry.className() + "#" + entry.methodSignature());
    }
}
