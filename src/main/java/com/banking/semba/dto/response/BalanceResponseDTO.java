package com.banking.semba.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponseDTO {
    private String accountNumber;
    private BigDecimal balance;
    private String currency;
    private String accountType;     // Savings, Current, etc.
    private String bankName;
}