package com.inspire.tasks.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.tasks.book.dto.BookRequest;
import com.inspire.tasks.common.MessageResponse;
import com.inspire.tasks.user.User;
import com.inspire.tasks.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookRestControllerTest {

    MockMvc mockMvc;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    BookService bookService;

    @Mock
    UserService userService;

    @InjectMocks
    BookRestController bookRestController;

    private User user;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(bookRestController)
                .build();

        user = new User("John Doe", "JDoe@email.com", "encoded-pass");
    }

    @Test
    void getAllBooks_ReturnsList() throws Exception {
        Book b1 = new Book("Book 1", "John", "Test book", user);
        b1.setId(1L);
        Book b2 = new Book("Book 2", "Mary", "Test book", user);
        b2.setId(2L);

        when(bookService.findAll()).thenReturn(List.of(b1, b2));

        mockMvc.perform(get("/api/lib/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Book 1"))
                .andExpect(jsonPath("$[1].title").value("Book 2"));
    }

    @Test
    void findBooksByAuthor_ReturnsList() throws Exception {
        Book b1 = new Book("Book 1", "John", "Test book", user);
        Book b2 = new Book("Book 2", "John", "Test book", user);

        when(bookService.findAllByAuthorName("John")).thenReturn(List.of(b1, b2));

        mockMvc.perform(get("/api/lib/books/author/John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].authorName").value("John"))
                .andExpect(jsonPath("$[1].authorName").value("John"));

    }


    @Test
    void findBooksByTitle_ReturnsBook() throws Exception {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Book 1");
        book.setAuthorName("John");

        // Mock the service
        when(bookService.findAllByTitle("Book 1")).thenReturn(book);

        mockMvc.perform(get("/api/lib/books/title/Book 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Book 1"))
                .andExpect(jsonPath("$.authorName").value("John"));
    }


    @Test
    void createBook_ReturnsSuccess() throws Exception {
        BookRequest request = new BookRequest();
        request.setTitle("New Book");

        when(bookService.createBook(any(BookRequest.class)))
                .thenAnswer(invocation -> ResponseEntity.ok(
                        new MessageResponse(200, "Book created successfully!")));

        mockMvc.perform(post("/api/lib/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Book created successfully!"));
    }

    @Test
    void updateBook_Success() throws Exception {
        Long bookId = 1L;
        Book existing = new Book("Old Title", "John", "Test book", user);
        existing.setId(bookId);

        when(bookService.findById(bookId)).thenReturn(existing);

        when(bookService.save(any(Book.class)))
                .thenAnswer(invocation -> ResponseEntity.ok(
                        new MessageResponse(200, "Book updated successfully!")));

        Map<String, Object> patch = Map.of("title", "New Title");

        mockMvc.perform(patch("/api/lib/books/{id}", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Book updated successfully!"));
    }

    @Test
    void updateBook_Fails_WhenIdInPayload() throws Exception {
        when(bookService.findById(1L)).thenReturn(new Book("Title", "John", "Test Book", user));

        Map<String, Object> patch = Map.of("id", 99);

        mockMvc.perform(patch("/api/lib/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Book id is not allowed")));
    }

    @Test
    void updateBook_Fails_WhenUsernameInPayload() throws Exception {
        when(bookService.findById(1L)).thenReturn(new Book("Title", "John", "Test Book", user));

        Map<String, Object> patch = Map.of("username", "someone");

        mockMvc.perform(patch("/api/lib/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Editing 'user' is not allowed")));
    }

    @Test
    void deleteBook_ReturnsSuccess() throws Exception {
        Book book = new Book("Book Title", "John", "Test Book", user);
        book.setId(1L);

        when(bookService.findById(1L)).thenReturn(book);
        when(bookService.deleteById(1L))
                .thenAnswer(invocation -> ResponseEntity.ok(
                        new MessageResponse(200, "Book has been deleted successfully!")));

        mockMvc.perform(delete("/api/lib/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Book has been deleted successfully!"));
    }
}
