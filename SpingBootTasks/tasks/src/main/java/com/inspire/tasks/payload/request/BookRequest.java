package com.inspire.tasks.payload.request;

import jakarta.validation.constraints.*;

public class BookRequest {
    @NotBlank
    String author;

    @NotBlank
    String title;

    @NotBlank
    String description;

    @NotBlank
    String username;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}