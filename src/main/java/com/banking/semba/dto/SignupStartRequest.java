package com.banking.semba.dto;

import lombok.Data;

@Data
public class SignupStartRequest {

    private String mobile;

    private String referralCode;

    private String ip;

    private String deviceId;

    private Double latitude;

    private Double longitude;
}
