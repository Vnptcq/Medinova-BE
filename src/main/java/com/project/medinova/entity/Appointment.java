package com.project.medinova.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "schedule_id", nullable = false, unique = true)
    private DoctorSchedule schedule;

    @Column(name = "appointment_time", nullable = false)
    private LocalDateTime appointmentTime;

    @Column(nullable = false)
    private String status; // PENDING | CONFIRMED | CHECKED_IN | IN_PROGRESS | REVIEW | COMPLETED | CANCELLED | NO_SHOW | REJECTED | EXPIRED | CANCELLED_BY_DOCTOR | CANCELLED_BY_PATIENT

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason; // Lý do từ chối (chỉ doctor/admin thấy)

    @Column(name = "age")
    private Integer age;

    @Column(name = "gender")
    private String gender; // MALE | FEMALE | OTHER

    @Column(name = "symptoms", columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // Doctor's consultation notes

    @Column(name = "deposit_amount")
    private Double depositAmount; // Deposit amount required

    @Column(name = "deposit_status")
    private String depositStatus; // PENDING | CONFIRMED | REJECTED

    @Column(name = "deposit_confirmed_at")
    private LocalDateTime depositConfirmedAt; // When admin confirmed the deposit

    @Column(name = "deposit_transfer_content", columnDefinition = "TEXT")
    private String depositTransferContent; // Expected transfer content for verification

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (depositStatus == null) {
            depositStatus = "PENDING";
        }
        if (depositAmount == null) {
            depositAmount = 0.0; // Default deposit amount, can be configured
        }
    }
}

