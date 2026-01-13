package com.project.medinova.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing ambulance information")
public class AmbulanceResponse {
    
    @Schema(description = "Ambulance ID", example = "1")
    private Long id;
    
    @Schema(description = "Clinic ID", example = "1")
    private Long clinicId;
    
    @Schema(description = "Clinic name", example = "Central Hospital")
    private String clinicName;
    
    @Schema(description = "Status", example = "AVAILABLE")
    private String status;
    
    @Schema(description = "Current latitude", example = "10.762622")
    private Double currentLat;
    
    @Schema(description = "Current longitude", example = "106.660172")
    private Double currentLng;
    
    @Schema(description = "License plate", example = "30A-12345")
    private String licensePlate;
    
    @Schema(description = "Ambulance type", example = "STANDARD")
    private String ambulanceType;
    
    @Schema(description = "Driver name", example = "Nguyen Van A")
    private String driverName;
    
    @Schema(description = "Driver phone", example = "0123456789")
    private String driverPhone;
    
    @Schema(description = "Last idle time")
    private LocalDateTime lastIdleAt;
    
    @Schema(description = "Created at")
    private LocalDateTime createdAt;
}


