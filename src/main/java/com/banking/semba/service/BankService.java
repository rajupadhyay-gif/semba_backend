package com.banking.semba.service;

import com.banking.semba.constants.LogMessages;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.*;
import com.banking.semba.globalException.CustomException;
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

    public ResponseEntity<HttpResponseDTO> getTransactions(String mobile, String accountNumber, LocalDate fromDate, LocalDate toDate, int limit, String searchTerm, String ip, String deviceId, Double latitude, Double longitude
    ) {
        validateDevice(ip, deviceId, latitude, longitude, mobile);

        try {
            String url = "/transactions?accountNumber=" + accountNumber;
            HttpHeaders headers = authService.buildHeaders(mobile, ip, deviceId, latitude, longitude);

//            List<TransactionResponseDTO> allTransactions;
//
//            try {
//                allTransactions = bankWebClient.get()
//                        .uri("https://dummyjson.com/transactions")
//                        .headers(httpHeaders -> httpHeaders.addAll(headers))
//                        .retrieve()
//                        .bodyToFlux(TransactionResponseDTO.class)
//                        .collectList()
//                        .block();
//            } catch (Exception ex) {
//                log.warn("Bank URL is not working, showing dummy data instead");
//
//            }
            List<TransactionResponseDTO> allTransactions = List.of(
                    new TransactionResponseDTO("Swiggy", "DEBIT", 650.00, "UPI", LocalDateTime.now().minusDays(2)),
                    new TransactionResponseDTO("Zomato", "DEBIT", 420.00, "UPI", LocalDateTime.now().minusDays(5)),
                    new TransactionResponseDTO("Amazon", "DEBIT", 2300.00, "CREDIT_CARD", LocalDateTime.now().minusDays(12)),
                    new TransactionResponseDTO("Big Bazaar", "DEBIT", 1150.00, "DEBIT_CARD", LocalDateTime.now().minusDays(20)),
                    new TransactionResponseDTO("Salary", "CREDIT", 55000.00, "NEFT", LocalDateTime.now().minusDays(27)),
                    new TransactionResponseDTO("Electricity Board", "DEBIT", 1350.00, "NET_BANKING", LocalDateTime.now().minusMonths(1).minusDays(4)),
                    new TransactionResponseDTO("Mobile Recharge", "DEBIT", 299.00, "UPI", LocalDateTime.now().minusMonths(2).minusDays(5)),
                    new TransactionResponseDTO("Credit Card Bill", "DEBIT", 18500.00, "NET_BANKING", LocalDateTime.now().minusMonths(3).minusDays(2)),
                    new TransactionResponseDTO("Mutual Fund SIP", "DEBIT", 2000.00, "AUTODEBIT", LocalDateTime.now().minusMonths(4).minusDays(8)),
                    new TransactionResponseDTO("Gym Membership", "DEBIT", 3500.00, "CREDIT_CARD", LocalDateTime.now().minusMonths(5).minusDays(2)),
                    new TransactionResponseDTO("Insurance Premium", "DEBIT", 12000.00, "NET_BANKING", LocalDateTime.now().minusMonths(6).minusDays(1)),
                    new TransactionResponseDTO("Car Loan EMI", "DEBIT", 8500.00, "AUTODEBIT", LocalDateTime.now().minusYears(1).minusMonths(2)),
                    new TransactionResponseDTO("FD Interest", "CREDIT", 6400.00, "BANK_TRANSFER", LocalDateTime.now().minusYears(1).minusMonths(5)),
                    new TransactionResponseDTO("Travel Booking", "DEBIT", 21000.00, "CREDIT_CARD", LocalDateTime.now().minusYears(2).minusDays(20)),
                    new TransactionResponseDTO("Home Renovation", "DEBIT", 68000.00, "NEFT", LocalDateTime.now().minusYears(2).minusMonths(8)),
                    new TransactionResponseDTO("Tax Refund", "CREDIT", 14500.00, "BANK_TRANSFER", LocalDateTime.now().minusYears(3).minusMonths(2)),
                    new TransactionResponseDTO("Laptop Purchase", "DEBIT", 56000.00, "CREDIT_CARD", LocalDateTime.now().minusYears(3).minusMonths(7))
            );

            if (allTransactions.isEmpty()) {
                return ResponseEntity.ok(
                        new HttpResponseDTO(ValidationMessages.SUCCESS, HttpStatus.OK.value(), ValidationMessages.NO_TRANSACTIONS_FOUND, Collections.emptyList())
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

                stream = stream.filter(txn -> {
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

            List<TransactionResponseDTO> filtered = stream
                    .sorted(Comparator.comparing(TransactionResponseDTO::getTransactionDate).reversed())
                    .limit(limit)
                    .toList();

            HttpResponseDTO response = new HttpResponseDTO(
                    ValidationMessages.SUCCESS,
                    200,
                    ValidationMessages.TRANSACTIONS_FETCHED_SUCCESSFULLY,
                    filtered
            );

            return ResponseEntity.ok(response);

        } catch (CustomException e) {
            log.error("External API error: {}", e.getMessage());
            HttpResponseDTO response = new HttpResponseDTO(
                    ValidationMessages.FAILED,
                    HttpStatus.BAD_GATEWAY.value(),
                    ValidationMessages.EXTERNAL_API_NO_RESPONSE + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);

        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            HttpResponseDTO response = new HttpResponseDTO(
                    ValidationMessages.FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    ValidationMessages.INTERNAL_ERROR
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    public ResponseEntity<HttpResponseDTO> searchTransactions(String mobile, String ip, String deviceId, Double latitude, Double longitude, String accountNumber, String searchTerm
    ) {
        validateDevice(ip, deviceId, latitude, longitude, mobile);
        List<TransactionResponseDTO> allTransactions = List.of(
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
                        new HttpResponseDTO(ValidationMessages.TRANSACTIONS_FETCHED_SUCCESSFULLY, 200, ValidationMessages.NO_TRANSACTIONS_FOUND, Collections.emptyList())
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
                    ValidationMessages.STATUS_OK,
                    HttpStatus.OK.value(),
                    filteredTransactions.isEmpty() ? ValidationMessages.NO_TRANSACTIONS_FOUND : ValidationMessages.TRANSACTIONS_FETCHED_SUCCESSFULLY,
                    filteredTransactions
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error while fetching transactions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HttpResponseDTO(ValidationMessages.FAILED, 500, ValidationMessages.INTERNAL_SERVER_ERROR));
        }
    }

    public HttpResponseDTO downloadAccountStatement(String mobile, String ip, String deviceId, Double latitude, Double longitude, String accountNumber, String range, LocalDate fromDate, LocalDate toDate
    ) {

        validateDevice(ip, deviceId, latitude, longitude, mobile);
        HttpHeaders headers = authService.buildHeaders(mobile, ip, deviceId, latitude, longitude);

////    try {
//////            List<TransactionResponseDTO> transactions = bankWebClient.get()
//////                    .uri(uriBuilder -> uriBuilder
//////                            .path("/external/transactions")
//////                            .queryParam("accountNumber", accountNumber)
//////                            .queryParam("fromDate", startDate)
//////                            .queryParam("toDate", endDate)
//////                            .build())
//////                    .headers(httpHeaders -> httpHeaders.addAll(headers))
//////                    .retrieve()
//////                    .bodyToFlux(TransactionResponseDTO.class)
//////                    .collectList()
//////                    .onErrorResume(ex -> {
//////                        log.error("Error fetching statement: {}", ex.getMessage());
//////                        return Mono.just(List.of());
//////                    })
//////                    .block();

        try {
            Map<String, LocalDate> dateRange = getStatementDateRange(range, fromDate, toDate);
            LocalDate startDate = dateRange.get("startDate");
            LocalDate endDate = dateRange.get("endDate");

            List<TransactionResponseDTO> transactions = getDummyTransactions();
            List<TransactionResponseDTO> filteredTxns = transactions.stream()
                    .filter(txn -> {
                        LocalDate txnDate = txn.getTransactionDate().toLocalDate();
                        return (!txnDate.isBefore(startDate)) && (!txnDate.isAfter(endDate));
                    })
                    .collect(Collectors.toList());

            if (filteredTxns.isEmpty()) {
                return new HttpResponseDTO(
                        ValidationMessages.FAILED,
                        HttpStatus.NOT_FOUND.value(),
                        "No transactions found for given period"
                );
            }

            double openingBalance = 10000.00; // Assume fetched from DB or last statement
            double balance = openingBalance;

            List<AccountTransactionDTO> txnList = new ArrayList<>();

            for (TransactionResponseDTO txn : filteredTxns) {
                double debit = txn.getTransactionType().equalsIgnoreCase("DEBIT") ? txn.getAmount() : 0.0;
                double credit = txn.getTransactionType().equalsIgnoreCase("CREDIT") ? txn.getAmount() : 0.0;
                balance += credit - debit;

                txnList.add(new AccountTransactionDTO(
                        txn.getTransactionDate().toLocalDate().toString(),
                        txn.getMerchantName(),
                        "—",
                        txn.getPaymentMode(),
                        debit,
                        credit,
                        balance
                ));
            }

            DownloadStatementDTO statement = DownloadStatementDTO.builder()
                    .accountHolderName("Mr. Raj Upadhyay")
                    .address("Sultanpur, Uttar Pradesh, India")
                    .statementDate(LocalDateTime.now())
                    .accountNumber(accountNumber)
                    .accountDescription("Savings Account")
                    .branch("Sultanpur Branch")
                    .drawingPower(0)
                    .interestRate(3.5)
                    .cifNumber("123456789")
                    .ifscCode("SBIN0000123")
                    .micrCode("110002345")
                    .ckYcNumber("KYC12345")
                    .nominationRegistered("Yes")
                    .balanceAsOn(balance)
                    .searchPeriod(startDate + " to " + endDate)
                    .transactions(txnList)
                    .build();

            return new HttpResponseDTO(
                    ValidationMessages.SUCCESS,
                    HttpStatus.OK.value(),
                    "Statement fetched successfully",
                    statement
            );

        } catch (Exception e) {
            log.error("Error fetching statement: {}", e.getMessage(), e);
            return new HttpResponseDTO(
                    ValidationMessages.FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error fetching statement data"
            );
        }
    }

    private Map<String, LocalDate> getStatementDateRange(String range, LocalDate fromDate, LocalDate toDate) {
        LocalDate today = LocalDate.now();
        Map<String, LocalDate> dateRange = new HashMap<>();
        LocalDate startDate;
        LocalDate endDate;

        if (fromDate != null && toDate != null) {
            if (fromDate.isAfter(toDate)) {
                LocalDate temp = fromDate;
                fromDate = toDate;
                toDate = temp;
            }

            startDate = fromDate;
            endDate = toDate;

        } else {
            switch (range != null ? range.toUpperCase() : "30_DAYS") {

                case "CURRENT_MONTH" -> {
                    startDate = today.withDayOfMonth(1);
                    endDate = today;
                }

                case "30_DAYS" -> {
                    startDate = today.minusDays(30);
                    endDate = today;
                }
                case "60_DAYS" -> {
                    startDate = today.minusDays(60);
                    endDate = today;
                }
                case "90_DAYS" -> {
                    startDate = today.minusDays(90);
                    endDate = today;
                }
                case "120_DAYS" -> {
                    startDate = today.minusDays(120);
                    endDate = today;
                }
                case "180_DAYS" -> {
                    startDate = today.minusDays(180);
                    endDate = today;
                }
                case "365_DAYS" -> {
                    startDate = today.minusDays(365);
                    endDate = today;
                }

                case "CURRENT_FY" -> {
                    startDate = getFinancialYearStart(today);
                    endDate = getFinancialYearEnd(today);
                }

                case "FY_PREV_1" -> {
                    startDate = getFinancialYearStart(today).minusYears(1);
                    endDate = getFinancialYearEnd(today).minusYears(1);
                }
                case "FY_PREV_2" -> {
                    startDate = getFinancialYearStart(today).minusYears(2);
                    endDate = getFinancialYearEnd(today).minusYears(2);
                }
                case "FY_PREV_3" -> {
                    startDate = getFinancialYearStart(today).minusYears(3);
                    endDate = getFinancialYearEnd(today).minusYears(3);
                }
                case "FY_PREV_4" -> {
                    startDate = getFinancialYearStart(today).minusYears(4);
                    endDate = getFinancialYearEnd(today).minusYears(4);
                }

                default -> {
                    startDate = today.minusDays(30);
                    endDate = today;
                }
            }
        }

        dateRange.put("startDate", startDate);
        dateRange.put("endDate", endDate);
        return dateRange;
    }

    private LocalDate getFinancialYearStart(LocalDate today) {
        int year = today.getMonthValue() < 4 ? today.getYear() - 1 : today.getYear();
        return LocalDate.of(year, 4, 1);
    }

    private LocalDate getFinancialYearEnd(LocalDate today) {
        int year = today.getMonthValue() < 4 ? today.getYear() : today.getYear() + 1;
        return LocalDate.of(year, 3, 31);
    }

    private List<TransactionResponseDTO> getDummyTransactions() {
        return List.of(
                new TransactionResponseDTO("Swiggy", "DEBIT", 599.00, "UPI", LocalDateTime.of(2025, 11, 1, 19, 30)),
                new TransactionResponseDTO("Zomato", "DEBIT", 420.00, "UPI", LocalDateTime.of(2025, 10, 27, 20, 15)),
                new TransactionResponseDTO("Amazon", "DEBIT", 1800.00, "CREDIT_CARD", LocalDateTime.of(2025, 10, 10, 14, 10)),
                new TransactionResponseDTO("PhonePe Recharge", "DEBIT", 299.00, "UPI", LocalDateTime.of(2025, 10, 5, 9, 40)),
                new TransactionResponseDTO("Salary", "CREDIT", 62000.00, "BANK_TRANSFER", LocalDateTime.of(2025, 10, 1, 10, 0)),
                new TransactionResponseDTO("Electricity Bill", "DEBIT", 1450.00, "NET_BANKING", LocalDateTime.of(2025, 9, 25, 17, 45)),
                new TransactionResponseDTO("Flipkart", "DEBIT", 2500.00, "DEBIT_CARD", LocalDateTime.of(2025, 9, 20, 15, 30)),

                new TransactionResponseDTO("Swiggy", "DEBIT", 550.00, "UPI", LocalDateTime.of(2025, 3, 10, 10, 30)),
                new TransactionResponseDTO("Amazon", "DEBIT", 2500.00, "CREDIT_CARD", LocalDateTime.of(2025, 2, 12, 14, 20)),
                new TransactionResponseDTO("Zomato", "DEBIT", 420.00, "UPI", LocalDateTime.of(2024, 12, 25, 18, 10)),
                new TransactionResponseDTO("Flipkart", "DEBIT", 1300.00, "DEBIT_CARD", LocalDateTime.of(2024, 8, 9, 12, 45)),
                new TransactionResponseDTO("Big Bazaar", "DEBIT", 780.00, "NET_BANKING", LocalDateTime.of(2024, 4, 15, 9, 30)),
                new TransactionResponseDTO("Electricity Board", "DEBIT", 1250.00, "NET_BANKING", LocalDateTime.of(2024, 2, 5, 16, 15)),
                new TransactionResponseDTO("Mobile Recharge", "DEBIT", 299.00, "UPI", LocalDateTime.of(2023, 11, 12, 11, 5)),
                new TransactionResponseDTO("Gym Membership", "DEBIT", 2000.00, "AUTODEBIT", LocalDateTime.of(2023, 8, 30, 7, 50)),
                new TransactionResponseDTO("Credit Card Bill", "DEBIT", 14500.00, "NET_BANKING", LocalDateTime.of(2023, 6, 10, 13, 20)),
                new TransactionResponseDTO("Salary", "CREDIT", 60000.00, "BANK_TRANSFER", LocalDateTime.of(2023, 3, 1, 10, 0)),
                new TransactionResponseDTO("Insurance Premium", "DEBIT", 12000.00, "NET_BANKING", LocalDateTime.of(2023, 1, 25, 17, 40)),
                new TransactionResponseDTO("FD Interest", "CREDIT", 5400.00, "BANK_TRANSFER", LocalDateTime.of(2023, 2, 15, 10, 10))
        );
    }

    public HttpResponseDTO downloadTransactionStatement(String mobile, String ip, String deviceId, Double latitude, Double longitude, String accountNumber, String range, LocalDate fromDate, LocalDate toDate) {
        validateDevice(ip, deviceId, latitude, longitude, mobile);
        HttpHeaders headers = authService.buildHeaders(mobile, ip, deviceId, latitude, longitude);

////    try {
//////            List<TransactionResponseDTO> transactions = bankWebClient.get()
//////                    .uri(uriBuilder -> uriBuilder
//////                            .path("/external/transactions")
//////                            .queryParam("accountNumber", accountNumber)
//////                            .queryParam("fromDate", startDate)
//////                            .queryParam("toDate", endDate)
//////                            .build())
//////                    .headers(httpHeaders -> httpHeaders.addAll(headers))
//////                    .retrieve()
//////                    .bodyToFlux(TransactionResponseDTO.class)
//////                    .collectList()
//////                    .onErrorResume(ex -> {
//////                        log.error("Error fetching statement: {}", ex.getMessage());
//////                        return Mono.just(List.of());
//////                    })
//////                    .block();

        try {
            Map<String, LocalDate> dateRange = getStatementDateRange(range, fromDate, toDate);
            LocalDate startDate = dateRange.get("startDate");
            LocalDate endDate = dateRange.get("endDate");

            List<TransactionResponseDTO> transactions = getDummyTransactions();
            List<TransactionResponseDTO> filteredTxns = transactions.stream()
                    .filter(txn -> {
                        LocalDate txnDate = txn.getTransactionDate().toLocalDate();
                        return (!txnDate.isBefore(startDate)) && (!txnDate.isAfter(endDate));
                    })
                    .collect(Collectors.toList());

            if (filteredTxns.isEmpty()) {
                return new HttpResponseDTO(
                        ValidationMessages.FAILED,
                        HttpStatus.NOT_FOUND.value(),
                        "No transactions found for given period"
                );
            }

            return new HttpResponseDTO(
                    ValidationMessages.SUCCESS,
                    HttpStatus.OK.value(),
                    "Statement fetched successfully",
                    filteredTxns
            );

        } catch (Exception e) {
            log.error("Error fetching statement: {}", e.getMessage(), e);
            return new HttpResponseDTO(
                    ValidationMessages.FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error fetching statement data"
            );
        }
    }
}
