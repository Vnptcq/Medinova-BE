package com.project.medinova.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ambulances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ambulance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(nullable = false)
    private String status; // AVAILABLE | BUSY | MAINTENANCE | DISPATCHED

    @Column(name = "current_lat")
    private Double currentLat;

    @Column(name = "current_lng")
    private Double currentLng;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "ambulance_type")
    private String ambulanceType;

    @Column(name = "driver_name")
    private String driverName;

    @Column(name = "driver_phone")
    private String driverPhone; // STANDARD | ICU | ADVANCED

    @Column(name = "last_idle_at")
    private LocalDateTime lastIdleAt; // Thời gian cuối cùng ở trạng thái AVAILABLE

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "AVAILABLE";
        }
        if ("AVAILABLE".equals(status) && lastIdleAt == null) {
            lastIdleAt = LocalDateTime.now();
        }
    }
}


