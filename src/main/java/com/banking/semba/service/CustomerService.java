package com.banking.semba.service;

import com.banking.semba.GlobalException.GlobalException;
import com.banking.semba.constants.LogMessages;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.ApiResponses;
import com.banking.semba.dto.response.BankAccountResponse;
import com.banking.semba.dto.response.BankProfileResponse;
import com.banking.semba.util.JwtUtil;
import com.banking.semba.util.UserServiceUtils;
import com.banking.semba.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
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

    private final boolean useMock = true;
    private static final String BANK_PROFILE_URL = "/user/profile";
    private static final String BANK_ACCOUNT_URL = "https://api.realbank.com/user/account/";

    public CustomerService(WebClient bankWebClient, JwtUtil jwtUtil,
                           UserServiceUtils userUtils, ValidationUtil validationUtil) {
        this.bankWebClient = bankWebClient;
        this.jwtUtil = jwtUtil;
        this.userUtils = userUtils;
        this.validationUtil = validationUtil;
    }
    public ApiResponses<Map<String, Object>> getProfile(String authHeader,
                                                        String ip,
                                                        String deviceId,
                                                        Double latitude,
                                                        Double longitude) {
        String mobile = jwtUtil.getMobileFromHeader(authHeader);
        log.info(LogMessages.PROFILE_FETCH_START, mobile);

        validateRequest(mobile, ip, deviceId, latitude, longitude);

        if (useMock) {
            // ---------------- MOCK PROFILE ----------------
            Map<String, Object> mockProfile = new HashMap<>();
            mockProfile.put("mobile", mobile);
            mockProfile.put("fullName", "John Doe");
            mockProfile.put("email", "john.doe@example.com");
            mockProfile.put("accountType", "SAVINGS");
            mockProfile.put("accountNumber", "123456789012");
            mockProfile.put("ifsc", "IFSC0001");
            mockProfile.put("bankName", "Bank of Dummy");
            mockProfile.put("balance", 50000.0);

            Map<String, Object> data = new HashMap<>();
            data.put("profile", mockProfile);

            log.info(LogMessages.PROFILE_FETCH_SUCCESS, mobile);
            return new ApiResponses<>("SUCCESS", HttpStatus.OK.value(),
                    ValidationMessages.PROFILE_FETCH_SUCCESS, data);
        }

        try {
            // ---------------- REAL BANK API ----------------
            BankProfileResponse bankResponse = bankWebClient.get()
                    .uri(BANK_PROFILE_URL) // replace with real bank URL
                    .headers(h -> {
                        h.set(HttpHeaders.AUTHORIZATION, authHeader);
                        h.set("X-Device-Id", deviceId);
                        h.set("X-IP", ip);
                        if (latitude != null) h.set("X-Latitude", latitude.toString());
                        if (longitude != null) h.set("X-Longitude", longitude.toString());
                        h.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                    })
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), clientResponse ->
                            Mono.error(new GlobalException(ValidationMessages.BANK_API_FAILED,
                                    clientResponse.statusCode().value()))
                    )
                    .bodyToMono(BankProfileResponse.class)
                    .block();

            if (bankResponse == null || !bankResponse.isSuccess()) {
                log.error(LogMessages.PROFILE_FETCH_FAILED, mobile, "Invalid response from bank");
                throw new GlobalException(ValidationMessages.PROFILE_FETCH_FAILED, HttpStatus.BAD_REQUEST.value());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("profile", bankResponse.getProfile());
            log.info(LogMessages.PROFILE_FETCH_SUCCESS, mobile);

            return new ApiResponses<>("SUCCESS", HttpStatus.OK.value(),
                    ValidationMessages.PROFILE_FETCH_SUCCESS, data);

        } catch (WebClientResponseException ex) {
            log.error(LogMessages.BANK_API_ERROR, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new GlobalException(ValidationMessages.BANK_API_FAILED, ex.getStatusCode().value());
        } catch (Exception ex) {
            log.error(LogMessages.PROFILE_FETCH_FAILED, mobile, ex.getMessage(), ex);
            throw new GlobalException(ValidationMessages.UNKNOWN_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    public ApiResponses<Map<String, Object>> getAccountById(Long id,
                                                            String authHeader,
                                                            String deviceId,
                                                            String ip,
                                                            Double latitude,
                                                            Double longitude) {

        String mobile = jwtUtil.getMobileFromHeader(authHeader);
        log.info(LogMessages.ACCOUNT_FETCH_START, id);

        validateRequest(mobile, ip, deviceId, latitude, longitude);

        if (useMock) {
            // ---------------- MOCK ACCOUNT RESPONSE ----------------
            Map<String, Object> mockAccount = new HashMap<>();
            mockAccount.put("accountId", id);
            mockAccount.put("mobile", mobile);
            mockAccount.put("accountNumber", "123456789012");
            mockAccount.put("ifsc", "IFSC0001");
            mockAccount.put("bankName", "Bank of Dummy");
            mockAccount.put("balance", 50000.0);

            Map<String, Object> data = new HashMap<>();
            data.put("account", mockAccount);

            log.info(LogMessages.ACCOUNT_FETCH_SUCCESS, id);
            return new ApiResponses<>("SUCCESS", HttpStatus.OK.value(),
                    ValidationMessages.PROFILE_FETCH_SUCCESS, data);
        }

        try {
            // ---------------- REAL BANK API ----------------
            BankAccountResponse bankResponse = bankWebClient.get()
                    .uri(BANK_ACCOUNT_URL + id) // <-- replace with real bank URL
                    .headers(h -> {
                        h.set(HttpHeaders.AUTHORIZATION, authHeader);
                        h.set("X-Device-Id", deviceId);
                        h.set("X-IP", ip);
                        if (latitude != null) h.set("X-Latitude", latitude.toString());
                        if (longitude != null) h.set("X-Longitude", longitude.toString());
                        h.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                    })
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), clientResponse ->
                            Mono.error(new GlobalException(ValidationMessages.BANK_API_FAILED,
                                    clientResponse.statusCode().value()))
                    )
                    .bodyToMono(BankAccountResponse.class)
                    .block();

            if (bankResponse == null || !bankResponse.isSuccess()) {
                log.error(LogMessages.ACCOUNT_FETCH_FAILED, id, "Invalid response from bank");
                throw new GlobalException(ValidationMessages.ACCOUNT_FETCH_ERROR, HttpStatus.BAD_REQUEST.value());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("account", bankResponse.getAccount());

            log.info(LogMessages.ACCOUNT_FETCH_SUCCESS, id);
            return new ApiResponses<>("SUCCESS", HttpStatus.OK.value(),
                    ValidationMessages.PROFILE_FETCH_SUCCESS, data);

        } catch (WebClientResponseException ex) {
            log.error(LogMessages.BANK_API_ERROR, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new GlobalException(ValidationMessages.BANK_API_FAILED, ex.getStatusCode().value());
        } catch (Exception ex) {
            log.error(LogMessages.ACCOUNT_FETCH_FAILED, id, ex.getMessage(), ex);
            throw new GlobalException(ValidationMessages.UNKNOWN_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    private void validateRequest(String mobile, String ip, String deviceId, Double latitude, Double longitude) {
        userUtils.validateDeviceInfo(ip, deviceId, latitude, longitude, mobile);
        validationUtil.validateIpFormat(ip, mobile);
        validationUtil.validateDeviceIdFormat(deviceId, mobile);
        validationUtil.validateLocation(latitude, String.valueOf(longitude), mobile);
    }

}