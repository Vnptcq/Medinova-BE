package com.project.medinova.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Payment information response")
public class PaymentResponse {
    
    @Schema(description = "Payment ID", example = "1")
    private Long id;
    
    @Schema(description = "Patient ID", example = "1")
    private Long patientId;
    
    @Schema(description = "Patient name", example = "Nguyễn Văn A")
    private String patientName;
    
    @Schema(description = "Appointment ID", example = "1")
    private Long appointmentId;
    
    @Schema(description = "Payment amount (deposit)", example = "50000.0")
    private Double amount; // Số tiền yêu cầu
    private Double actualAmount; // Số tiền thực tế nhận được
    
    @Schema(description = "Payment method", example = "BANK_TRANSFER")
    private String paymentMethod;
    
    @Schema(description = "Payment status", example = "PAID", allowableValues = {"PENDING", "PAID", "FAILED", "REFUNDED", "CANCELLED"})
    private String status;
    
    @Schema(description = "Transaction ID from payment gateway", example = "TXN123456789")
    private String transactionId;
    
    @Schema(description = "Payment gateway used", example = "VNPAY")
    private String paymentGateway;
    
    @Schema(description = "Payment date and time")
    private LocalDateTime paidAt;
    
    @Schema(description = "Refund date and time")
    private LocalDateTime refundedAt;
    
    @Schema(description = "Refund reason")
    private String refundReason;
    
    @Schema(description = "Additional notes")
    private String notes;
    
    @Schema(description = "Payment creation date and time")
    private LocalDateTime createdAt;
    
    @Schema(description = "Payment last update date and time")
    private LocalDateTime updatedAt;
}
