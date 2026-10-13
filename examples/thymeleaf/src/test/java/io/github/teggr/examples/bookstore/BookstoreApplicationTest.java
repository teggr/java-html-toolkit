package io.github.teggr.examples.bookstore;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import io.github.teggr.examples.bookstore.previews.BookstoreComponentPreviews;
import io.github.teggr.examples.bookstore.previews.BookstorePagePreviews;
import io.github.teggr.html.preview.Preview;
import io.github.teggr.html.preview.spi.PreviewGenerators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookstoreApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rendersBookstoreHomeAndBookPages() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chapter &amp; Co.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Northbound")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("The Quiet Garden")));

        mockMvc.perform(get("/books/northbound"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("A quiet journey")));
    }

    @Test
    void rendersEveryAnnotatedPageAndComponentPreview() throws Exception {
        List<Class<?>> previewClasses = List.of(
                BookstorePagePreviews.class,
                BookstoreComponentPreviews.class);

        for (Class<?> previewClass : previewClasses) {
            for (Method method : previewClass.getDeclaredMethods()) {
                Preview preview = method.getAnnotation(Preview.class);
                if (preview == null) {
                    continue;
                }

                String html = PreviewGenerators.generate(previewClass, method, preview);
                assertFalse(html.isBlank(), previewClass.getSimpleName() + "#" + method.getName());
                assertTrue(html.startsWith("<!DOCTYPE html>"),
                        previewClass.getSimpleName() + "#" + method.getName()
                                + " should render a standalone HTML document");
                assertFalse(html.matches("(?s).*\\s+th:[\\w-]+=.*"),
                        previewClass.getSimpleName() + "#" + method.getName()
                                + " should not retain Thymeleaf attributes");
            }
        }
    }
}
