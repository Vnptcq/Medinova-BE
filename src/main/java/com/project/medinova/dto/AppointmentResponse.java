package com.project.medinova.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {
    private Long id;
    
    // Patient info (simplified)
    private Long patientId;
    private String patientName;
    private String patientEmail;
    
    // Doctor info (simplified)
    private Long doctorId;
    private String doctorName;
    private String doctorDepartment; // Department enum value (e.g., CARDIOLOGY)
    private String doctorDepartmentDisplayName; // Department display name (e.g., "Tim mạch")
    
    // Clinic info (simplified)
    private Long clinicId;
    private String clinicName;
    
    // Schedule info (simplified)
    private Long scheduleId;
    private LocalDate scheduleWorkDate;
    private LocalTime scheduleStartTime;
    private LocalTime scheduleEndTime;
    private String scheduleStatus;
    
    // Appointment details
    private LocalDateTime appointmentTime;
    private String status; // PENDING | CONFIRMED | CHECKED_IN | IN_PROGRESS | REVIEW | COMPLETED | CANCELLED | NO_SHOW | REJECTED | EXPIRED | CANCELLED_BY_DOCTOR | CANCELLED_BY_PATIENT
    private Integer age;
    private String gender; // MALE | FEMALE | OTHER
    private String symptoms;
    private String notes; // Doctor's consultation notes
    private String rejectionReason; // Internal reason for rejection/cancellation (only visible to doctor/admin)
    private LocalDateTime createdAt;
    
    // Payment info (optional - only if payment exists)
    private Long paymentId;
    private String paymentStatus; // PENDING | PAID | REFUNDED | FAILED | CANCELLED
}


