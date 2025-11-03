package com.banking.semba.globalException;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class CustomException extends RuntimeException {
    private final HttpStatus errorCode;

    public CustomException(String message, HttpStatus errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public HttpStatusCode getErrorCode() {
        return errorCode;
    }
}
