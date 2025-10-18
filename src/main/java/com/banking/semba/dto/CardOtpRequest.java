package com.banking.semba.dto;


import lombok.Data;

@Data
public class CardOtpRequest {
    private String cardNumber;
    private String otp;
    private String ip;
    private String deviceId;
    private Double latitude;
    private Double longitude;
}


