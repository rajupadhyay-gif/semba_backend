package com.banking.semba.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDetailsDTO {
    private String transactionId;
    private PaymentType paymentType;
    private String fromUpi;
    private String toUpi;
    private String bankAccount;
    private Double amount;
    private String date;
    private String beneficiaryName;
    private String status;
    private String message;
}

