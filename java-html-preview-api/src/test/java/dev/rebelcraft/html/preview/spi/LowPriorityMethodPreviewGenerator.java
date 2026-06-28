package dev.rebelcraft.html.preview.spi;

import java.lang.reflect.Method;

import dev.rebelcraft.html.preview.Preview;

public final class LowPriorityMethodPreviewGenerator implements PreviewGenerator {

    @Override
    public String id() {
        return "low-priority-method-preview";
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public boolean supports(Class<?> clazz, Method method) {
        return "page".equals(method.getName());
    }

    @Override
    public String generate(Class<?> clazz, Method method, Preview preview) {
        return "low-priority-generator";
    }
}
