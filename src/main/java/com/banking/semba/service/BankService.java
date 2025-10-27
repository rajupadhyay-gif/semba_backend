package com.banking.semba.service;

import com.banking.semba.GlobalException.CustomException;
import com.banking.semba.constants.LogMessages;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.HttpResponseDTO;
import com.banking.semba.security.JwtTokenService;
import com.banking.semba.util.UserServiceUtils;
import com.banking.semba.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankService {

    private final JwtTokenService jwtTokenService;
    private final UserServiceUtils userUtils;
    private final ValidationUtil validationUtil;
    private final WebClient bankWebClient;

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

            Object bankList = bankWebClient.get()
                    .uri("https://api.paystack.co/bank")
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

        userUtils.validateDeviceInfo(ip, deviceId, latitude, longitude, mobile);
        validationUtil.validateIpFormat(ip, mobile);
        validationUtil.validateDeviceIdFormat(deviceId, mobile);
        if (latitude != null && longitude != null) {
            validationUtil.validateLocation(latitude, String.valueOf(longitude), mobile);
        }

        Object bankListObj = bankWebClient.get()
                .uri("https://api.paystack.co/bank")
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
            );        }

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



}
