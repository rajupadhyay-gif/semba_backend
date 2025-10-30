package com.banking.semba.service;

import com.banking.semba.GlobalException.CustomException;
import com.banking.semba.constants.LogMessages;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.*;
import com.banking.semba.security.JwtTokenService;
import com.banking.semba.util.MPINValidatorUtil;
import com.banking.semba.util.UserServiceUtils;
import com.banking.semba.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BankService {

    private final JwtTokenService jwtTokenService;
    private final UserServiceUtils userUtils;
    private final ValidationUtil validationUtil;
    private final WebClient bankWebClient;
    private final MPINValidatorUtil mpinValidatorUtil;
    private final AuthService authService;

    public BankService(JwtTokenService jwtTokenService, UserServiceUtils userUtils, ValidationUtil validationUtil, WebClient bankWebClient, MPINValidatorUtil mpinValidatorUtil, AuthService authService) {
        this.jwtTokenService = jwtTokenService;
        this.userUtils = userUtils;
        this.validationUtil = validationUtil;
        this.bankWebClient = bankWebClient;
        this.mpinValidatorUtil = mpinValidatorUtil;
        this.authService = authService;
    }

    private void validateDevice(String ip, String deviceId, Double latitude, Double longitude, String mobile) {
        userUtils.validateDeviceInfo(ip, deviceId, latitude, longitude, mobile);
        validationUtil.validateIpFormat(ip, mobile);
        validationUtil.validateDeviceIdFormat(deviceId, mobile);
        if (latitude != null && longitude != null) {
            validationUtil.validateLocation(latitude, String.valueOf(longitude), mobile);
        }
    }

    public HttpResponseDTO fetchTopBanksList(String auth, String ip, String deviceId, Double latitude, Double longitude) {
        log.info(LogMessages.FETCH_BANKS_STARTED);
        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            return new HttpResponseDTO(
                    ValidationMessages.STATUS_UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value(),
                    ValidationMessages.INVALID_JWT
            );
        }
        validateDevice(ip, deviceId, latitude, longitude, mobile);

        try {
            log.info(LogMessages.API_CALL, "Calling external bank list API...");
            HttpHeaders headers = authService.buildHeaders(auth, ip, deviceId, latitude, longitude);

            Object bankList = bankWebClient.get()
                    .uri("https://api.paystack.co/bank")
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class).flatMap(errorBody -> {
                                log.error(LogMessages.FETCH_BANKS_ERROR, errorBody);
                                return Mono.error(new CustomException(
                                        ValidationMessages.FETCHING_FAILED + " " + errorBody,
                                        ValidationMessages.ERROR_CODE_FETCH_FAILED
                                ));
                            })
                    )
                    .bodyToMono(Object.class)
                    .block();

            if (bankList == null) {
                log.warn(LogMessages.FETCH_BANKS_NULL);
                throw new CustomException(
                        ValidationMessages.NO_BANKS_FOUND,
                        ValidationMessages.ERROR_CODE_NO_BANKS
                );
            }

            log.info(LogMessages.FETCH_BANKS_SUCCESS);
            return new HttpResponseDTO(
                    ValidationMessages.STATUS_OK,
                    HttpStatus.OK.value(),
                    ValidationMessages.FETCHED_SUCCESSFULLY,
                    bankList
            );

        } catch (CustomException e) {
            log.error("Custom exception while fetching banks: {}", e.getMessage());
            return new HttpResponseDTO(
                    ValidationMessages.STATUS_FAILED,
                    HttpStatus.BAD_REQUEST.value(),
                    e.getMessage()
            );

        } catch (Exception e) {
            log.error("Unexpected error fetching banks list: {}", e.getMessage(), e);
            return new HttpResponseDTO(
                    ValidationMessages.STATUS_FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    ValidationMessages.FETCHING_FAILED
            );
        }
    }

    public HttpResponseDTO searchBanks(String auth, String ip, String deviceId,
                                       Double latitude, Double longitude, String bankName) {

        log.info(LogMessages.SEARCH_BANKS_STARTED, bankName);

        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            return new HttpResponseDTO(
                    ValidationMessages.STATUS_UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value(),
                    ValidationMessages.INVALID_JWT
            );
        }
        validateDevice(ip, deviceId, latitude, longitude, mobile);
        HttpHeaders headers = authService.buildHeaders(auth, ip, deviceId, latitude, longitude);
        Object bankListObj = bankWebClient.get()
                .uri("https://api.paystack.co/bank")
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error(LogMessages.FETCH_BANKS_ERROR, errorBody);
                                    return Mono.error(new CustomException(
                                            ValidationMessages.FETCHING_FAILED + " " + errorBody,
                                            ValidationMessages.ERROR_CODE_FETCH_FAILED
                                    ));
                                })
                )
                .bodyToMono(Object.class)
                .block();

        if (bankListObj == null || ((Map<?, ?>) bankListObj).isEmpty()) {
            log.warn(LogMessages.FETCH_BANKS_NULL);
            throw new CustomException(
                    ValidationMessages.NO_BANKS_FOUND,
                    ValidationMessages.ERROR_CODE_NO_BANKS
            );
        }

        Map<String, Object> bankMap = (Map<String, Object>) bankListObj;

        List<Map<String, Object>> filteredBanks = bankMap.values().stream()
                .filter(v -> v instanceof Map)
                .map(v -> (Map<String, Object>) v)
                .filter(m -> m.get("BANK") != null
                        && m.get("BANK").toString().toLowerCase().contains(bankName.toLowerCase()))
                .collect(Collectors.toList());

        log.info(LogMessages.SEARCH_BANKS_SUCCESS, filteredBanks.size(), bankName);

        return new HttpResponseDTO(
                ValidationMessages.STATUS_OK,
                HttpStatus.OK.value(),
                filteredBanks.isEmpty() ? ValidationMessages.NO_BANKS_FOUND : ValidationMessages.FETCHED_SUCCESSFULLY,
                filteredBanks
        );
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
        validateDevice(ip, deviceId, latitude, longitude, mobile);
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
            Double liveBalance = bankWebClient
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
        validateDevice(ip, deviceId, latitude, longitude, mobile);
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

            TransactionDetailsDTO bankResponse = bankWebClient.get()
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
                    null //
            );
        }
    }
}
