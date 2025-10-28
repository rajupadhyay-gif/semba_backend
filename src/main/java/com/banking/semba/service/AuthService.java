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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class AuthService {

    private final WebClient bankWebClient;
    private final ValidationUtil validationUtil;
    private final UserServiceUtils userUtils;
    private final JwtTokenService jwtTokenService;

    private static final boolean USE_MOCK = true; // Switch mock/real

    public AuthService(WebClient bankWebClient, ValidationUtil validationUtil, UserServiceUtils userUtils, JwtTokenService jwtTokenService) {
        this.bankWebClient = bankWebClient;
        this.validationUtil = validationUtil;
        this.userUtils = userUtils;
        this.jwtTokenService = jwtTokenService;
    }

    // ---------------- Bank Request DTOs ----------------
    private record BankOtpRequest(String mobile, String otp, String referralCode, String ip, String deviceId,
                                  Double latitude, Double longitude) {
    }

    private record BankLoginRequest(String mobile, String mpin, String ip, String deviceId,
                                    Double latitude, Double longitude) {
    }

    // ---------------- COMMON VALIDATION ----------------
    private void validateCommon(String mobile, String ip, String deviceId, Double latitude, Double longitude) {
        userUtils.validateMobileNotBlank(mobile);
        userUtils.validateDeviceInfo(ip, deviceId, latitude, longitude, mobile);
        validationUtil.validateIpFormat(ip, mobile);
        validationUtil.validateDeviceIdFormat(deviceId, mobile);
        validationUtil.validateLocation(latitude, longitude != null ? longitude.toString() : null, mobile);

        if (!mobile.matches("^[6-9][0-9]{9}$")) {
            log.warn(LogMessages.MOBILE_INVALID_PATTERN, mobile);
            throw new GlobalException(ValidationMessages.MOBILE_INVALID_PATTERN, HttpStatus.BAD_REQUEST.value());
        }
    }

    public HttpHeaders buildHeaders(String mobile, String ip, String deviceId, Double latitude, Double longitude) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, mobile);
        headers.set("X-IP", ip);
        headers.set("X-Device-Id", deviceId);
        if (latitude != null) headers.set("X-Latitude", latitude.toString());
        if (longitude != null) headers.set("X-Longitude", longitude.toString());
        return headers;
    }

    // ---------------- SIGNUP START (Send OTP) ----------------
    public Mono<ApiResponseDTO<BankOtpResponse>> signupStart(SignupStartRequest req) {
        String mobile = req.getMobile().trim();
        validateCommon(mobile, req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude());

        if (USE_MOCK) {
            return Mono.fromSupplier(() -> {
                BankOtpResponse response = new BankOtpResponse();
                response.setOtpValid(true);
                response.setTransactionId("TXN-MOCK-OTP-123");
                log.info("OTP sent successfully (MOCK) for mobile: {}. Transaction ID: {}", mobile, response.getTransactionId());
                return new ApiResponseDTO<>(ValidationMessages.STATUS_OK, HttpStatus.CREATED.value(), ValidationMessages.OTP_SENT_SUCCESS, response);
            });
        }

        BankOtpRequest bankRequest = new BankOtpRequest(mobile, null, req.getReferralCode(),
                req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude());

        return bankWebClient.post()
                .uri("/bank/send-otp")
                .headers(h -> h.addAll(buildHeaders(mobile, req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude())))
                .bodyValue(bankRequest)
                .retrieve()
                .bodyToMono(BankOtpResponse.class)
                .map(resp -> {
                    log.info("OTP sent successfully for mobile: {}. Transaction ID: {}", mobile, resp.getTransactionId());
                    return new ApiResponseDTO<>(
                            ValidationMessages.STATUS_OK,
                            HttpStatus.CREATED.value(),
                            ValidationMessages.OTP_SENT_SUCCESS,
                            resp);
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("BANK API ERROR during OTP send for mobile: {}. Status: {}, ResponseBody: {}", mobile, ex.getStatusCode().value(), ex.getResponseBodyAsString(), ex);
                    return Mono.error(new GlobalException(ValidationMessages.BANKING_FAILED + ": " + ex.getResponseBodyAsString(), ex.getStatusCode().value()));
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Unexpected exception during OTP send for mobile: {}. Error: {}", mobile, ex.getMessage(), ex);
                    return Mono.error(new GlobalException(ValidationMessages.ERROR_CALL_API + ": " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
                });
    }

    // ---------------- VERIFY OTP ----------------
    public Mono<ApiResponseDTO<BankOtpResponse>> verifyOtp(VerifyOtpRequest req) {
        String mobile = req.getMobile().trim();
        String otp = req.getOtp();

        validateCommon(mobile, req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude());
        userUtils.validateOtpNotBlank(otp, mobile);

        if (USE_MOCK) {
            return Mono.fromSupplier(() -> {
                BankOtpResponse response = new BankOtpResponse();
                if ("1234".equals(otp)) {
                    response.setOtpValid(true);
                    response.setTransactionId("TXN-MOCK-VERIFY-123");
                    log.info("OTP verified successfully (MOCK) for mobile: {}. Transaction ID: {}", mobile, response.getTransactionId());
                    return new ApiResponseDTO<>(ValidationMessages.STATUS_OK, HttpStatus.OK.value(), ValidationMessages.OTP_VERIFIED_SUCCESS, response);
                } else {
                    response.setOtpValid(false);
                    log.warn("{\"event\":\"otp_invalid_attempt\",\"mobile\":\"{}\"}",mobile);
                    throw new GlobalException(ValidationMessages.OTP_INVALID, HttpStatus.BAD_REQUEST.value());
                }
            });
        }

        BankOtpRequest bankRequest = new BankOtpRequest(mobile, otp, null, req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude());

        return bankWebClient.post()
                .uri("/bank/verify-otp")
                .headers(h -> h.addAll(buildHeaders(mobile, req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude())))
                .bodyValue(bankRequest)
                .retrieve()
                .bodyToMono(BankOtpResponse.class)
                .map(resp -> {
                    if (resp.getOtpValid() == null || !resp.getOtpValid()) {
                        log.warn("{\"event\":\"otp_verification_failed\",\"mobile\":\"{}\",\"transactionId\":\"{}\"}",mobile, resp.getTransactionId());
                        throw new GlobalException(
                                ValidationMessages.OTP_INVALID,
                                HttpStatus.BAD_REQUEST.value());
                    }
                    log.info("OTP verified successfully for mobile: {}. Transaction ID: {}", mobile, resp.getTransactionId());
                    return new ApiResponseDTO<>(
                            ValidationMessages.STATUS_OK,
                            HttpStatus.OK.value(),
                            ValidationMessages.OTP_VERIFIED_SUCCESS, resp);
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("BANK API ERROR during OTP verification for mobile: {}. Status: {}, ResponseBody: {}", mobile, ex.getStatusCode().value(), ex.getResponseBodyAsString(), ex);
                    return Mono.error(new GlobalException(ValidationMessages.BANKING_FAILED + ": " + ex.getResponseBodyAsString(), ex.getStatusCode().value()));
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Unexpected exception during OTP verification for mobile: {}. Error: {}", mobile, ex.getMessage(), ex);
                    return Mono.error(new GlobalException(ValidationMessages.ERROR_CALL_API + ": " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
                });
    }

    // ---------------- SET MPIN ----------------
    public Mono<ApiResponseDTO<BankMpinResponse>> setMpin(@Valid BankMpinRequest req) {
        String mobile = req.getMobile().trim();
        String mpin = req.getMpin();
        String confirmMpin = req.getConfirmMpin();

        validateCommon(mobile, req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude());
        userUtils.validateMpinNotBlank(mpin, mobile);
        userUtils.validateConfirmMpinNotBlank(confirmMpin, mobile);

        if (!mpin.equals(confirmMpin)) {
             log.warn("{\"event\":\"mpin_mismatch\",\"mobile\":\"{}\"}", mobile);
            throw new GlobalException(ValidationMessages.MPIN_NOT_MATCH, HttpStatus.BAD_REQUEST.value());
        }

        if (USE_MOCK) {
            return Mono.fromSupplier(() -> {
                BankMpinResponse response = new BankMpinResponse();
                response.setTransactionId("TXN-MOCK-MPIN-123");
                response.setMessage(ValidationMessages.MPIN_SET_SUCCESS);
                log.info("Mock MPIN set successfully for mobile: {}. Transaction ID: {}", mobile, response.getTransactionId());
                return new ApiResponseDTO<>(ValidationMessages.STATUS_OK, HttpStatus.OK.value(), ValidationMessages.MPIN_SET_SUCCESS, response);
            });
        }

        BankMpinRequest bankRequest = new BankMpinRequest(mobile, mpin, confirmMpin, req.getDeviceId(), req.getIp(), req.getLatitude(), req.getLongitude());

        return bankWebClient.post()
                .uri("/bank/set-mpin")
                .headers(h -> h.addAll(buildHeaders(mobile, req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude())))
                .bodyValue(bankRequest)
                .retrieve()
                .bodyToMono(BankMpinResponse.class)
                .map(resp -> {
                    log.info("MPIN set successfully for mobile: {}. Transaction ID: {}", mobile, resp.getTransactionId());
                    return new ApiResponseDTO<>(
                            ValidationMessages.STATUS_OK,
                            HttpStatus.OK.value(),
                            ValidationMessages.MPIN_SET_SUCCESS,
                            resp);
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("BANK API ERROR while setting MPIN for mobile: {}. Status: {}, ResponseBody: {}", mobile, ex.getStatusCode().value(), ex.getResponseBodyAsString(), ex);
                    return Mono.error(new GlobalException(ValidationMessages.BANKING_FAILED + ": " + ex.getResponseBodyAsString(), ex.getStatusCode().value()));
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Unexpected exception while setting MPIN for mobile: {}. Error: {}", mobile, ex.getMessage(), ex);
                    return Mono.error(new GlobalException(ValidationMessages.ERROR_CALL_API + ": " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
                });
    }

    // ---------------- LOGIN ----------------
    public Mono<ApiResponseDTO<Map<String, Object>>> login(LoginRequest req) {
        String mobile = req.getMobile().trim();
        String mpin = req.getMpin();

        validateCommon(mobile, req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude());
        userUtils.validateMpinNotBlank(mpin, mobile);

        if (USE_MOCK) {
            return Mono.fromSupplier(() -> {
                if (!"1234".equals(mpin)) {
                      log.warn("{\"event\":\"login_failed\",\"mobile\":\"{}\"}", mobile);
                    throw new GlobalException(ValidationMessages.MPIN_INVALID, HttpStatus.UNAUTHORIZED.value());
                }

                String accessToken = jwtTokenService.generateAccessToken(Map.of("mobile", mobile, "deviceId", req.getDeviceId()), mobile);
                Map<String, Object> data = new HashMap<>();
                data.put("accessToken", accessToken);
                data.put("bankJwt", "MOCK_BANK_JWT_" + mobile);

                log.info("Mock login successful for mobile: {}", mobile);
                return new ApiResponseDTO<>(ValidationMessages.STATUS_OK, HttpStatus.OK.value(), ValidationMessages.LOGIN_SUCCESS, data);
            });
        }

        BankLoginRequest bankRequest = new BankLoginRequest(mobile, mpin, req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude());

        return bankWebClient.post()
                .uri("/bank/login")
                .headers(h -> h.addAll(buildHeaders(mobile, req.getIp(), req.getDeviceId(), req.getLatitude(), req.getLongitude())))
                .bodyValue(bankRequest)
                .retrieve()
                .bodyToMono(BankLoginResponse.class)
                .flatMap(bankResp -> {
                    if (!bankResp.isSuccess()) {
                        log.warn("{\"event\":\"login_failed\",\"mobile\":\"{}\",\"bankMessage\":\"{}\"}",mobile, bankResp.getMessage());
                        return Mono.error(new GlobalException(bankResp.getMessage(), HttpStatus.UNAUTHORIZED.value()));
                    }

                    String accessToken = jwtTokenService.generateAccessToken(Map.of("mobile", mobile, "deviceId", req.getDeviceId()), mobile);
                    Map<String, Object> data = new HashMap<>();
                    data.put("accessToken", accessToken);
                    data.put("bankJwt", bankResp.getBankJwt());

                    log.info("Login successful for mobile: {}. Bank JWT received.", mobile);
                    return Mono.just(new ApiResponseDTO<>(ValidationMessages.STATUS_OK, HttpStatus.OK.value(), ValidationMessages.LOGIN_SUCCESS, data));
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("BANK API ERROR during login for mobile: {}. Status: {}, ResponseBody: {}", mobile, ex.getStatusCode().value(), ex.getResponseBodyAsString(), ex);
                    return Mono.error(new GlobalException(ValidationMessages.BANKING_FAILED + ": " + ex.getResponseBodyAsString(), ex.getStatusCode().value()));
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Unexpected exception during login for mobile: {}. Error: {}", mobile, ex.getMessage(), ex);
                    return Mono.error(new GlobalException(ValidationMessages.ERROR_CALL_API + ": " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
                });
    }
}