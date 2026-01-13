package com.project.medinova.service;

import com.project.medinova.dto.AppointmentResponse;
import com.project.medinova.dto.BusyScheduleResponse;
import com.project.medinova.dto.ConfirmAppointmentRequest;
import com.project.medinova.dto.CreateAppointmentRequest;
import com.project.medinova.dto.HoldSlotRequest;
import com.project.medinova.dto.UpdateAppointmentStatusRequest;
import com.project.medinova.dto.UpdateAppointmentStatusByDoctorRequest;
import com.project.medinova.dto.UpdateAppointmentNotesRequest;
import com.project.medinova.dto.RejectAppointmentRequest;
import com.project.medinova.entity.Appointment;
import com.project.medinova.entity.Doctor;
import com.project.medinova.entity.DoctorLeaveRequest;
import com.project.medinova.entity.DoctorSchedule;
import com.project.medinova.entity.User;
import com.project.medinova.exception.BadRequestException;
import com.project.medinova.exception.ForbiddenException;
import com.project.medinova.exception.NotFoundException;
import com.project.medinova.repository.AppointmentRepository;
import com.project.medinova.repository.ClinicRepository;
import com.project.medinova.repository.DoctorLeaveRequestRepository;
import com.project.medinova.repository.DoctorRepository;
import com.project.medinova.repository.DoctorScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorScheduleRepository scheduleRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ClinicRepository clinicRepository;

    @Autowired
    private DoctorLeaveRequestRepository leaveRequestRepository;

    @Autowired
    private AuthService authService;

    /**
     * Helper method to convert Appointment entity to AppointmentResponse DTO
     * to avoid recursive serialization issues
     */
    private AppointmentResponse toAppointmentResponse(Appointment appointment) {
        AppointmentResponse response = new AppointmentResponse();
        response.setId(appointment.getId());
        
        // Patient info
        if (appointment.getPatient() != null) {
            response.setPatientId(appointment.getPatient().getId());
            response.setPatientName(appointment.getPatient().getFullName());
            response.setPatientEmail(appointment.getPatient().getEmail());
        }
        
        // Doctor info
        if (appointment.getDoctor() != null) {
            response.setDoctorId(appointment.getDoctor().getId());
            if (appointment.getDoctor().getUser() != null) {
                response.setDoctorName(appointment.getDoctor().getUser().getFullName());
            }
            if (appointment.getDoctor().getDepartment() != null) {
                response.setDoctorDepartment(appointment.getDoctor().getDepartment().name());
                response.setDoctorDepartmentDisplayName(appointment.getDoctor().getDepartment().getDisplayName());
            }
        }
        
        // Clinic info
        if (appointment.getClinic() != null) {
            response.setClinicId(appointment.getClinic().getId());
            response.setClinicName(appointment.getClinic().getName());
        }
        
        // Schedule info
        if (appointment.getSchedule() != null) {
            DoctorSchedule schedule = appointment.getSchedule();
            response.setScheduleId(schedule.getId());
            response.setScheduleWorkDate(schedule.getWorkDate());
            response.setScheduleStartTime(schedule.getStartTime());
            response.setScheduleEndTime(schedule.getEndTime());
            response.setScheduleStatus(schedule.getStatus());
        }
        
        // Appointment details
        response.setAppointmentTime(appointment.getAppointmentTime());
        response.setStatus(appointment.getStatus());
        response.setAge(appointment.getAge());
        response.setGender(appointment.getGender());
        response.setSymptoms(appointment.getSymptoms());
        response.setNotes(appointment.getNotes());
        response.setRejectionReason(appointment.getRejectionReason());
        response.setCreatedAt(appointment.getCreatedAt());
        
        // Deposit information
        response.setDepositAmount(appointment.getDepositAmount());
        response.setDepositStatus(appointment.getDepositStatus());
        response.setDepositConfirmedAt(appointment.getDepositConfirmedAt());
        response.setDepositTransferContent(appointment.getDepositTransferContent());
        
        return response;
    }

    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        // Lấy user hiện tại từ JWT
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Kiểm tra user có role PATIENT
        if (!"PATIENT".equals(currentUser.getRole())) {
            throw new ForbiddenException("Only patients can create appointments");
        }

        // Tìm doctor và clinic
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new NotFoundException("Doctor not found with id: " + request.getDoctorId()));

        com.project.medinova.entity.Clinic clinic = clinicRepository.findById(request.getClinicId())
                .orElseThrow(() -> new NotFoundException("Clinic not found with id: " + request.getClinicId()));

        // Kiểm tra doctor có thuộc clinic này không
        if (!doctor.getClinic().getId().equals(clinic.getId())) {
            throw new BadRequestException("Doctor does not work at this clinic");
        }

        LocalDateTime appointmentTime = request.getAppointmentTime();
        LocalDate appointmentDate = appointmentTime.toLocalDate();
        LocalTime appointmentTimeOnly = appointmentTime.toLocalTime();

        // Kiểm tra doctor có đang nghỉ (leave request approved) trong thời gian này không
        List<DoctorLeaveRequest> approvedLeaves = leaveRequestRepository
                .findByDoctorIdAndStatus(doctor.getId(), "APPROVED");
        
        for (DoctorLeaveRequest leave : approvedLeaves) {
            if (!appointmentDate.isBefore(leave.getStartDate()) && 
                !appointmentDate.isAfter(leave.getEndDate())) {
                throw new BadRequestException("Doctor is on approved leave during this time");
            }
        }

        // Kiểm tra xem có appointment nào trùng thời gian không (trừ CANCELLED và HOLD đã hết hạn)
        int duration = request.getDurationMinutes() != null ? request.getDurationMinutes() : 60;
        LocalDateTime newAppointmentStart = appointmentTime;
        LocalDateTime newAppointmentEnd = appointmentTime.plusMinutes(duration);
        
        // Lấy tất cả appointments của doctor (trừ CANCELLED)
        List<Appointment> allAppointments = appointmentRepository.findByDoctorId(doctor.getId());
        LocalDateTime now = LocalDateTime.now();
        
        // Kiểm tra overlap với các appointments khác
        for (Appointment existingApt : allAppointments) {
            // Bỏ qua CANCELLED
            if ("CANCELLED".equals(existingApt.getStatus())) {
                continue;
            }
            
            // Nếu là HOLD, kiểm tra xem đã hết hạn chưa
            if ("PENDING".equals(existingApt.getStatus()) && existingApt.getSchedule() != null) {
                DoctorSchedule aptSchedule = existingApt.getSchedule();
                if ("HOLD".equals(aptSchedule.getStatus()) && aptSchedule.getHoldExpiresAt() != null) {
                    if (aptSchedule.getHoldExpiresAt().isBefore(now)) {
                        continue; // HOLD đã hết hạn, bỏ qua
                    }
                }
            }
            
            // Lấy thời gian của appointment hiện tại
            LocalDateTime existingStart = existingApt.getAppointmentTime();
            LocalDateTime existingEnd;
            
            if (existingApt.getSchedule() != null && existingApt.getSchedule().getStartTime() != null && 
                existingApt.getSchedule().getEndTime() != null) {
                // Sử dụng thời gian từ schedule
                existingEnd = LocalDateTime.of(
                    existingApt.getSchedule().getWorkDate(), 
                    existingApt.getSchedule().getEndTime()
                );
            } else {
                // Fallback: mặc định 60 phút
                existingEnd = existingStart.plusMinutes(60);
            }
            
            // Kiểm tra overlap: hai khoảng thời gian overlap nếu:
            // - newStart < existingEnd VÀ newEnd > existingStart
            boolean hasOverlap = newAppointmentStart.isBefore(existingEnd) && 
                                newAppointmentEnd.isAfter(existingStart);
            
            if (hasOverlap) {
                throw new BadRequestException("There is already an appointment at this time. The slot overlaps with an existing appointment.");
            }
        }

        // Tạo schedule mới với status HOLD (5 phút)
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctor(doctor);
        schedule.setClinic(clinic);
        schedule.setWorkDate(appointmentDate);
        schedule.setStartTime(appointmentTimeOnly);
        schedule.setEndTime(appointmentTimeOnly.plusMinutes(duration));
        schedule.setStatus("HOLD");
        schedule.setHoldExpiresAt(now.plusMinutes(5)); // HOLD trong 5 phút

        // Tạo appointment với status PENDING
        Appointment appointment = new Appointment();
        appointment.setPatient(currentUser);
        appointment.setDoctor(doctor);
        appointment.setClinic(clinic);
        appointment.setSchedule(schedule);
        appointment.setAppointmentTime(appointmentTime);
        appointment.setStatus("PENDING");
        appointment.setAge(request.getAge());
        appointment.setGender(request.getGender());
        appointment.setSymptoms(request.getSymptoms());
        
        // Set deposit amount (default 100000 VND)
        appointment.setDepositAmount(100000.0);
        appointment.setDepositStatus("PENDING");

        // Lưu schedule trước (vì appointment có foreign key đến schedule)
        schedule = scheduleRepository.save(schedule);
        appointment.setSchedule(schedule);
        
        Appointment savedAppointment = appointmentRepository.save(appointment);
        return toAppointmentResponse(savedAppointment);
    }

    public AppointmentResponse holdSlot(HoldSlotRequest request) {
        // Tương tự createAppointment nhưng chỉ tạo HOLD slot, chưa tạo appointment
        // Method này có thể được dùng nếu muốn tách riêng hold và confirm
        // Hiện tại đã tích hợp vào createAppointment
        return createAppointment(new CreateAppointmentRequest() {{
            setDoctorId(request.getDoctorId());
            setClinicId(request.getClinicId());
            setAppointmentTime(request.getAppointmentTime());
            setDurationMinutes(request.getDurationMinutes());
        }});
    }

    public AppointmentResponse confirmAppointment(Long appointmentId, ConfirmAppointmentRequest request) {
        // Lấy user hiện tại từ JWT
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Tìm appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + appointmentId));

        // Kiểm tra appointment thuộc về patient hiện tại
        if (!appointment.getPatient().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only confirm your own appointments");
        }

        // Kiểm tra appointment phải là PENDING và schedule phải là HOLD
        if (!"PENDING".equals(appointment.getStatus())) {
            throw new BadRequestException("Only pending appointments can be confirmed");
        }

        DoctorSchedule schedule = appointment.getSchedule();
        if (schedule == null || !"HOLD".equals(schedule.getStatus())) {
            throw new BadRequestException("Appointment slot is not in HOLD status");
        }

        // Kiểm tra HOLD chưa hết hạn
        if (schedule.getHoldExpiresAt() != null && schedule.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Hold period has expired. Please create a new appointment.");
        }

        // Cập nhật thông tin bệnh nhân nếu có trong request
        if (request != null) {
            if (request.getAge() != null) {
                appointment.setAge(request.getAge());
            }
            if (request.getGender() != null && !request.getGender().trim().isEmpty()) {
                appointment.setGender(request.getGender());
            }
            if (request.getSymptoms() != null && !request.getSymptoms().trim().isEmpty()) {
                appointment.setSymptoms(request.getSymptoms());
            }
        }

        // Set deposit amount (default 100000 VND, can be configured)
        if (appointment.getDepositAmount() == null || appointment.getDepositAmount() == 0.0) {
            appointment.setDepositAmount(100000.0);
        }
        
        // Set deposit status to PENDING
        appointment.setDepositStatus("PENDING");
        
        // Generate deposit transfer content: Tên bệnh nhân,số điện thoại,tên bác sĩ,ngày tháng năm khám,chuyên khoa
        String patientName = appointment.getPatient().getFullName();
        String patientPhone = appointment.getPatient().getPhone() != null ? appointment.getPatient().getPhone() : "";
        String doctorName = appointment.getDoctor().getUser() != null ? appointment.getDoctor().getUser().getFullName() : "";
        String appointmentDate = appointment.getAppointmentTime().toLocalDate().toString();
        String department = appointment.getDoctor().getDepartment() != null ? appointment.getDoctor().getDepartment().getDisplayName() : "";
        
        String transferContent = String.format("%s,%s,%s,%s,%s", 
            patientName, patientPhone, doctorName, appointmentDate, department);
        appointment.setDepositTransferContent(transferContent);
        
        // Confirm: chuyển schedule từ HOLD sang BOOKED
        schedule.setStatus("BOOKED");
        schedule.setHoldExpiresAt(null);
        scheduleRepository.save(schedule);

        // Appointment vẫn giữ status PENDING (chờ deposit confirmation và doctor confirm)
        Appointment savedAppointment = appointmentRepository.save(appointment);
        return toAppointmentResponse(savedAppointment);
    }

    public List<BusyScheduleResponse> getBusySchedules(Long doctorId) {
        // Kiểm tra doctor tồn tại
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new NotFoundException("Doctor not found with id: " + doctorId));

        List<BusyScheduleResponse> busySchedules = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Lấy tất cả appointments của doctor (trừ CANCELLED)
        List<Appointment> appointments = appointmentRepository.findByDoctorId(doctorId);
        appointments = appointments.stream()
                .filter(apt -> !"CANCELLED".equals(apt.getStatus()))
                .collect(Collectors.toList());

        for (Appointment appointment : appointments) {
            BusyScheduleResponse response = new BusyScheduleResponse();
            DoctorSchedule schedule = appointment.getSchedule();
            
            // Xác định type dựa trên schedule status
            if (schedule != null && "HOLD".equals(schedule.getStatus())) {
                // Kiểm tra HOLD chưa hết hạn
                if (schedule.getHoldExpiresAt() != null && schedule.getHoldExpiresAt().isAfter(now)) {
                    response.setType("HOLD");
                    response.setReason("Slot is being held (expires in " + 
                        java.time.Duration.between(now, schedule.getHoldExpiresAt()).toMinutes() + " minutes)");
                } else {
                    // HOLD đã hết hạn, bỏ qua
                    continue;
                }
            } else {
                response.setType("APPOINTMENT");
                response.setReason("Appointment with patient");
            }
            
            response.setStartDateTime(appointment.getAppointmentTime());
            // Sử dụng duration từ schedule (mặc định 60 phút)
            if (schedule != null && schedule.getStartTime() != null && schedule.getEndTime() != null) {
                LocalDateTime endTime = LocalDateTime.of(schedule.getWorkDate(), schedule.getEndTime());
                response.setEndDateTime(endTime);
            } else {
                // Fallback: mặc định 60 phút
                response.setEndDateTime(appointment.getAppointmentTime().plusMinutes(60));
            }
            response.setAppointmentId(appointment.getId());
            busySchedules.add(response);
        }

        // Lấy tất cả HOLD schedules chưa hết hạn (có thể không có appointment nếu đang trong quá trình hold)
        List<DoctorSchedule> holdSchedules = scheduleRepository.findByDoctorIdAndStatus(doctorId, "HOLD");
        for (DoctorSchedule schedule : holdSchedules) {
            // Chỉ lấy HOLD chưa hết hạn
            if (schedule.getHoldExpiresAt() != null && schedule.getHoldExpiresAt().isAfter(now)) {
                // Kiểm tra xem đã có trong appointments chưa (để tránh duplicate)
                boolean alreadyAdded = appointments.stream()
                        .anyMatch(apt -> apt.getSchedule() != null && apt.getSchedule().getId().equals(schedule.getId()));
                
                if (!alreadyAdded) {
                    BusyScheduleResponse response = new BusyScheduleResponse();
                    response.setType("HOLD");
                    LocalDateTime startDateTime = LocalDateTime.of(schedule.getWorkDate(), schedule.getStartTime());
                    LocalDateTime endDateTime = LocalDateTime.of(schedule.getWorkDate(), schedule.getEndTime());
                    response.setStartDateTime(startDateTime);
                    response.setEndDateTime(endDateTime);
                    response.setReason("Slot is being held (expires in " + 
                        java.time.Duration.between(now, schedule.getHoldExpiresAt()).toMinutes() + " minutes)");
                    busySchedules.add(response);
                }
            }
        }

        // Lấy tất cả leave requests đã được approve
        List<DoctorLeaveRequest> approvedLeaves = leaveRequestRepository
                .findByDoctorIdAndStatus(doctorId, "APPROVED");

        for (DoctorLeaveRequest leave : approvedLeaves) {
            BusyScheduleResponse response = new BusyScheduleResponse();
            response.setType("LEAVE");
            response.setStartDate(leave.getStartDate());
            response.setEndDate(leave.getEndDate());
            
            // Nếu có startTime và endTime, sử dụng chúng (partial day leave)
            if (leave.getStartTime() != null && leave.getEndTime() != null) {
                response.setStartDateTime(LocalDateTime.of(leave.getStartDate(), leave.getStartTime()));
                response.setEndDateTime(LocalDateTime.of(leave.getEndDate(), leave.getEndTime()));
            } else {
                // Nếu không có time, set cho cả ngày (full day leave)
                response.setStartDateTime(leave.getStartDate().atStartOfDay());
                response.setEndDateTime(leave.getEndDate().atTime(23, 59, 59));
            }
            
            response.setReason(leave.getReason() != null ? leave.getReason() : "Approved leave");
            response.setLeaveRequestId(leave.getId());
            busySchedules.add(response);
        }

        // Sắp xếp theo thời gian bắt đầu
        busySchedules.sort((a, b) -> {
            LocalDateTime aStart = a.getStartDateTime() != null ? a.getStartDateTime() : a.getStartDate().atStartOfDay();
            LocalDateTime bStart = b.getStartDateTime() != null ? b.getStartDateTime() : b.getStartDate().atStartOfDay();
            return aStart.compareTo(bStart);
        });

        return busySchedules;
    }

    public AppointmentResponse updateAppointmentStatus(Long id, UpdateAppointmentStatusRequest request) {
        // Lấy user hiện tại từ JWT
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Kiểm tra user có role PATIENT
        if (!"PATIENT".equals(currentUser.getRole())) {
            throw new ForbiddenException("Only patients can update their appointment status");
        }

        // Tìm appointment
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + id));

        // Kiểm tra appointment thuộc về patient hiện tại
        if (!appointment.getPatient().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only update your own appointments");
        }

        // Kiểm tra status hiện tại - chỉ cho phép cancel nếu chưa completed hoặc cancelled
        String currentStatus = appointment.getStatus();
        if ("COMPLETED".equals(currentStatus)) {
            throw new BadRequestException("Cannot update status of a completed appointment");
        }
        
        if ("CANCELLED".equals(currentStatus)) {
            throw new BadRequestException("Appointment is already cancelled");
        }

        // Kiểm tra status mới - patient chỉ có thể cancel
        if (!"CANCELLED".equals(request.getStatus())) {
            throw new BadRequestException("Patients can only cancel appointments");
        }

        // Cập nhật status
        appointment.setStatus(request.getStatus());

        // Nếu cancel, cập nhật schedule status (mỗi appointment có schedule riêng 1-1)
        if ("CANCELLED".equals(request.getStatus())) {
            DoctorSchedule schedule = appointment.getSchedule();
            // Update schedule status để đánh dấu đã bị cancel
            schedule.setStatus("BLOCKED");
            scheduleRepository.save(schedule);
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return toAppointmentResponse(savedAppointment);
    }

    /**
     * Update appointment status by doctor
     * Doctors can mark appointments as COMPLETED or update other statuses
     */
    public AppointmentResponse updateAppointmentStatusByDoctor(Long id, UpdateAppointmentStatusByDoctorRequest request) {
        // Lấy user hiện tại từ JWT
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Kiểm tra user có role DOCTOR
        if (!"DOCTOR".equals(currentUser.getRole())) {
            throw new ForbiddenException("Only doctors can update appointment status");
        }

        // Tìm doctor profile
        Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Doctor profile not found"));

        // Tìm appointment
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + id));

        // Kiểm tra appointment được assign cho doctor hiện tại
        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new ForbiddenException("You can only update appointments assigned to you");
        }

        // Validate status
        String newStatus = request.getStatus();
        String[] allowedStatuses = {"PENDING", "CONFIRMED", "CHECKED_IN", "IN_PROGRESS", "REVIEW", "COMPLETED", "CANCELLED", "NO_SHOW", "REJECTED", "EXPIRED", "CANCELLED_BY_DOCTOR", "CANCELLED_BY_PATIENT"};
        boolean isValidStatus = false;
        for (String status : allowedStatuses) {
            if (status.equals(newStatus)) {
                isValidStatus = true;
                break;
            }
        }

        if (!isValidStatus) {
            throw new BadRequestException("Invalid status: " + newStatus);
        }

        // Kiểm tra không thể update nếu đã completed, expired, rejected (trừ khi đang set lại)
        String currentStatus = appointment.getStatus();
        if ("COMPLETED".equals(currentStatus) && !"COMPLETED".equals(newStatus)) {
            throw new BadRequestException("Cannot change status of a completed appointment");
        }

        if ("EXPIRED".equals(currentStatus) || "REJECTED".equals(currentStatus)) {
            throw new BadRequestException("Cannot change status of an expired or rejected appointment");
        }

        // Validate status transitions theo state machine chuẩn bệnh viện
        // PENDING → CONFIRMED → CHECKED_IN → IN_PROGRESS → REVIEW → COMPLETED
        // PENDING → REJECTED (doctor reject)
        // PENDING → EXPIRED (timeout)
        // CONFIRMED → CANCELLED_BY_DOCTOR (doctor cancel after confirm)
        // Có thể CANCELLED hoặc NO_SHOW ở bất kỳ giai đoạn nào (trừ COMPLETED, EXPIRED, REJECTED)
        if ("PENDING".equals(currentStatus)) {
            if (!"CONFIRMED".equals(newStatus) && !"REJECTED".equals(newStatus) && !"CANCELLED".equals(newStatus) && !"NO_SHOW".equals(newStatus)) {
                throw new BadRequestException("Can only change PENDING appointment to CONFIRMED, REJECTED, CANCELLED, or NO_SHOW");
            }
        } else if ("CONFIRMED".equals(currentStatus)) {
            if (!"CHECKED_IN".equals(newStatus) && !"CANCELLED_BY_DOCTOR".equals(newStatus) && !"CANCELLED".equals(newStatus) && !"NO_SHOW".equals(newStatus)) {
                throw new BadRequestException("Can only change CONFIRMED appointment to CHECKED_IN, CANCELLED_BY_DOCTOR, CANCELLED, or NO_SHOW");
            }
        } else if ("CHECKED_IN".equals(currentStatus)) {
            if (!"IN_PROGRESS".equals(newStatus) && !"CANCELLED".equals(newStatus) && !"NO_SHOW".equals(newStatus)) {
                throw new BadRequestException("Can only change CHECKED_IN appointment to IN_PROGRESS, CANCELLED, or NO_SHOW");
            }
        } else if ("IN_PROGRESS".equals(currentStatus)) {
            if (!"REVIEW".equals(newStatus) && !"CANCELLED".equals(newStatus)) {
                throw new BadRequestException("Can only change IN_PROGRESS appointment to REVIEW or CANCELLED");
            }
        } else if ("REVIEW".equals(currentStatus)) {
            if (!"COMPLETED".equals(newStatus)) {
                throw new BadRequestException("Can only change REVIEW appointment to COMPLETED");
            }
        }

        // Cập nhật status
        appointment.setStatus(newStatus);

        // Cập nhật schedule status tương ứng
        DoctorSchedule schedule = appointment.getSchedule();
        if (schedule != null) {
            if ("COMPLETED".equals(newStatus)) {
                schedule.setStatus("COMPLETED");
            } else if ("REVIEW".equals(newStatus)) {
                schedule.setStatus("REVIEW");
            } else if ("CANCELLED".equals(newStatus) || "NO_SHOW".equals(newStatus) || "CANCELLED_BY_DOCTOR".equals(newStatus)) {
                // Khi cancel hoặc no-show, có thể giải phóng slot nếu chưa quá thời gian
                schedule.setStatus("BLOCKED");
            } else if ("REJECTED".equals(newStatus) || "EXPIRED".equals(newStatus)) {
                // Release slot khi reject hoặc expire
                schedule.setStatus("BLOCKED");
            } else if ("IN_PROGRESS".equals(newStatus)) {
                schedule.setStatus("BOOKED"); // Giữ BOOKED khi đang khám
            } else if ("CHECKED_IN".equals(newStatus)) {
                schedule.setStatus("BOOKED"); // Giữ BOOKED khi đã check-in
            } else if ("CONFIRMED".equals(newStatus)) {
                schedule.setStatus("BOOKED"); // Đảm bảo BOOKED khi confirmed
            }
            scheduleRepository.save(schedule);
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return toAppointmentResponse(savedAppointment);
    }

    /**
     * Get all appointments of the current authenticated user
     * Patients see their own appointments, doctors see appointments assigned to them
     */
    public List<AppointmentResponse> getMyAppointments(String status) {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        List<Appointment> appointments;
        String userRole = currentUser.getRole();
        
        if ("PATIENT".equals(userRole)) {
            // Patient lấy appointments của mình
            if (status != null && !status.trim().isEmpty()) {
                appointments = appointmentRepository.findByPatientIdAndStatus(currentUser.getId(), status);
            } else {
                appointments = appointmentRepository.findByPatientId(currentUser.getId());
            }
        } else if ("DOCTOR".equals(userRole)) {
            // Doctor lấy appointments được assign cho mình
            Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("Doctor profile not found"));
            
            if (status != null && !status.trim().isEmpty()) {
                appointments = appointmentRepository.findByDoctorIdAndStatus(doctor.getId(), status);
            } else {
                appointments = appointmentRepository.findByDoctorId(doctor.getId());
            }
        } else {
            // ADMIN có thể xem tất cả (hoặc throw exception tùy yêu cầu)
            throw new ForbiddenException("This endpoint is for patients and doctors only");
        }

        // Convert to DTO và sắp xếp theo thời gian (mới nhất trước)
        return appointments.stream()
                .map(this::toAppointmentResponse)
                .sorted((a, b) -> b.getAppointmentTime().compareTo(a.getAppointmentTime()))
                .collect(Collectors.toList());
    }

    /**
     * Get all appointments for today
     * Can filter by doctorId if provided
     */
    public List<AppointmentResponse> getTodayAppointments(Long doctorId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        List<Appointment> appointments;
        
        if (doctorId != null) {
            // Lấy appointments của doctor cụ thể trong ngày
            appointments = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, startOfDay, endOfDay);
        } else {
            // Lấy tất cả appointments trong ngày
            appointments = appointmentRepository.findByAppointmentTimeBetween(startOfDay, endOfDay);
        }

        // Lọc bỏ CANCELLED appointments và sắp xếp theo thời gian
        return appointments.stream()
                .filter(apt -> !"CANCELLED".equals(apt.getStatus()))
                .map(this::toAppointmentResponse)
                .sorted((a, b) -> a.getAppointmentTime().compareTo(b.getAppointmentTime()))
                .collect(Collectors.toList());
    }

    /**
     * Get all appointments for a specific doctor
     * Can filter by status and date
     */
    public List<AppointmentResponse> getDoctorAppointments(Long doctorId, String status, LocalDate date) {
        // Kiểm tra doctor tồn tại
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new NotFoundException("Doctor not found with id: " + doctorId));

        List<Appointment> appointments;
        
        if (date != null) {
            // Lấy appointments trong ngày cụ thể
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            
            if (status != null && !status.trim().isEmpty()) {
                // Filter theo cả status và date
                appointments = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, startOfDay, endOfDay)
                        .stream()
                        .filter(apt -> status.equals(apt.getStatus()))
                        .collect(Collectors.toList());
            } else {
                appointments = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, startOfDay, endOfDay);
            }
        } else {
            // Lấy tất cả appointments của doctor
            if (status != null && !status.trim().isEmpty()) {
                appointments = appointmentRepository.findByDoctorIdAndStatus(doctorId, status);
            } else {
                appointments = appointmentRepository.findByDoctorId(doctorId);
            }
        }

        // Sắp xếp theo thời gian
        return appointments.stream()
                .map(this::toAppointmentResponse)
                .sorted((a, b) -> a.getAppointmentTime().compareTo(b.getAppointmentTime()))
                .collect(Collectors.toList());
    }

    /**
     * Get all appointments for a specific doctor with paging
     * Orders: appointments >= now first (ASC), then appointments < now (DESC)
     * Can filter by status
     */
    public Page<AppointmentResponse> getDoctorAppointmentsWithPaging(Long doctorId, String status, Pageable pageable) {
        // Kiểm tra doctor tồn tại
        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new NotFoundException("Doctor not found with id: " + doctorId));

        LocalDateTime now = LocalDateTime.now();
        
        // Query với paging và custom ordering
        Page<Appointment> appointmentPage = appointmentRepository.findByDoctorIdWithOrdering(
            doctorId, 
            status, 
            now, 
            pageable);

        // Convert to DTO
        List<AppointmentResponse> content = appointmentPage.getContent().stream()
                .map(this::toAppointmentResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, appointmentPage.getTotalElements());
    }

    /**
     * Get all appointments with pagination (ADMIN only)
     */
    public Page<AppointmentResponse> getAllAppointments(String status, Pageable pageable) {
        List<Appointment> appointments;
        
        if (status != null && !status.trim().isEmpty()) {
            appointments = appointmentRepository.findByStatus(status);
        } else {
            appointments = appointmentRepository.findAll();
        }
        
        // Sort: future/current appointments first (ascending), then past appointments (descending)
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> futureAppointments = appointments.stream()
                .filter(apt -> apt.getAppointmentTime() != null && apt.getAppointmentTime().isAfter(now) || apt.getAppointmentTime().isEqual(now))
                .sorted((a, b) -> a.getAppointmentTime().compareTo(b.getAppointmentTime()))
                .collect(Collectors.toList());
        
        List<Appointment> pastAppointments = appointments.stream()
                .filter(apt -> apt.getAppointmentTime() != null && apt.getAppointmentTime().isBefore(now))
                .sorted((a, b) -> b.getAppointmentTime().compareTo(a.getAppointmentTime()))
                .collect(Collectors.toList());
        
        List<Appointment> sortedAppointments = new ArrayList<>();
        sortedAppointments.addAll(futureAppointments);
        sortedAppointments.addAll(pastAppointments);
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedAppointments.size());
        List<Appointment> paginatedAppointments = sortedAppointments.subList(start, end);
        
        // Convert to DTO
        List<AppointmentResponse> content = paginatedAppointments.stream()
                .map(this::toAppointmentResponse)
                .collect(Collectors.toList());
        
        return new PageImpl<>(content, pageable, sortedAppointments.size());
    }

    /**
     * Update appointment consultation notes by doctor
     * Doctors can add consultation notes, diagnosis, and treatment plan
     */
    public AppointmentResponse updateAppointmentNotes(Long id, UpdateAppointmentNotesRequest request) {
        // Lấy user hiện tại từ JWT
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Kiểm tra user có role DOCTOR
        if (!"DOCTOR".equals(currentUser.getRole())) {
            throw new ForbiddenException("Only doctors can update appointment notes");
        }

        // Tìm doctor profile
        Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Doctor profile not found"));

        // Tìm appointment
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + id));

        // Kiểm tra appointment được assign cho doctor hiện tại
        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new ForbiddenException("You can only update notes for appointments assigned to you");
        }

        // Cập nhật notes
        appointment.setNotes(request.getNotes());

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return toAppointmentResponse(savedAppointment);
    }

    /**
     * Check-in appointment - changes status from CONFIRMED to CHECKED_IN
     * Can be called by staff or doctor
     */
    public AppointmentResponse checkInAppointment(Long id) {
        // Lấy user hiện tại từ JWT
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Tìm appointment
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + id));

        // Kiểm tra status phải là CONFIRMED
        if (!"CONFIRMED".equals(appointment.getStatus())) {
            throw new BadRequestException("Can only check-in appointments with CONFIRMED status. Current status: " + appointment.getStatus());
        }

        // Cập nhật status
        appointment.setStatus("CHECKED_IN");

        // Cập nhật schedule status
        DoctorSchedule schedule = appointment.getSchedule();
        if (schedule != null) {
            schedule.setStatus("BOOKED");
            scheduleRepository.save(schedule);
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return toAppointmentResponse(savedAppointment);
    }

    /**
     * Start consultation - changes status from CHECKED_IN to IN_PROGRESS
     * Only doctors assigned to the appointment can start consultation
     */
    public AppointmentResponse startConsultation(Long id) {
        // Lấy user hiện tại từ JWT
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Kiểm tra user có role DOCTOR
        if (!"DOCTOR".equals(currentUser.getRole())) {
            throw new ForbiddenException("Only doctors can start consultation");
        }

        // Tìm doctor profile
        Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Doctor profile not found"));

        // Tìm appointment
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + id));

        // Kiểm tra appointment được assign cho doctor hiện tại
        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new ForbiddenException("You can only start consultation for appointments assigned to you");
        }

        // Kiểm tra status phải là CHECKED_IN
        if (!"CHECKED_IN".equals(appointment.getStatus())) {
            throw new BadRequestException("Can only start consultation for appointments with CHECKED_IN status. Current status: " + appointment.getStatus());
        }

        // Cập nhật status
        appointment.setStatus("IN_PROGRESS");

        // Cập nhật schedule status
        DoctorSchedule schedule = appointment.getSchedule();
        if (schedule != null) {
            schedule.setStatus("BOOKED");
            scheduleRepository.save(schedule);
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return toAppointmentResponse(savedAppointment);
    }

    /**
     * Complete consultation - changes status from IN_PROGRESS to REVIEW
     * Only doctors assigned to the appointment can complete consultation
     * After completion, patient can review the doctor
     */
    public AppointmentResponse completeConsultation(Long id) {
        // Lấy user hiện tại từ JWT
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Kiểm tra user có role DOCTOR
        if (!"DOCTOR".equals(currentUser.getRole())) {
            throw new ForbiddenException("Only doctors can complete consultation");
        }

        // Tìm doctor profile
        Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Doctor profile not found"));

        // Tìm appointment
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + id));

        // Kiểm tra appointment được assign cho doctor hiện tại
        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new ForbiddenException("You can only complete consultation for appointments assigned to you");
        }

        // Kiểm tra status phải là IN_PROGRESS
        if (!"IN_PROGRESS".equals(appointment.getStatus())) {
            throw new BadRequestException("Can only complete consultation for appointments with IN_PROGRESS status. Current status: " + appointment.getStatus());
        }

        // Cập nhật status - chuyển sang REVIEW để patient có thể đánh giá
        appointment.setStatus("REVIEW");

        // Cập nhật schedule status
        DoctorSchedule schedule = appointment.getSchedule();
        if (schedule != null) {
            schedule.setStatus("REVIEW");
            scheduleRepository.save(schedule);
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return toAppointmentResponse(savedAppointment);
    }

    /**
     * Doctor confirms a PENDING appointment
     * Changes status from PENDING to CONFIRMED
     * Locks the slot (BOOKED)
     */
    public AppointmentResponse confirmByDoctor(Long id) {
        // Lấy user hiện tại từ JWT
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Kiểm tra user có role DOCTOR
        if (!"DOCTOR".equals(currentUser.getRole())) {
            throw new ForbiddenException("Only doctors can confirm appointments");
        }

        // Tìm doctor profile
        Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Doctor profile not found"));

        // Tìm appointment
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + id));

        // Kiểm tra appointment được assign cho doctor hiện tại
        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new ForbiddenException("You can only confirm appointments assigned to you");
        }

        // Kiểm tra status phải là PENDING
        if (!"PENDING".equals(appointment.getStatus())) {
            throw new BadRequestException("Can only confirm appointments with PENDING status. Current status: " + appointment.getStatus());
        }

        // Kiểm tra deposit đã được xác nhận chưa
        if (!"CONFIRMED".equals(appointment.getDepositStatus())) {
            throw new BadRequestException("Cannot confirm appointment. Deposit has not been confirmed yet. Current deposit status: " + appointment.getDepositStatus());
        }

        // Cập nhật status
        appointment.setStatus("CONFIRMED");

        // Lock slot (chuyển schedule sang BOOKED)
        DoctorSchedule schedule = appointment.getSchedule();
        if (schedule != null) {
            schedule.setStatus("BOOKED");
            schedule.setHoldExpiresAt(null); // Xóa hold expiry nếu có
            scheduleRepository.save(schedule);
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return toAppointmentResponse(savedAppointment);
    }

    /**
     * Doctor rejects a PENDING appointment
     * Changes status from PENDING to REJECTED
     * Releases the slot
     * Reason is stored internally (only visible to doctor/admin)
     */
    public AppointmentResponse rejectByDoctor(Long id, RejectAppointmentRequest request) {
        // Lấy user hiện tại từ JWT
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Kiểm tra user có role DOCTOR
        if (!"DOCTOR".equals(currentUser.getRole())) {
            throw new ForbiddenException("Only doctors can reject appointments");
        }

        // Tìm doctor profile
        Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Doctor profile not found"));

        // Tìm appointment
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + id));

        // Kiểm tra appointment được assign cho doctor hiện tại
        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new ForbiddenException("You can only reject appointments assigned to you");
        }

        // Kiểm tra status phải là PENDING
        if (!"PENDING".equals(appointment.getStatus())) {
            throw new BadRequestException("Can only reject appointments with PENDING status. Current status: " + appointment.getStatus());
        }

        // Lưu lý do từ chối (internal, chỉ doctor/admin thấy)
        if (request != null && request.getReason() != null && !request.getReason().trim().isEmpty()) {
            appointment.setRejectionReason(request.getReason());
        }

        // Cập nhật status
        appointment.setStatus("REJECTED");

        // Release slot (xóa schedule để giải phóng slot)
        DoctorSchedule schedule = appointment.getSchedule();
        if (schedule != null) {
            scheduleRepository.delete(schedule);
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return toAppointmentResponse(savedAppointment);
    }

    /**
     * Doctor cancels a CONFIRMED appointment
     * Changes status from CONFIRMED to CANCELLED_BY_DOCTOR
     * Releases the slot
     */
    public AppointmentResponse cancelByDoctor(Long id, RejectAppointmentRequest request) {
        // Lấy user hiện tại từ JWT
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Kiểm tra user có role DOCTOR
        if (!"DOCTOR".equals(currentUser.getRole())) {
            throw new ForbiddenException("Only doctors can cancel appointments");
        }

        // Tìm doctor profile
        Doctor doctor = doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Doctor profile not found"));

        // Tìm appointment
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + id));

        // Kiểm tra appointment được assign cho doctor hiện tại
        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new ForbiddenException("You can only cancel appointments assigned to you");
        }

        // Kiểm tra status phải là CONFIRMED
        if (!"CONFIRMED".equals(appointment.getStatus())) {
            throw new BadRequestException("Can only cancel appointments with CONFIRMED status. Current status: " + appointment.getStatus());
        }

        // Lưu lý do hủy (internal, chỉ doctor/admin thấy)
        if (request != null && request.getReason() != null && !request.getReason().trim().isEmpty()) {
            appointment.setRejectionReason(request.getReason());
        }

        // Cập nhật status
        appointment.setStatus("CANCELLED_BY_DOCTOR");

        // Release slot
        DoctorSchedule schedule = appointment.getSchedule();
        if (schedule != null) {
            schedule.setStatus("BLOCKED");
            scheduleRepository.save(schedule);
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return toAppointmentResponse(savedAppointment);
    }

    /**
     * Admin confirms deposit payment
     * Changes deposit status from PENDING to CONFIRMED
     * This allows the doctor to confirm the appointment
     */
    public AppointmentResponse confirmDeposit(Long id) {
        // Lấy user hiện tại từ JWT
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Kiểm tra user có role ADMIN
        if (!"ADMIN".equals(currentUser.getRole())) {
            throw new ForbiddenException("Only admins can confirm deposits");
        }

        // Tìm appointment
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + id));

        // Kiểm tra deposit status phải là PENDING
        if (!"PENDING".equals(appointment.getDepositStatus())) {
            throw new BadRequestException("Deposit is not in PENDING status. Current status: " + appointment.getDepositStatus());
        }

        // Cập nhật deposit status
        appointment.setDepositStatus("CONFIRMED");
        appointment.setDepositConfirmedAt(LocalDateTime.now());

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return toAppointmentResponse(savedAppointment);
    }
}

