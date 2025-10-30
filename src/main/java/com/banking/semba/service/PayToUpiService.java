package com.banking.semba.service;

import com.banking.semba.globalException.CustomException;
import com.banking.semba.constants.LogMessages;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.*;
import com.banking.semba.security.JwtTokenService;
import com.banking.semba.util.MPINValidatorUtil;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PayToUpiService {

    private final JwtTokenService jwtTokenService;
    private final UserServiceUtils userUtils;
    private final ValidationUtil validationUtil;
    private final WebClient webClient;
    private final AuthService authService;
    private final MPINValidatorUtil mpinValidatorUtil;

    public PayToUpiService(JwtTokenService jwtTokenService, UserServiceUtils userUtils,
                           ValidationUtil validationUtil, WebClient webClient, AuthService authService, MPINValidatorUtil mpinValidatorUtil) {
        this.jwtTokenService = jwtTokenService;
        this.userUtils = userUtils;
        this.validationUtil = validationUtil;
        this.webClient = webClient;
        this.authService = authService;
        this.mpinValidatorUtil = mpinValidatorUtil;
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
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
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
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    })
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

            log.info("{\"event\":\"recent_upi_fetch_success\",\"upiId\":\"{}\",\"count\":{}}", upiId, dtoList.size());
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

    public ApiResponseDTO<BalanceValidationDataDTO> validateBankBalance(String auth, String ip, String deviceId, Double latitude, Double longitude, String accountNumber, Double enteredAmount, String mpin
    ) {

        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            return new ApiResponseDTO<>(
                    ValidationMessages.STATUS_UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value(),
                    ValidationMessages.INVALID_JWT,
                    null
            );
        }
        checkDeviceInfo(mobile, ip, deviceId, latitude, longitude);

        try {
            log.info("Fetching live balance for account: {}", accountNumber);
            if (enteredAmount == null || enteredAmount < 1) {
                throw new IllegalArgumentException("Entered amount must be greater than or equal to 1");
            }
            HttpHeaders headers = authService.buildHeaders(auth, ip, deviceId, latitude, longitude);

            if (mpin == null || mpin.trim().isEmpty()) {
                return new ApiResponseDTO<>(
                        ValidationMessages.STATUS_FAILED,
                        HttpStatus.BAD_REQUEST.value(),
                        "MPIN is blank. Please enter a valid MPIN.",
                        null
                );
            }
            Double liveBalance = webClient
                    .get()
                    .uri("https://dummy-bank-api.com/api/balance?accountNumber={accountNumber}", accountNumber)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .retrieve()
                    .bodyToMono(Double.class)
                    .onErrorResume(ex -> {
                        log.warn("Dummy API failed: {}", ex.getMessage());
                        return Mono.just(8500.0);
                    })
                    .block();

            if (liveBalance == null) {
                liveBalance = 8500.0;
            }

            log.info(LogMessages.LIVE_BALANCE_FETCHED_SUCCESSFULLY);
            String transactionId = UUID.randomUUID().toString();
            BalanceValidationDataDTO responseData = new BalanceValidationDataDTO(
                    enteredAmount,
                    (liveBalance >= enteredAmount)
                            ? ValidationMessages.TRANSACTION_ALLOWED
                            : ValidationMessages.TRANSACTION_NOT_ALLOWED,
                    transactionId
            );

            if (liveBalance < enteredAmount) {
                return new ApiResponseDTO<>(
                        ValidationMessages.STATUS_FAILED,
                        HttpStatus.BAD_REQUEST.value(),
                        ValidationMessages.INSUFFICIENT_FUNDS,
                        responseData
                );
            }

            ApiResponseDTO<MPINValidationResponseDTO> mpinResponse =
                    mpinValidatorUtil.validateMPIN(auth, ip, deviceId, latitude, longitude, accountNumber, mpin, transactionId);

            if (!"SUCCESS".equalsIgnoreCase(mpinResponse.getStatus())) {
                return new ApiResponseDTO<>(
                        ValidationMessages.STATUS_FAILED,
                        HttpStatus.BAD_REQUEST.value(),
                        "MPIN validation failed: " + mpinResponse.getResponseMessage(),
                        responseData
                );
            }

            return new ApiResponseDTO<>(
                    ValidationMessages.STATUS_OK,
                    HttpStatus.OK.value(),
                    ValidationMessages.SUFFICIENT_FUNDS + " Transaction ID: " + transactionId,
                    responseData
            );

        } catch (Exception e) {
            log.error("Error validating bank balance: {}", e.getMessage(), e);
            return new ApiResponseDTO<>(
                    ValidationMessages.STATUS_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    ValidationMessages.UNKNOWN_ERROR + e.getMessage(),
                    null
            );
        }
    }

    public ApiResponseDTO<TransactionDetailsDTO> getTransactionDetails(String auth, String ip, String deviceId, Double latitude, Double longitude, String transactionId) {
        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            return new ApiResponseDTO<>(
                    ValidationMessages.STATUS_UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value(),
                    ValidationMessages.INVALID_JWT,
                    null
            );
        }
        checkDeviceInfo(mobile, ip, deviceId, latitude, longitude);
        if (transactionId == null || transactionId.trim().isEmpty()) {
            return new ApiResponseDTO<>(
                    ValidationMessages.STATUS_FAILED,
                    HttpStatus.BAD_REQUEST.value(),
                    "Transaction ID cannot be null or empty.",
                    null
            );
        }

        try {

            log.info("Fetching transaction details from bank API for ID: {}", transactionId);
            HttpHeaders headers = authService.buildHeaders(auth, ip, deviceId, latitude, longitude);

            TransactionDetailsDTO bankResponse = webClient.get()
                    .uri("bankTransactionApiUrl")
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .retrieve()
                    .bodyToMono(TransactionDetailsDTO.class)
                    .onErrorResume(ex -> {
                        TransactionDetailsDTO fallback = new TransactionDetailsDTO(
                                transactionId,
                                PaymentType.UPI,
                                "rajesh@upi",
                                "shop@upi",
                                "Bank of India ••••8888",
                                2000.0,
                                "27 Oct 2025, 10:35 AM",
                                "Rajesh MBU",
                                "SUCCESS",
                                "Transaction Success"
                        );
                        return Mono.just(fallback);
                    })
                    .block();

            assert bankResponse != null;
            String responseMsg = (bankResponse.getStatus().equalsIgnoreCase("SUCCESS"))
                    ? "Transaction successful."
                    : "Transaction failed.";

            return new ApiResponseDTO<>(
                    "SUCCESS",
                    HttpStatus.OK.value(),
                    responseMsg,
                    bankResponse
            );

        } catch (Exception e) {
            log.error("Error fetching transaction details: {}", e.getMessage(), e);
            return new ApiResponseDTO<>(
                    "FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Unable to fetch transaction details: " + e.getMessage(),
                    null
            );
        }
    }
}
