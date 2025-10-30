package com.banking.semba.service;

import com.banking.semba.GlobalException.GlobalException;
import com.banking.semba.constants.LogMessages;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.*;
import com.banking.semba.dto.response.BankCardResponse;
import com.banking.semba.dto.response.BankTransactionResponse;
import com.banking.semba.util.MPINValidatorUtil;
import com.banking.semba.util.UserServiceUtils;
import com.banking.semba.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final WebClient bankWebClient;
    private final UserServiceUtils userUtils;
    private final ValidationUtil validationUtil;
    private final MPINValidatorUtil mpinValidatorUtil;
    private final OtpService otpService;

    private static final String PROD_CARD_URL = "https://api.bank.com/cards";
    private static final String PROD_VERIFY_CARD_URL = "https://api.bank.com/cards/verify";
    private static final String PROD_DEBIT_URL = "https://api.bank.com/transactions/debit";
    private static final String PROD_CREDIT_URL = "https://api.bank.com/transactions/credit";

    private static final boolean USE_MOCK = true;
    private static final String DEMO_OTP = "123456";
    private static final String DEMO_MPIN = "1234";

    // ---------------- ADD CARD ----------------
    public ApiResponseDTO<Map<String, Object>> addCard(CardRequest req, String mobile,
                                                       String ip, String deviceId,
                                                       Double latitude, Double longitude) {
        log.info(LogMessages.CARD_ADD_REQUEST, mobile, maskPan(req.getCardNumber()));
        validateRequest(mobile, ip, deviceId, latitude, longitude);
        validateCardDetails(req);
        if (USE_MOCK) {
            return buildMockCardResponse(req, mobile, false);
        }

        try {
            // --- Step 1: Call Core Banking API ---
            String bankUrl = req.getCardType().equalsIgnoreCase("DEBIT")
                    ? PROD_CARD_URL + "/debit/add"
                    : PROD_CARD_URL + "/credit/add";

            BankCardResponse bankResp = bankWebClient.post()
                    .uri(bankUrl)
                    .headers(h -> setBankHeaders(h, mobile, ip, deviceId, latitude, longitude))
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(BankCardResponse.class)
                    .block();

            if (bankResp == null || !bankResp.isSuccess()) {
                throw new GlobalException(ValidationMessages.CARD_ADD_FAILED, HttpStatus.BAD_REQUEST.value());
            }

            // --- Step 2: Prepare OTP Request ---
            String referenceId = "CARD-" + System.currentTimeMillis();

            OtpSendRequestDTO otpRequest = OtpSendRequestDTO.builder()
                    .mobile(mobile)
                    .context("CARD_ADD")
                    .referenceId(referenceId)
                    .build();

            // --- Step 3: Trigger OTP via OtpService ---
            HttpResponseDTO otpResponse = otpService.sendOtp(
                    null, // post-login OTP (no Authorization header)
                    ip, deviceId, latitude, longitude,
                    otpRequest,
                    false
            );

            log.info("OTP triggered successfully for card add | mobile={} | refId={} | status={}",
                    mobile, referenceId, otpResponse.getResponseMessage());

            // --- Step 4: Prepare Final Client Response ---
            Map<String, Object> data = new HashMap<>();
            data.put("mobile", mobile);
            data.put("referenceId", referenceId);
            data.put("otpSentAt", LocalDateTime.now());
            data.put("message", ValidationMessages.OTP_SENT_SUCCESS);

            return new ApiResponseDTO<>(
                    ValidationMessages.STATUS_OK,
                    HttpStatus.OK.value(),
                    ValidationMessages.OTP_SENT_SUCCESS,
                    data
            );

        } catch (WebClientResponseException ex) {
            log.error("Bank API Error: {}", ex.getMessage());
            throw new GlobalException("Bank API failed", ex.getStatusCode().value());
        } catch (Exception ex) {
            log.error("Error in addCard: {}", ex.getMessage());
            throw new GlobalException("Unable to process card add request", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    // ---------------- VERIFY OTP ----------------
    public ApiResponseDTO<Map<String, Object>> verifyCardOtp(CardOtpRequest req,
                                                             String mobile, String ip,
                                                             String deviceId,
                                                             Double latitude, Double longitude) {
        log.info(LogMessages.OTP_VERIFY_REQUEST, mobile, maskPan(req.getCardNumber()));
        validateRequest(mobile, ip, deviceId, latitude, longitude);
        userUtils.validateOtpNotBlank(req.getOtp(), mobile);

        if (USE_MOCK) {
            if (!DEMO_OTP.equals(req.getOtp()))
                throw new GlobalException("Invalid OTP", HttpStatus.BAD_REQUEST.value());
            return buildOtpResponse(req.getCardType());
        }

        try {
            String verifyUrl = req.getCardType().equalsIgnoreCase("DEBIT")
                    ? PROD_VERIFY_CARD_URL + "/debit"
                    : PROD_VERIFY_CARD_URL + "/credit";

            BankCardResponse response = bankWebClient.post()
                    .uri(verifyUrl)
                    .headers(h -> setBankHeaders(h, mobile, ip, deviceId, latitude, longitude))
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(BankCardResponse.class)
                    .block();

            if (response == null || !response.isSuccess())
                throw new GlobalException(ValidationMessages.CARD_VARIFY_FAIL, HttpStatus.BAD_REQUEST.value());

            return buildOtpResponse(req.getCardType());
        } catch (WebClientResponseException ex) {
            throw new GlobalException("Bank API failed", ex.getStatusCode().value());
        }
    }

    // ---------------- GET CARDS ----------------
    public ApiResponseDTO<Map<String, Object>> getCards(String mobile, String ip,
                                                        String deviceId, Double latitude,
                                                        Double longitude, String type) {
        validateRequest(mobile, ip, deviceId, latitude, longitude);
        List<BankCardResponse.CardDetail> allCards;

        if (USE_MOCK) {
            allCards = buildMockCardList();
        } else {
            BankCardResponse response = bankWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path(PROD_CARD_URL)
                            .queryParam("type", "DEBIT,CREDIT").build())
                    .headers(h -> setBankHeaders(h, mobile, ip, deviceId, latitude, longitude))
                    .retrieve()
                    .bodyToMono(BankCardResponse.class)
                    .block();

            if (response == null || !response.isSuccess())
                throw new GlobalException("Failed to fetch cards", HttpStatus.BAD_REQUEST.value());
            allCards = response.getCards();
        }

        if (type != null && !type.isBlank()) {
            allCards = allCards.stream()
                    .filter(c -> type.equalsIgnoreCase((String) c.getMetadata().get("type")))
                    .toList();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("cards", allCards);
        return new ApiResponseDTO<>(ValidationMessages.STATUS_OK, HttpStatus.OK.value(), ValidationMessages.CARD_FETCHED_SUCESS, data);
    }

    // ---------------- PROCESS PAYMENT ----------------
    public ApiResponseDTO<Map<String, Object>> processPayment(PayNowRequest req,
                                                              String mobile, String ip,
                                                              String deviceId,
                                                              Double latitude, Double longitude) {
        log.info(LogMessages.PAYMENT_REQUEST, mobile, maskPan(req.getCardNumber()));
        validateRequest(mobile, ip, deviceId, latitude, longitude);
        validateCardForPayment(req);

        String transactionId = "TXN" + System.currentTimeMillis();

        // Debit: validate MPIN
        if ("DEBIT".equalsIgnoreCase(req.getCardType())) {
            ApiResponseDTO<MPINValidationResponseDTO> mpinResp =
                    mpinValidatorUtil.validateCardMPIN(mobile, ip, deviceId, latitude, longitude,
                            req.getAccountNumber(), req.getMpin(), transactionId);

            if (mpinResp.getData() == null || !mpinResp.getData().isValid()) {
                throw new GlobalException("Invalid MPIN", HttpStatus.BAD_REQUEST.value());
            }
        }

        if (USE_MOCK) {
            return buildMockPaymentResponse(req, transactionId);
        }

        try {
            String bankUrl = req.getCardType().equalsIgnoreCase("DEBIT")
                    ? PROD_DEBIT_URL : PROD_CREDIT_URL;

            BankTransactionResponse response = bankWebClient.post()
                    .uri(bankUrl)
                    .headers(h -> setBankHeaders(h, mobile, ip, deviceId, latitude, longitude))
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(BankTransactionResponse.class)
                    .block();

            if (response == null || !response.isSuccess())
                throw new GlobalException(ValidationMessages.BANK_TRANSECTION_FAIL, HttpStatus.BAD_REQUEST.value());

            Map<String, Object> data = new HashMap<>();
            data.put("transactionId", response.getTransactionId());
            data.put("amount", req.getAmount());
            data.put("cardType", req.getCardType());
            data.put("timestamp", LocalDateTime.now());
            return new ApiResponseDTO<>(
                    ValidationMessages.STATUS_OK,
                    HttpStatus.OK.value(),
                    ValidationMessages.PAYMENT_SUCCESS,
                    data);

        } catch (WebClientResponseException ex) {
            throw new GlobalException("Bank API failed", ex.getStatusCode().value());
        }
    }

    // ---------------- HELPERS ----------------
    private void setBankHeaders(HttpHeaders headers, String mobile, String ip, String deviceId, Double lat, Double lon) {
        headers.set(HttpHeaders.AUTHORIZATION, mobile);
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
    }

    private List<BankCardResponse.CardDetail> buildMockCardList() {
        BankCardResponse.CardDetail debit = new BankCardResponse.CardDetail("************5678", "Alice Brown", "08/25", true, null, LocalDateTime.now(), Map.of("type", "DEBIT"));
        BankCardResponse.CardDetail credit = new BankCardResponse.CardDetail("************4321", "John Doe", "12/26", true, null, LocalDateTime.now(), Map.of("type", "CREDIT"));
        return List.of(debit, credit);
    }

    private void validateCardForPayment(PayNowRequest req) {
        if (req.getCardNumber() == null || req.getCardNumber().isBlank())
            throw new GlobalException(ValidationMessages.CARD_ID_BLANK, HttpStatus.BAD_REQUEST.value());
        if (req.getValidThru() == null || req.getValidThru().isBlank())
            throw new GlobalException(ValidationMessages.VALID_THRU_BLANK, HttpStatus.BAD_REQUEST.value());
        if (req.getCardType().equalsIgnoreCase("DEBIT") && !DEMO_MPIN.equals(req.getMpin()))
            throw new GlobalException("Invalid MPIN", HttpStatus.BAD_REQUEST.value());
    }

    private void validateCardDetails(CardRequest req) {
        if (req.getCardNumber() == null || req.getCardNumber().isBlank())
            throw new GlobalException(ValidationMessages.CARD_ID_BLANK, HttpStatus.BAD_REQUEST.value());
        if (!req.getCardNumber().matches("\\d{16}"))
            throw new GlobalException(ValidationMessages.CARD_ID_INVALID, HttpStatus.BAD_REQUEST.value());
        if (req.getHolderName() == null || req.getHolderName().isBlank())
            throw new GlobalException(ValidationMessages.HOLDER_NAME_BLANK, HttpStatus.BAD_REQUEST.value());
        if (!req.getHolderName().matches("^[A-Za-z ]{2,50}$"))
            throw new GlobalException(ValidationMessages.HOLDER_NAME_INVALID, HttpStatus.BAD_REQUEST.value());
        if (req.getValidThru() == null || req.getValidThru().isBlank())
            throw new GlobalException(ValidationMessages.VALID_THRU_BLANK, HttpStatus.BAD_REQUEST.value());
        if (!req.getValidThru().matches("^(0[1-9]|1[0-2])/\\d{2}$"))
            throw new GlobalException(ValidationMessages.VALID_THRU_INVALID, HttpStatus.BAD_REQUEST.value());
    }

    private ApiResponseDTO<Map<String, Object>> buildOtpResponse(String type) {
        Map<String, Object> data = new HashMap<>();
        data.put("verified", true);
        data.put("cardType", type);
        data.put("verifiedAt", LocalDateTime.now());
        return new ApiResponseDTO<>(
                ValidationMessages.STATUS_OK,
                HttpStatus.OK.value(),
                ValidationMessages.CARD_OTP_VERIFY_SUCCESS,
                data);
    }

    private ApiResponseDTO<Map<String, Object>> buildMockPaymentResponse(PayNowRequest req, String txnId) {
        Map<String, Object> data = new HashMap<>();
        data.put("transactionId", txnId);
        data.put("amount", req.getAmount());
        data.put("cardType", req.getCardType());
        data.put("timestamp", LocalDateTime.now());
        data.put("status", "SUCCESS");
        return new ApiResponseDTO<>(ValidationMessages.STATUS_OK,
                HttpStatus.OK.value(),
                ValidationMessages.PAYMENT_SUCCESS,
                data);
    }

    private String maskPan(String pan) {
        if (pan == null || pan.length() < 4) return "****";
        return "************" + pan.substring(pan.length() - 4);
    }

    private ApiResponseDTO<Map<String, Object>> buildResponse(Object payload, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("data", payload);
        return new ApiResponseDTO<>(
                ValidationMessages.STATUS_OK,
                HttpStatus.OK.value(),
                message,
                data);
    }

    private ApiResponseDTO<Map<String, Object>> buildMockCardResponse(CardRequest req, String mobile, boolean verified) {
        Map<String, Object> card = new HashMap<>();
        card.put("mobile", mobile);
        card.put("cardNumber", maskPan(req.getCardNumber()));
        card.put("holderName", req.getHolderName());
        card.put("validThru", req.getValidThru());
        card.put("cardType", req.getCardType());
        card.put("verified", verified);
        card.put("otpSentAt", LocalDateTime.now());
        return buildResponse(card, ValidationMessages.CARD_ADDED_SUCCESS);
    }
}