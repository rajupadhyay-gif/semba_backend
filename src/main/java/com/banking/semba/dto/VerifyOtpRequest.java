package com.banking.semba.dto;

import lombok.Data;

@Data
public class VerifyOtpRequest {

    private String mobile;

    private String otp;

    private String ip;

    private String deviceId;

    private Double latitude;

    private Double longitude;
}
