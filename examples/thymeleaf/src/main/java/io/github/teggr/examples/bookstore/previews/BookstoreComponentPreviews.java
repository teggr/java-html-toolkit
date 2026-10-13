package io.github.teggr.examples.bookstore.previews;

import java.util.Map;

import io.github.teggr.examples.bookstore.BookstoreSampleData;
import io.github.teggr.html.preview.Preview;
import io.github.teggr.html.preview.thymeleaf.ThymeleafPreview;

public class BookstoreComponentPreviews {

    @Preview(value = "Site header", group = "Components",
            description = "The bookstore wordmark and shared navigation")
    public ThymeleafPreview header() {
        return new ThymeleafPreview("previews/header",
                Map.of("activePath", "/"));
    }

    @Preview(value = "Menu", group = "Components",
            description = "The shared navigation menu in its home-page state")
    public ThymeleafPreview menu() {
        return new ThymeleafPreview("previews/menu",
                Map.of("activePath", "/"));
    }

    @Preview(value = "Book card", group = "Components",
            description = "A reusable card for a book in the collection")
    public ThymeleafPreview bookCard() {
        return new ThymeleafPreview("previews/book-card",
                Map.of("book", BookstoreSampleData.books().get(0)));
    }

    @Preview(value = "Book collection", group = "Components",
            description = "A responsive grid of reusable book cards")
    public ThymeleafPreview bookCollection() {
        return new ThymeleafPreview("previews/book-collection",
                Map.of("books", BookstoreSampleData.books(),
                        "activePath", "/"));
    }

    @Preview(value = "Book details", group = "Components",
            description = "The detail panel used by the book page")
    public ThymeleafPreview bookDetails() {
        return new ThymeleafPreview("previews/book-details",
                Map.of("book", BookstoreSampleData.books().get(0), "activePath", "/books"));
    }

    @Preview(value = "Footer", group = "Components",
            description = "The shared bookstore sign-off")
    public ThymeleafPreview footer() {
        return new ThymeleafPreview("previews/footer", Map.of());
    }
}
