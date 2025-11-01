package com.banking.semba.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TransactionResponseDTO {
    private String merchantName;
    private String transactionType;
    private double amount;
    private String paymentMode;
    private LocalDateTime transactionDate;
}

