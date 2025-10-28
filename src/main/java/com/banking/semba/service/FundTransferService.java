package com.banking.semba.service;

import com.banking.semba.GlobalException.GlobalException;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.FundTransferRequestDTO;
import com.banking.semba.dto.HttpResponseDTO;
import com.banking.semba.dto.OtpVerifyRequestDTO;
import com.banking.semba.dto.response.FundTransferResponse;
import com.banking.semba.dto.response.FundVerifyOtpResponse;
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
import java.util.List;

@Slf4j
@Service
public class FundTransferService {

    private final WebClient bankWebClient;
    private final ValidationUtil validationUtil;
    private final UserServiceUtils userUtils;

    private static final boolean USE_MOCK = true; // switch to false for real bank call
    private static final List<String> ALLOWED_TRANSFER_TYPES = List.of("IMPS", "NEFT", "RTGS");

    public FundTransferService(WebClient bankWebClient, ValidationUtil validationUtil, UserServiceUtils userUtils) {
        this.bankWebClient = bankWebClient;
        this.validationUtil = validationUtil;
        this.userUtils = userUtils;
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

    // -------------------------------- VERIFY OTP --------------------------------
    public HttpResponseDTO verifyOtp(String mobile, String ip, String deviceId,
                                     Double latitude, Double longitude, OtpVerifyRequestDTO otpRequest) {
        log.info("Verifying OTP | mobile={} | txnId={}", mobile, otpRequest.getTransactionId());

        try {
            validateRequest(mobile, ip, deviceId, latitude, longitude);

            if (otpRequest.getTransactionId() == null || otpRequest.getTransactionId().isBlank()) {
                throw new GlobalException(ValidationMessages.MISSING_TRANSACTION_ID, HttpStatus.BAD_REQUEST.value());
            }
            if (otpRequest.getOtpCode() == null || otpRequest.getOtpCode().isBlank()) {
                throw new GlobalException(ValidationMessages.OTP_REQUIRED, HttpStatus.BAD_REQUEST.value());
            }

            if (USE_MOCK) {
                if ("1234".equals(otpRequest.getOtpCode())) {
                    FundVerifyOtpResponse resp = new FundVerifyOtpResponse(
                            otpRequest.getTransactionId(),
                            true,
                            "₹1,000 transferred successfully via IMPS.",
                            LocalDateTime.now(),
                            "Jiya",
                            "232323454545",
                            "Shivangi",
                            "323232454545"
                    );
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
