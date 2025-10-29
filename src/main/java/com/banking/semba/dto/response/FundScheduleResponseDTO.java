package com.banking.semba.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class FundScheduleResponseDTO {
    private String transactionId;
    private String status; // SCHEDULED / EXECUTED / FAILED
    private String message;
    private LocalDateTime createdAt;
}