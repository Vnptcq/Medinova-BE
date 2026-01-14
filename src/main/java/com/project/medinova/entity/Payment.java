package com.project.medinova.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", unique = true)
    private Appointment appointment; // Payment cho appointment

    @Column(name = "amount", nullable = false)
    private Double amount; // Số tiền cọc yêu cầu (50,000 VND)
    
    @Column(name = "actual_amount")
    private Double actualAmount; // Số tiền thực tế nhận được (có thể >= 100,000 VND)

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; // BANK_TRANSFER | CREDIT_CARD | E_WALLET | CASH

    @Column(nullable = false)
    private String status; // PENDING | PAID | FAILED | REFUNDED | CANCELLED

    @Column(name = "transaction_id")
    private String transactionId; // ID từ payment gateway (nếu có)

    @Column(name = "payment_gateway")
    private String paymentGateway; // VNPAY | MOMO | ZALOPAY | etc.

    @Column(name = "paid_at")
    private LocalDateTime paidAt; // Thời gian thanh toán thành công

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt; // Thời gian hoàn tiền

    @Column(name = "refund_reason", columnDefinition = "TEXT")
    private String refundReason; // Lý do hoàn tiền

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // Ghi chú

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
