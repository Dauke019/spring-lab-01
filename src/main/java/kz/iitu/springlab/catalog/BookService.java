package kz.iitu.springlab.catalog;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    /** All books when author is null or blank, otherwise only the books whose author contains the fragment. */
    public List<Book> findAll(String author) {
        return repository.findAll().stream()
                .filter(book -> author == null || author.isBlank()
                        || book.author().toLowerCase().contains(author.toLowerCase()))
                .sorted(Comparator.comparing(Book::id))
                .toList();
    }

    public Optional<Book> findById(long id) {
        return repository.findById(id);
    }

    public Book create(Book book) {
        // the client must not choose the identifier: it is always generated
        return repository.save(new Book(null, book.title(), book.author(), book.year()));
    }

    /** Replaces the record with the given id; empty if there is no such record. */
    public Optional<Book> replace(long id, Book book) {
        return repository.findById(id)
                .map(existing -> repository.save(new Book(id, book.title(), book.author(), book.year())));
    }

    public boolean delete(long id) {
        return repository.deleteById(id);
    }
}
