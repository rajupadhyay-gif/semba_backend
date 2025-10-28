package com.banking.semba.service;

import com.banking.semba.GlobalException.GlobalException;
import com.banking.semba.constants.LogMessages;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.ApiResponseDTO;
import com.banking.semba.dto.response.BankAccountResponse;
import com.banking.semba.dto.response.BankProfileResponse;
import com.banking.semba.util.JwtUtil;
import com.banking.semba.util.UserServiceUtils;
import com.banking.semba.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class CustomerService {

    private final WebClient bankWebClient;
    private final JwtUtil jwtUtil;
    private final UserServiceUtils userUtils;
    private final ValidationUtil validationUtil;

    private final boolean useMock = true; //  false = production (bank call)
    private static final String BANK_PROFILE_URL = "/user/profile";
    private static final String BANK_ACCOUNT_URL = "/user/account/";

    public CustomerService(WebClient bankWebClient, JwtUtil jwtUtil,
                           UserServiceUtils userUtils, ValidationUtil validationUtil) {
        this.bankWebClient = bankWebClient;
        this.jwtUtil = jwtUtil;
        this.userUtils = userUtils;
        this.validationUtil = validationUtil;
    }

    // ---------------- PROFILE ----------------
    public ApiResponseDTO<Map<String, Object>> getProfile(
            String authHeader, String ip, String deviceId, Double latitude, Double longitude) {

        String mobile = jwtUtil.getMobileFromHeader(authHeader);
        log.info(LogMessages.PROFILE_FETCH_START, mobile);
        validateRequest(mobile, ip, deviceId, latitude, longitude);

        if (useMock) {
            // Mock for local testing
            Map<String, Object> mockProfile = Map.of(
                    "mobile", mobile,
                    "fullName", "John Doe",
                    "email", "john.doe@example.com",
                    "accountType", "SAVINGS",
                    "balance", 50000.00
            );
            return new ApiResponseDTO<>("SUCCESS", 200, ValidationMessages.PROFILE_FETCH_SUCCESS,
                    Map.of("profile", mockProfile));
        }

        try {
            BankProfileResponse bankResponse = bankWebClient.get()
                    .uri(BANK_PROFILE_URL)
                    .headers(h -> setBankHeaders(h, authHeader, ip, deviceId, latitude, longitude))
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            clientResponse -> Mono.error(new GlobalException(
                                    ValidationMessages.BANK_API_FAILED,
                                    clientResponse.statusCode().value())))
                    .bodyToMono(BankProfileResponse.class)
                    .block();

            if (bankResponse == null || !bankResponse.isSuccess())
                throw new GlobalException(
                        ValidationMessages.PROFILE_FETCH_FAILED,
                        HttpStatus.BAD_REQUEST.value());

            Map<String, Object> data = new HashMap<>();
            data.put("profile", bankResponse.getProfile());
            log.info(LogMessages.PROFILE_FETCH_SUCCESS, mobile);
            return new ApiResponseDTO<>(
                    ValidationMessages.STATUS_OK,
                    HttpStatus.OK.value(),
                    ValidationMessages.PROFILE_FETCH_SUCCESS,
                    data);

        } catch (WebClientResponseException ex) {
            log.error("Bank API Error: {}", ex.getResponseBodyAsString());
            throw new GlobalException("Bank API Error", ex.getStatusCode().value());
        } catch (Exception ex) {
            throw new GlobalException("Unexpected error: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    // ---------------- ACCOUNT DETAILS ----------------
    public ApiResponseDTO<Map<String, Object>> getAccountById(
            Long id, String authHeader, String deviceId, String ip, Double latitude, Double longitude) {

        String mobile = jwtUtil.getMobileFromHeader(authHeader);
        validateRequest(mobile, ip, deviceId, latitude, longitude);

        if (useMock) {
            Map<String, Object> mockAccount = Map.of(
                    "accountId", id,
                    "accountNumber", "XXXXXX8901",
                    "balance", 75000.0,
                    "ifsc", "HDFC000123",
                    "type", "SAVINGS"
            );
            return new ApiResponseDTO<>("SUCCESS", 200, "Mock account fetched", Map.of("account", mockAccount));
        }

        try {
            BankAccountResponse bankResponse = bankWebClient.get()
                    .uri(BANK_ACCOUNT_URL + id)
                    .headers(h -> setBankHeaders(h, authHeader, ip, deviceId, latitude, longitude))
                    .retrieve()
                    .bodyToMono(BankAccountResponse.class)
                    .block();

            if (bankResponse == null || !bankResponse.isSuccess())
                throw new GlobalException(ValidationMessages.ACCOUNT_FETCH_ERROR, HttpStatus.BAD_REQUEST.value());

            Map<String, Object> data = new HashMap<>();
            data.put("account", bankResponse.getAccount());
            log.info(LogMessages.ACCOUNT_FETCH_SUCCESS, id);

            return new ApiResponseDTO<>(
                    ValidationMessages.STATUS_OK,
                    HttpStatus.OK.value(),
                    ValidationMessages.ACCOUNT_FETCH_SUCCESS,
                    data);

        } catch (Exception ex) {
            throw new GlobalException(ValidationMessages.ACCOUNT_FETCH_ERROR + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    // ---------------- HELPERS ----------------
    private void setBankHeaders(HttpHeaders headers, String auth, String ip, String deviceId, Double lat, Double lon) {
        headers.set(HttpHeaders.AUTHORIZATION, auth);
        headers.set("X-Device-Id", deviceId);
        headers.set("X-IP", ip);
        if (lat != null) headers.set("X-Latitude", lat.toString());
        if (lon != null) headers.set("X-Longitude", lon.toString());
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    }

    private void validateRequest(String mobile, String ip, String deviceId, Double lat, Double lon) {
        userUtils.validateDeviceInfo(ip, deviceId, lat, lon, mobile);
        validationUtil.validateIpFormat(ip, mobile);
        validationUtil.validateDeviceIdFormat(deviceId, mobile);
        validationUtil.validateLocation(lat, String.valueOf(lon), mobile);
    }
}
