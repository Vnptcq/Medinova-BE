package com.project.medinova.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for updating user role. If role is DOCTOR and clinicId is not provided, the first clinic in the database will be used as default.")
public class UpdateUserRoleRequest {
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "PATIENT|DOCTOR|ADMIN|RECEPTIONIST|DRIVER", message = "Role must be PATIENT, DOCTOR, ADMIN, RECEPTIONIST, or DRIVER")
    @Schema(description = "New role for the user", example = "DOCTOR")
    private String role;

    @Positive(message = "Clinic ID must be positive")
    @Schema(description = "Clinic ID (optional when role is DOCTOR). If not provided, the first clinic in the database will be used as default.", example = "1")
    private Long clinicId;
}

