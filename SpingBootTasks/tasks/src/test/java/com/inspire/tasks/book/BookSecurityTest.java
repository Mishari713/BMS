package com.inspire.tasks.book;


import com.inspire.tasks.user.User;
import com.inspire.tasks.common.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookSecurityTest {

    @Mock
    private BookService bookService;

    @InjectMocks
    private BookSecurity bookSecurity;

    @Test
    void userCanEdit_AsOwner_ReturnsTrue() {
        // Arrange
        Long bookId = 1L;
        String username = "author1";

        User owner = new User();
        owner.setUsername(username);

        Book book = new Book();
        book.setId(bookId);
        book.setUserId(owner);

        when(bookService.findById(bookId)).thenReturn(book);

        // Mock SecurityContext
        SecurityContext context = mock(SecurityContext.class);
        SecurityContextHolder.setContext(context);
        when(context.getAuthentication())
                .thenReturn(new UsernamePasswordAuthenticationToken(username, null));

        // Act
        boolean result = bookSecurity.userCanEdit(bookId);

        // Assert
        assertTrue(result);
    }

    @Test
    void userCanEdit_AsNotOwner_ThrowsUnauthorizedException() {
        // Arrange
        Long bookId = 1L;
        String username = "notAuthor";

        User owner = new User();
        owner.setUsername("author1");

        Book book = new Book();
        book.setId(bookId);
        book.setUserId(owner);

        when(bookService.findById(bookId)).thenReturn(book);

        // Mock SecurityContext
        SecurityContext context = mock(SecurityContext.class);
        SecurityContextHolder.setContext(context);
        when(context.getAuthentication())
                .thenReturn(new UsernamePasswordAuthenticationToken(username, null));

        // Act & Assert
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> bookSecurity.userCanEdit(bookId)
        );

        assertEquals("Only the owner can modify this book.", exception.getMessage());
    }
}
