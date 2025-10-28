package com.banking.semba.dto;

import lombok.Data;

@Data
public class CardRequest {

    private String cardNumber;     // 16-digit card number
    private String holderName;     // Name on card
    private String validThru;      // Expiry date MM/YY
    private String cardType;       // "DEBIT" or "CREDIT"
    private String bankCode;       // Bank unique code (e.g., HDFC01, SBI02)
}