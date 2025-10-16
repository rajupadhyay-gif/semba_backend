package com.banking.semba.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BankMpinRequest {

    private String mobile;

    private String mpin;

    private String confirmMpin;

    private String ip;

    private String deviceId;

    private Double latitude;

    private Double longitude;
}
