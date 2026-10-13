package io.github.teggr.examples.bookstore;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class BookstoreController {

    private final Bookstore bookstore;

    public BookstoreController(Bookstore bookstore) {
        this.bookstore = bookstore;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("books", bookstore.findAll());
        model.addAttribute("activePath", "/");
        return "index";
    }

    @GetMapping("/books/{id}")
    public String book(@PathVariable("id") String id, Model model) {
        Book book = bookstore.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        model.addAttribute("book", book);
        model.addAttribute("activePath", "/books");
        return "book";
    }
}
