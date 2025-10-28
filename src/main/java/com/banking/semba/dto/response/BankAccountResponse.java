package com.banking.semba.dto.response;

import lombok.Data;

import java.util.Map;

@Data
public class BankAccountResponse {
    private boolean success;
    private Map<String, Object> account;
}