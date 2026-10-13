package io.github.teggr.html.preview.spi;

import java.lang.reflect.Method;

import io.github.teggr.html.preview.Preview;

public final class AccessFailingMethodPreviewGenerator implements PreviewGenerator {

    @Override
    public String id() {
        return "access-failing-method-preview";
    }

    @Override
    public int priority() {
        return 200;
    }

    @Override
    public boolean supports(Class<?> clazz, Method method) {
        return "accessFails".equals(method.getName());
    }

    @Override
    public String generate(Class<?> clazz, Method method, Preview preview) throws Exception {
        throw new IllegalAccessException(
                "class io.github.teggr.html.preview.j2html.J2HtmlPreviewGenerator cannot access a member of class "
                        + clazz.getName()
                        + " with package access"
        );
    }
}
