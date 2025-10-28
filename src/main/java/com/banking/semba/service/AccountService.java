package com.banking.semba.service;

import com.banking.semba.GlobalException.GlobalException;
import com.banking.semba.constants.LogMessages;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.ApiResponseDTO;
import com.banking.semba.dto.FundTransferDTO;
import com.banking.semba.dto.response.AccountResponse;
import com.banking.semba.dto.response.PaymentResponse;
import com.banking.semba.util.UserServiceUtils;
import com.banking.semba.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final WebClient bankWebClient;
    private final UserServiceUtils userUtils;
    private final ValidationUtil validationUtil;

    private static final String BASE_ACCOUNT_URL = "https://jsonplaceholder.typicode.com";
    private static final boolean useMock = true;

    // Fetch Account Details//
    public ApiResponseDTO<Map<String, Object>> getAccountById(Long id, String mobile, String ip, String deviceId,
                                                              Double latitude, Double longitude) {
        log.info(LogMessages.ACCOUNT_FETCH_START, mobile);
        validateRequest(mobile, ip, deviceId, latitude, longitude);

        Map<String, Object> data = new HashMap<>();
        AccountResponse account;

        if (useMock) {
            account = getMockAccount();
        } else {
            try {
                String url = BASE_ACCOUNT_URL + "/1";

                account = bankWebClient.get()
                        .uri(url)
                        .headers(headers -> {
                            headers.set(HttpHeaders.AUTHORIZATION, mobile);
                            headers.set("X-Device-Id", deviceId);
                            headers.set("X-IP", ip);
                            if (latitude != null) headers.set("X-Latitude", latitude.toString());
                            if (longitude != null) headers.set("X-Longitude", longitude.toString());
                            headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                        })
                        .retrieve()
                        .bodyToMono(AccountResponse.class)
                        .block();

                if (account == null) {
                    throw new GlobalException(
                            ValidationMessages.ACCOUNT_FETCH_FAILED,
                            HttpStatus.BAD_REQUEST.value()
                    );
                }

            } catch (WebClientResponseException ex) {
                log.error(LogMessages.BANK_API_ERROR, ex.getStatusCode().value(), ex.getResponseBodyAsString());
                throw new GlobalException(
                        ValidationMessages.BANK_API_FAILED + ": " + ex.getResponseBodyAsString(),
                        ex.getStatusCode().value()
                );

            } catch (Exception ex) {
                log.error(LogMessages.ACCOUNT_FETCH_FAILED, mobile, ex.getMessage(), ex);
                throw new GlobalException(
                        ValidationMessages.UNKNOWN_ERROR,
                        HttpStatus.INTERNAL_SERVER_ERROR.value()
                );
            }
        }
        data.put("account", account);
        log.info(LogMessages.ACCOUNT_FETCH_SUCCESS, mobile);
        return new ApiResponseDTO<>(ValidationMessages.STATUS_OK, HttpStatus.OK.value(),
                ValidationMessages.ACCOUNT_FETCH_SUCCESS, data);
    }

    // Fetch Live Balance //
    public ApiResponseDTO<Map<String, Object>> getLiveBalance(String accountNumber, String mobile,
                                                              String ip, String deviceId,
                                                              Double latitude, Double longitude) {
        validateRequest(mobile, ip, deviceId, latitude, longitude);

        Map<String, Object> data = new HashMap<>();
        BigDecimal balance;

        if (useMock) {
           balance = BigDecimal.valueOf(2500.35);
        } else {
            try {
                String url = BASE_ACCOUNT_URL + "/" + accountNumber + "/balance";

                balance = bankWebClient.get()
                        .uri(url)
                        .headers(headers -> {
                            headers.set(HttpHeaders.AUTHORIZATION, mobile);
                            headers.set("X-Device-Id", deviceId);
                            headers.set("X-IP", ip);
                            if (latitude != null) headers.set("X-Latitude", latitude.toString());
                            if (longitude != null) headers.set("X-Longitude", longitude.toString());
                            headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                        })
                        .retrieve()
                        .bodyToMono(BigDecimal.class)
                        .block();

                if (balance == null) balance = BigDecimal.ZERO;

            } catch (WebClientResponseException ex) {
                log.error("Bank API error while fetching balance: {} - {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
                throw new GlobalException(ValidationMessages.BANK_API_FAILED, ex.getStatusCode().value());

            } catch (Exception ex) {
                log.error("Unexpected error while fetching balance: {}", ex.getMessage(), ex);
                throw new GlobalException(ValidationMessages.UNKNOWN_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        }

        data.put("balance", balance);
        return new ApiResponseDTO<>(ValidationMessages.STATUS_OK, HttpStatus.OK.value(),
                ValidationMessages.ACCOUNT_FETCH_SUCCESS, data);
    }

    //Fund Transfer//
    public ApiResponseDTO<Map<String, Object>> transferFunds(FundTransferDTO dto, String mobile,
                                                             String ip, String deviceId,
                                                             Double latitude, Double longitude) {
        validateRequest(mobile, ip, deviceId, latitude, longitude);
        validateTransfer(dto, mobile);

        if (dto.getTransactionId() == null)
            dto.setTransactionId(UUID.randomUUID().toString());

        Map<String, Object> data = new HashMap<>();
        PaymentResponse paymentResponse;

        if (useMock) {
            paymentResponse = new PaymentResponse("SUCCESS",
                    "Mock " + dto.getPaymentType() + " transfer completed",
                    dto.getTransactionId());
        } else {
            try {
                String url = switch (dto.getPaymentType()) {
                    case UPI -> "/payments/upi";
                    case MOBILE -> "/payments/mobile";
                    case BANK -> "/payments/transfer";
                    case CREDIT_CARD -> "/payments/credit-card";
                    case DEBIT_CARD -> "/payments/debit-card";
                };

                paymentResponse = bankWebClient.post()
                        .uri(url)
                        .headers(headers -> {
                            headers.set(HttpHeaders.AUTHORIZATION, mobile);
                            headers.set("X-Device-Id", deviceId);
                            headers.set("X-IP", ip);
                            if (latitude != null) headers.set("X-Latitude", latitude.toString());
                            if (longitude != null) headers.set("X-Longitude", longitude.toString());
                            headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                        })
                        .bodyValue(dto)
                        .retrieve()
                        .bodyToMono(PaymentResponse.class)
                        .block();

                if (paymentResponse == null) {
                    paymentResponse = new PaymentResponse(ValidationMessages.STATUS_FAILED,
                            "Bank did not return a response",
                            dto.getTransactionId());
                }

            } catch (WebClientResponseException ex) {
                log.error("Bank API error during fund transfer: {} - {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
                throw new GlobalException(ValidationMessages.BANK_API_FAILED, ex.getStatusCode().value());

            } catch (Exception ex) {
                log.error("Unexpected error during fund transfer: {}", ex.getMessage(), ex);
                throw new GlobalException(ValidationMessages.UNKNOWN_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        }

        data.put("payment", paymentResponse);
        return new ApiResponseDTO<>(
                ValidationMessages.STATUS_OK,
                HttpStatus.OK.value(),
                ValidationMessages.BANK_TRANSACTION,
                data);
    }

    /** Common header validations */
    private void validateRequest(String mobile, String ip, String deviceId,
                                 Double latitude, Double longitude) {
        userUtils.validateDeviceInfo(ip, deviceId, latitude, longitude, mobile);
        validationUtil.validateIpFormat(ip, mobile);
        validationUtil.validateDeviceIdFormat(deviceId, mobile);
        if (latitude != null && longitude != null)
            validationUtil.validateLocation(latitude, String.valueOf(longitude), mobile);
    }

    /** Transfer request validations */
    private void validateTransfer(FundTransferDTO dto, String mobile) {
        if (dto.getFromAccount() == null || dto.getToAccount() == null || dto.getAmount() == null)
            throw new GlobalException("Invalid transfer request", HttpStatus.BAD_REQUEST.value());
    }

    /** Mock account for testing */
    private AccountResponse getMockAccount() {
        Map<String, Double> breakdown = new HashMap<>();
        breakdown.put("NetWithdraw", 23500.0);
        breakdown.put("OverDraft", 0.0);
        breakdown.put("SweepBalance", 0.0);
        breakdown.put("UnclearedFunds", 0.0);
        breakdown.put("HoldFunds", 0.0);

        return new AccountResponse(
                "1227277878",
                "Ms. Jaya",
                "122234343434",
                "BRIB00022243",
                "BRI",
                "Gomti Nagar Lucknow",
                "123456789@ybl",
                2500.35,
                breakdown
        );
    }
}