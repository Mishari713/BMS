package com.inspire.tasks.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.inspire.tasks.book.dto.BookRequest;
import com.inspire.tasks.book.dto.BookResponse;
import com.inspire.tasks.common.MessageResponse;
import com.inspire.tasks.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@PreAuthorize("hasRole('ADMIN') or hasRole('AUTHOR')")
@RequestMapping("/api/lib")
public class BookRestController {

    BookService bookService;

    ObjectMapper objectMapper;

    UserService userService;

    public BookRestController(BookService bookService,     UserService userService,     ObjectMapper objectMapper){
        this.bookService = bookService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('AUTHOR') or hasRole('USER')")
    @GetMapping("open-library/{bookName}")
    public BookResponse getBookFromOpenLibrary(@PathVariable String bookName){
        return bookService.findBookByNameOL(bookName);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('AUTHOR') or hasRole('USER')")
    @GetMapping("/books")
    public List<Book> findAll() {
        return bookService.findAll();
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('AUTHOR') or hasRole('USER')")
    @GetMapping("/books/author/{authorName}")
    public List<Book> findBooksByAuthor(@PathVariable String authorName) {

        return bookService.findAllByAuthorName(authorName);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('AUTHOR') or hasRole('USER')")
    @GetMapping("/books/title/{title}")
    public Book findBooksByTitle(@PathVariable String title) {

        return bookService.findAllByTitle(title);
    }

    @PostMapping("/books")
    public ResponseEntity<?> createBook(@RequestBody BookRequest bookRequest){
        return bookService.createBook(bookRequest);
    }

    @PreAuthorize("hasRole('ADMIN') or @bookSecurity.userCanEdit(#bookId)")
    @PatchMapping("/books/{bookId}")
    public ResponseEntity<?> updateBook(@PathVariable Long bookId,
                                         @RequestBody Map<String, Object> patchPayload){

        Book book = bookService.findById(bookId);

        // throw exception if request body contains "id" key
        if(patchPayload.containsKey("id")){
            return new ResponseEntity<>
                    (new MessageResponse(400, "Book id is not allowed in request body - " + bookId), HttpStatus.UNAUTHORIZED);
        } else if (patchPayload.containsKey("username")) {
            return new ResponseEntity<>
                    (new MessageResponse(400, "Editing 'user' is not allowed"), HttpStatus.UNAUTHORIZED);
        }

        return bookService.save(apply(patchPayload, book));
    }

    Book apply(Map<String, Object> patchPayload, Book book) {
        ObjectNode bookNode = objectMapper.convertValue(book, ObjectNode.class);
        ObjectNode patchNode = objectMapper.convertValue(patchPayload, ObjectNode.class);
        bookNode.setAll(patchNode);
        Book patched = objectMapper.convertValue(bookNode, Book.class);
        patched.setUserId(book.getUserId());
        return patched;
    }

    @PreAuthorize("hasRole('ADMIN') or @bookSecurity.userCanEdit(#bookId)")
    @DeleteMapping("/books/{bookId}")
    public ResponseEntity<?> deleteBook(@PathVariable Long bookId){

        Book book = bookService.findById(bookId);

         return bookService.deleteById(bookId);
    }

    // Pagination usage
//    @GetMapping("/lastUsers")
//    public Page<User> getLastUsers(){
//        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());
//
//        return userRepository.findAll(pageable);
//    }
}