package com.project.medinova.controller;

import com.project.medinova.dto.AssignEmergencyRequest;
import com.project.medinova.dto.AvailableStaffResponse;
import com.project.medinova.dto.CreateEmergencyRequest;
import com.project.medinova.dto.EmergencyResponse;
import com.project.medinova.dto.UpdateEmergencyStatusRequest;
import com.project.medinova.service.EmergencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergencies")
@CrossOrigin(origins = "*")
@Tag(name = "Emergency Management", description = "Emergency request and ambulance dispatch APIs")
public class EmergencyController {

    @Autowired
    private EmergencyService emergencyService;

    @Operation(
            summary = "Create emergency request",
            description = "Create an emergency request. The system will automatically find the nearest available ambulance and assign a doctor. Uses Haversine formula to calculate distance. If clinicId is not provided (or 0), the system will automatically find the nearest clinic based on patient location. If no ambulance is available, the emergency will be created with PENDING status."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Emergency request created successfully",
                    content = @Content(schema = @Schema(implementation = EmergencyResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request - Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Clinic not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    @PostMapping
    public ResponseEntity<EmergencyResponse> createEmergency(@Valid @RequestBody CreateEmergencyRequest request) {
        EmergencyResponse response = emergencyService.createEmergency(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get available staff (doctors) for emergency assignment",
            description = "Get paginated list of available staff (doctors) who can be assigned to emergencies. Only approved doctors who are not currently assigned to active emergencies or ongoing appointments are returned."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Available staff retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN and DOCTOR can access")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @GetMapping("/available-staff")
    public ResponseEntity<Page<AvailableStaffResponse>> getAvailableStaff(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "100") int size) {
        
        if (page < 0) page = 0;
        if (size < 1) size = 10;
        if (size > 1000) size = 1000; // Limit max page size
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AvailableStaffResponse> staff = emergencyService.getAvailableStaff(pageable);
        return ResponseEntity.ok(staff);
    }

    @Operation(
            summary = "Get emergency by ID",
            description = "Get emergency request information by ID including assigned ambulance and doctor. Patients can only view their own emergencies, while ADMIN and DOCTOR can view any emergency."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Emergency retrieved successfully",
                    content = @Content(schema = @Schema(implementation = EmergencyResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Cannot view other patients' emergencies"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Emergency not found")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<EmergencyResponse> getEmergencyById(@PathVariable Long id) {
        EmergencyResponse response = emergencyService.getEmergencyById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get all emergencies of a specific doctor",
            description = "Get all emergency cases assigned to a specific doctor. Can filter by status (PENDING, DISPATCHED, IN_TRANSIT, COMPLETED, CANCELLED). Results are sorted by creation time (newest first)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "List of emergencies retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN and DOCTOR can access"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Doctor not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @GetMapping("/doctors/{doctorId}")
    public ResponseEntity<List<EmergencyResponse>> getEmergenciesByDoctorId(
            @PathVariable Long doctorId,
            @RequestParam(required = false) String status) {
        List<EmergencyResponse> responses = emergencyService.getEmergenciesByDoctorId(doctorId, status);
        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "Get my emergencies (current doctor)",
            description = "Get all emergency cases assigned to the currently authenticated doctor. Can filter by status (PENDING, DISPATCHED, IN_TRANSIT, COMPLETED, CANCELLED). Results are sorted by creation time (newest first)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "List of emergencies retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only doctors can access")
    })
    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/my-emergencies")
    public ResponseEntity<List<EmergencyResponse>> getMyEmergencies(
            @RequestParam(required = false) String status) {
        List<EmergencyResponse> responses = emergencyService.getMyEmergencies(status);
        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "Get my emergencies (current patient)",
            description = "Get all emergency cases created by the currently authenticated patient. Can filter by status (PENDING, DISPATCHED, IN_TRANSIT, COMPLETED, CANCELLED). Results are sorted by creation time (newest first)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "List of emergencies retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only patients can access")
    })
    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/my-patient-emergencies")
    public ResponseEntity<List<EmergencyResponse>> getMyPatientEmergencies(
            @RequestParam(required = false) String status) {
        List<EmergencyResponse> responses = emergencyService.getMyPatientEmergencies(status);
        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "Manually assign doctor and ambulance to emergency",
            description = "Manually assign a specific doctor and ambulance to an emergency. Use this when automatic assignment failed or needs to be changed. The doctor must be available (no active emergencies or ongoing appointments) and the ambulance must be AVAILABLE. Ambulance is optional - can be null if no ambulance is available."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Emergency assigned successfully",
                    content = @Content(schema = @Schema(implementation = EmergencyResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request - Doctor/ambulance not available, wrong clinic, or validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN and DOCTOR can assign"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Emergency, doctor, or ambulance not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @PutMapping("/{id}/assign")
    public ResponseEntity<EmergencyResponse> assignEmergency(
            @PathVariable Long id,
            @Valid @RequestBody AssignEmergencyRequest request) {
        EmergencyResponse response = emergencyService.assignEmergency(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update emergency status",
            description = "Update emergency status. When doctor confirms (status = DISPATCHED), the system will automatically set dispatchedAt timestamp. When status is set to COMPLETED or CANCELLED, the assigned ambulance will be set back to AVAILABLE."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Emergency status updated successfully",
                    content = @Content(schema = @Schema(implementation = EmergencyResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request - Cannot set status to DISPATCHED without assigned doctor, or validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN and DOCTOR can update status"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Emergency not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @PutMapping("/{id}/status")
    public ResponseEntity<EmergencyResponse> updateEmergencyStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmergencyStatusRequest request) {
        EmergencyResponse response = emergencyService.updateEmergencyStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get all emergencies (ADMIN only)",
            description = "Get all emergency cases with optional status filter. Results are sorted by creation time (newest first). Only ADMIN can access all emergencies."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "List of emergencies retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN can access all emergencies")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<EmergencyResponse>> getAllEmergencies(
            @RequestParam(required = false) String status) {
        List<EmergencyResponse> responses = emergencyService.getAllEmergencies(status);
        return ResponseEntity.ok(responses);
    }
}

