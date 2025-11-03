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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
                                        HttpStatus.BAD_REQUEST
                                ));
                            })
                    )
                    .bodyToMono(Object.class)
                    .block();

            if (bankList == null) {
                log.warn(LogMessages.FETCH_BANKS_NULL);
                throw new CustomException(
                        ValidationMessages.NO_BANKS_FOUND,
                        HttpStatus.NOT_FOUND
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
                                            HttpStatus.BAD_REQUEST
                                    ));
                                })
                )
                .bodyToMono(Object.class)
                .block();

        if (bankListObj == null || ((Map<?, ?>) bankListObj).isEmpty()) {
            log.warn(LogMessages.FETCH_BANKS_NULL);
            throw new CustomException(
                    ValidationMessages.NO_BANKS_FOUND,
                    HttpStatus.NOT_FOUND
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

//    public HttpResponseDTO getTransactions(String mobile, String ip, String deviceId,
//                                           Double latitude, Double longitude,
//                                           String accountNumber,
//                                           LocalDate fromDate, LocalDate toDate,
//                                           String filterType, String transactionType, int limit) {
//
//        HttpResponseDTO response = new HttpResponseDTO();
//        validateDevice(ip, deviceId, latitude, longitude, mobile);
//        HttpHeaders headers = authService.buildHeaders(mobile, ip, deviceId, latitude, longitude);
//
//        try {
//            List<TransactionResponseDTO> transactions;
//
//            try {
//                transactions = bankWebClient.get()
//                        .uri("https://dummyjson.com/transactions")
//                        .headers(httpHeaders -> httpHeaders.addAll(headers))
//                        .retrieve()
//                        .bodyToFlux(TransactionResponseDTO.class)
//                        .collectList()
//                        .block();
//            } catch (Exception ex) {
//                log.warn("Bank URL is not working, showing dummy data instead");
//                transactions = getDummyShowTransactions();
//            }
//
//            LocalDate today = LocalDate.now();
//            LocalDate startDate;
//            LocalDate endDate = today;
//
//            if (filterType != null) {
//                switch (filterType.toUpperCase()) {
//                    case "1M":
//                        startDate = today.minusMonths(1);
//                        break;
//                    case "3M":
//                        startDate = today.minusMonths(3);
//                        break;
//                    case "6M":
//                        startDate = today.minusMonths(6);
//                        break;
//                    case "CUSTOM":
//                        startDate = fromDate != null ? fromDate : today.minusMonths(1);
//                        endDate = toDate != null ? toDate : today;
//                        break;
//                    default:
//                        startDate = today.minusMonths(1);
//                }
//            } else {
//                startDate = today.minusMonths(1);
//            }
//
//            LocalDate finalStartDate = startDate;
//            LocalDate finalEndDate = endDate;
//            transactions = transactions.stream()
//                    .filter(txn -> txn.getTransactionDate() != null)
//                    .filter(txn -> {
//                        LocalDate txnDate = txn.getTransactionDate().toLocalDate();
//                        return (txnDate.isEqual(finalStartDate) || txnDate.isAfter(finalStartDate))
//                                && (txnDate.isEqual(finalEndDate) || txnDate.isBefore(finalEndDate));
//                    })
//                    .filter(txn -> {
//                        if ("ALL".equalsIgnoreCase(transactionType)) return true;
//                        return txn.getTransactionType().equalsIgnoreCase(transactionType);
//                    })
//                    .sorted(Comparator.comparing(TransactionResponseDTO::getTransactionDate).reversed())
//                    .limit(limit)
//                    .collect(Collectors.toList());
//
//            response.setResponseCode(200);
//            response.setResponseMessage("Transactions fetched successfully");
//            response.setStatus("SUCCESS");
//            response.setResponseData(transactions);
//
//            return response;
//
//        } catch (Exception e) {
//            log.error("Error fetching transactions", e);
//            response.setResponseCode(500);
//            response.setResponseMessage("Error while fetching transactions: " + e.getMessage());
//            response.setStatus("FAILED");
//            return response;
//        }
//    }

    public ResponseEntity<HttpResponseDTO> getTransactions(String mobile, String accountNumber, LocalDate fromDate, LocalDate toDate, String searchTerm, int limit, String ip, String deviceId, Double latitude, Double longitude
    ) {

        validateDevice(ip, deviceId, latitude, longitude, mobile);

        try {

          String url = "/transactions?accountNumber=" + accountNumber;
            HttpHeaders headers = authService.buildHeaders(mobile, ip, deviceId, latitude, longitude);

//            List<TransactionResponseDTO> allTransactions = bankWebClient.get()
//                    .uri(url)
//                    .headers(httpHeaders -> httpHeaders.addAll(headers))
//                    .retrieve()
//                    .bodyToFlux(TransactionResponseDTO.class)
//                    .collectList()
//                    .block();

            List<TransactionResponseDTO> allTransactions = List.of(
                    // 🔹 Last 1 month
                    new TransactionResponseDTO("Swiggy", "DEBIT", 650.00, "UPI", LocalDateTime.now().minusDays(2)),
                    new TransactionResponseDTO("Zomato", "DEBIT", 420.00, "UPI", LocalDateTime.now().minusDays(5)),
                    new TransactionResponseDTO("Amazon", "DEBIT", 2300.00, "CREDIT_CARD", LocalDateTime.now().minusDays(12)),
                    new TransactionResponseDTO("Big Bazaar", "DEBIT", 1150.00, "DEBIT_CARD", LocalDateTime.now().minusDays(20)),
                    new TransactionResponseDTO("Salary", "CREDIT", 55000.00, "NEFT", LocalDateTime.now().minusDays(27)),

                    // 🔹 Last 3 months
                    new TransactionResponseDTO("Electricity Board", "DEBIT", 1350.00, "NET_BANKING", LocalDateTime.now().minusMonths(1).minusDays(4)),
                    new TransactionResponseDTO("Mobile Recharge", "DEBIT", 299.00, "UPI", LocalDateTime.now().minusMonths(2).minusDays(5)),
                    new TransactionResponseDTO("Credit Card Bill", "DEBIT", 18500.00, "NET_BANKING", LocalDateTime.now().minusMonths(3).minusDays(2)),

                    // 🔹 Last 6 months
                    new TransactionResponseDTO("Mutual Fund SIP", "DEBIT", 2000.00, "AUTODEBIT", LocalDateTime.now().minusMonths(4).minusDays(8)),
                    new TransactionResponseDTO("Gym Membership", "DEBIT", 3500.00, "CREDIT_CARD", LocalDateTime.now().minusMonths(5).minusDays(2)),
                    new TransactionResponseDTO("Insurance Premium", "DEBIT", 12000.00, "NET_BANKING", LocalDateTime.now().minusMonths(6).minusDays(1)),

                    // 🔹 Last 2 years
                    new TransactionResponseDTO("Car Loan EMI", "DEBIT", 8500.00, "AUTODEBIT", LocalDateTime.now().minusYears(1).minusMonths(2)),
                    new TransactionResponseDTO("FD Interest", "CREDIT", 6400.00, "BANK_TRANSFER", LocalDateTime.now().minusYears(1).minusMonths(5)),
                    new TransactionResponseDTO("Travel Booking", "DEBIT", 21000.00, "CREDIT_CARD", LocalDateTime.now().minusYears(2).minusDays(20)),

                    // 🔹 Last 3 years
                    new TransactionResponseDTO("Home Renovation", "DEBIT", 68000.00, "NEFT", LocalDateTime.now().minusYears(2).minusMonths(8)),
                    new TransactionResponseDTO("Tax Refund", "CREDIT", 14500.00, "BANK_TRANSFER", LocalDateTime.now().minusYears(3).minusMonths(2)),
                    new TransactionResponseDTO("Laptop Purchase", "DEBIT", 56000.00, "CREDIT_CARD", LocalDateTime.now().minusYears(3).minusMonths(7))
            );



            if (allTransactions == null || allTransactions.isEmpty()) {
                return ResponseEntity.ok(
                        new HttpResponseDTO("SUCCESS", 200, "No transactions found", Collections.emptyList())
                );
            }

            Stream<TransactionResponseDTO> stream = allTransactions.stream();
            if (fromDate != null && toDate != null) {
                stream = stream.filter(t -> {
                    LocalDate txnDate = t.getTransactionDate().toLocalDate();
                    return !txnDate.isBefore(fromDate) && !txnDate.isAfter(toDate);
                });
            }

            if (searchTerm != null && !searchTerm.isEmpty()) {
                String lowerSearch = searchTerm.toLowerCase();
                stream = stream.filter(t ->
                        t.getMerchantName().toLowerCase().contains(lowerSearch)
                                || t.getTransactionType().toLowerCase().contains(lowerSearch)
                                || t.getPaymentMode().toLowerCase().contains(lowerSearch)
                                || String.valueOf(t.getAmount()).contains(lowerSearch)
                                || t.getTransactionDate().toString().contains(lowerSearch)
                );
            }

            List<TransactionResponseDTO> filtered = stream
                    .sorted(Comparator.comparing(TransactionResponseDTO::getTransactionDate).reversed())
                    .limit(limit)
                    .toList();

            HttpResponseDTO response = new HttpResponseDTO(
                    "SUCCESS",
                    200,
                    "Transactions fetched successfully",
                    filtered
            );
            return ResponseEntity.ok(response);

        } catch (CustomException e) {
            log.error("External API error: {}", e.getMessage());
            HttpResponseDTO response = new HttpResponseDTO(
                    "FAILED",
                    e.getErrorCode().value(),
                    "Error from external API: " + e.getMessage()
            );
            return ResponseEntity.status(e.getErrorCode().value()).body(response);

        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            HttpResponseDTO response = new HttpResponseDTO(
                    "FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    private List<TransactionResponseDTO> getDummySearchTransactions() {
        List<TransactionResponseDTO> dummy = new ArrayList<>();

        dummy.add(TransactionResponseDTO.builder()
                .merchantName("Amazon India")
                .transactionType("DEBIT")
                .amount(2499.75)
                .paymentMode("UPI")
                .transactionDate(LocalDateTime.now().minusDays(2))
                .build());

        dummy.add(TransactionResponseDTO.builder()
                .merchantName("HDFC Salary Credit")
                .transactionType("CREDIT")
                .amount(55000.00)
                .paymentMode("NEFT")
                .transactionDate(LocalDateTime.now().minusDays(1))
                .build());

        dummy.add(TransactionResponseDTO.builder()
                .merchantName("Zomato")
                .transactionType("DEBIT")
                .amount(425.50)
                .paymentMode("UPI")
                .transactionDate(LocalDateTime.now().minusDays(4))
                .build());

        dummy.add(TransactionResponseDTO.builder()
                .merchantName("IRCTC Refund")
                .transactionType("CREDIT")
                .amount(1280.00)
                .paymentMode("UPI")
                .transactionDate(LocalDateTime.now().minusDays(12))
                .build());

        dummy.add(TransactionResponseDTO.builder()
                .merchantName("HP Petrol Pump")
                .transactionType("DEBIT")
                .amount(1200.00)
                .paymentMode("CARD")
                .transactionDate(LocalDateTime.now().minusDays(18))
                .build());

        dummy.add(TransactionResponseDTO.builder()
                .merchantName("Netflix Subscription")
                .transactionType("DEBIT")
                .amount(499.00)
                .paymentMode("AUTOPAY")
                .transactionDate(LocalDateTime.now().minusDays(28))
                .build());

        dummy.add(TransactionResponseDTO.builder()
                .merchantName("Google Play Refund")
                .transactionType("CREDIT")
                .amount(200.00)
                .paymentMode("UPI")
                .transactionDate(LocalDateTime.now().minusDays(45))
                .build());

        dummy.add(TransactionResponseDTO.builder()
                .merchantName("Tata Sky Recharge")
                .transactionType("DEBIT")
                .amount(399.00)
                .paymentMode("NETBANKING")
                .transactionDate(LocalDateTime.now().minusDays(60))
                .build());

        dummy.add(TransactionResponseDTO.builder()
                .merchantName("Uber Ride")
                .transactionType("DEBIT")
                .amount(250.00)
                .paymentMode("UPI")
                .transactionDate(LocalDateTime.now().minusDays(75))
                .build());

        dummy.add(TransactionResponseDTO.builder()
                .merchantName("LIC Premium Refund")
                .transactionType("CREDIT")
                .amount(950.00)
                .paymentMode("NEFT")
                .transactionDate(LocalDateTime.now().minusDays(100))
                .build());

        return dummy;
    }


    public ResponseEntity<HttpResponseDTO> searchTransactions(
            String mobile,
            String ip,
            String deviceId,
            Double latitude,
            Double longitude,
            String accountNumber,
            String searchTerm
    ) {
        validateDevice(ip, deviceId, latitude, longitude, mobile);
         List<TransactionResponseDTO> allTransactions= List.of(
                new TransactionResponseDTO("Swiggy", "DEBIT", 650.00, "UPI", LocalDateTime.now().minusDays(2)),
                new TransactionResponseDTO("Zomato", "DEBIT", 420.00, "UPI", LocalDateTime.now().minusDays(5)),
                new TransactionResponseDTO("Amazon", "DEBIT", 2300.00, "CREDIT_CARD", LocalDateTime.now().minusDays(12)),
                new TransactionResponseDTO("Big Bazaar", "DEBIT", 1150.00, "DEBIT_CARD", LocalDateTime.now().minusDays(20)),
                new TransactionResponseDTO("Salary", "CREDIT", 55000.00, "NEFT", LocalDateTime.now().minusDays(27)),
                new TransactionResponseDTO("Mobile Recharge", "DEBIT", 299.00, "UPI", LocalDateTime.now().minusMonths(2)),
                new TransactionResponseDTO("Car Loan EMI", "DEBIT", 8500.00, "AUTODEBIT", LocalDateTime.now().minusYears(1).minusMonths(2)),
                new TransactionResponseDTO("Travel Booking", "DEBIT", 21000.00, "CREDIT_CARD", LocalDateTime.now().minusYears(2).minusDays(20)),
                new TransactionResponseDTO("Tax Refund", "CREDIT", 14500.00, "BANK_TRANSFER", LocalDateTime.now().minusYears(3).minusMonths(2))
        );
        try {
            String url = "/transactions?accountNumber=" + accountNumber;

            HttpHeaders headers = authService.buildHeaders(mobile, ip, deviceId, latitude, longitude);

//            List<TransactionResponseDTO> allTransactions = bankWebClient.get()
//                    .uri(url)
//                    .headers(httpHeaders -> httpHeaders.addAll(headers))
//                    .retrieve()
//                    .bodyToFlux(TransactionResponseDTO.class)
//                    .collectList()
//                    .block();

            if (allTransactions == null || allTransactions.isEmpty()) {
                return ResponseEntity.ok(
                        new HttpResponseDTO("SUCCESS", 200, "No transactions found", Collections.emptyList())
                );
            }

            Stream<TransactionResponseDTO> filteredStream = allTransactions.stream();
            if (searchTerm != null && !searchTerm.isEmpty()) {
                String lowerSearch = searchTerm.toLowerCase();

                filteredStream = filteredStream.filter(txn -> {
                    String merchant = txn.getMerchantName() != null ? txn.getMerchantName().toLowerCase() : "";
                    boolean matchesMerchant = merchant.startsWith(lowerSearch);
                    boolean matchesAmount = false;
                    try {
                        double searchAmount = Double.parseDouble(searchTerm);
                        matchesAmount = txn.getAmount() == searchAmount;
                    } catch (NumberFormatException ignored) {
                    }

                    return matchesMerchant || matchesAmount;
                });
            }


            List<TransactionResponseDTO> filteredTransactions = filteredStream
                    .sorted(Comparator.comparing(TransactionResponseDTO::getTransactionDate).reversed())
                    .toList();

            HttpResponseDTO response = new HttpResponseDTO(
                    "SUCCESS",
                    200,
                    filteredTransactions.isEmpty() ? "No matching transactions found" : "Transactions fetched successfully",
                    filteredTransactions
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error while fetching transactions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HttpResponseDTO("FAILED", 500, "Internal Server Error"));
        }
    }


}
