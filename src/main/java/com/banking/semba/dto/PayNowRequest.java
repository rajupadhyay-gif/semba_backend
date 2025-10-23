package com.banking.semba.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayNowRequest {

    private String cardNumber;
    private String holderName;
    private String validThru;
    private String mpin;
    private Double amount;
    private String bankCode;
}