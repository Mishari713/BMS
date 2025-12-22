package com.inspire.tasks.book;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.tasks.book.client.OpenLibraryService;
import com.inspire.tasks.common.exception.BadRequestException;
import com.inspire.tasks.book.dto.BookRequest;
import com.inspire.tasks.book.dto.BookResponse;
import com.inspire.tasks.common.MessageResponse;
import com.inspire.tasks.user.User;
import com.inspire.tasks.user.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class BookService {

    BookRepository bookRepository;

    UserService userService;

    OpenLibraryService openLibraryService;

    ObjectMapper objectMapper;


    BookService(BookRepository bookRepository, UserService userService, OpenLibraryService openLibraryService, ObjectMapper objectMapper){
        this.bookRepository = bookRepository;
        this.userService = userService;
        this.openLibraryService = openLibraryService;
        this.objectMapper = objectMapper;
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
        log.info("Book patch request by user {}", book.getUserId());
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

    public BookResponse findBookByNameOL(String bookName) {
        try {
            String searchResponse = openLibraryService.findBookByName(bookName);
            JsonNode searchNode = objectMapper.readTree(searchResponse);

            JsonNode firstDoc = searchNode.get("docs").get(0);

            String workId = firstDoc.get("key").asText().replace("/works/", "");
            JsonNode workNode = objectMapper.valueToTree(openLibraryService.getWorkById(workId));

            String authors = "";
            JsonNode authorNamesNode = firstDoc.get("author_name");
            if (authorNamesNode != null && authorNamesNode.isArray()) {
                authors = StreamSupport.stream(authorNamesNode.spliterator(), false)
                        .map(JsonNode::asText)
                        .collect(Collectors.joining(", "));
            }

            String description = "";
            if (workNode.has("description")) {
                JsonNode descNode = workNode.get("description");
                if (descNode.has("value")) {
                    description = descNode.get("value").asText();
                } else if (descNode.isTextual()) {
                    description = descNode.asText();
                }
            }
            description = description.replace("\r\n", " ").replace("\n", " ");

            return new BookResponse(workNode.get("title").asText(), authors, description);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
