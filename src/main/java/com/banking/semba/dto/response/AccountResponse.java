package com.banking.semba.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private String customerId;
    private String name;
    private String accountNumber;
    private String ifsc;
    private String bankName;
    private String branch;
    private String upiId;
    private Double totalBalance;
    private Map<String, Double> breakdown; // e.g., withdraw, hold funds
}