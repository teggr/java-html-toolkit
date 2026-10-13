package io.github.teggr.html.preview.thymeleaf;

import java.lang.reflect.Method;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import io.github.teggr.html.preview.Preview;
import io.github.teggr.html.preview.PreviewRunner;
import io.github.teggr.html.preview.spi.PreviewGenerator;

/**
 * Renders {@link ThymeleafPreview} results with templates from the classpath.
 */
public final class ThymeleafPreviewGenerator implements PreviewGenerator {

    private static final TemplateEngine TEMPLATE_ENGINE = createTemplateEngine();

    private static TemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Override
    public String id() {
        return "thymeleaf";
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public boolean supports(Class<?> clazz, Method method) {
        return method.getParameterCount() == 0
                && method.getReturnType() == ThymeleafPreview.class;
    }

    @Override
    public String generate(Class<?> clazz, Method method, Preview preview) throws Exception {
        Object instance = PreviewRunner.newAccessibleInstance(clazz);
        Object result = method.invoke(instance);
        if (!(result instanceof ThymeleafPreview thymeleafPreview)) {
            throw new IllegalStateException(
                    "Thymeleaf preview method '" + clazz.getName() + "#" + method.getName()
                            + "' must return a non-null " + ThymeleafPreview.class.getName());
        }

        Context context = new Context();
        context.setVariables(thymeleafPreview.model());
        return TEMPLATE_ENGINE.process(thymeleafPreview.template(), context);
    }
}
