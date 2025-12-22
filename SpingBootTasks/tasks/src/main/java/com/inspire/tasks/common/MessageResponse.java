package com.inspire.tasks.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageResponse {

    private String message;

    private int code;

    public MessageResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }
}