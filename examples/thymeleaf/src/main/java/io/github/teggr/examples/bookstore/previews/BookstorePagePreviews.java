package io.github.teggr.examples.bookstore.previews;

import java.util.Map;

import io.github.teggr.examples.bookstore.BookstoreSampleData;
import io.github.teggr.html.preview.Preview;
import io.github.teggr.html.preview.thymeleaf.ThymeleafPreview;

public class BookstorePagePreviews {

    @Preview(value = "Bookstore home", group = "Pages",
            description = "The bookstore home page with its navigation and book collection")
    public ThymeleafPreview index() {
        return new ThymeleafPreview("index",
                Map.of("books", BookstoreSampleData.books(), "activePath", "/"));
    }

    @Preview(value = "Book detail", group = "Pages",
            description = "A book detail page with shared bookstore navigation")
    public ThymeleafPreview book() {
        return new ThymeleafPreview("book",
                Map.of("book", BookstoreSampleData.books().get(0), "activePath", "/books"));
    }
}
