package io.github.teggr.examples.bookstore;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class Bookstore {

    public List<Book> findAll() {
        return BookstoreSampleData.books();
    }

    public Optional<Book> findById(String id) {
        return findAll().stream()
                .filter(book -> book.getId().equals(id))
                .findFirst();
    }
}
