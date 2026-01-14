package com.project.medinova.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to process/confirm a payment")
public class ProcessPaymentRequest {
    
    @NotNull(message = "Transaction ID is required")
    @Schema(description = "Transaction ID from payment gateway", example = "TXN123456789")
    private String transactionId;
    
    @Schema(description = "Payment gateway response data (JSON string)", example = "{\"code\":\"00\",\"message\":\"Success\"}")
    private String gatewayResponse;
    
    @Schema(description = "Actual amount received (may be >= 100,000 VND)", example = "100000.0")
    private Double actualAmount; // Số tiền thực tế nhận được
}
