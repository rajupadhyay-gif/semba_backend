package com.banking.semba.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponseDTO<T> {
    private String status;
    private int responseCode;
    private String responseMessage;
    private T data;

    public ApiResponseDTO(String status, int responseCode, String responseMessage) {
        this.status = status;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
    }

}