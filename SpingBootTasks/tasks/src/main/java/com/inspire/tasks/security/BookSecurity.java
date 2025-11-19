package com.inspire.tasks.security;

import com.inspire.tasks.book.Book;
import com.inspire.tasks.book.BookService;
import com.inspire.tasks.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("bookSecurity")
@Slf4j
public class BookSecurity {

    private static final Logger log = LoggerFactory.getLogger(BookSecurity.class);
    @Autowired
    private BookService bookService;

    public boolean userCanEdit(Long bookId) {
        Book book = bookService.findById(bookId);

        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        if(!book.getAuthor().getUsername().equals(currentUser)){
            log.warn("Unauthorized edit attempt: User {} tried to modify book {}", currentUser, bookId);
            throw new UnauthorizedException("Only the owner can modify this book.");
        }
        return true;
    }
}
