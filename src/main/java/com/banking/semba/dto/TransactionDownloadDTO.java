package com.banking.semba.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TransactionDownloadDTO {
    private String transactionId;
    private String paymentType;
    private String toAccount;
    private String fromAccount;
    private String bankName;
    private String date;
    private Double amount;
    private String status;
    private String receiverName;

}

