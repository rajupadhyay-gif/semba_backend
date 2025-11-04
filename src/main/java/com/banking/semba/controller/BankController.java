package com.banking.semba.controller;

import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.*;
import com.banking.semba.security.JwtTokenService;
import com.banking.semba.service.AccountService;
import com.banking.semba.service.BankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("semba/api/")
@RequiredArgsConstructor
public class BankController {

    private final AccountService accountService;
    private final JwtTokenService jwtService;
    private final BankService bankService;

    /* Fetch Account Details */
    @GetMapping("/accounts/{id}")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getAccount(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude) {

        String mobile = jwtService.extractMobileFromHeader(authHeader);

        ApiResponseDTO<Map<String, Object>> response =
                accountService.getAccountById(id, mobile, ip, deviceId, latitude, longitude);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /*Fetch Live Balance */
    @GetMapping("/accounts/{accountNumber}/balance")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getBalance(
            @PathVariable String accountNumber,
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude) {

        String mobile = jwtService.extractMobileFromHeader(authHeader);

        ApiResponseDTO<Map<String, Object>> response =
                accountService.getLiveBalance(accountNumber, mobile, ip, deviceId, latitude, longitude);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/payments")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> makePayment(
            @RequestBody FundTransferDTO dto,
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude) {

        String mobile = jwtService.extractMobileFromHeader(authHeader);

        ApiResponseDTO<Map<String, Object>> response =
                accountService.transferFunds(dto, mobile, ip, deviceId, latitude, longitude);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/download-statement")
    public ResponseEntity<byte[]> downloadStatement(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @RequestParam String accountNumber,
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "pdf") String format) {

        String mobile = jwtService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }

        byte[] fileBytes = accountService.downloadStatementFile(
                auth, ip, deviceId, latitude, longitude, accountNumber, fromDate, toDate, type, format);

        String contentType = format.equalsIgnoreCase("excel") ?
                "application/xlsx" : "application/pdf";

        String fileExtension = format.equalsIgnoreCase("excel") ? "xlsx" : "pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=statement." + fileExtension)
                .contentType(MediaType.parseMediaType(contentType))
                .body(fileBytes);
    }

    @GetMapping("/filterTransactions")
    public ResponseEntity<HttpResponseDTO> getTransactions(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @RequestParam String accountNumber,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "150") int limit
    ) {
        String mobile = jwtService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            HttpResponseDTO response = new HttpResponseDTO(
                    ValidationMessages.FAILED,
                    HttpStatus.UNAUTHORIZED.value(),
                    ValidationMessages.USER_NOT_FOUND
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        ResponseEntity<HttpResponseDTO> serviceResponse = bankService.getTransactions(
                mobile, accountNumber, fromDate, toDate, limit, searchTerm, ip, deviceId, latitude, longitude
        );
        return serviceResponse;
    }

    @GetMapping("/defaultTransactions")
    public ResponseEntity<HttpResponseDTO> searchTransactions(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @RequestParam String accountNumber,
            @RequestParam (required = false)String searchTerm
    ) {
        String mobile = jwtService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            HttpResponseDTO response = new HttpResponseDTO(
                    ValidationMessages.FAILED,
                    HttpStatus.UNAUTHORIZED.value(),
                    ValidationMessages.USER_NOT_FOUND
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        ResponseEntity<HttpResponseDTO> serviceResponse= bankService.searchTransactions(mobile, ip, deviceId, latitude, longitude, accountNumber, searchTerm);
        return serviceResponse;
    }

    @GetMapping("/downloadStatement")
    public ResponseEntity<HttpResponseDTO> downloadAccountStatement(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @RequestParam String accountNumber,
            @RequestParam(required = false) String range,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate
    ) {
        String mobile = jwtService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            HttpResponseDTO response = new HttpResponseDTO(
                    ValidationMessages.FAILED,
                    HttpStatus.UNAUTHORIZED.value(),
                    ValidationMessages.USER_NOT_FOUND
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        HttpResponseDTO response = bankService.downloadAccountStatement(
                mobile, ip, deviceId, latitude, longitude, accountNumber, range, fromDate, toDate
        );
        return ResponseEntity.status(response.getResponseCode()).body(response);

    }

    @GetMapping("/downloadTransactionStatement")
    public ResponseEntity<HttpResponseDTO> downloadTransactionStatement(
            @RequestHeader("Authorization") String auth,
            @RequestHeader("X-IP") String ip,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader(value = "X-Latitude", required = false) Double latitude,
            @RequestHeader(value = "X-Longitude", required = false) Double longitude,
            @RequestParam String accountNumber,
            @RequestParam(required = false) String range,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate
    ) {
        String mobile = jwtService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            HttpResponseDTO response = new HttpResponseDTO(
                    ValidationMessages.FAILED,
                    HttpStatus.UNAUTHORIZED.value(),
                    ValidationMessages.USER_NOT_FOUND
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        HttpResponseDTO response = bankService.downloadTransactionStatement(
                mobile, ip, deviceId, latitude, longitude, accountNumber, range, fromDate, toDate
        );
        return ResponseEntity.status(response.getResponseCode()).body(response);

    }

}