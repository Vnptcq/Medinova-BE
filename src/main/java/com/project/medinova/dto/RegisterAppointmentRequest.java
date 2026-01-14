package com.project.medinova.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "Request to register for an appointment - System will automatically assign available slot")
public class RegisterAppointmentRequest {
    
    @Schema(description = "ID of the doctor (optional - if not provided, system will assign any available doctor)", example = "1")
    private Long doctorId; // Optional - nếu không có, hệ thống tự động chọn bác sĩ
    
    @Schema(description = "ID of the clinic (required)", example = "1")
    @Positive(message = "Clinic ID must be positive")
    private Long clinicId;
    
    @Schema(description = "Preferred date (optional - system will find nearest available slot)", example = "2025-02-15")
    private String preferredDate; // Optional - format: "yyyy-MM-dd"
    
    @Schema(description = "Preferred time range - morning/afternoon/evening (optional)", example = "MORNING", allowableValues = {"MORNING", "AFTERNOON", "EVENING", "ANY"})
    private String preferredTimeRange; // MORNING (8-12), AFTERNOON (12-17), EVENING (17-20), ANY
    
    @Schema(description = "Duration of the appointment in minutes (default: 60)", example = "60")
    private Integer durationMinutes = 60;

    @Schema(description = "Patient's age", example = "35")
    private Integer age;

    @Schema(description = "Patient's gender", example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER"})
    private String gender;

    @Schema(description = "Patient's symptoms or reason for appointment", example = "Headache and fever for 3 days")
    private String symptoms;
    
    @Schema(description = "Patient's phone number (for contact)", example = "0123456789")
    private String phone;
}
