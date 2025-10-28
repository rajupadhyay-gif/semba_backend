package com.banking.semba.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayNowRequest {

    private String cardNumber;   // 16-digit PAN
    private String holderName;   // Card holder
    private String validThru;    // MM/YY
    private String mpin;         // 4-digit MPIN
    private Double amount;       // Transaction amount
    private String bankCode;     // For identifying bank
    private String accountNumber; // For debit card linkage
    private String cardType;     // CREDIT or DEBIT
    private String transactionNote; // Optional (for receipts/logs)
}