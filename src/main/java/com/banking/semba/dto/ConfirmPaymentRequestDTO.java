package com.banking.semba.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPaymentRequestDTO {

    private String transactionId;
    private String toName;
    private String bankName;
    private String toAccountNumber;
    private String transferType;
    private Double amount;
}