package com.inspire.tasks.payload.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BookRequest {
    @NotBlank
    String author;

    @NotBlank
    String title;

    @NotBlank
    String description;

    @NotBlank
    String username;

}