package com.project.medinova.repository;

import com.project.medinova.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByAppointmentId(Long appointmentId);
    
    List<Payment> findByPatientId(Long patientId);
    
    List<Payment> findByStatus(String status);
    
    List<Payment> findByPatientIdAndStatus(Long patientId, String status);
    
    // Query payments by patient and date range
    List<Payment> findByPatientIdAndCreatedAtBetween(Long patientId, LocalDateTime startDate, LocalDateTime endDate);
    
    // Query payments by patient, status and date range
    List<Payment> findByPatientIdAndStatusAndCreatedAtBetween(Long patientId, String status, LocalDateTime startDate, LocalDateTime endDate);
    
    // Query total amount paid by patient in date range
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.patient.id = :patientId AND p.status = 'PAID' AND p.createdAt BETWEEN :startDate AND :endDate")
    Double getTotalPaidAmountByPatientInDateRange(@Param("patientId") Long patientId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
