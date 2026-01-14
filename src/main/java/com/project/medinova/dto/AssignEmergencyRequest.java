package com.project.medinova.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to manually assign doctor and ambulance to an emergency")
public class AssignEmergencyRequest {
    
    @Schema(description = "ID of the doctor to assign (optional)", example = "1")
    private Long doctorId;
    
    @NotNull(message = "Ambulance ID is required")
    @Schema(description = "ID of the ambulance to assign", example = "1")
    private Long ambulanceId;
}

