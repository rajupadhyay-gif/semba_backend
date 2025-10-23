package com.banking.semba.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentPaymentsDTO {
    private Integer id;
    private String senderMobile;
    private String receiverMobile;
    private Double amount;
    private String status;
    private LocalDateTime transactionTime;
}
