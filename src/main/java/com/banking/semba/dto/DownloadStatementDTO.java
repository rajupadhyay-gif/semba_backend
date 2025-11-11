package com.banking.semba.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DownloadStatementDTO {

    private String accountHolderName;
    private String address;
    private LocalDateTime statementDate;
    private String accountNumber;
    private String accountDescription;
    private String branch;
    private double drawingPower;
    private double interestRate;
    private String cifNumber;
    private String ifscCode;
    private String micrCode;
    private String ckYcNumber;
    private String nominationRegistered;
    private double balanceAsOn;
    private String searchPeriod;

    private List<AccountTransactionDTO> transactions;

    public DownloadStatementDTO(String s, String s1, String accountNumber, String s2, String savingsAccount, String s3, double v, List<AccountTransactionDTO> txns) {
    }
}

