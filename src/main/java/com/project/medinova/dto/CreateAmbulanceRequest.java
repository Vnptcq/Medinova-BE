package com.project.medinova.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create an ambulance")
public class CreateAmbulanceRequest {
    
    @NotNull(message = "Clinic ID is required")
    @Schema(description = "ID of the clinic", example = "1")
    private Long clinicId;
    
    @Schema(description = "Status of the ambulance", example = "AVAILABLE", allowableValues = {"AVAILABLE", "BUSY", "MAINTENANCE", "DISPATCHED"})
    private String status;
    
    @Schema(description = "Current latitude", example = "10.762622")
    private Double currentLat;
    
    @Schema(description = "Current longitude", example = "106.660172")
    private Double currentLng;
    
    @Schema(description = "License plate number", example = "30A-12345")
    private String licensePlate;
    
    @Schema(description = "Ambulance type", example = "STANDARD", allowableValues = {"STANDARD", "ICU", "ADVANCED"})
    private String ambulanceType;
    
    @Schema(description = "Driver name", example = "Nguyen Van A")
    private String driverName;
    
    @Schema(description = "Driver phone", example = "0123456789")
    private String driverPhone;
}


