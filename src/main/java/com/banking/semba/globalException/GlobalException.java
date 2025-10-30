package com.banking.semba.globalException;

import lombok.Getter;

@Getter
public class GlobalException extends RuntimeException {
    private final int status;

    public GlobalException(String message, int status) {
        super(message);
        this.status = status;
    }

    public GlobalException(int status, String message) {
        super(message);
        this.status = status;
    }
}
