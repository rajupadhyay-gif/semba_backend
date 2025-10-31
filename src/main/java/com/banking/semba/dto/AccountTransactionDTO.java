package com.banking.semba.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
public class AccountTransactionDTO {

    private String date;
    private String details;
    private String chequeNo;
    private String type;
    private double debit;
    private double credit;
    private double balance;

    // Custom constructor used in dummy data
    public AccountTransactionDTO(String date, String details, String chequeNo, String type,double debit, double credit, double balance) {
        this.date = date;
        this.details = details;
        this.chequeNo = chequeNo;
        this.type=type;
        this.debit = debit;
        this.credit = credit;
        this.balance = balance;
    }
}
