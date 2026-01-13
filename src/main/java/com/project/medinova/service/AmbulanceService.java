package com.project.medinova.service;

import com.project.medinova.dto.*;
import com.project.medinova.entity.Ambulance;
import com.project.medinova.entity.Clinic;
import com.project.medinova.exception.NotFoundException;
import com.project.medinova.repository.AmbulanceRepository;
import com.project.medinova.repository.ClinicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AmbulanceService {

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    @Autowired
    private ClinicRepository clinicRepository;

    public AmbulanceResponse createAmbulance(CreateAmbulanceRequest request) {
        // Kiểm tra clinic tồn tại
        Clinic clinic = clinicRepository.findById(request.getClinicId())
                .orElseThrow(() -> new NotFoundException("Clinic not found with id: " + request.getClinicId()));

        // Tạo ambulance
        Ambulance ambulance = new Ambulance();
        ambulance.setClinic(clinic);
        ambulance.setStatus(request.getStatus() != null ? request.getStatus() : "AVAILABLE");
        ambulance.setCurrentLat(request.getCurrentLat());
        ambulance.setCurrentLng(request.getCurrentLng());
        ambulance.setLicensePlate(request.getLicensePlate());
        ambulance.setAmbulanceType(request.getAmbulanceType());
        ambulance.setDriverName(request.getDriverName());
        ambulance.setDriverPhone(request.getDriverPhone());
        
        // Nếu status là AVAILABLE, set lastIdleAt
        if ("AVAILABLE".equals(ambulance.getStatus())) {
            ambulance.setLastIdleAt(LocalDateTime.now());
        }

        ambulance = ambulanceRepository.save(ambulance);
        return toAmbulanceResponse(ambulance);
    }

    public AmbulanceResponse getAmbulanceById(Long id) {
        Ambulance ambulance = ambulanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ambulance not found with id: " + id));
        return toAmbulanceResponse(ambulance);
    }

    public List<AmbulanceResponse> getAllAmbulances() {
        return ambulanceRepository.findAll().stream()
                .map(this::toAmbulanceResponse)
                .collect(Collectors.toList());
    }

    public List<AmbulanceResponse> getAmbulancesByClinicId(Long clinicId) {
        return ambulanceRepository.findByClinicId(clinicId).stream()
                .map(this::toAmbulanceResponse)
                .collect(Collectors.toList());
    }

    public List<AmbulanceResponse> getAmbulancesByStatus(String status) {
        return ambulanceRepository.findByStatus(status).stream()
                .map(this::toAmbulanceResponse)
                .collect(Collectors.toList());
    }

    public AmbulanceResponse updateAmbulance(Long id, UpdateAmbulanceRequest request) {
        Ambulance ambulance = ambulanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ambulance not found with id: " + id));

        // Cập nhật clinic nếu có
        if (request.getClinicId() != null) {
            Clinic clinic = clinicRepository.findById(request.getClinicId())
                    .orElseThrow(() -> new NotFoundException("Clinic not found with id: " + request.getClinicId()));
            ambulance.setClinic(clinic);
        }

        // Cập nhật các trường
        if (request.getStatus() != null) {
            String oldStatus = ambulance.getStatus();
            ambulance.setStatus(request.getStatus());
            
            // Nếu chuyển sang AVAILABLE, cập nhật lastIdleAt
            if ("AVAILABLE".equals(request.getStatus()) && !"AVAILABLE".equals(oldStatus)) {
                ambulance.setLastIdleAt(LocalDateTime.now());
            } else if (!"AVAILABLE".equals(request.getStatus())) {
                ambulance.setLastIdleAt(null);
            }
        }
        
        if (request.getCurrentLat() != null) {
            ambulance.setCurrentLat(request.getCurrentLat());
        }
        if (request.getCurrentLng() != null) {
            ambulance.setCurrentLng(request.getCurrentLng());
        }
        if (request.getLicensePlate() != null) {
            ambulance.setLicensePlate(request.getLicensePlate());
        }
        if (request.getAmbulanceType() != null) {
            ambulance.setAmbulanceType(request.getAmbulanceType());
        }
        if (request.getDriverName() != null) {
            ambulance.setDriverName(request.getDriverName());
        }
        if (request.getDriverPhone() != null) {
            ambulance.setDriverPhone(request.getDriverPhone());
        }

        ambulance = ambulanceRepository.save(ambulance);
        return toAmbulanceResponse(ambulance);
    }

    public AmbulanceResponse updateAmbulanceLocation(Long id, UpdateAmbulanceLocationRequest request) {
        Ambulance ambulance = ambulanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ambulance not found with id: " + id));

        ambulance.setCurrentLat(request.getCurrentLat());
        ambulance.setCurrentLng(request.getCurrentLng());

        ambulance = ambulanceRepository.save(ambulance);
        return toAmbulanceResponse(ambulance);
    }

    public AmbulanceResponse updateAmbulanceStatus(Long id, UpdateAmbulanceStatusRequest request) {
        Ambulance ambulance = ambulanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ambulance not found with id: " + id));

        String oldStatus = ambulance.getStatus();
        ambulance.setStatus(request.getStatus());
        
        // Nếu chuyển sang AVAILABLE, cập nhật lastIdleAt
        if ("AVAILABLE".equals(request.getStatus()) && !"AVAILABLE".equals(oldStatus)) {
            ambulance.setLastIdleAt(LocalDateTime.now());
        } else if (!"AVAILABLE".equals(request.getStatus())) {
            ambulance.setLastIdleAt(null);
        }

        ambulance = ambulanceRepository.save(ambulance);
        return toAmbulanceResponse(ambulance);
    }

    public void deleteAmbulance(Long id) {
        Ambulance ambulance = ambulanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ambulance not found with id: " + id));
        
        // TODO: Có thể cần kiểm tra xem ambulance có đang được sử dụng trong emergency không
        // Nếu có thì không cho xóa hoặc đánh dấu là không sử dụng được
        
        ambulanceRepository.delete(ambulance);
    }

    private AmbulanceResponse toAmbulanceResponse(Ambulance ambulance) {
        AmbulanceResponse response = new AmbulanceResponse();
        response.setId(ambulance.getId());
        response.setClinicId(ambulance.getClinic().getId());
        response.setClinicName(ambulance.getClinic().getName());
        response.setStatus(ambulance.getStatus());
        response.setCurrentLat(ambulance.getCurrentLat());
        response.setCurrentLng(ambulance.getCurrentLng());
        response.setLicensePlate(ambulance.getLicensePlate());
        response.setAmbulanceType(ambulance.getAmbulanceType());
        response.setDriverName(ambulance.getDriverName());
        response.setDriverPhone(ambulance.getDriverPhone());
        response.setLastIdleAt(ambulance.getLastIdleAt());
        response.setCreatedAt(ambulance.getCreatedAt());
        return response;
    }
}


