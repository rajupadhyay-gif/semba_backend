package com.banking.semba.service;

import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.ApiResponseDTO;
import com.banking.semba.dto.TransactionDownloadDTO;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class TransactionDownloadService {

    private final WebClient webClient = WebClient.builder().build();
    private final AuthService authService;

    public TransactionDownloadService(AuthService authService) {
        this.authService = authService;
    }

    public ResponseEntity<byte[]> downloadTransactionReceipt(String auth, String ip, String deviceId,
                                                             Double latitude, Double longitude,
                                                             String transactionId, String format) {

        ApiResponseDTO<TransactionDownloadDTO> transactionResponse =
                fetchTransactionDetails(auth, ip, deviceId, latitude, longitude, transactionId);

        if (transactionResponse.getData() == null) {
            String errorMsg = "Error: " + transactionResponse.getResponseMessage();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(errorMsg.getBytes(StandardCharsets.UTF_8));
        }

        TransactionDownloadDTO dto = transactionResponse.getData();
        byte[] fileBytes;
        HttpHeaders headers = new HttpHeaders();

        if (format.equalsIgnoreCase("csv")) {
            fileBytes = generateTransactionCSV(dto);
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", "Transaction_" + transactionId + ".csv");
        } else {
            fileBytes = generateTransactionPDF(dto);
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Transaction_" + transactionId + ".pdf");
        }

        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }

    public ApiResponseDTO<TransactionDownloadDTO> fetchTransactionDetails(String auth, String ip,
                                                                          String deviceId, Double latitude,
                                                                          Double longitude, String transactionId) {

        ApiResponseDTO<TransactionDownloadDTO> responseDTO = new ApiResponseDTO<>();

        try {
            String apiUrl = "https://dummyjson.com/users/1";
            HttpHeaders headers = authService.buildHeaders(auth, ip, deviceId, latitude, longitude);

            Map<String, Object> apiResponse = webClient.get()
                    .uri(apiUrl)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .onErrorResume(error -> {
                        System.err.println("External API failed: " + error.getMessage());
                        return Mono.empty();
                    })
                    .block();

            TransactionDownloadDTO dto = new TransactionDownloadDTO();
            dto.setTransactionId(transactionId);
            dto.setPaymentType("PAY_TO_UPI");
            dto.setReceiverName(apiResponse != null
                    ? apiResponse.get("firstName") + " " + apiResponse.get("lastName")
                    : "Aarav Sharma");
            dto.setToAccount("aarav@ybl");
            dto.setFromAccount("rajesh@axis");
            dto.setBankName("Axis Bank");
            dto.setAmount(2850.00);
            dto.setDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            dto.setStatus(simulateBankStatus(transactionId));

            responseDTO.setStatus(ValidationMessages.STATUS_OK);
            responseDTO.setResponseCode(HttpStatus.OK.value());
            responseDTO.setResponseMessage("Transaction Fetched Successfully");
            responseDTO.setData(dto);

        } catch (Exception e) {
            System.err.println("Bank API call failed: " + e.getMessage());
            responseDTO.setStatus(ValidationMessages.STATUS_FAILED);
            responseDTO.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseDTO.setResponseMessage("Error fetching transaction details: " + e.getMessage());
            responseDTO.setData(null);
        }

        return responseDTO;
    }

    private byte[] generateTransactionPDF(TransactionDownloadDTO dto) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Transaction Receipt"));
            document.add(new Paragraph("Generated on: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))));
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Transaction ID: " + dto.getTransactionId()));
            document.add(new Paragraph("Payment Type: " + dto.getPaymentType()));
            document.add(new Paragraph("Receiver Name: " + dto.getReceiverName()));
            document.add(new Paragraph("To Account: " + dto.getToAccount()));
            document.add(new Paragraph("From Account: " + dto.getFromAccount()));
            document.add(new Paragraph("Bank Name: " + dto.getBankName()));
            document.add(new Paragraph("Amount: ₹" + dto.getAmount()));
            document.add(new Paragraph("Date: " + dto.getDate()));
            document.add(new Paragraph("Status: " + dto.getStatus()));
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Thank you for using SEMBA Banking Services."));

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        }
    }

    private byte[] generateTransactionCSV(TransactionDownloadDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("Transaction Receipt\n");
        sb.append("Generated on,").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))).append("\n\n");
        sb.append("Transaction ID,").append(dto.getTransactionId()).append("\n");
        sb.append("Payment Type,").append(dto.getPaymentType()).append("\n");
        sb.append("Receiver Name,").append(dto.getReceiverName()).append("\n");
        sb.append("To Account,").append(dto.getToAccount()).append("\n");
        sb.append("From Account,").append(dto.getFromAccount()).append("\n");
        sb.append("Bank Name,").append(dto.getBankName()).append("\n");
        sb.append("Amount,₹").append(dto.getAmount()).append("\n");
        sb.append("Date,").append(dto.getDate()).append("\n");
        sb.append("Status,").append(dto.getStatus()).append("\n");
        sb.append("\nThank you for using SEMBA Banking Services.");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String simulateBankStatus(String transactionId) {
        int code = Math.abs(transactionId.hashCode()) % 3;
        return switch (code) {
            case 0 -> "SUCCESS";
            case 1 -> "FAILED";
            default -> "PENDING";
        };
    }
}
