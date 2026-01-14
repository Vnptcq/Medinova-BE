package com.project.medinova.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing emergency information with assigned ambulance and doctor")
public class EmergencyResponse {
    
    @Schema(description = "Emergency ID", example = "1")
    private Long id;
    
    @Schema(description = "Clinic ID", example = "1")
    private Long clinicId;
    
    @Schema(description = "Clinic name", example = "Central Hospital")
    private String clinicName;
    
    @Schema(description = "Patient latitude", example = "10.762622")
    private Double patientLat;
    
    @Schema(description = "Patient longitude", example = "106.660172")
    private Double patientLng;
    
    @Schema(description = "Patient address")
    private String patientAddress;
    
    @Schema(description = "Patient name")
    private String patientName;
    
    @Schema(description = "Patient phone")
    private String patientPhone;
    
    @Schema(description = "Emergency description")
    private String description;
    
    @Schema(description = "Status", example = "DISPATCHED")
    private String status;
    
    @Schema(description = "Priority level", example = "HIGH")
    private String priority;
    
    @Schema(description = "Assigned ambulance ID", example = "1")
    private Long ambulanceId;
    
    @Schema(description = "Assigned ambulance license plate", example = "30A-12345")
    private String ambulanceLicensePlate;
    
    @Schema(description = "Ambulance driver name")
    private String driverName;
    
    @Schema(description = "Ambulance driver phone")
    private String driverPhone;
    
    @Schema(description = "Distance from ambulance to patient (km)", example = "2.5")
    private Double distanceKm;
    
    @Schema(description = "Assigned doctor ID", example = "1")
    private Long doctorId;
    
    @Schema(description = "Assigned doctor name")
    private String doctorName;
    
    @Schema(description = "Assigned doctor phone")
    private String doctorPhone;
    
    @Schema(description = "Created at")
    private LocalDateTime createdAt;
    
    @Schema(description = "Dispatched at")
    private LocalDateTime dispatchedAt;
}


