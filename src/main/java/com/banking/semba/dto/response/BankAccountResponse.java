package com.banking.semba.dto.response;

import lombok.Data;

@Data
public class BankAccountResponse {
    private boolean success;
    private String message;
    private BankAccount account;
}