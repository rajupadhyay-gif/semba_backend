package com.banking.semba.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TransactionRequestDTO {
    private String accountNumber;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String filterType;
    private String transactionType;
    private int limit = 150;

}

