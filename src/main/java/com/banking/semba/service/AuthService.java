package com.banking.semba.service;

import com.banking.semba.GlobalException.GlobalException;
import com.banking.semba.constants.LogMessages;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.ApiResponses;
import com.banking.semba.dto.SignupStartRequest;
import com.banking.semba.dto.VerifyOtpRequest;
import com.banking.semba.dto.response.BankOtpResponse;
import com.banking.semba.util.UserServiceUtils;
import com.banking.semba.util.ValidationUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class AuthService {

    private final WebClient bankWebClient;
    private final ValidationUtil validationUtil;
    private final UserServiceUtils userUtils;

    public AuthService(WebClient bankWebClient, ValidationUtil validationUtil, UserServiceUtils userUtils) {
        this.bankWebClient = bankWebClient;
        this.validationUtil = validationUtil;
        this.userUtils = userUtils;
    }

    private record BankOtpRequest(String mobile, String otp, String referralCode, String ip, String deviceId,
                                  Double latitude, Double longitude) {
    }

    // ---------------- Signup (Send OTP) ----------------
    public Mono<ApiResponses<BankOtpResponse>> signupStart(SignupStartRequest req) {
        String mobile = req.getMobile().trim();
        log.info(LogMessages.SIGNUP_REQUEST_RECEIVED, mobile);

        // Local validations
        userUtils.validateDeviceInfo(req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude(), mobile);
        userUtils.validateMobileNotBlank(mobile);
        validationUtil.validateIpFormat(req.getIp(), mobile);
        validationUtil.validateDeviceIdFormat(req.getDeviceId(), mobile);
        validationUtil.validateLocation(req.getLatitude(), String.valueOf(req.getLongitude()), mobile);

        BankOtpRequest bankRequest = new BankOtpRequest(
                mobile, null, req.getReferralCode(), req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude()
        );

        return bankWebClient.post()
                .uri("/posts") // replace with actual bank endpoint
                .bodyValue(bankRequest)
                .retrieve()
                .bodyToMono(BankOtpResponse.class)
//                .timeout(Duration.ofSeconds(10))
//                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(2)))
                .map(response -> {
                    log.info(LogMessages.OTP_SUCCESS, mobile);
                    return new ApiResponses<>("SUCCESS", HttpStatus.CREATED.value(), ValidationMessages.OTP_SENT_SUCCESS, response);
                })
                .onErrorMap(WebClientResponseException.class, ex -> {
                    log.error("Bank API HTTP error [{}]: {}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
                    return new GlobalException("Bank API failed: " + ex.getResponseBodyAsString(), ex.getRawStatusCode());
                })
                .onErrorMap(Exception.class, ex -> {
                    log.error("Unexpected error during signupStart for mobile {}: {}", mobile, ex.getMessage(), ex);
                    return new GlobalException("Unexpected error while calling bank API", HttpStatus.INTERNAL_SERVER_ERROR.value());
                });
    }

    // ---------------- Verify OTP ----------------
    public Mono<ApiResponses<BankOtpResponse>> verifyOtp(VerifyOtpRequest req) {
        String mobile = req.getMobile().trim();
        String otp = req.getOtp();
        log.info(LogMessages.OTP_VERIFY_REQUEST_RECEIVED, mobile);

        // Local validations
        userUtils.validateDeviceInfo(req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude(), mobile);
        userUtils.validateMobileNotBlank(mobile);
        userUtils.validateOtpNotBlank(otp, mobile);
        validationUtil.validateIpFormat(req.getIp(), mobile);
        validationUtil.validateDeviceIdFormat(req.getDeviceId(), mobile);
        validationUtil.validateLocation(req.getLatitude(), String.valueOf(req.getLongitude()), mobile);

        // Switch flag: true = use mock, false = call bank API
        boolean useMock = true; // change to false in production

        if (useMock) {
            // ---------------- MOCK BANK RESPONSE ----------------
            return Mono.fromSupplier(() -> {
                BankOtpResponse response = new BankOtpResponse();
                if ("1234".equals(otp)) {
                    response.setOtpValid(true); // OTP success
                    response.setTransactionId("TXN123456");
                } else {
                    response.setOtpValid(false); // OTP failed
                }

                return getBankOtpResponseApiResponses(mobile, response);
            });
        } else {
            // ---------------- CALL BANK API ----------------
            BankOtpRequest bankRequest = new BankOtpRequest(
                    mobile, otp, null, req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude()
            );

            return bankWebClient.post()
                    .uri("/otp/verify") // real bank endpoint
                    .bodyValue(bankRequest)
                    .retrieve()
                    .bodyToMono(BankOtpResponse.class)
                    .map(response -> {
                        return getBankOtpResponseApiResponses(mobile, response);
                    })
                    .onErrorMap(WebClientResponseException.class, ex -> {
                        log.error("Bank API HTTP error [{}]: {}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
                        return new GlobalException("Bank API failed: " + ex.getResponseBodyAsString(), ex.getRawStatusCode());
                    })
                    .onErrorMap(Exception.class, ex -> {
                        log.error("Unexpected error during OTP verification for mobile {}: {}", mobile, ex.getMessage(), ex);
                        return new GlobalException("Unexpected error while calling bank API", HttpStatus.INTERNAL_SERVER_ERROR.value());
                    });
        }
    }
//
    private ApiResponses<BankOtpResponse> getBankOtpResponseApiResponses(String mobile, BankOtpResponse response) {
        if (response.getOtpValid() == null || !response.getOtpValid()) {
            log.warn(LogMessages.OTP_INVALID, mobile);
            throw new GlobalException(ValidationMessages.OTP_INVALID, HttpStatus.BAD_REQUEST.value());
        }
        log.info(LogMessages.OTP_VERIFIED_SUCCESS, mobile);
        return new ApiResponses<>("SUCCESS", HttpStatus.OK.value(), ValidationMessages.OTP_VERIFIED_SUCCESS, response);
    }
}