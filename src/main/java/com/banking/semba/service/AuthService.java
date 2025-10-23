package com.banking.semba.service;

import com.banking.semba.GlobalException.GlobalException;
import com.banking.semba.constants.LogMessages;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.*;
import com.banking.semba.dto.response.BankLoginResponse;
import com.banking.semba.dto.response.BankMpinResponse;
import com.banking.semba.dto.response.BankOtpResponse;
import com.banking.semba.security.JwtTokenService;
import com.banking.semba.util.UserServiceUtils;
import com.banking.semba.util.ValidationUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AuthService {

    private final WebClient bankWebClient;
    private final ValidationUtil validationUtil;
    private final UserServiceUtils userUtils;
    private final JwtTokenService jwtTokenService;

    public AuthService(WebClient bankWebClient, ValidationUtil validationUtil, UserServiceUtils userUtils, JwtTokenService jwtTokenService) {
        this.bankWebClient = bankWebClient;
        this.validationUtil = validationUtil;
        this.userUtils = userUtils;
        this.jwtTokenService = jwtTokenService;
    }

    private record BankOtpRequest(String mobile, String otp, String referralCode, String ip, String deviceId,
                                  Double latitude, Double longitude) {
    }

    // ---------------- Signup (Send OTP) ----------------
    public Mono<ApiResponseDTO<BankOtpResponse>> signupStart(SignupStartRequest req) {
        String mobile = req.getMobile().trim();
        log.info(LogMessages.SIGNUP_REQUEST_RECEIVED, mobile);


        // Local validations
        userUtils.validateDeviceInfo(req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude(), mobile);
        userUtils.validateMobileNotBlank(mobile);
        validationUtil.validateIpFormat(req.getIp(), mobile);
        validationUtil.validateDeviceIdFormat(req.getDeviceId(), mobile);
        validationUtil.validateLocation(req.getLatitude(), String.valueOf(req.getLongitude()), mobile);

        if (!mobile.matches("^[6-9][0-9]{9}$")) {
            log.warn(LogMessages.MOBILE_INVALID_PATTERN, mobile);
            throw new GlobalException(ValidationMessages.MOBILE_INVALID_PATTERN, HttpStatus.BAD_REQUEST.value());
        }

        BankOtpRequest bankRequest = new BankOtpRequest(mobile, null, req.getReferralCode(), req.getIp(),
                req.getDeviceId(), req.getLatitude(), req.getLongitude());

        return bankWebClient.post()
                .uri("/posts") // replace with actual bank endpoint
                .bodyValue(bankRequest)
                .retrieve()
                .bodyToMono(BankOtpResponse.class)
                .map(response -> {
                    log.info(LogMessages.OTP_SUCCESS, mobile);
                    return new ApiResponseDTO<>(
                            "SUCCESS",
                            HttpStatus.CREATED.value(),
                            ValidationMessages.OTP_SENT_SUCCESS,
                            response);

                }).onErrorMap(WebClientResponseException.class, ex -> {
                    log.error(LogMessages.BANK_API_ERROR, ex.getStatusCode().value(), ex.getResponseBodyAsString());
                    return new GlobalException(ValidationMessages.BANKING_FAILED + ex.getResponseBodyAsString(),
                            ex.getStatusCode().value());
                }).onErrorMap(Exception.class, ex -> {
                    log.error(LogMessages.SIGNUP_REQUEST_UNEXCEPTED,mobile, ex.getMessage(), ex);
                    return new GlobalException(ValidationMessages.ERROR_CALL_API,
                            HttpStatus.INTERNAL_SERVER_ERROR.value());
                });

    }

    // ---------------- Verify OTP ----------------
    public Mono<ApiResponseDTO<BankOtpResponse>> verifyOtp(VerifyOtpRequest req) {
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
            BankOtpRequest bankRequest = new BankOtpRequest(mobile, otp, null, req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude());

            return bankWebClient
                    .post()
                    .uri("/otp/verify") // real bank endpoint
                    .bodyValue(bankRequest)
                    .retrieve()
                    .bodyToMono(BankOtpResponse.class)
                    .map(response -> {
                        return getBankOtpResponseApiResponses(mobile, response);

                    }).onErrorMap(WebClientResponseException.class, ex -> {
                        log.error("Bank API HTTP error [{}]: {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
                        return new GlobalException("Bank API failed: " + ex.getResponseBodyAsString(), ex.getStatusCode().value());
                    }).onErrorMap(Exception.class, ex -> {
                        log.error(LogMessages.OTP_VERIFY_FAILED, mobile, ex.getMessage(), ex);
                        return new GlobalException(ValidationMessages.OTP_FAILED_BANK, HttpStatus.INTERNAL_SERVER_ERROR.value());
                    });
        }
    }

    public Mono<ApiResponseDTO<BankMpinResponse>> setMpin(@Valid BankMpinRequest req) {
        String mobile = req.getMobile().trim();
        String mpin = req.getMpin();
        String confirmMpin = req.getConfirmMpin();

        log.info(LogMessages.SET_MPIN_REQUEST_RECEIVED, mobile);

        userUtils.validateDeviceInfo(req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude(), mobile);
        userUtils.validateMobileNotBlank(mobile);
        userUtils.validateMpinNotBlank(req.getMpin(), mobile);
        userUtils.validateConfirmMpinNotBlank(req.getConfirmMpin(), mobile);

        // Check if MPIN and Confirm MPIN match
        if (!req.getMpin().equals(req.getConfirmMpin())) {
            log.warn(LogMessages.MPIN_NOT_MATCH, mobile);
            throw new GlobalException(ValidationMessages.MPIN_NOT_MATCH, HttpStatus.BAD_REQUEST.value());
        }

        // Switch: mock or real
        boolean useMock = true; // set false for real bank

        if (useMock) {
            // ---------------- MOCK RESPONSE ----------------
            return Mono.fromSupplier(() -> {
                BankMpinResponse response = new BankMpinResponse("TXN123456", "MPIN set successfully");
                log.info("Mock MPIN set success for {}", mobile);
                return new ApiResponseDTO<>("SUCCESS",
                        HttpStatus.OK.value(),
                        "MPIN set successfully",
                        response);
            });
        } else {
            // ---------------- CALL REAL BANK ----------------
            BankMpinRequest bankRequest = new BankMpinRequest(mobile, mpin, confirmMpin, req.getDeviceId(),
                    req.getIp(), req.getLatitude(), req.getLatitude());

            return bankWebClient.post()
                    .uri("/mpin/set") // real bank endpoint
                    .bodyValue(bankRequest)
                    .retrieve()
                    .bodyToMono(BankMpinResponse.class)
                    .map(response -> new ApiResponseDTO<>("SUCCESS",
                            HttpStatus.OK.value(),
                            ValidationMessages.MPIN_SET_SUCCESS,
                            response))

                    .onErrorMap(WebClientResponseException.class, ex -> {
                        log.error("Bank API error [{}]: {}",ex.getStatusCode().value(), ex.getResponseBodyAsString());
                        return new GlobalException("Bank API failed: " + ex.getResponseBodyAsString(),
                                ex.getStatusCode().value());
                    }).onErrorMap(Exception.class, ex -> {
                        log.error(LogMessages.MPIN_ERROR, mobile, ex.getMessage(), ex);
                        return new GlobalException("Unexpected error while calling bank API",
                                HttpStatus.INTERNAL_SERVER_ERROR.value());
                    });
        }
    }

    public Mono<ApiResponseDTO<Map<String, Object>>> login(LoginRequest req) {
        String mobile = req.getMobile().trim();
        String mpin = req.getMpin();
        log.info(LogMessages.LOGIN_REQUEST, mobile);

        // ---------------- Local validations ----------------
        userUtils.validateDeviceInfo(req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude(), mobile);
        userUtils.validateMobileNotBlank(mobile);
        userUtils.validateMpinNotBlank(mpin, mobile);

        validationUtil.validateIpFormat(req.getIp(), mobile);
        validationUtil.validateDeviceIdFormat(req.getDeviceId(), mobile);
        validationUtil.validateLocation(req.getLatitude(), String.valueOf(req.getLongitude()), mobile);

        boolean useMock = true;

        if (useMock) {
            // ---------------- MOCK Bank verification ----------------
            return Mono.fromSupplier(() -> {
                if (!"1234".equals(mpin)) {
                    throw new GlobalException(ValidationMessages.MPIN_INVALID, HttpStatus.UNAUTHORIZED.value());
                }

                // Generate internal JWT
                String accessToken = jwtTokenService.generateAccessToken(Map.of("mobile", mobile, "deviceId", req.getDeviceId()), mobile);

                Map<String, Object> data = new HashMap<>();
                data.put("accessToken", accessToken);
                data.put("bankJwt", "MOCK_BANK_JWT_" + mobile);

                log.info(LogMessages.LOGIN_SUCCESS, mobile);
                return new ApiResponseDTO<>("SUCCESS", HttpStatus.OK.value(), ValidationMessages.LOGIN_SUCCESS, data);
            });
        } else {
            // ---------------- Call real bank API ----------------
            BankLoginRequest bankRequest = new BankLoginRequest();
            bankRequest.setMobile(mobile);
            bankRequest.setMpin(mpin);
            bankRequest.setDeviceId(req.getDeviceId());
            bankRequest.setIp(req.getIp());
            bankRequest.setLatitude(req.getLatitude());
            bankRequest.setLongitude(req.getLongitude());

            return bankWebClient.post()
                    .uri("/bank/login") // replace with real bank endpoint
                    .bodyValue(bankRequest)
                    .retrieve()
                    .bodyToMono(BankLoginResponse.class).
                    flatMap(bankResp -> {
                        if (!bankResp.isSuccess()) {
                            return Mono.error(new GlobalException(bankResp.getMessage(), HttpStatus.UNAUTHORIZED.value()));
                        }

                        // Generate internal JWT
                        String accessToken = jwtTokenService.generateAccessToken(Map.of(
                                "mobile", mobile,
                                "deviceId",
                                req.getDeviceId()), mobile);

                        Map<String, Object> data = new HashMap<>();
                        data.put("accessToken", accessToken);
                        data.put("bankJwt", bankResp.getBankJwt());

                        log.info(LogMessages.LOGIN_SUCCESS, mobile);
                        return Mono.just(new ApiResponseDTO<>(
                                "SUCCESS",
                                HttpStatus.OK.value(),
                                ValidationMessages.LOGIN_SUCCESS,
                                data));
                    }).onErrorMap(WebClientResponseException.class, ex -> {
                        log.error("Bank API failed [{}]: {}",ex.getStatusCode().value(), ex.getResponseBodyAsString());
                        return new GlobalException("Bank API failed: " + ex.getResponseBodyAsString(),
                                ex.getStatusCode().value());
                    }).onErrorMap(Exception.class, ex -> {
                        log.error("Unexpected error during login for {}: {}", mobile, ex.getMessage(), ex);
                        return new GlobalException("Unexpected error during login",
                                HttpStatus.INTERNAL_SERVER_ERROR.value());
                    });
        }
    }

    private ApiResponseDTO<BankOtpResponse> getBankOtpResponseApiResponses(String mobile, BankOtpResponse response) {
        if (response.getOtpValid() == null || !response.getOtpValid()) {
            log.warn(LogMessages.OTP_INVALID, mobile);
            throw new GlobalException(ValidationMessages.OTP_INVALID, HttpStatus.BAD_REQUEST.value());
        }
        log.info(LogMessages.OTP_VERIFIED_SUCCESS, mobile);
        return new ApiResponseDTO<>(
                "SUCCESS",
                HttpStatus.OK.value(),
                ValidationMessages.OTP_VERIFIED_SUCCESS,
                response);
    }
}

