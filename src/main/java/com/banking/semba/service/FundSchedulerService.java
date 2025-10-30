package com.banking.semba.service;

import com.banking.semba.globalException.GlobalException;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.FundScheduleRequestDTO;
import com.banking.semba.dto.HttpResponseDTO;
import com.banking.semba.dto.OtpVerifyRequestDTO;
import com.banking.semba.dto.response.FundVerifyResponse;
import com.banking.semba.util.UserServiceUtils;
import com.banking.semba.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FundSchedulerService {

    private final WebClient bankWebClient;
    private final UserServiceUtils userUtils;
    private final ValidationUtil validationUtil;

    private static final boolean USE_MOCK = true; //Toggle mock vs real bank
    private static final String BANK_SCHEDULE_URL = "/bank/transfer/schedule";
    private static final String BANK_EXECUTE_URL = "/bank/transfer/execute";
    private static final String BANK_VERIFY_OTP_URL = "/bank/transfer/verify-otp";

    // Mock store for in-memory testing
    private final Map<String, FundScheduleRequestDTO> scheduledTransfers = new ConcurrentHashMap<>();
    private final Set<String> executedTransfers = Collections.synchronizedSet(new HashSet<>());
    private static final List<String> ALLOWED_TRANSFER_TYPES = List.of("IMPS", "NEFT", "RTGS");

    public FundSchedulerService(WebClient bankWebClient, UserServiceUtils userUtils, ValidationUtil validationUtil) {
        this.bankWebClient = bankWebClient;
        this.userUtils = userUtils;
        this.validationUtil = validationUtil;
    }

    // ---------------- SCHEDULE TRANSFER ----------------
    public HttpResponseDTO scheduleTransfer(String mobile, String ip, String deviceId,
                                            Double latitude, Double longitude, FundScheduleRequestDTO req) {
        log.info("Scheduling transfer | mobile={} | from={} | to={} | amount={} | type={} | date={} | time={}",
                mobile, req.getFromAccountNumber(), req.getToAccountNumber(),
                req.getAmount(), req.getTransferType(), req.getScheduledDate(), req.getScheduledTime());

        try {
            //Common header + location validation
            userUtils.validateDeviceInfo(ip, deviceId, latitude, longitude, mobile);
            validationUtil.validateIpFormat(ip, mobile);
            validationUtil.validateDeviceIdFormat(deviceId, mobile);

            //Business-level validations
            validateScheduleRequest(req);

            // Generate transaction ID
            String txnId = "SCHED-" + System.currentTimeMillis();

            if (USE_MOCK) {
                scheduledTransfers.put(txnId, req);
                log.info("MOCK: Transfer scheduled successfully | txnId={}", txnId);

                return new HttpResponseDTO(
                        ValidationMessages.STATUS_OK,
                        HttpStatus.OK.value(),
                        "Scheduled transfer created successfully. Please verify OTP.",
                        Map.of("transactionId", txnId, "status", "PENDING")
                );
            }

            // Real Bank API call
            Object bankResponse = bankWebClient.post()
                    .uri(BANK_SCHEDULE_URL)
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            return new HttpResponseDTO(
                    ValidationMessages.STATUS_OK,
                    HttpStatus.OK.value(),
                    ValidationMessages.TRANSFER_INITIATED,
                    bankResponse
            );

        } catch (GlobalException ex) {
            log.warn("Validation failed during scheduling | reason={} | mobile={}", ex.getMessage(), mobile);
            return new HttpResponseDTO(ValidationMessages.STATUS_FAILED, ex.getStatus(), ex.getMessage(), null);

        } catch (WebClientResponseException ex) {
            log.error("Bank API failed during scheduling | status={} | body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new GlobalException(ValidationMessages.BANK_API_FAILED, ex.getStatusCode().value());

        } catch (Exception ex) {
            log.error("Unexpected error during scheduling | mobile={} | error={}", mobile, ex.getMessage(), ex);
            throw new GlobalException(ValidationMessages.UNKNOWN_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    // ---------------- VERIFY OTP ----------------
    public HttpResponseDTO verifyOtp(String mobile, String ip, String deviceId,
                                     Double latitude, Double longitude, OtpVerifyRequestDTO otpRequest) {
        log.info("Verifying OTP for scheduled transfer | mobile={} | txnId={}", mobile, otpRequest.getTransactionId());

        try {
            // Validate device info
            userUtils.validateDeviceInfo(ip, deviceId, latitude, longitude, mobile);

            //  Validate request params
            if (otpRequest.getTransactionId() == null || otpRequest.getTransactionId().isBlank()) {
                throw new GlobalException(ValidationMessages.MISSING_TRANSACTION_ID, HttpStatus.BAD_REQUEST.value());
            }

            //  Validate OTP not blank
            userUtils.validateOtpNotBlank(otpRequest.getOtpCode(), mobile);

            //  MOCK mode
            if (USE_MOCK) {
                if ("1234".equals(otpRequest.getOtpCode())) {
                    FundVerifyResponse response = FundVerifyResponse.builder()
                            .transactionId("INBDGH6757575757575")
                            .toName("Joya")
                            .toAccountNumber("232323454545")
                            .fromName("Shivangi")
                            .fromAccountNumber("232323454545")
                            .paymentMode("NEFT")
                            .amount(1000.00)
                            .remark("Scheduled Payment")
                            .scheduledDateTime(LocalDateTime.of(2025, 8, 25, 17, 36))
                            .scheduledDate(LocalDate.of(2025, 8, 26))
                            .build();

                    log.info("MOCK: OTP verified successfully | txnId={}", otpRequest.getTransactionId());
                    return new HttpResponseDTO(
                            ValidationMessages.STATUS_OK,
                            HttpStatus.OK.value(),
                            ValidationMessages.OTP_VERIFIED_SUCCESS,
                            response
                    );
                } else {
                    log.warn("Invalid OTP for txnId={} | mobile={}", otpRequest.getTransactionId(), mobile);
                    throw new GlobalException(ValidationMessages.INVALID_OTP, HttpStatus.BAD_REQUEST.value());
                }
            }

            // REAL BANK CALL
            FundVerifyResponse bankResponse = bankWebClient.post()
                    .uri(BANK_VERIFY_OTP_URL)
                    .bodyValue(otpRequest)
                    .retrieve()
                    .bodyToMono(FundVerifyResponse.class)
                    .block();

            assert bankResponse != null;
            log.info("Bank OTP verification success | txnId={}", bankResponse.getTransactionId());

            return new HttpResponseDTO(
                    ValidationMessages.STATUS_OK,
                    HttpStatus.OK.value(),
                    ValidationMessages.OTP_VERIFIED_SUCCESS,
                    bankResponse
            );

        } catch (WebClientResponseException ex) {
            log.error("Bank API error | status={} | body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return new HttpResponseDTO(
                    ValidationMessages.STATUS_FAILED,
                    ex.getStatusCode().value(),
                    "Bank API failed: " + ex.getResponseBodyAsString(),
                    null
            );

        } catch (GlobalException ex) {
            log.warn("Business validation failed | msg={} | mobile={}", ex.getMessage(), mobile);
            return new HttpResponseDTO(
                    ValidationMessages.STATUS_FAILED,
                    ex.getStatus(),
                    ex.getMessage(),
                    null
            );

        } catch (Exception ex) {
            log.error("Unexpected error verifying OTP: {}", ex.getMessage(), ex);
            return new HttpResponseDTO(
                    ValidationMessages.STATUS_FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    ValidationMessages.UNKNOWN_ERROR,
                    null
            );
        }
    }
    // ---------------- MOCK SCHEDULER JOB ----------------
    @Scheduled(fixedRate = 30000)
    public void executeScheduledTransfers() {
        if (scheduledTransfers.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();

        scheduledTransfers.forEach((txnId, req) -> {
            LocalDateTime triggerTime = req.getScheduledDate().atTime(req.getScheduledTime());
            if (now.isAfter(triggerTime) && !executedTransfers.contains(txnId)) {
                log.info("Triggering scheduled transfer | txnId={}", txnId);
                executeTransfer(txnId, req);
            }
        });
    }

    // ---------------- EXECUTE TRANSFER ----------------
    private void executeTransfer(String txnId, FundScheduleRequestDTO req) {
        try {
            if (USE_MOCK) {
                log.info("MOCK EXECUTED: {} -> {} ₹{} | Type={}", req.getFromAccountNumber(),
                        req.getToAccountNumber(), req.getAmount(), req.getTransferType());
                executedTransfers.add(txnId);
                return;
            }

            bankWebClient.post()
                    .uri(BANK_EXECUTE_URL)
                    .bodyValue(req)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            executedTransfers.add(txnId);
            log.info(" Real transfer executed successfully | txnId={}", txnId);

        } catch (WebClientResponseException ex) {
            log.error("Bank execution failed | txnId={} | status={} | body={}", txnId, ex.getStatusCode(), ex.getResponseBodyAsString());
            retryFailedTransfer(txnId, req);
        } catch (Exception ex) {
            log.error("Execution failed | txnId={} | {}", txnId, ex.getMessage());
        }
    }

    // ---------------- RETRY LOGIC ----------------
    private void retryFailedTransfer(String txnId, FundScheduleRequestDTO req) {
        log.warn("Retrying failed transfer | txnId={}", txnId);
        try {
            Thread.sleep(5000); // Wait before retry
            executeTransfer(txnId, req);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Retry interrupted | txnId={}", txnId);
        }
    }

    // ---------------- VALIDATION ----------------
    private void validateScheduleRequest(FundScheduleRequestDTO req) {
        if (req.getFromAccountNumber() == null || req.getFromAccountNumber().isBlank())
            throw new GlobalException(ValidationMessages.FROM_ACCOUNT_REQUIRED, HttpStatus.BAD_REQUEST.value());

        if (req.getToAccountNumber() == null || req.getToAccountNumber().isBlank())
            throw new GlobalException(ValidationMessages.TO_ACCOUNT_REQUIRED, HttpStatus.BAD_REQUEST.value());

        if (req.getAmount() == null || req.getAmount().doubleValue() <= 0)
            throw new GlobalException(ValidationMessages.INVALID_AMOUNT, HttpStatus.BAD_REQUEST.value());

        if (req.getTransferType() == null || !ALLOWED_TRANSFER_TYPES.contains(req.getTransferType().toUpperCase()))
            throw new GlobalException(ValidationMessages.INVALID_TRANSFER_TYPE, HttpStatus.BAD_REQUEST.value());

        if (req.getScheduledDate() == null || req.getScheduledDate().isBefore(LocalDate.now()))
            throw new GlobalException("Scheduled date must be today or future date", HttpStatus.BAD_REQUEST.value());

        if (req.getScheduledTime() == null)
            throw new GlobalException("Scheduled time cannot be null", HttpStatus.BAD_REQUEST.value());

        if (req.getScheduledDate().isEqual(LocalDate.now()) && req.getScheduledTime().isBefore(LocalTime.now()))
            throw new GlobalException("Scheduled time must be in future", HttpStatus.BAD_REQUEST.value());
    }
}
