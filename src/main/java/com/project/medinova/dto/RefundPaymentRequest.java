package com.project.medinova.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request to refund a payment")
public class RefundPaymentRequest {
    
    @Schema(description = "Reason for refund", example = "Appointment cancelled by patient")
    private String reason;
    
    @Schema(description = "Additional notes", example = "Full refund as per cancellation policy")
    private String notes;
}
