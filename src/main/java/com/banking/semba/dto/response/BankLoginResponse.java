package com.banking.semba.dto.response;

import lombok.Data;

@Data
public class BankLoginResponse {
    private boolean success;
    private String bankJwt; // JWT issued by bank
    private String message;
}