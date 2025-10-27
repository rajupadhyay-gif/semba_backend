package com.banking.semba.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceValidationRequestDTO {
    private String accountNumber;
    private Double enteredAmount;
    private String mpin;
}
