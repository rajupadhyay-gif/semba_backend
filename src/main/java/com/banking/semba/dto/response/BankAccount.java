package com.banking.semba.dto.response;

import lombok.Data;

@Data
public class BankAccount {
    private String accountNumber;
    private String accountType;
    private Double balance;
    private String ifsc;
    private String bankName;
    private boolean active;
}