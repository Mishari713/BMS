package com.inspire.tasks.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspire.tasks.auth.TestSecurityConfig;

import com.inspire.tasks.auth.jwt.AuthTokenFilter;
import com.inspire.tasks.book.dto.BookRequest;
import com.inspire.tasks.book.dto.BookResponse;
import com.inspire.tasks.common.MessageResponse;
import com.inspire.tasks.user.User;
import com.inspire.tasks.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BookRestController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthTokenFilter.class
        )
)
@Import(TestSecurityConfig.class)
class BookRestControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    BookService bookService;

    @MockitoBean
    UserService userService;


    private User user;

    @BeforeEach
    void setup() {
        user = new User("John Doe", "JDoe@email.com", "encoded-pass");
    }

    @WithMockUser(roles = "USER")
    @Test
    void getAllBooks_ReturnsList() throws Exception {
        Book b1 = new Book("Book 1", "John", "Test book", user);
        b1.setId(1L);
        Book b2 = new Book("Book 2", "Mary", "Test book", user);
        b2.setId(2L);

//        String requestBody = objectMapper.writeValueAsString(List.of(b1, b2));

        when(bookService.findAll()).thenReturn(List.of(b1, b2));

        mockMvc.perform(get("/api/lib/books"))
//                        .content(requestBody)
//                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Book 1"))
                .andExpect(jsonPath("$[0].authorName").value("John"))
                .andExpect(jsonPath("$[1].title").value("Book 2"))
                .andExpect(jsonPath("$[1].authorName").value("Mary"));

    }

    @WithMockUser
    @Test
    void findBooksByAuthor_ReturnsList() throws Exception {
        Book b1 = new Book("Book 1", "John", "Test book", user);
        Book b2 = new Book("Book 2", "John", "Test book", user);

        when(bookService.findAllByAuthorName("John")).thenReturn(List.of(b1, b2));

        mockMvc.perform(get("/api/lib/books/author/John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Book 1"))
                .andExpect(jsonPath("$[0].authorName").value("John"))
                .andExpect(jsonPath("$[1].title").value("Book 2"))
                .andExpect(jsonPath("$[1].authorName").value("John"));

    }

    @WithMockUser
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

    @WithMockUser(roles = "AUTHOR")
    @Test
    void createBook_ReturnsSuccess() throws Exception {
        BookRequest request = new BookRequest();
        request.setTitle("New Book");
        request.setDescription("Test Book");
        request.setAuthor("John");
        request.setUsername(user.getUsername());

        when(bookService.createBook(any(BookRequest.class)))
                .thenAnswer(invocation -> ResponseEntity.ok(
                        new MessageResponse(200, "Book created successfully!")));

        mockMvc.perform(post("/api/lib/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Book created successfully!"));
    }

    @WithMockUser(roles = "ADMIN")
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

    @WithMockUser(roles = "ADMIN")
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

    @WithMockUser
    @Test
    void getBookFromOpenLibrary_ReturnsBook() throws Exception {
        BookResponse response =
                new BookResponse("Clean Code", "Robert C. Martin", "Agile craftsmanship");

        when(bookService.findBookByNameOL("clean-code"))
                .thenReturn(response);

        mockMvc.perform(get("/api/lib/open-library/clean-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code"));
    }
}
