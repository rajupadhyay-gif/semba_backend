package com.banking.semba.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class BalanceValidationDataDTO {
    private Double enteredAmount;
    private String message;
    private String transactionId;
}
