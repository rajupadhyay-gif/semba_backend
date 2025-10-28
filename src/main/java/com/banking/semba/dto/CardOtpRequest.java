package com.banking.semba.dto;


import lombok.Data;

@Data
public class CardOtpRequest {
    private String cardNumber;    // masked or plain card number
    private String otp;           // 6-digit OTP from user
    private String cardType;      // "DEBIT" or "CREDIT"
    private String bankCode;      // bank identifier
}


