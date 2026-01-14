package com.project.medinova.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "Request to create a payment for appointment deposit")
public class CreatePaymentRequest {
    
    @NotNull(message = "Appointment ID is required")
    @Positive(message = "Appointment ID must be positive")
    @Schema(description = "ID of the appointment to pay deposit for", example = "1")
    private Long appointmentId;
    
    @NotNull(message = "Payment method is required")
    @Schema(description = "Payment method", example = "BANK_TRANSFER", allowableValues = {"BANK_TRANSFER", "CREDIT_CARD", "E_WALLET", "CASH"})
    private String paymentMethod;
    
    @Schema(description = "Transaction ID from payment gateway (if applicable)", example = "TXN123456789")
    private String transactionId;
    
    @Schema(description = "Payment gateway used", example = "VNPAY", allowableValues = {"VNPAY", "MOMO", "ZALOPAY", "MANUAL"})
    private String paymentGateway;
    
    @Schema(description = "Additional notes", example = "Payment via VNPAY")
    private String notes;
}
