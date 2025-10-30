package com.banking.semba.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpResponseDTO {

    private String mobile;
    private String otpCode;
    private String message;
    private boolean success;
    private LocalDateTime sentAt;
    private int expirySeconds;
    private Map<String, Object> extra;
}
