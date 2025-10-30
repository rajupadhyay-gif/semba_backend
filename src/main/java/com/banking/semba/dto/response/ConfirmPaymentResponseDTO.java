package com.banking.semba.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfirmPaymentResponseDTO {
    private Double amount;
    private String toName;
    private String bankDetails; // e.g., "Bank of Baroda - 232323454545"
    private String paymentMethod; // e.g., "NEFT"
    private String otpStatus; // "OTP sent successfully"
}