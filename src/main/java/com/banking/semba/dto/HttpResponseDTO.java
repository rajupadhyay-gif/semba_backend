package com.banking.semba.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
public class HttpResponseDTO {
    private String status;
    private int responseCode;
    private String responseMessage;
    private Object responseData;

    public HttpResponseDTO(String status, int responseCode, String responseMessage) {
        this.status = status;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
    }

    public HttpResponseDTO(String status, int responseCode, String responseMessage, Object responseData) {
        this.status = status;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.responseData = responseData;
    }
}
