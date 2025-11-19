package com.inspire.tasks.book;

import com.inspire.tasks.exception.BadRequestException;
import com.inspire.tasks.payload.request.BookRequest;
import com.inspire.tasks.payload.response.MessageResponse;
import com.inspire.tasks.user.User;
import com.inspire.tasks.user.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
@Slf4j
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);
    BookRepository bookRepository;

    @Autowired
    UserService userService;


    BookService(BookRepository bookRepository){
        this.bookRepository = bookRepository;
    }

    public ResponseEntity<?> createBook(@Valid @RequestBody BookRequest bookRequest){
        if (bookRepository.existsByTitle(bookRequest.getTitle())) {
            throw new BadRequestException("Error: A book with this title already exists.");
        }

        User user = userService.findByUsername(bookRequest.getUsername());

        // Create new book
        Book book = new Book(bookRequest.getTitle().toLowerCase(),
                bookRequest.getAuthor().toLowerCase(),
                bookRequest.getDescription().toLowerCase(),
                user
                );

        bookRepository.save(book);

        log.info("Book creation request by user {}", user.getUsername());

        return ResponseEntity.ok(new MessageResponse(200, "Book created successfully!"));
    }

    ResponseEntity<?> save(Book book) {
        bookRepository.save(book);
        log.info("Book patch request by user {}", book.getAuthor());
        return ResponseEntity.ok(new MessageResponse(200, "Book updated successfully!"));
    }

    public Book findById(Long bookId) {
        return bookRepository.findById(bookId).orElseThrow(() -> {
            log.warn("Book with id {} not found", bookId);
            return new BadRequestException("Book id : " + bookId + " doesn't exists");
        });
    }

    ResponseEntity<?> deleteById(Long bookId) {
        log.info("Deleting book with id {}", bookId);
        bookRepository.deleteById(bookId);
        return ResponseEntity.ok(new MessageResponse(200, "Book has been deleted successfully!"));
    }

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    public List<Book> findAllByAuthorName(String author) {
        return bookRepository.findAllByAuthorName(author);
    }

    public Book findAllByTitle(String title) {
        return bookRepository.findByTitle(title).orElseThrow(() -> {
            log.warn("Book with title {} not found", title);
            return new BadRequestException("Book title : " + title + " doesn't exists");
        });
    }
}
