package com.banking.semba.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class BalanceValidationDataDTO {
    private Double enteredAmount;
    private Double availableBalance;
    private String message;
    private String transactionId;
}
