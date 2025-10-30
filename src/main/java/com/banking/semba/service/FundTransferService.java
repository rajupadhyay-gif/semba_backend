package com.banking.semba.service;

import com.banking.semba.GlobalException.GlobalException;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.*;
import com.banking.semba.dto.response.ConfirmPaymentResponseDTO;
import com.banking.semba.dto.response.FundTransferResponse;
import com.banking.semba.dto.response.FundVerifyOtpResponse;
import com.banking.semba.util.OtpUtil;
import com.banking.semba.util.UserServiceUtils;
import com.banking.semba.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FundTransferService {

    private final WebClient bankWebClient;
    private final ValidationUtil validationUtil;
    private final UserServiceUtils userUtils;
    private final OtpUtil otpUtil;

    private static final boolean USE_MOCK = true; // switch to false for real bank call
    private static final List<String> ALLOWED_TRANSFER_TYPES = List.of("IMPS", "NEFT", "RTGS");

    public FundTransferService(WebClient bankWebClient, ValidationUtil validationUtil, UserServiceUtils userUtils,OtpUtil otpUtil) {
        this.bankWebClient = bankWebClient;
        this.validationUtil = validationUtil;
        this.userUtils = userUtils;
        this.otpUtil = otpUtil;
    }

    // -------------------------------- COMMON HEADER VALIDATION --------------------------------
    /** Common header validations */
    private void validateRequest(String mobile, String ip, String deviceId,
                                 Double latitude, Double longitude) {
        userUtils.validateDeviceInfo(ip, deviceId, latitude, longitude, mobile);
        validationUtil.validateIpFormat(ip, mobile);
        validationUtil.validateDeviceIdFormat(deviceId, mobile);

        if (latitude != null && longitude != null) {
            validationUtil.validateLocation(latitude, String.valueOf(longitude), mobile);
        }
    }

    // -------------------------------- HEADERS --------------------------------
    private HttpHeaders buildHeaders(String mobile, String ip, String deviceId, Double latitude, Double longitude) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-IP", ip);
        headers.set("X-Device-Id", deviceId);
        if (latitude != null) headers.set("X-Latitude", latitude.toString());
        if (longitude != null) headers.set("X-Longitude", longitude.toString());
        headers.set("Authorization", mobile);
        return headers;
    }

    // -------------------------------- INITIATE TRANSFER --------------------------------
    public HttpResponseDTO initiateTransfer(String mobile, String ip, String deviceId, Double latitude,
                                            Double longitude, FundTransferRequestDTO request) {
        log.info("Initiating fund transfer | mobile={} | from={} | to={} | amount={} | type={}",
                mobile, request.getFromAccountNumber(), request.getToAccountNumber(),
                request.getAmount(), request.getTransferType());

        try {
            // Header + body validations
            validateRequest(mobile, ip, deviceId, latitude, longitude);
            validateFundTransferRequest(request);

            // MOCK response (for testing without bank integration)
            if (USE_MOCK) {
                FundTransferResponse mockResponse = new FundTransferResponse(
                        request.getFromAccountNumber(),
                        request.getAmount(),
                        "INR"
                );
                mockResponse.setTransactionId("TXN-MOCK-" + System.currentTimeMillis());
                mockResponse.setTransferType(request.getTransferType());
                mockResponse.setCreatedAt(LocalDateTime.now());

                log.info("Transfer initiated successfully (MOCK) | mobile={} | txnId={}",
                        mobile, mockResponse.getTransactionId());

                return new HttpResponseDTO(
                        ValidationMessages.STATUS_OK,
                        HttpStatus.OK.value(),
                        ValidationMessages.TRANSFER_INITIATED,
                        mockResponse);
            }

            // REAL Bank API call
            FundTransferResponse response = bankWebClient.post()
                    .uri("/bank/transfer/initiate")
                    .headers(h -> h.addAll(buildHeaders(mobile, ip, deviceId, latitude, longitude)))
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FundTransferResponse.class)
                    .block();

            return new HttpResponseDTO(
                    ValidationMessages.STATUS_OK,
                    HttpStatus.OK.value(),
                    ValidationMessages.TRANSFER_INITIATED,
                    response);

        } catch (GlobalException ex) {
            log.warn("Validation failed during transfer initiation | reason={} | mobile={}", ex.getMessage(), mobile);
            return new HttpResponseDTO(ValidationMessages.STATUS_FAILED, ex.getStatus(), ex.getMessage(), null);

        } catch (WebClientResponseException ex) {
            log.error("Bank API ERROR | status={} | body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new GlobalException(ValidationMessages.BANK_API_FAILED, ex.getStatusCode().value());

        } catch (Exception ex) {
            log.error("Unexpected error during transfer initiation | mobile={} | error={}", mobile, ex.getMessage(), ex);
            throw new GlobalException(ValidationMessages.UNKNOWN_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    public HttpResponseDTO confirmPayment(String mobile, String ip, String deviceId,
                                          Double latitude, Double longitude,
                                          ConfirmPaymentRequestDTO request) {
        log.info("Confirming payment | txnId={} | amount={}", request.getTransactionId(), request.getAmount());
        try {
            // Validate request parameters
            validateRequest(mobile, ip, deviceId, latitude, longitude);

            if (request.getTransactionId() == null || request.getTransactionId().isBlank()) {
                throw new GlobalException(ValidationMessages.MISSING_TRANSACTION_ID, HttpStatus.BAD_REQUEST.value());
            }

            ConfirmPaymentResponseDTO responseDTO;

            // — Fetch payment details (MOCK or REAL)
            if (USE_MOCK) {
                responseDTO = ConfirmPaymentResponseDTO.builder()
                        .toName(request.getToName() != null ? request.getToName() : "Unknown")
                        .otpStatus("OTP sent successfully")
                        .paymentMethod(request.getTransferType() != null ? request.getTransferType() : "NEFT")
                        .bankDetails((request.getTransferType() != null ? request.getTransferType() : "NEFT")
                                + " - " + request.getToAccountNumber())
                        .amount(request.getAmount())
                        .build();

                log.info("Fetched MOCK payment details for txnId={}", request.getTransactionId());
            } else {
                // Real Bank Call
                responseDTO = bankWebClient.post()
                        .uri("/bank/transfer/details")
                        .headers(h -> h.addAll(buildHeaders(mobile, ip, deviceId, latitude, longitude)))
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(ConfirmPaymentResponseDTO.class)
                        .block();

                log.info("Fetched REAL payment details for txnId={}", request.getTransactionId());
            }

            // — Prepare and send OTP
            OtpSendRequestDTO otpRequest = OtpSendRequestDTO.builder()
                    .mobile(mobile)
                    .context("TRANSFER")
                    .referenceId(request.getTransactionId())
                    .build();

            HttpHeaders headers = buildHeaders(mobile, ip, deviceId, latitude, longitude);
            otpUtil.sendOtp(otpRequest, headers); // only triggers OTP, not part of response

            // — Final clean response
            return new HttpResponseDTO(
                    ValidationMessages.STATUS_OK,
                    HttpStatus.OK.value(),
                    ValidationMessages.OTP_SENT_SUCCESS,
                    responseDTO
            );

        } catch (GlobalException ex) {
            log.warn("Confirm payment failed | reason={} | mobile={}", ex.getMessage(), mobile);
            return new HttpResponseDTO(
                    ValidationMessages.STATUS_FAILED,
                    ex.getStatus(),
                    ex.getMessage(),
                    null
            );

        } catch (WebClientResponseException ex) {
            log.error("Bank API error during confirmPayment | status={} | body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new GlobalException(ValidationMessages.BANK_API_FAILED, ex.getStatusCode().value());

        } catch (Exception ex) {
            log.error("Unexpected error during confirmPayment | mobile={} | error={}", mobile, ex.getMessage(), ex);
            throw new GlobalException(ValidationMessages.UNKNOWN_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    // -------------------------------- VERIFY OTP --------------------------------
    public HttpResponseDTO verifyOtp(String mobile, String ip, String deviceId,
                                     Double latitude, Double longitude, OtpVerifyRequestDTO otpRequest) {
        log.info("Verifying OTP | mobile={} | txnId={}", mobile, otpRequest.getTransactionId());

        //  Validate OTP not blank
        userUtils.validateOtpNotBlank(otpRequest.getOtpCode(), mobile);
        try {
            validateRequest(mobile, ip, deviceId, latitude, longitude);

            if (otpRequest.getTransactionId() == null || otpRequest.getTransactionId().isBlank()) {
                throw new GlobalException(ValidationMessages.MISSING_TRANSACTION_ID, HttpStatus.BAD_REQUEST.value());
            }
            userUtils.validateOtpNotBlank(otpRequest.getOtpCode(), mobile);


            if (USE_MOCK) {
                if ("1234".equals(otpRequest.getOtpCode())) {
                    FundVerifyOtpResponse resp = FundVerifyOtpResponse.builder()
                            .transactionId("INBDGH6757575757575")
                            .success(true)
                            .message("₹1,000 sent successfully via IMPS.")
                            .completedAt(LocalDateTime.now())
                            .toName("Joya")
                            .toAccount("232323454545")
                            .fromName("Shivangi")
                            .fromAccount("323232454545")
                            .transferType("IMPS")
                            .amount(new BigDecimal("1000.00"))
                            .remark("Payment Successful")
                            .build();

                    log.info("OTP verified successfully (MOCK) | txnId={}", otpRequest.getTransactionId());
                    return new HttpResponseDTO(ValidationMessages.STATUS_OK, HttpStatus.OK.value(),
                            ValidationMessages.OTP_VERIFIED_SUCCESS, resp);
                } else {
                    log.warn("Invalid OTP entered | mobile={} | txnId={}", mobile, otpRequest.getTransactionId());
                    throw new GlobalException(ValidationMessages.OTP_INVALID, HttpStatus.BAD_REQUEST.value());
                }
            }

            FundVerifyOtpResponse response = bankWebClient.post()
                    .uri("/bank/transfer/verify-otp")
                    .headers(h -> h.addAll(buildHeaders(mobile, ip, deviceId, latitude, longitude)))
                    .bodyValue(otpRequest)
                    .retrieve()
                    .bodyToMono(FundVerifyOtpResponse.class)
                    .block();

            return new HttpResponseDTO(
                    ValidationMessages.STATUS_OK,
                    HttpStatus.OK.value(),
                    ValidationMessages.OTP_VERIFIED_SUCCESS,
                    response);

        } catch (GlobalException ex) {
            log.warn("OTP verification failed | reason={} | mobile={}", ex.getMessage(), mobile);
            return new HttpResponseDTO(
                    ValidationMessages.STATUS_FAILED,
                    ex.getStatus(),
                    ex.getMessage(),
                    null);

        } catch (WebClientResponseException ex) {
            log.error("Bank API error during OTP verification | status={} | body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new GlobalException(ValidationMessages.BANK_API_FAILED, ex.getStatusCode().value());

        } catch (Exception ex) {
            log.error("Unexpected error during OTP verification | mobile={} | error={}", mobile, ex.getMessage(), ex);
            throw new GlobalException(
                    ValidationMessages.UNKNOWN_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    // -------------------------------- BODY VALIDATION --------------------------------
    private void validateFundTransferRequest(FundTransferRequestDTO req) {
        if (req.getFromAccountNumber() == null || req.getFromAccountNumber().isBlank()) {
            throw new GlobalException(ValidationMessages.FROM_ACCOUNT_REQUIRED, HttpStatus.BAD_REQUEST.value());
        }
        if (req.getToAccountNumber() == null || req.getToAccountNumber().isBlank()) {
            throw new GlobalException(ValidationMessages.TO_ACCOUNT_REQUIRED, HttpStatus.BAD_REQUEST.value());
        }
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new GlobalException(ValidationMessages.INVALID_AMOUNT, HttpStatus.BAD_REQUEST.value());
        }
        if (req.getTransferType() == null || !ALLOWED_TRANSFER_TYPES.contains(req.getTransferType().toUpperCase())) {
            throw new GlobalException(ValidationMessages.INVALID_TRANSFER_TYPE, HttpStatus.BAD_REQUEST.value());
        }
    }
}
