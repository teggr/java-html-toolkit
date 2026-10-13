package io.github.teggr.examples.bookstore;

import java.math.BigDecimal;
import java.util.List;

public final class BookstoreSampleData {

    private static final List<Book> BOOKS = List.of(
            new Book("northbound", "Northbound", "Mara Ellis",
                    "A quiet journey through small towns and second chances.",
                    new BigDecimal("18.00"), "Fiction"),
            new Book("quiet-garden", "The Quiet Garden", "Jonah Reed",
                    "A practical guide to making a little room for nature.",
                    new BigDecimal("24.50"), "Gardening"),
            new Book("paper-moons", "Paper Moons", "Leila Okafor",
                    "An illustrated collection of stories about the night sky.",
                    new BigDecimal("16.75"), "Short stories"));

    private BookstoreSampleData() {
    }

    public static List<Book> books() {
        return BOOKS;
    }
}
