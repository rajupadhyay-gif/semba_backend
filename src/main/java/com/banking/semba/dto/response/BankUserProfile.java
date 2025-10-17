package com.banking.semba.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class BankUserProfile {
    private String mobile;
    private String fullName;
    private String email;
    private List<BankAccount> accounts;
}