package com.inspire.tasks.book;

import com.inspire.tasks.exception.BadRequestException;
import com.inspire.tasks.payload.request.BookRequest;
import com.inspire.tasks.payload.response.MessageResponse;
import com.inspire.tasks.user.User;
import com.inspire.tasks.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private BookService bookService;


    @Test
    void createBook_Success() {
        BookRequest request = new BookRequest();
        request.setTitle("My Book");
        request.setAuthor("John");
        request.setDescription("A great book");
        request.setUsername("john doe");

        User user = new User();
        user.setUsername("john doe");

        when(bookRepository.existsByTitle("My Book")).thenReturn(false);
        when(userService.findByUsername("john doe")).thenReturn(user);
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = bookService.createBook(request);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof MessageResponse);

        MessageResponse body = (MessageResponse) response.getBody();
        assertEquals("Book created successfully!", body.getMessage());
    }

    @Test
    void createBook_TitleExists_ThrowsException() {
        BookRequest request = new BookRequest();
        request.setTitle("My Book");

        when(bookRepository.existsByTitle("My Book")).thenReturn(true);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> bookService.createBook(request)
        );

        assertEquals("Error: A book with this title already exists.", ex.getMessage());
    }

    @Test
    void createBook_UserNotFound_ThrowsException() {
        BookRequest request = new BookRequest();
        request.setTitle("My Book");
        request.setAuthor("John");
        request.setDescription("Desc");
        request.setUsername("unknown");

        when(bookRepository.existsByTitle("My Book")).thenReturn(false);
        when(userService.findByUsername("unknown"))
                .thenThrow(new BadRequestException("Username: unknown doesn't exists"));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> bookService.createBook(request)
        );

        assertTrue(ex.getMessage().contains("unknown"));
    }

    @Test
    void findById_Success() {
        Book book = new Book();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        Book result = bookService.findById(1L);

        assertNotNull(result);
    }

    @Test
    void findById_NotFound_ThrowsException() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> bookService.findById(1L)
        );

        assertEquals("Book id : 1 doesn't exists", ex.getMessage());
    }

    @Test
    void save_ReturnsSuccessMessage() {
        Book book = new Book();
        when(bookRepository.save(book)).thenReturn(book);

        ResponseEntity<?> response = bookService.save(book);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof MessageResponse);
    }

    @Test
    void deleteById_Success() {
        doNothing().when(bookRepository).deleteById(1L);

        ResponseEntity<?> response = bookService.deleteById(1L);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void findAll_ReturnsList() {
        Book b1 = new Book();
        Book b2 = new Book();

        when(bookRepository.findAll()).thenReturn(Arrays.asList(b1, b2));

        var result = bookService.findAll();

        assertEquals(2, result.size());
    }

    @Test
    void findAllByAuthorName_ReturnsList() {
        Book b1 = new Book();

        when(bookRepository.findAllByAuthorName("john"))
                .thenReturn(Arrays.asList(b1));

        var result = bookService.findAllByAuthorName("john");

        assertEquals(1, result.size());
    }

    @Test
    void findAllByTitle_Success() {
        Book book = new Book();
        when(bookRepository.findByTitle("mybook"))
                .thenReturn(Optional.of(book));

        Book result = bookService.findAllByTitle("mybook");

        assertNotNull(result);
    }

    @Test
    void findAllByTitle_NotFound_ThrowsException() {
        when(bookRepository.findByTitle("missing"))
                .thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> bookService.findAllByTitle("missing")
        );

        assertEquals("Book title : missing doesn't exists", ex.getMessage());
    }
}
