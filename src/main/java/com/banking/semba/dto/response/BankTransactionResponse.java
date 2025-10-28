package com.banking.semba.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankTransactionResponse {
    private boolean success;
    private String transactionId;
    private String message;
    private Double amount;
    private String cardType;
}