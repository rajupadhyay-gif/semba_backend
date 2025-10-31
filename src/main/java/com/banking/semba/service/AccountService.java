package com.banking.semba.service;

import com.banking.semba.dto.AccountTransactionDTO;
import com.banking.semba.dto.DownloadStatementDTO;
import com.banking.semba.globalException.CustomException;
import com.banking.semba.globalException.GlobalException;
import com.banking.semba.constants.LogMessages;
import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.ApiResponseDTO;
import com.banking.semba.dto.FundTransferDTO;
import com.banking.semba.dto.response.AccountResponse;
import com.banking.semba.dto.response.PaymentResponse;
import com.banking.semba.util.UserServiceUtils;
import com.banking.semba.util.ValidationUtil;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AccountService {

    private static final String BASE_ACCOUNT_URL = "https://jsonplaceholder.typicode.com";
    private static final boolean useMock = true;
    private final WebClient bankWebClient;
    private final UserServiceUtils userUtils;
    private final ValidationUtil validationUtil;
    private final AuthService authService;

    public AccountService(WebClient bankWebClient, UserServiceUtils userUtils, ValidationUtil validationUtil, AuthService authService) {
        this.bankWebClient = bankWebClient;
        this.userUtils = userUtils;
        this.validationUtil = validationUtil;
        this.authService = authService;
    }


    public ApiResponseDTO<Map<String, Object>> getAccountById(Long id, String mobile, String ip, String deviceId,
                                                              Double latitude, Double longitude) {
        log.info(LogMessages.ACCOUNT_FETCH_START, mobile);
        validateRequest(mobile, ip, deviceId, latitude, longitude);

        Map<String, Object> data = new HashMap<>();
        AccountResponse account;

        if (useMock) {
            account = getMockAccount();
        } else {
            try {
                String url = BASE_ACCOUNT_URL + "/1";

                account = bankWebClient.get()
                        .uri(url)
                        .headers(headers -> {
                            headers.set(HttpHeaders.AUTHORIZATION, mobile);
                            headers.set("X-Device-Id", deviceId);
                            headers.set("X-IP", ip);
                            if (latitude != null) headers.set("X-Latitude", latitude.toString());
                            if (longitude != null) headers.set("X-Longitude", longitude.toString());
                            headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                        })
                        .retrieve()
                        .bodyToMono(AccountResponse.class)
                        .block();

                if (account == null) {
                    throw new GlobalException(
                            ValidationMessages.ACCOUNT_FETCH_FAILED,
                            HttpStatus.BAD_REQUEST.value()
                    );
                }

            } catch (WebClientResponseException ex) {
                log.error(LogMessages.BANK_API_ERROR, ex.getStatusCode().value(), ex.getResponseBodyAsString());
                throw new GlobalException(
                        ValidationMessages.BANK_API_FAILED + ": " + ex.getResponseBodyAsString(),
                        ex.getStatusCode().value()
                );

            } catch (Exception ex) {
                log.error(LogMessages.ACCOUNT_FETCH_FAILED, mobile, ex.getMessage(), ex);
                throw new GlobalException(
                        ValidationMessages.UNKNOWN_ERROR,
                        HttpStatus.INTERNAL_SERVER_ERROR.value()
                );
            }
        }
        data.put("account", account);
        log.info(LogMessages.ACCOUNT_FETCH_SUCCESS, mobile);
        return new ApiResponseDTO<>(ValidationMessages.STATUS_OK, HttpStatus.OK.value(),
                ValidationMessages.ACCOUNT_FETCH_SUCCESS, data);
    }

    // Fetch Live Balance //
    public ApiResponseDTO<Map<String, Object>> getLiveBalance(String accountNumber, String mobile,
                                                              String ip, String deviceId,
                                                              Double latitude, Double longitude) {
        validateRequest(mobile, ip, deviceId, latitude, longitude);

        Map<String, Object> data = new HashMap<>();
        BigDecimal balance;

        if (useMock) {
            balance = BigDecimal.valueOf(2500.35);
        } else {
            try {
                String url = BASE_ACCOUNT_URL + "/" + accountNumber + "/balance";

                balance = bankWebClient.get()
                        .uri(url)
                        .headers(headers -> {
                            headers.set(HttpHeaders.AUTHORIZATION, mobile);
                            headers.set("X-Device-Id", deviceId);
                            headers.set("X-IP", ip);
                            if (latitude != null) headers.set("X-Latitude", latitude.toString());
                            if (longitude != null) headers.set("X-Longitude", longitude.toString());
                            headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                        })
                        .retrieve()
                        .bodyToMono(BigDecimal.class)
                        .block();

                if (balance == null) balance = BigDecimal.ZERO;

            } catch (WebClientResponseException ex) {
                log.error("Bank API error while fetching balance: {} - {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
                throw new GlobalException(ValidationMessages.BANK_API_FAILED, ex.getStatusCode().value());

            } catch (Exception ex) {
                log.error("Unexpected error while fetching balance: {}", ex.getMessage(), ex);
                throw new GlobalException(ValidationMessages.UNKNOWN_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        }

        data.put("balance", balance);
        return new ApiResponseDTO<>(ValidationMessages.STATUS_OK, HttpStatus.OK.value(),
                ValidationMessages.ACCOUNT_FETCH_SUCCESS, data);
    }

    //Fund Transfer//
    public ApiResponseDTO<Map<String, Object>> transferFunds(FundTransferDTO dto, String mobile,
                                                             String ip, String deviceId,
                                                             Double latitude, Double longitude) {
        validateRequest(mobile, ip, deviceId, latitude, longitude);
        validateTransfer(dto, mobile);

        if (dto.getTransactionId() == null)
            dto.setTransactionId(UUID.randomUUID().toString());

        Map<String, Object> data = new HashMap<>();
        PaymentResponse paymentResponse;

        if (useMock) {
            paymentResponse = new PaymentResponse("SUCCESS",
                    "Mock " + dto.getPaymentType() + " transfer completed",
                    dto.getTransactionId());
        } else {
            try {
                String url = switch (dto.getPaymentType()) {
                    case UPI -> "/payments/upi";
                    case MOBILE -> "/payments/mobile";
                    case BANK -> "/payments/transfer";
                    case CREDIT_CARD -> "/payments/credit-card";
                    case DEBIT_CARD -> "/payments/debit-card";
                };

                paymentResponse = bankWebClient.post()
                        .uri(url)
                        .headers(headers -> {
                            headers.set(HttpHeaders.AUTHORIZATION, mobile);
                            headers.set("X-Device-Id", deviceId);
                            headers.set("X-IP", ip);
                            if (latitude != null) headers.set("X-Latitude", latitude.toString());
                            if (longitude != null) headers.set("X-Longitude", longitude.toString());
                            headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                        })
                        .bodyValue(dto)
                        .retrieve()
                        .bodyToMono(PaymentResponse.class)
                        .block();

                if (paymentResponse == null) {
                    paymentResponse = new PaymentResponse(ValidationMessages.STATUS_FAILED,
                            "Bank did not return a response",
                            dto.getTransactionId());
                }

            } catch (WebClientResponseException ex) {
                log.error("Bank API error during fund transfer: {} - {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
                throw new GlobalException(ValidationMessages.BANK_API_FAILED, ex.getStatusCode().value());

            } catch (Exception ex) {
                log.error("Unexpected error during fund transfer: {}", ex.getMessage(), ex);
                throw new GlobalException(ValidationMessages.UNKNOWN_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        }

        data.put("payment", paymentResponse);
        return new ApiResponseDTO<>(
                ValidationMessages.STATUS_OK,
                HttpStatus.OK.value(),
                ValidationMessages.BANK_TRANSACTION,
                data);
    }

    /**
     * Common header validations
     */
    private void validateRequest(String mobile, String ip, String deviceId,
                                 Double latitude, Double longitude) {
        userUtils.validateDeviceInfo(ip, deviceId, latitude, longitude, mobile);
        validationUtil.validateIpFormat(ip, mobile);
        validationUtil.validateDeviceIdFormat(deviceId, mobile);
        if (latitude != null && longitude != null)
            validationUtil.validateLocation(latitude, String.valueOf(longitude), mobile);
    }

    /**
     * Transfer request validations
     */
    private void validateTransfer(FundTransferDTO dto, String mobile) {
        if (dto.getFromAccount() == null || dto.getToAccount() == null || dto.getAmount() == null)
            throw new GlobalException("Invalid transfer request", HttpStatus.BAD_REQUEST.value());
    }

    private AccountResponse getMockAccount() {
        Map<String, Double> breakdown = new HashMap<>();
        breakdown.put("NetWithdraw", 23500.0);
        breakdown.put("OverDraft", 0.0);
        breakdown.put("SweepBalance", 0.0);
        breakdown.put("UnclearedFunds", 0.0);
        breakdown.put("HoldFunds", 0.0);

        return new AccountResponse(
                "1227277878",
                "Ms. Jaya",
                "122234343434",
                "BRIB00022243",
                "BRI",
                "Gomti Nagar Lucknow",
                "123456789@ybl",
                2500.35,
                breakdown
        );
    }

    public ResponseEntity<?> downloadStatement(String auth,
                                               String ip,
                                               String deviceId,
                                               Double latitude,
                                               Double longitude,
                                               String accountNumber,
                                               String range,
                                               String format) {
        validateRequest(auth, ip, deviceId, latitude, longitude);

        try {
            log.info("Fetching account statement for Account: {} | Range: {}", accountNumber, range);

            String externalUrl = "https://dummy-bank-api.com/api/account/statement";
            HttpHeaders headers = authService.buildHeaders(auth, ip, deviceId, latitude, longitude);

            DownloadStatementDTO statement = bankWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(externalUrl)
                            .queryParam("accountNumber", accountNumber)
                            .queryParam("range", range)
                            .queryParam("format", format)
                            .build())
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .retrieve()
                    .bodyToMono(DownloadStatementDTO.class)
                    .onErrorResume(ex -> {
                        log.error("External API failed: {}", ex.getMessage());
                        return generateDummyData(accountNumber, range);
                    })
                    .block();

            if (statement == null || statement.getTransactions() == null || statement.getTransactions().isEmpty()) {
                log.warn("No transactions found for this period", "NOT_FOUND");
                statement = getDummyStatementData(accountNumber);

            }

            if (format.equalsIgnoreCase("pdf")) {
                byte[] pdf = generatePdf(statement);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Account_Statement_" + accountNumber + ".pdf")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(new ByteArrayResource(pdf));
            } else if (format.equalsIgnoreCase("excel")) {
                byte[] excel = generateExcel(statement);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Account_Statement_" + accountNumber + ".xlsx")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(new ByteArrayResource(excel));
            } else {
                return ResponseEntity.ok(statement);
            }

        } catch (CustomException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating statement: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unable to generate account statement"));
        }
    }

    private DownloadStatementDTO getDummyStatementData(String accountNumber) {
        DownloadStatementDTO dto = new DownloadStatementDTO();
        dto.setAccountNumber(accountNumber);
        dto.setAccountHolderName("Mr. Raj Upadhyay");
        dto.setStatementDate(LocalDateTime.now());
        dto.setInterestRate(4.00);
        dto.setCifNumber("34534534");
        dto.setIfscCode("SBI090933");
        dto.setMicrCode("3243242");
        dto.setCkYcNumber("sbi567567");
        dto.setBranch("SULTANPUR (OUDH)");
        dto.setAddress("TEH LAMBHUA SAKHWA BARSARA, SULTANPUR UP, 222302");
        dto.setAccountDescription("Savings Account");
        dto.setBalanceAsOn(10896.48);
        dto.setSearchPeriod("ALL Transactions");

        List<AccountTransactionDTO> transactions = new ArrayList<>();
        transactions.add(new AccountTransactionDTO("01 JAN 2025", "ATM Withdrawal", "—","debit", 500.00, 0.0, 10396.48));
        transactions.add(new AccountTransactionDTO("15 FEB 2025", "Salary Credit", "—","credit", 0.0, 20000.0, 30396.48));
        transactions.add(new AccountTransactionDTO("03 MAR 2025", "Online Purchase", "—","debit", 2500.0, 0.0, 27896.48));
        transactions.add(new AccountTransactionDTO("26 OCT 2025", "NEFT Transfer", "—", "debit",0.0, 2000.0, 29896.48));

        dto.setTransactions(transactions);

        return dto;
    }


    private byte[] generatePdf(DownloadStatementDTO dto) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BaseColor.BLUE);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);

            Paragraph title = new Paragraph("Secure Edge Fin Tech India", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(Chunk.NEWLINE);

            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);
            info.addCell(makeCell("Account Name:", dto.getAccountHolderName(), normalFont));
            info.addCell(makeCell("Account Number:", dto.getAccountNumber(), normalFont));
            info.addCell(makeCell("Address:", dto.getAddress(), normalFont));
            info.addCell(makeCell("Branch:", dto.getBranch(), normalFont));
            info.addCell(makeCell("Account Type:", dto.getAccountDescription(), normalFont));
            info.addCell(makeCell("Balance As On:", String.valueOf(dto.getBalanceAsOn()), normalFont));
            info.addCell(makeCell("Search Period:", dto.getSearchPeriod(), normalFont));
            doc.add(info);

            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Transaction Details", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
            doc.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            addTableHeader(table, "Date", "Narration", "Cheque No.", "Debit", "Credit", "Balance");

            for (AccountTransactionDTO t : dto.getTransactions()) {
                table.addCell(t.getDetails());
                table.addCell(t.getChequeNo());
                table.addCell(String.valueOf(t.getDebit()));
                table.addCell(String.valueOf(t.getCredit()));
                table.addCell(String.valueOf(t.getBalance()));
            }

            doc.add(table);
            doc.add(Chunk.NEWLINE);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    private byte[] generateExcel(DownloadStatementDTO dto) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Account Statement");

            String[] headers = {"Date", "Details", "Cheque No.", "Debit", "Credit", "Balance"};

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNum = 1;
            for (AccountTransactionDTO t : dto.getTransactions()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(1).setCellValue(t.getDetails());
                row.createCell(2).setCellValue(t.getChequeNo());
                row.createCell(3).setCellValue(t.getDebit());
                row.createCell(4).setCellValue(t.getCredit());
                row.createCell(5).setCellValue(t.getBalance());
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            workbook.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Excel generation failed: " + e.getMessage(), e);
        }
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        Font font = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(new BaseColor(230, 230, 250));
            table.addCell(cell);
        }
    }

    private PdfPCell makeCell(String key, String value, Font font) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.addElement(new Paragraph(key + " " + value, font));
        return cell;
    }

        private static Mono<DownloadStatementDTO> generateDummyData(String accountNumber, String range) {
        List<AccountTransactionDTO> txns = List.of(
                new AccountTransactionDTO("26 OCT 2025", "TRANSFER FROM 829173", "-","credit", 0.0, 2000.0, 12896.48),
                new AccountTransactionDTO("28 OCT 2025", "TRANSFER TO 487199", "-","debit", 1000.0, 0.0, 11896.48)
        );

        DownloadStatementDTO dummy = new DownloadStatementDTO(
                "Mr. RAJ UPADHYAY",
                "TEH LAMBHUA SAKHWA BARSARA, BARSARA SULTANPUR UP, 222302",
                accountNumber,
                "SULTANPUR (OUDH)",
                "Savings Account",
                "26 OCT 2025 to 31 OCT 2025",
                10896.48,
                txns
        );
        return Mono.just(dummy);
    }

//    public List<?> getFilteredTransactions(String mobile, String filterType,
//                                                             LocalDate fromDate, LocalDate toDate, String financialYear) {
//
//        List<DownloadStatementDTO> allTransactions = new ArrayList<>();
//        String bankApiUrl="https://localhost:8080/bank/transactions";
//        try {
//            allTransactions = bankWebClient.get()
//                    .uri(bankApiUrl + "/transactions?mobile=" + mobile)
//                    .retrieve()
//                    .bodyToFlux(DownloadStatementDTO.class)
//                    .collectList()
//                    .onErrorResume(ex -> {
//                        System.out.println("External API failed: " + ex.getMessage());
//                        return Mono.just(getDummyTransactions());
//                    })
//                    .block();
//
//            if (allTransactions == null || allTransactions.isEmpty()) {
//                allTransactions = getDummyTransactions();
//            }
//        } catch (Exception e) {
//            System.out.println("Error fetching data, using dummy data: " + e.getMessage());
//            allTransactions = getDummyTransactions();
//        }
//
//        // Determine date range
//        LocalDate today = LocalDate.now();
//
//        if ("30DAYS".equalsIgnoreCase(filterType)) {
//            fromDate = today.minusDays(30);
//            toDate = today;
//        } else if ("90DAYS".equalsIgnoreCase(filterType)) {
//            fromDate = today.minusDays(90);
//            toDate = today;
//        } else if ("180DAYS".equalsIgnoreCase(filterType)) {
//            fromDate = today.minusDays(180);
//            toDate = today;
//        } else if ("365DAYS".equalsIgnoreCase(filterType)) {
//            fromDate = today.minusDays(365);
//            toDate = today;
//        } else if ("FY".equalsIgnoreCase(filterType) && financialYear != null) {
//            String[] fy = financialYear.split("-");
//            int startYear = Integer.parseInt(fy[0]);
//            fromDate = LocalDate.of(startYear, Month.APRIL, 1);
//            toDate = LocalDate.of(startYear + 1, Month.MARCH, 31);
//        } else if ("CUSTOM".equalsIgnoreCase(filterType)) {
//            // already given fromDate & toDate
//        } else {
//            // Default: return all transactions
//            return allTransactions;
//        }
//
//        LocalDate finalFrom = fromDate;
//        LocalDate finalTo = toDate;
//
//        return allTransactions.stream()
//                .filter(txn -> {
//                    LocalDate date = txn.().toLocalDate();
//                    return (date.isEqual(finalFrom) || date.isAfter(finalFrom)) &&
//                            (date.isEqual(finalTo) || date.isBefore(finalTo));
//                })
//                .collect(Collectors.toList());
//    }
//
//    // ✅ Dummy data fallback
//    private List<DownloadStatementDTO> getDummyTransactions() {
//        List<DownloadStatementDTO> list = new ArrayList<>();
//        list.add(createTxn("TXN1001", "ATM Withdrawal", 2000.00, null, 48000.00, "DEBIT", LocalDateTime.now().minusDays(10)));
//        list.add(createTxn("TXN1002", "Salary Credit", null, 55000.00, 103000.00, "CREDIT", LocalDateTime.now().minusDays(25)));
//        list.add(createTxn("TXN1003", "UPI Payment", 1500.00, null, 101500.00, "DEBIT", LocalDateTime.now().minusDays(45)));
//        list.add(createTxn("TXN1004", "UPI Received", null, 2500.00, 104000.00, "CREDIT", LocalDateTime.now().minusDays(60)));
//        list.add(createTxn("TXN1005", "Electricity Bill", 1800.00, null, 102200.00, "DEBIT", LocalDateTime.now().minusDays(120)));
//        list.add(createTxn("TXN1006", "Loan EMI", 5000.00, null, 97200.00, "DEBIT", LocalDateTime.now().minusDays(200)));
//        list.add(createTxn("TXN1007", "Interest Credit", null, 1200.00, 98400.00, "CREDIT", LocalDateTime.now().minusDays(320)));
//        return list;
//    }
//
//    private AccountStatementDTO createTxn(String id, String narration, Double debit, Double credit, Double balance, String type, LocalDateTime date) {
//        AccountStatementDTO dto = new AccountStatementDTO();
//        dto.setTransactionId(id);
//        dto.setNarration(narration);
//        dto.setDebitAmount(debit);
//        dto.setCreditAmount(credit);
//        dto.setBalance(balance);
//        dto.setTransactionType(type);
//        dto.setTransactionDate(date);
//        return dto;
//    }

}