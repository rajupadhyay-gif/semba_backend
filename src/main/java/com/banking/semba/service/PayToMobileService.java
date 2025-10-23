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
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PayToMobileService {

    private final JwtTokenService jwtTokenService;
    private final UserServiceUtils userUtils;
    private final ValidationUtil validationUtil;
    private final WebClient webClient;

    public PayToMobileService(JwtTokenService jwtTokenService, UserServiceUtils userUtils, ValidationUtil validationUtil, WebClient webClient) {
        this.jwtTokenService = jwtTokenService;
        this.userUtils = userUtils;
        this.validationUtil = validationUtil;
        this.webClient = webClient;
    }

    public ApiResponseDTO<List<Map<String, Object>>> searchContacts(String auth, String ip, String deviceId,
                                                                    Double latitude, Double longitude,
                                                                    String mobileNumber, String name) {
        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        if (mobile == null || mobile.isEmpty()) {
            return new ApiResponseDTO<>(
                    ValidationMessages.STATUS_UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value(),
                    ValidationMessages.INVALID_JWT
            );
        }
        log.info(LogMessages.SEARCH_CONTACTS_START, mobileNumber, name);
        userUtils.validateDeviceInfo(ip, deviceId, latitude, longitude, mobile);
        validationUtil.validateIpFormat(ip, mobile);
        validationUtil.validateDeviceIdFormat(deviceId, mobile);
        if (latitude != null && longitude != null) {
            validationUtil.validateLocation(latitude, String.valueOf(longitude), mobile);
        }
//
//        List<Map<String, Object>> contacts;
//        try {
//            contacts = webClient.get()
//                    .uri("https://api.paystack.co/contact")
//                    .header(HttpHeaders.AUTHORIZATION, "Bearer test_secret_key")
//                    .accept(MediaType.APPLICATION_JSON)
//                    .retrieve()
//                    .onStatus(status -> status.isError(),
//                            response -> response.bodyToMono(String.class)
//                                    .flatMap(error -> Mono.error(new CustomException("External API failed: " + error,"Error"))))
//                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
//                    .block();
//        } catch (Exception ex) {
//            log.error(LogMessages.SEARCH_CONTACTS_ERROR, ex.getMessage(), ex);
//            return new ApiResponses<>(
//                    ValidationMessages.STATUS_ERROR,
//                    HttpStatus.BAD_GATEWAY.value(),
//                    "External API failed: " + ex.getMessage(),
//                    null
//            );
//        }
//
//        if (contacts == null) contacts = List.of();
List<Map<String, Object>> contacts = new ArrayList<>();
    contacts.add(Map.of(
            "name", "Rajesh Kumar",
            "mobileNumber", "7729955925",
            "email", "rajesh.kumar@example.com"
            ));
    contacts.add(Map.of(
            "name", "Priya Singh",
            "mobileNumber", "7700112233",
            "email", "priya.singh@example.com"
            ));
    contacts.add(Map.of(
            "name", "John Doe",
            "mobileNumber", "7711223344",
            "email", "john.doe@example.com"
            ));
        List<Map<String, Object>> filteredContacts = contacts.stream()
                .filter(contact -> {
                    if ((mobileNumber == null || mobileNumber.isBlank()) &&
                            (name == null || name.isBlank())) {
                        return true;
                    }

                    boolean matchesMobile = mobileNumber != null && !mobileNumber.isBlank() &&
                            contact.get("mobileNumber") != null &&
                            contact.get("mobileNumber").toString().contains(mobileNumber);

                    boolean matchesName = name != null && !name.isBlank() &&
                            contact.get("name") != null &&
                            contact.get("name").toString().toLowerCase().contains(name.toLowerCase());

                    return matchesMobile || matchesName;
                })
                .collect(Collectors.toList());
        log.info(LogMessages.SEARCH_CONTACTS_SUCCESS, filteredContacts.size());
        String message = filteredContacts.isEmpty() ? ValidationMessages.NO_CONTACTS_FOUND : ValidationMessages.CONTACTS_FETCHED_SUCCESSFULLY;
        return new ApiResponseDTO<>(
                ValidationMessages.STATUS_OK,
                HttpStatus.OK.value(),
                message,
                filteredContacts
        );
    }

    public ApiResponseDTO<List<RecentPaymentsDTO>> getRecentPayments(
            String auth, String ip, String deviceId, Double latitude, Double longitude
    ) {
        String mobile = jwtTokenService.extractMobileFromHeader(auth);
        userUtils.validateDeviceInfo(ip, deviceId, latitude, longitude, mobile);
        validationUtil.validateIpFormat(ip, mobile);
        validationUtil.validateDeviceIdFormat(deviceId, mobile);
        if (latitude != null && longitude != null) {
            validationUtil.validateLocation(latitude, String.valueOf(longitude), mobile);
        }

        List<RecentPaymentsDTO> dtoList;

        try {
            List<Map<String, Object>> dummyPayments = webClient.get()
                    .uri("https://jsonplaceholder.typicode.com/posts") // dummy URL
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .flatMap(error -> Mono.error(
                                            new CustomException("External API failed: " + error, "Error")
                                    ))
                    )
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block();

            if (dummyPayments == null) dummyPayments = List.of();

            dtoList = dummyPayments.stream().limit(5)
                    .map(p -> new RecentPaymentsDTO(
                            (Integer) p.get("id"),
                            mobile,
                            "0700000000",
                            1000.0,
                            "SUCCESS",
                            LocalDateTime.now()
                    ))
                    .collect(Collectors.toList());

        }catch (Exception ex) {
            return new ApiResponseDTO<>(
                    "ERROR",
                    HttpStatus.BAD_GATEWAY.value(),
                    "External API failed: " + ex.getMessage(),
                    null
            );
        }
        return new ApiResponseDTO<>("Success",
                HttpStatus.OK.value(),
                ValidationMessages.RECENT_PAYMENTS_FETCHED,
                dtoList
        );
    }


}
