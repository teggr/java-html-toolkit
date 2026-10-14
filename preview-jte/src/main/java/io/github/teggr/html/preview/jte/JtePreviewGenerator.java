package io.github.teggr.html.preview.jte;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import gg.jte.resolve.ResourceCodeResolver;

import io.github.teggr.html.preview.Preview;
import io.github.teggr.html.preview.PreviewRunner;
import io.github.teggr.html.preview.spi.PreviewGenerator;

/**
 * Compiles and renders classpath JTE templates in HTML mode using the current JDK.
 */
public final class JtePreviewGenerator implements PreviewGenerator {

    @Override
    public String id() {
        return "jte";
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public boolean supports(Class<?> clazz, Method method) {
        return method.getParameterCount() == 0
                && method.getReturnType() == JtePreview.class;
    }

    @Override
    public String generate(Class<?> clazz, Method method, Preview preview) throws Exception {
        Object instance = PreviewRunner.newAccessibleInstance(clazz);
        Object result = method.invoke(instance);
        if (!(result instanceof JtePreview jtePreview)) {
            throw new IllegalStateException(
                    "JTE preview method '" + clazz.getName() + "#" + method.getName()
                            + "' must return a non-null " + JtePreview.class.getName());
        }

        Path classes = Files.createTempDirectory("java-html-toolkit-jte-");
        try {
            ClassLoader loader = clazz.getClassLoader();
            TemplateEngine engine = TemplateEngine.create(
                    new ResourceCodeResolver("templates", loader), classes, ContentType.Html, loader);
            StringOutput output = new StringOutput();
            engine.render(jtePreview.template(), jtePreview.params(), output);
            return output.toString();
        } finally {
            try (var paths = Files.walk(classes)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
