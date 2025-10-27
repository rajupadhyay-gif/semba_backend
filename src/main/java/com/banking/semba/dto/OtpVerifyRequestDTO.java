package com.banking.semba.dto;


import lombok.Data;

@Data
public class OtpVerifyRequestDTO {

    private String transactionId;
    private String otpCode;
}