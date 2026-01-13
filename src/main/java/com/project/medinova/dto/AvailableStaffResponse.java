package com.project.medinova.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing available staff information (doctors/drivers)")
public class AvailableStaffResponse {
    
    @Schema(description = "Staff ID", example = "1")
    private Long id;
    
    @Schema(description = "Staff type: DOCTOR or DRIVER", example = "DOCTOR")
    private String staffType;
    
    @Schema(description = "Staff name", example = "Dr. John Doe")
    private String name;
    
    @Schema(description = "Staff email", example = "doctor@example.com")
    private String email;
    
    @Schema(description = "Staff phone", example = "0123456789")
    private String phone;
    
    @Schema(description = "Staff status", example = "APPROVED")
    private String status;
    
    @Schema(description = "Clinic ID", example = "1")
    private Long clinicId;
    
    @Schema(description = "Clinic name", example = "Central Hospital")
    private String clinicName;
    
    @Schema(description = "Department (for doctors)", example = "CARDIOLOGY")
    private String department;
    
    @Schema(description = "Experience years (for doctors)", example = "5")
    private Integer experienceYears;
}
