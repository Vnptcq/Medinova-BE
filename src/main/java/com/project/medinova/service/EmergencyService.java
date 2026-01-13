package com.project.medinova.service;

import com.project.medinova.dto.AssignEmergencyRequest;
import com.project.medinova.dto.AvailableStaffResponse;
import com.project.medinova.dto.CreateEmergencyRequest;
import com.project.medinova.dto.EmergencyResponse;
import com.project.medinova.dto.UpdateEmergencyStatusRequest;
import com.project.medinova.entity.*;
import com.project.medinova.exception.BadRequestException;
import com.project.medinova.exception.ForbiddenException;
import com.project.medinova.exception.NotFoundException;
import com.project.medinova.exception.UnauthorizedException;
import com.project.medinova.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmergencyService {

    @Autowired
    private EmergencyRepository emergencyRepository;

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    @Autowired
    private EmergencyAssignmentRepository assignmentRepository;

    @Autowired
    private ClinicRepository clinicRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AuthService authService;

    // Earth radius in kilometers
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculate distance between two points using Haversine formula
     * @param lat1 Latitude of point 1
     * @param lon1 Longitude of point 1
     * @param lat2 Latitude of point 2
     * @param lon2 Longitude of point 2
     * @return Distance in kilometers
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public EmergencyResponse createEmergency(CreateEmergencyRequest request) {
        // Tìm clinic: nếu có clinicId thì dùng, nếu không thì tìm clinic gần nhất
        Clinic clinic;
        if (request.getClinicId() != null && request.getClinicId() > 0) {
            clinic = clinicRepository.findById(request.getClinicId())
                    .orElseThrow(() -> new NotFoundException("Clinic not found with id: " + request.getClinicId()));
        } else {
            // Tự động tìm clinic gần nhất
            clinic = findNearestClinic(request.getPatientLat(), request.getPatientLng());
            if (clinic == null) {
                throw new NotFoundException("Không tìm thấy cơ sở y tế gần nhất. Vui lòng cung cấp clinicId.");
            }
        }

        // Lấy user hiện tại nếu có (để link với patient)
        User currentUser = null;
        try {
            currentUser = authService.getCurrentUser();
        } catch (Exception e) {
            // Nếu không có user authenticated, vẫn tạo emergency với thông tin từ request
        }

        // Tạo emergency request
        Emergency emergency = new Emergency();
        emergency.setClinic(clinic);
        emergency.setPatient(currentUser); // Link với user nếu có
        emergency.setPatientLat(request.getPatientLat());
        emergency.setPatientLng(request.getPatientLng());
        emergency.setPatientAddress(request.getPatientAddress());
        emergency.setPatientName(request.getPatientName());
        emergency.setPatientPhone(request.getPatientPhone());
        emergency.setDescription(request.getDescription());
        emergency.setPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
        emergency.setStatus("PENDING");

        emergency = emergencyRepository.save(emergency);

        // Tìm xe gần nhất
        Ambulance nearestAmbulance = findNearestAvailableAmbulance(
                clinic.getId(),
                request.getPatientLat(),
                request.getPatientLng(),
                request.getPriority()
        );

        if (nearestAmbulance == null) {
            // Không có xe rảnh - vẫn tìm bác sĩ và tạo emergency với status PENDING
            Doctor assignedDoctor = findAvailableDoctor(clinic.getId());
            
            // Nếu có bác sĩ, tạo assignment (không có ambulance)
            // Note: ambulance_id có thể null trong database
            if (assignedDoctor != null) {
                EmergencyAssignment assignment = new EmergencyAssignment();
                assignment.setEmergency(emergency);
                assignment.setAmbulance(null); // Không có ambulance - nullable = true
                assignment.setDoctor(assignedDoctor);
                assignment.setDistanceKm(null);
                assignment.setAssignedAt(LocalDateTime.now());
                assignmentRepository.save(assignment);
            }
            
            // Emergency vẫn được tạo với status PENDING
            // Client sẽ nhận được response với status PENDING
            return toEmergencyResponse(emergency, null, assignedDoctor, null);
        }

        // Cập nhật status xe thành DISPATCHED
        nearestAmbulance.setStatus("DISPATCHED");
        nearestAmbulance.setLastIdleAt(null);
        ambulanceRepository.save(nearestAmbulance);

        // Tính khoảng cách từ xe đến bệnh nhân (chỉ nếu xe có location)
        Double distance = null;
        if (nearestAmbulance.getCurrentLat() != null && nearestAmbulance.getCurrentLng() != null) {
            distance = calculateDistance(
                    nearestAmbulance.getCurrentLat(),
                    nearestAmbulance.getCurrentLng(),
                    request.getPatientLat(),
                    request.getPatientLng()
            );
        }

        // Tìm và gán bác sĩ đang rảnh của clinic này (tự động assign)
        Doctor assignedDoctor = findAvailableDoctor(clinic.getId());

        // Tạo assignment với ambulance và doctor
        EmergencyAssignment assignment = new EmergencyAssignment();
        assignment.setEmergency(emergency);
        assignment.setAmbulance(nearestAmbulance);
        assignment.setDoctor(assignedDoctor); // Có thể null nếu không có bác sĩ rảnh
        assignment.setDistanceKm(distance); // Có thể null nếu xe không có location
        assignment.setAssignedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        // Cập nhật emergency status thành DISPATCHED
        emergency.setStatus("DISPATCHED");
        emergency.setDispatchedAt(LocalDateTime.now());
        emergency = emergencyRepository.save(emergency);

        return toEmergencyResponse(emergency, nearestAmbulance, assignedDoctor, distance);
    }

    /**
     * Tìm clinic gần nhất với vị trí bệnh nhân
     * Ưu tiên: clinic có location > clinic không có location
     */
    private Clinic findNearestClinic(Double patientLat, Double patientLng) {
        // Bước 1: Tìm clinics có location và emergency enabled
        List<Clinic> clinicsWithLocation = clinicRepository.findAllWithLocationAndEmergencyEnabled();
        
        if (!clinicsWithLocation.isEmpty()) {
            // Tính khoảng cách và chọn clinic gần nhất
            return clinicsWithLocation.stream()
                    .map(clinic -> {
                        double distance = calculateDistance(
                                clinic.getLatitude(),
                                clinic.getLongitude(),
                                patientLat,
                                patientLng
                        );
                        return new ClinicWithDistance(clinic, distance);
                    })
                    .sorted((c1, c2) -> Double.compare(c1.distance, c2.distance))
                    .map(c -> c.clinic)
                    .findFirst()
                    .orElse(null);
        }
        
        // Bước 2: Nếu không có clinic có location, fallback về clinic đầu tiên có emergency enabled
        List<Clinic> emergencyEnabledClinics = clinicRepository.findAllEmergencyEnabled();
        if (!emergencyEnabledClinics.isEmpty()) {
            return emergencyEnabledClinics.get(0); // Trả về clinic đầu tiên
        }
        
        // Bước 3: Nếu vẫn không có, trả về null
        return null;
    }

    /**
     * Helper class để lưu clinic và khoảng cách
     */
    private static class ClinicWithDistance {
        Clinic clinic;
        double distance;

        ClinicWithDistance(Clinic clinic, double distance) {
            this.clinic = clinic;
            this.distance = distance;
        }
    }

    /**
     * Tìm xe cấp cứu rảnh gần nhất của clinic
     * Chỉ tìm trong các xe của clinic được chỉ định
     * Logic: Ưu tiên xe có location → fallback về xe không có location
     */
    private Ambulance findNearestAvailableAmbulance(Long clinicId, Double patientLat, Double patientLng, String priority) {
        // Bước 1: Tìm xe AVAILABLE của clinic có vị trí (lat/lng) - ưu tiên
        List<Ambulance> ambulancesWithLocation = ambulanceRepository
                .findByClinicIdAndStatusAndCurrentLatIsNotNullAndCurrentLngIsNotNull(clinicId, "AVAILABLE");

        if (!ambulancesWithLocation.isEmpty()) {
            // Tính khoảng cách và sắp xếp
            List<AmbulanceWithDistance> ambulancesWithDistance = ambulancesWithLocation.stream()
                    .map(ambulance -> {
                        double distance = calculateDistance(
                                ambulance.getCurrentLat(),
                                ambulance.getCurrentLng(),
                                patientLat,
                                patientLng
                        );
                        return new AmbulanceWithDistance(ambulance, distance);
                    })
                    .sorted((a1, a2) -> {
                        // Ưu tiên ICU cho ca CRITICAL/HIGH
                        if ("CRITICAL".equals(priority) || "HIGH".equals(priority)) {
                            boolean a1IsIcu = "ICU".equals(a1.ambulance.getAmbulanceType());
                            boolean a2IsIcu = "ICU".equals(a2.ambulance.getAmbulanceType());
                            if (a1IsIcu != a2IsIcu) {
                                return a1IsIcu ? -1 : 1; // ICU trước
                            }
                        }
                        
                        // Ưu tiên khoảng cách gần hơn
                        int distanceCompare = Double.compare(a1.distance, a2.distance);
                        if (distanceCompare != 0) {
                            return distanceCompare;
                        }
                        
                        // Nếu khoảng cách bằng nhau, ưu tiên xe idle lâu hơn
                        if (a1.ambulance.getLastIdleAt() != null && a2.ambulance.getLastIdleAt() != null) {
                            return a1.ambulance.getLastIdleAt().compareTo(a2.ambulance.getLastIdleAt());
                        }
                        
                        return 0;
                    })
                    .collect(Collectors.toList());

            return ambulancesWithDistance.get(0).ambulance;
        }

        // Bước 2: Fallback - Tìm xe AVAILABLE của clinic không có location
        List<Ambulance> ambulancesWithoutLocation = ambulanceRepository
                .findByClinicIdAndStatus(clinicId, "AVAILABLE")
                .stream()
                .filter(ambulance -> ambulance.getCurrentLat() == null || ambulance.getCurrentLng() == null)
                .collect(Collectors.toList());

        if (!ambulancesWithoutLocation.isEmpty()) {
            // Sắp xếp theo priority và lastIdleAt
            return ambulancesWithoutLocation.stream()
                    .sorted((a1, a2) -> {
                        // Ưu tiên ICU cho ca CRITICAL/HIGH
                        if ("CRITICAL".equals(priority) || "HIGH".equals(priority)) {
                            boolean a1IsIcu = "ICU".equals(a1.getAmbulanceType());
                            boolean a2IsIcu = "ICU".equals(a2.getAmbulanceType());
                            if (a1IsIcu != a2IsIcu) {
                                return a1IsIcu ? -1 : 1; // ICU trước
                            }
                        }
                        
                        // Ưu tiên xe idle lâu hơn
                        if (a1.getLastIdleAt() != null && a2.getLastIdleAt() != null) {
                            return a1.getLastIdleAt().compareTo(a2.getLastIdleAt());
                        }
                        if (a1.getLastIdleAt() != null) {
                            return -1; // a1 có lastIdleAt, ưu tiên
                        }
                        if (a2.getLastIdleAt() != null) {
                            return 1; // a2 có lastIdleAt, ưu tiên
                        }
                        
                        return 0;
                    })
                    .findFirst()
                    .orElse(null);
        }

        // Không có xe rảnh của clinic này
        return null;
    }

    /**
     * Tìm bác sĩ đang rảnh của clinic (ER Doctor)
     * Chỉ tìm trong các bác sĩ của clinic được chỉ định và đã được APPROVED
     */
    private Doctor findAvailableDoctor(Long clinicId) {
        // Lấy danh sách doctors của clinic
        List<Doctor> clinicDoctors = doctorRepository.findByClinicId(clinicId);
        
        // Lọc chỉ lấy doctors đã được APPROVED
        clinicDoctors = clinicDoctors.stream()
                .filter(doctor -> "APPROVED".equals(doctor.getStatus()))
                .collect(Collectors.toList());
        
        if (clinicDoctors.isEmpty()) {
            return null; // Không có doctor nào được APPROVED trong clinic này
        }

        LocalDate today = LocalDate.now();
        
        // Lọc doctors đang rảnh (không có appointment đang diễn ra và không có emergency active)
        List<Doctor> availableDoctors = clinicDoctors.stream()
                .filter(doctor -> {
                    // Kiểm tra có emergency assignment active không
                    List<EmergencyAssignment> activeAssignments = assignmentRepository
                            .findActiveAssignmentsByDoctorId(doctor.getId());
                    if (!activeAssignments.isEmpty()) {
                        return false;
                    }
                    
                    // Kiểm tra có appointment đang diễn ra trong khung giờ hiện tại không
                    LocalDateTime now = LocalDateTime.now();
                    List<com.project.medinova.entity.Appointment> doctorAppointments = 
                            appointmentRepository.findByDoctorId(doctor.getId());
                    
                    for (com.project.medinova.entity.Appointment appointment : doctorAppointments) {
                        // Bỏ qua CANCELLED
                        if ("CANCELLED".equals(appointment.getStatus())) {
                            continue;
                        }
                        
                        LocalDateTime appointmentStart = appointment.getAppointmentTime();
                        LocalDateTime appointmentEnd;
                        
                        // Lấy duration từ schedule (startTime và endTime)
                        if (appointment.getSchedule() != null) {
                            appointmentEnd = appointment.getSchedule().getEndTime()
                                    .atDate(appointmentStart.toLocalDate());
                        } else {
                            // Mặc định 60 phút nếu không có schedule
                            appointmentEnd = appointmentStart.plusMinutes(60);
                        }
                        
                        // Kiểm tra xem thời điểm hiện tại có nằm trong khung giờ appointment không
                        if (!now.isBefore(appointmentStart) && !now.isAfter(appointmentEnd)) {
                            return false; // Bác sĩ đang có appointment trong khung giờ hiện tại
                        }
                    }
                    
                    return true; // Bác sĩ rảnh
                })
                .sorted((d1, d2) -> {
                    // Ưu tiên doctor ít emergency assignments hôm nay
                    Long count1 = assignmentRepository.countByDoctorIdAndDate(d1.getId(), today);
                    Long count2 = assignmentRepository.countByDoctorIdAndDate(d2.getId(), today);
                    return count1.compareTo(count2);
                })
                .collect(Collectors.toList());

        return availableDoctors.isEmpty() ? null : availableDoctors.get(0);
    }

    /**
     * Helper class để lưu ambulance và khoảng cách
     */
    private static class AmbulanceWithDistance {
        Ambulance ambulance;
        double distance;

        AmbulanceWithDistance(Ambulance ambulance, double distance) {
            this.ambulance = ambulance;
            this.distance = distance;
        }
    }

    /**
     * Convert Emergency entity to EmergencyResponse DTO
     */
    private EmergencyResponse toEmergencyResponse(Emergency emergency, Ambulance ambulance, Doctor doctor, Double distance) {
        EmergencyResponse response = new EmergencyResponse();
        response.setId(emergency.getId());
        response.setClinicId(emergency.getClinic().getId());
        response.setClinicName(emergency.getClinic().getName());
        response.setPatientLat(emergency.getPatientLat());
        response.setPatientLng(emergency.getPatientLng());
        response.setPatientAddress(emergency.getPatientAddress());
        response.setPatientName(emergency.getPatientName());
        response.setPatientPhone(emergency.getPatientPhone());
        response.setDescription(emergency.getDescription());
        response.setStatus(emergency.getStatus());
        response.setPriority(emergency.getPriority());
        response.setCreatedAt(emergency.getCreatedAt());
        response.setDispatchedAt(emergency.getDispatchedAt());

        if (ambulance != null) {
            response.setAmbulanceId(ambulance.getId());
            response.setAmbulanceLicensePlate(ambulance.getLicensePlate());
            response.setDistanceKm(distance);
        }

        if (doctor != null && doctor.getUser() != null) {
            response.setDoctorId(doctor.getId());
            response.setDoctorName(doctor.getUser().getFullName());
        }

        return response;
    }

    public EmergencyResponse getEmergencyById(Long id) {
        Emergency emergency = emergencyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Emergency not found with id: " + id));

        // Kiểm tra quyền truy cập: PATIENT chỉ có thể xem emergency của chính họ
        User currentUser = authService.getCurrentUser();
        if (currentUser != null && "PATIENT".equals(currentUser.getRole())) {
            // Nếu emergency không có patient hoặc patient không phải là user hiện tại
            if (emergency.getPatient() == null || !emergency.getPatient().getId().equals(currentUser.getId())) {
                throw new ForbiddenException("Access denied. You can only view your own emergencies.");
            }
        }

        EmergencyAssignment assignment = assignmentRepository.findByEmergencyId(id)
                .stream()
                .findFirst()
                .orElse(null);

        Ambulance ambulance = assignment != null ? assignment.getAmbulance() : null;
        Doctor doctor = assignment != null ? assignment.getDoctor() : null;
        Double distance = assignment != null ? assignment.getDistanceKm() : null;

        return toEmergencyResponse(emergency, ambulance, doctor, distance);
    }

    /**
     * Lấy tất cả các ca cấp cứu của một bác sĩ cụ thể
     * Có thể filter theo status
     */
    public List<EmergencyResponse> getEmergenciesByDoctorId(Long doctorId, String status) {
        // Kiểm tra doctor tồn tại
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new NotFoundException("Doctor not found with id: " + doctorId));

        // Lấy tất cả assignments của doctor
        List<EmergencyAssignment> assignments = assignmentRepository.findByDoctorId(doctorId);

        // Filter theo status nếu có
        if (status != null && !status.trim().isEmpty()) {
            assignments = assignments.stream()
                    .filter(assignment -> status.equals(assignment.getEmergency().getStatus()))
                    .collect(Collectors.toList());
        }

        // Convert sang EmergencyResponse và sắp xếp theo thời gian (mới nhất trước)
        return assignments.stream()
                .map(assignment -> {
                    Emergency emergency = assignment.getEmergency();
                    Ambulance ambulance = assignment.getAmbulance();
                    Double distance = assignment.getDistanceKm();
                    return toEmergencyResponse(emergency, ambulance, doctor, distance);
                })
                .sorted((e1, e2) -> {
                    // Sắp xếp theo createdAt (mới nhất trước)
                    if (e1.getCreatedAt() != null && e2.getCreatedAt() != null) {
                        return e2.getCreatedAt().compareTo(e1.getCreatedAt());
                    }
                    return 0;
                })
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả các ca cấp cứu của bác sĩ hiện tại (đang đăng nhập)
     * Có thể filter theo status
     */
    public List<EmergencyResponse> getMyEmergencies(String status) {
        // Lấy user hiện tại
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        // Kiểm tra user có role DOCTOR
        if (!"DOCTOR".equals(currentUser.getRole())) {
            throw new ForbiddenException("Only doctors can access their emergencies");
        }

        // Tìm doctor profile
        Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Doctor profile not found"));

        // Lấy emergencies của doctor này
        return getEmergenciesByDoctorId(doctor.getId(), status);
    }

    /**
     * Get all emergencies created by the current patient
     */
    public List<EmergencyResponse> getMyPatientEmergencies(String status) {
        // Lấy user hiện tại
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        // Kiểm tra user có role PATIENT
        if (!"PATIENT".equals(currentUser.getRole())) {
            throw new ForbiddenException("Only patients can access their emergencies");
        }

        // Lấy emergencies của patient này
        List<Emergency> emergencies;
        if (status != null && !status.trim().isEmpty()) {
            emergencies = emergencyRepository.findByPatientIdAndStatus(currentUser.getId(), status);
        } else {
            emergencies = emergencyRepository.findByPatientId(currentUser.getId());
        }

        // Convert to DTO và sắp xếp theo thời gian (mới nhất trước)
        return emergencies.stream()
                .map(emergency -> {
                    // Lấy assignment nếu có
                    List<EmergencyAssignment> assignments = assignmentRepository.findByEmergencyId(emergency.getId());
                    EmergencyAssignment assignment = assignments.isEmpty() ? null : assignments.get(0);
                    
                    Doctor doctor = assignment != null ? assignment.getDoctor() : null;
                    Ambulance ambulance = assignment != null ? assignment.getAmbulance() : null;
                    Double distance = assignment != null ? assignment.getDistanceKm() : null;
                    
                    return toEmergencyResponse(emergency, ambulance, doctor, distance);
                })
                .sorted((a, b) -> {
                    // Sắp xếp theo createdAt (mới nhất trước)
                    if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    }
                    return 0;
                })
                .collect(Collectors.toList());
    }

    /**
     * Assign thủ công bác sĩ và xe cho emergency
     * Dùng khi emergency chưa được assign tự động hoặc cần thay đổi assignment
     */
    public EmergencyResponse assignEmergency(Long emergencyId, AssignEmergencyRequest request) {
        // Kiểm tra emergency tồn tại
        Emergency emergency = emergencyRepository.findById(emergencyId)
                .orElseThrow(() -> new NotFoundException("Emergency not found with id: " + emergencyId));

        // Kiểm tra emergency có thể assign (status PENDING hoặc DISPATCHED)
        if (!"PENDING".equals(emergency.getStatus()) && !"DISPATCHED".equals(emergency.getStatus())) {
            throw new BadRequestException("Cannot assign doctor/ambulance to emergency with status: " + emergency.getStatus());
        }

        // Kiểm tra doctor tồn tại và thuộc clinic của emergency
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new NotFoundException("Doctor not found with id: " + request.getDoctorId()));

        // Kiểm tra doctor thuộc clinic của emergency
        if (!doctor.getClinic().getId().equals(emergency.getClinic().getId())) {
            throw new BadRequestException("Doctor does not belong to the emergency's clinic");
        }

        // Kiểm tra doctor có status APPROVED
        if (!"APPROVED".equals(doctor.getStatus())) {
            throw new BadRequestException("Doctor is not approved. Status: " + doctor.getStatus());
        }

        // Allow assignment at any time - removed checks for active assignments and ongoing appointments

        // Xử lý ambulance nếu có
        Ambulance ambulance = null;
        Double distance = null;
        
        if (request.getAmbulanceId() != null) {
            ambulance = ambulanceRepository.findById(request.getAmbulanceId())
                    .orElseThrow(() -> new NotFoundException("Ambulance not found with id: " + request.getAmbulanceId()));

            // Kiểm tra ambulance thuộc clinic của emergency
            if (!ambulance.getClinic().getId().equals(emergency.getClinic().getId())) {
                throw new BadRequestException("Ambulance does not belong to the emergency's clinic");
            }

            // Kiểm tra ambulance có AVAILABLE không
            if (!"AVAILABLE".equals(ambulance.getStatus())) {
                throw new BadRequestException("Ambulance is not available. Status: " + ambulance.getStatus());
            }

            // Cập nhật status ambulance thành DISPATCHED
            ambulance.setStatus("DISPATCHED");
            ambulance.setLastIdleAt(null);
            ambulanceRepository.save(ambulance);

            // Tính khoảng cách nếu ambulance có location
            if (ambulance.getCurrentLat() != null && ambulance.getCurrentLng() != null) {
                distance = calculateDistance(
                        ambulance.getCurrentLat(),
                        ambulance.getCurrentLng(),
                        emergency.getPatientLat(),
                        emergency.getPatientLng()
                );
            }
        }

        // Tìm assignment hiện tại của emergency (nếu có)
        EmergencyAssignment existingAssignment = assignmentRepository.findByEmergencyId(emergencyId)
                .stream()
                .findFirst()
                .orElse(null);

        EmergencyAssignment assignment;
        if (existingAssignment != null) {
            // Cập nhật assignment hiện tại
            assignment = existingAssignment;
            
            // Nếu có ambulance cũ, cập nhật status về AVAILABLE
            if (assignment.getAmbulance() != null && !assignment.getAmbulance().getId().equals(request.getAmbulanceId())) {
                Ambulance oldAmbulance = assignment.getAmbulance();
                oldAmbulance.setStatus("AVAILABLE");
                oldAmbulance.setLastIdleAt(LocalDateTime.now());
                ambulanceRepository.save(oldAmbulance);
            }
        } else {
            // Tạo assignment mới
            assignment = new EmergencyAssignment();
            assignment.setEmergency(emergency);
        }

        // Cập nhật assignment
        assignment.setDoctor(doctor);
        assignment.setAmbulance(ambulance);
        assignment.setDistanceKm(distance);
        assignment.setAssignedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        // Cập nhật emergency status thành DISPATCHED
        emergency.setStatus("DISPATCHED");
        emergency.setDispatchedAt(LocalDateTime.now());
        emergency = emergencyRepository.save(emergency);

        return toEmergencyResponse(emergency, ambulance, doctor, distance);
    }

    /**
     * Cập nhật status của emergency
     * Khi bác sĩ confirm (status = DISPATCHED), sẽ tự động set dispatchedAt
     */
    public EmergencyResponse updateEmergencyStatus(Long emergencyId, UpdateEmergencyStatusRequest request) {
        // Kiểm tra emergency tồn tại
        Emergency emergency = emergencyRepository.findById(emergencyId)
                .orElseThrow(() -> new NotFoundException("Emergency not found with id: " + emergencyId));

        String newStatus = request.getStatus();
        String oldStatus = emergency.getStatus();

        // Nếu bác sĩ confirm (chuyển sang DISPATCHED), kiểm tra assignment
        if ("DISPATCHED".equals(newStatus)) {
            // Kiểm tra emergency đã có assignment chưa
            List<EmergencyAssignment> assignments = assignmentRepository.findByEmergencyId(emergencyId);
            if (assignments.isEmpty() || assignments.get(0).getDoctor() == null) {
                throw new BadRequestException("Cannot set status to DISPATCHED. Emergency must have an assigned doctor first.");
            }

            // Nếu đang từ PENDING chuyển sang DISPATCHED, set dispatchedAt
            if ("PENDING".equals(oldStatus)) {
                emergency.setDispatchedAt(LocalDateTime.now());
            }
        }

        // Cập nhật status
        emergency.setStatus(newStatus);

        // Set các timestamp tương ứng
        LocalDateTime now = LocalDateTime.now();
        if ("ARRIVED".equals(newStatus) && emergency.getArrivedAt() == null) {
            emergency.setArrivedAt(now);
        } else if ("COMPLETED".equals(newStatus) && emergency.getCompletedAt() == null) {
            emergency.setCompletedAt(now);
            
            // Nếu có ambulance, cập nhật status về AVAILABLE
            List<EmergencyAssignment> assignments = assignmentRepository.findByEmergencyId(emergencyId);
            if (!assignments.isEmpty()) {
                EmergencyAssignment assignment = assignments.get(0);
                if (assignment.getAmbulance() != null) {
                    Ambulance ambulance = assignment.getAmbulance();
                    ambulance.setStatus("AVAILABLE");
                    ambulance.setLastIdleAt(now);
                    ambulanceRepository.save(ambulance);
                }
            }
        } else if ("CANCELLED".equals(newStatus)) {
            // Nếu cancel, cập nhật ambulance về AVAILABLE nếu có
            List<EmergencyAssignment> assignments = assignmentRepository.findByEmergencyId(emergencyId);
            if (!assignments.isEmpty()) {
                EmergencyAssignment assignment = assignments.get(0);
                if (assignment.getAmbulance() != null) {
                    Ambulance ambulance = assignment.getAmbulance();
                    ambulance.setStatus("AVAILABLE");
                    ambulance.setLastIdleAt(now);
                    ambulanceRepository.save(ambulance);
                }
            }
        }

        emergency = emergencyRepository.save(emergency);

        // Lấy assignment để trả về response
        EmergencyAssignment assignment = assignmentRepository.findByEmergencyId(emergencyId)
                .stream()
                .findFirst()
                .orElse(null);

        Ambulance ambulance = assignment != null ? assignment.getAmbulance() : null;
        Doctor doctor = assignment != null ? assignment.getDoctor() : null;
        Double distance = assignment != null ? assignment.getDistanceKm() : null;

        return toEmergencyResponse(emergency, ambulance, doctor, distance);
    }

    /**
     * Get all emergencies with optional status filter (ADMIN only)
     */
    public List<EmergencyResponse> getAllEmergencies(String status) {
        List<Emergency> emergencies;
        
        if (status != null && !status.trim().isEmpty()) {
            emergencies = emergencyRepository.findByStatus(status);
        } else {
            emergencies = emergencyRepository.findAll();
        }
        
        // Convert to DTO và sắp xếp theo thời gian (mới nhất trước)
        return emergencies.stream()
                .map(emergency -> {
                    List<EmergencyAssignment> assignments = assignmentRepository.findByEmergencyId(emergency.getId());
                    EmergencyAssignment assignment = assignments.isEmpty() ? null : assignments.get(0);
                    
                    Doctor doctor = assignment != null ? assignment.getDoctor() : null;
                    Ambulance ambulance = assignment != null ? assignment.getAmbulance() : null;
                    Double distance = assignment != null ? assignment.getDistanceKm() : null;
                    
                    return toEmergencyResponse(emergency, ambulance, doctor, distance);
                })
                .sorted((a, b) -> {
                    // Sắp xếp theo createdAt (mới nhất trước)
                    if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    }
                    return 0;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get available staff (doctors) for emergency assignment with pagination
     */
    public Page<AvailableStaffResponse> getAvailableStaff(Pageable pageable) {
        LocalDate today = LocalDate.now();

        // Get all approved doctors - allow assignment at any time
        List<Doctor> allDoctors = doctorRepository.findByStatus("APPROVED");

        // Convert all approved doctors to AvailableStaffResponse (no filtering)
        List<AvailableStaffResponse> availableStaff = allDoctors.stream()
                .sorted((d1, d2) -> {
                    // Sort by number of emergency assignments today (less is better)
                    Long count1 = assignmentRepository.countByDoctorIdAndDate(d1.getId(), today);
                    Long count2 = assignmentRepository.countByDoctorIdAndDate(d2.getId(), today);
                    return count1.compareTo(count2);
                })
                .map(this::toAvailableStaffResponse)
                .collect(Collectors.toList());

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), availableStaff.size());
        List<AvailableStaffResponse> pageContent = availableStaff.subList(start, end);

        return new PageImpl<>(pageContent, pageable, availableStaff.size());
    }

    /**
     * Convert Doctor entity to AvailableStaffResponse DTO
     */
    private AvailableStaffResponse toAvailableStaffResponse(Doctor doctor) {
        AvailableStaffResponse response = new AvailableStaffResponse();
        response.setId(doctor.getId());
        response.setStaffType("DOCTOR");
        
        if (doctor.getUser() != null) {
            response.setName(doctor.getUser().getFullName());
            response.setEmail(doctor.getUser().getEmail());
            response.setPhone(doctor.getUser().getPhone());
        }
        
        response.setStatus(doctor.getStatus());
        
        if (doctor.getClinic() != null) {
            response.setClinicId(doctor.getClinic().getId());
            response.setClinicName(doctor.getClinic().getName());
        }
        
        if (doctor.getDepartment() != null) {
            response.setDepartment(doctor.getDepartment().name());
        }
        
        response.setExperienceYears(doctor.getExperienceYears());
        
        return response;
    }

}

