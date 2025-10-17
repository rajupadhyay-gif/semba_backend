package com.banking.semba.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Profile {
    private String mobile;
    private String fullName;
    private String email;
    private String accountType;
    private String accountNumber;
    private String ifsc;
    private String bankName;
    private Double balance;
}
