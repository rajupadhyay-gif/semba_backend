package com.banking.semba.service;

import com.banking.semba.constants.ValidationMessages;
import com.banking.semba.dto.ApiResponseDTO;
import com.banking.semba.dto.TransactionDownloadDTO;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class TransactionDownloadService {

    private final WebClient webClient = WebClient.builder().build();
    private final AuthService authService;

    public TransactionDownloadService(AuthService authService) {
        this.authService = authService;
    }

    public ResponseEntity<byte[]> downloadTransactionPDF(String auth, String ip, String deviceId,
                                                         Double latitude, Double longitude,
                                                         String transactionId) {

        ApiResponseDTO<TransactionDownloadDTO> transactionResponse =
                fetchTransactionDetails(auth, ip, deviceId, latitude, longitude, transactionId);

        if (transactionResponse.getData() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(("Error: " + transactionResponse.getResponseMessage()).getBytes());
        }

        byte[] pdfBytes = generateTransactionPDF(transactionResponse.getData());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Transaction_" + transactionId + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
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

            String simulatedBankStatus = simulateBankStatus(transactionId);
            dto.setStatus(simulatedBankStatus);

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

    private String simulateBankStatus(String transactionId) {
        int code = Math.abs(transactionId.hashCode()) % 3;
        return switch (code) {
            case 0 -> "SUCCESS";
            case 1 -> "FAILED";
            default -> "PENDING";
        };
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

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        }
    }

}
