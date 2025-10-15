package com.banking.semba.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankOtpResponse {

    private String transactionId; // Unique transaction ID
    private String mobile;        // Mobile number for OTP
    private int expiresIn;        // OTP expiry in seconds
    private String message;
    private Boolean otpValid;  // Optional message from bank
}