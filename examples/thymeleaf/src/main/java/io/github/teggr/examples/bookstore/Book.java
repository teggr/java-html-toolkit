package io.github.teggr.examples.bookstore;

import java.math.BigDecimal;

public final class Book {

    private final String id;
    private final String title;
    private final String author;
    private final String description;
    private final BigDecimal price;
    private final String category;

    public Book(String id, String title, String author, String description,
                BigDecimal price, String category) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.description = description;
        this.price = price;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }
}
