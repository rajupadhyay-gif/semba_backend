package com.banking.semba.dto;

import lombok.Data;

@Data
public class BankLoginRequest {
    private String mobile;
    private String mpin;
    private String deviceId;
    private String ip;
    private Double latitude;
    private Double longitude;
}