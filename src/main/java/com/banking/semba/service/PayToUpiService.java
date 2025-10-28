package com.banking.semba.service;

import com.banking.semba.GlobalException.CustomException;
import com.banking.semba.constants.LogMessages;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.ApiResponseDTO;
import com.banking.semba.dto.RecentPaymentsDTO;
import com.banking.semba.security.JwtTokenService;
import com.banking.semba.util.UserServiceUtils;
import com.banking.semba.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PayToUpiService {

    private final JwtTokenService jwtTokenService;
    private final UserServiceUtils userUtils;
    private final ValidationUtil validationUtil;
    private final WebClient webClient;
    private final AuthService authService;

    public PayToUpiService(JwtTokenService jwtTokenService, UserServiceUtils userUtils,
                           ValidationUtil validationUtil, WebClient webClient, AuthService authService) {
        this.jwtTokenService = jwtTokenService;
        this.userUtils = userUtils;
        this.validationUtil = validationUtil;
        this.webClient = webClient;
        this.authService = authService;
    }

    private void checkDeviceInfo(String mobile, String ip, String deviceId, Double latitude, Double longitude) {
        userUtils.validateDeviceInfo(ip, deviceId, latitude, longitude, mobile);
        validationUtil.validateIpFormat(ip, mobile);
        validationUtil.validateDeviceIdFormat(deviceId, mobile);

        if (latitude != null && longitude != null) {
            validationUtil.validateLocation(latitude, String.valueOf(longitude), mobile);
        }
    }

    public ApiResponseDTO<Map<String, Object>> validateUpiId(
            String auth, String ip, String deviceId,
            Double latitude, Double longitude, String upiId) {

        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            log.warn(LogMessages.UPIID_VALIDATION_UNAUTHORIZED, ValidationMessages.INVALID_JWT);
            return new ApiResponseDTO<>(
                    ValidationMessages.STATUS_UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value(),
                    ValidationMessages.INVALID_JWT,
                    null
            );
        }

        log.info(LogMessages.UPIID_VALIDATION_START, upiId);

        checkDeviceInfo(mobile, ip, deviceId, latitude, longitude);
        if (upiId == null || upiId.isBlank()) {
            return new ApiResponseDTO<>(
                    ValidationMessages.STATUS_ERROR,
                    HttpStatus.BAD_REQUEST.value(),
                    "UPI ID cannot be empty",
                    null
            );
        }

        String upiPattern = "^[a-zA-Z0-9\\.\\-_]{2,256}@[a-zA-Z]{2,64}$";
        if (!upiId.matches(upiPattern)) {
            return new ApiResponseDTO<>(
                    ValidationMessages.STATUS_ERROR,
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid UPI ID format",
                    null
            );
        }

        Map<String, Object> verifiedUser;
        HttpHeaders headers = authService.buildHeaders(mobile, ip, deviceId, latitude, longitude);

        try {
            verifiedUser = webClient.get()
                    .uri("https://jsonplaceholder.typicode.com/users/1")
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .flatMap(error -> Mono.error(
                                            new CustomException("External API failed: " + error, "Failed")
                                    ))
                    )
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

        } catch (Exception ex) {
            log.error(LogMessages.UPIID_VALIDATION_ERROR, ex.getMessage(), ex);
            return new ApiResponseDTO<>(
                    ValidationMessages.STATUS_ERROR,
                    HttpStatus.BAD_GATEWAY.value(),
                    "External API failed: " + ex.getMessage(),
                    null
            );
        }

        if (verifiedUser == null || verifiedUser.isEmpty()) {
            log.warn(LogMessages.UPIID_VALIDATION_NOT_FOUND, upiId);
            return new ApiResponseDTO<>(
                    ValidationMessages.USER_NOT_FOUND,
                    HttpStatus.NOT_FOUND.value(),
                    ValidationMessages.NO_UPIID_FOUND,
                    null
            );
        }

        log.info(LogMessages.UPIID_VALIDATION_SUCCESS, upiId, verifiedUser.get("name"));
        return new ApiResponseDTO<>(
                ValidationMessages.STATUS_OK,
                HttpStatus.OK.value(),
                ValidationMessages.UPI_ID_VERIFIED_SUCCESSFULLY,
                verifiedUser
        );
    }

    public ApiResponseDTO<List<RecentPaymentsDTO>> getRecentPaymentsByUpiId(
            String auth, String ip, String deviceId,
            Double latitude, Double longitude, String upiId) {

        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        log.info("{\"event\":\"recent_upi_fetch_start\",\"upiId\":\"{}\",\"mobile\":\"{}\"}", upiId, mobile);

        if (upiId == null || upiId.trim().isEmpty()) {
            log.warn("{\"event\":\"recent_upi_fetch_invalid_request\",\"message\":\"UPI ID is missing\"}");
            return new ApiResponseDTO<>(
                    ValidationMessages.BAD_REQUEST,
                    HttpStatus.BAD_REQUEST.value(),
                    "UPI ID is required to fetch recent payments",
                    null
            );
        }
        checkDeviceInfo(mobile, ip, deviceId, latitude, longitude);
        HttpHeaders headers = authService.buildHeaders(mobile, ip, deviceId, latitude, longitude);

        try {
            List<Map<String, Object>> externalData = webClient.get()
                    .uri("https://jsonplaceholder.typicode.com/posts")
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .flatMap(error -> Mono.error(
                                            new CustomException("External API failed: " + error, "Failed")
                                    ))
                    )
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block();

            if (externalData == null || externalData.isEmpty()) {
                log.warn("{\"event\":\"recent_upi_fetch_not_found\",\"upiId\":\"{}\"}", upiId);
                return new ApiResponseDTO<>(
                        ValidationMessages.NO_RECENT_PAYMENTS,
                        HttpStatus.NOT_FOUND.value(),
                        "No recent payments found for the given UPI ID",
                        List.of()
                );
            }

            List<RecentPaymentsDTO> dtoList = externalData.stream()
                    .limit(5)
                    .map(post -> new RecentPaymentsDTO(
                            ((Integer) post.get("id")),
                            "9876543210",
                            "9998887776",
                            1500.00,
                            "SUCCESS",
                            LocalDateTime.now().minusMinutes(((Number) post.get("id")).longValue())
                    ))
                    .collect(Collectors.toList());

            log.info("{\"event\":\"recent_upi_fetch_success\",\"upiId\":\"{}\",\"count\":{}}", upiId,dtoList.size());
            return new ApiResponseDTO<>(
                    ValidationMessages.STATUS_OK,
                    HttpStatus.OK.value(),
                    "Recent UPI payments fetched successfully",
                    dtoList
            );

        } catch (Exception ex) {
            log.error("{\"event\":\"recent_upi_fetch_error\",\"upiId\":\"{}\",\"error\":\"{}\"}", upiId, ex.getMessage(), ex);
            return new ApiResponseDTO<>(
                    ValidationMessages.STATUS_ERROR,
                    HttpStatus.BAD_GATEWAY.value(),
                    "External API failed: " + ex.getMessage(),
                    null
            );
        }
    }

}
