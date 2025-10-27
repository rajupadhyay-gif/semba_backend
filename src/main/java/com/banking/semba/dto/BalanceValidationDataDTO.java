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

    @NotNull(message = "Entered amount is required")
    @Min(value = 1, message = "Entered amount must be greater than zero")
    private Double enteredAmount;
    private Double availableBalance;
    private String message;
}
