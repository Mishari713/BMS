package com.inspire.tasks.book.dto;

public record BookResponse(
        String title,
        String authorName,
        String description
) {}