package com.inspire.tasks.book;

import com.inspire.tasks.common.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("bookSecurity")
@Slf4j
public class BookSecurity {

    @Autowired
    private BookService bookService;

    public boolean userCanEdit(Long bookId) {
        Book book = bookService.findById(bookId);

        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        if(!book.getUserId().getUsername().equals(currentUser)){
            log.warn("Unauthorized edit attempt: User {} tried to modify book {}", currentUser, bookId);
            throw new UnauthorizedException("Only the owner can modify this book.");
        }
        return true;
    }
}
