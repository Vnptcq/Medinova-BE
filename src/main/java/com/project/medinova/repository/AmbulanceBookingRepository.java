package com.project.medinova.repository;

import com.project.medinova.entity.AmbulanceBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmbulanceBookingRepository extends JpaRepository<AmbulanceBooking, Long> {
    List<AmbulanceBooking> findByPatientId(Long patientId);
    List<AmbulanceBooking> findByAmbulanceId(Long ambulanceId);
    List<AmbulanceBooking> findByClinicId(Long clinicId);
    List<AmbulanceBooking> findByStatus(String status);
    List<AmbulanceBooking> findByPatientIdAndStatus(Long patientId, String status);
    List<AmbulanceBooking> findByAmbulanceIdAndStatus(Long ambulanceId, String status);
    Optional<AmbulanceBooking> findByEmergencyId(Long emergencyId); // Find booking by emergency ID
}




