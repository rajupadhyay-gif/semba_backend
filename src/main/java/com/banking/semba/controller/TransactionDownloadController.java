package com.banking.semba.controller;

import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.ApiResponseDTO;
import com.banking.semba.dto.TransactionDownloadDTO;
import com.banking.semba.security.JwtTokenService;
import com.banking.semba.service.TransactionDownloadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/semba/api/transactions")
public class TransactionDownloadController {

    private final TransactionDownloadService transactionDownloadService;
    private final JwtTokenService JwtTokenService;

    public TransactionDownloadController(TransactionDownloadService transactionDownloadService, JwtTokenService jwtTokenService) {
        this.transactionDownloadService = transactionDownloadService;
        JwtTokenService = jwtTokenService;
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadTransaction(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @RequestParam String transactionId,
            @RequestParam(defaultValue = "json") String format) { // format=json or pdf

        String mobile = JwtTokenService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            ApiResponseDTO<TransactionDownloadDTO> unauthorizedResponse = new ApiResponseDTO<>(
                    ValidationMessages.STATUS_UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value(),
                    ValidationMessages.INVALID_JWT,
                    null
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(unauthorizedResponse);
        }

        if (format.equalsIgnoreCase("pdf")) {
            return transactionDownloadService.downloadTransactionPDF(auth, ip, deviceId, latitude, longitude, transactionId);
        }

        ApiResponseDTO<TransactionDownloadDTO> response =
                transactionDownloadService.fetchTransactionDetails(auth, ip, deviceId, latitude, longitude, transactionId);

        return ResponseEntity.status(response.getResponseCode()).body(response);
    }

}

