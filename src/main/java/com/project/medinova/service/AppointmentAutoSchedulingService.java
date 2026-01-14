package com.project.medinova.service;

import com.project.medinova.dto.RegisterAppointmentRequest;
import com.project.medinova.entity.*;
import com.project.medinova.exception.BadRequestException;
import com.project.medinova.exception.NotFoundException;
import com.project.medinova.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AppointmentAutoSchedulingService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ClinicRepository clinicRepository;

    @Autowired
    private DoctorScheduleRepository scheduleRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorLeaveRequestRepository leaveRequestRepository;

    @Autowired
    private DoctorWorkingDaysRepository workingDaysRepository;

    private static final int DEFAULT_DURATION_MINUTES = 60;
    private static final int MAX_SEARCH_DAYS = 30; // Tìm trong 30 ngày tới

    /**
     * Tự động tìm slot rảnh và tạo appointment
     */
    public AppointmentScheduleResult findAndAssignSlot(RegisterAppointmentRequest request, User patient) {
        // 1. Xác định bác sĩ
        Doctor doctor = determineDoctor(request);
        
        // 2. Xác định clinic
        Clinic clinic = clinicRepository.findById(request.getClinicId())
                .orElseThrow(() -> new NotFoundException("Clinic not found with id: " + request.getClinicId()));

        // 3. Kiểm tra doctor có thuộc clinic không
        if (!doctor.getClinic().getId().equals(clinic.getId())) {
            throw new BadRequestException("Doctor does not work at this clinic");
        }

        // 4. Tìm slot rảnh
        SlotInfo availableSlot = findAvailableSlot(doctor, request);
        
        if (availableSlot == null) {
            throw new BadRequestException("Không tìm thấy lịch rảnh trong vòng " + MAX_SEARCH_DAYS + " ngày tới. Vui lòng thử lại sau hoặc chọn bác sĩ khác.");
        }

        // 5. Tạo schedule và appointment
        DoctorSchedule schedule = createSchedule(doctor, clinic, availableSlot, request.getDurationMinutes());
        Appointment appointment = createAppointment(patient, doctor, clinic, schedule, request);

        return new AppointmentScheduleResult(appointment, schedule, availableSlot);
    }

    /**
     * Xác định bác sĩ (từ request hoặc tự động chọn)
     */
    private Doctor determineDoctor(RegisterAppointmentRequest request) {
        if (request.getDoctorId() != null) {
            // Người dùng đã chọn bác sĩ
            return doctorRepository.findById(request.getDoctorId())
                    .orElseThrow(() -> new NotFoundException("Doctor not found with id: " + request.getDoctorId()));
        } else {
            // Tự động chọn bác sĩ từ clinic
            List<Doctor> clinicDoctors = doctorRepository.findByClinicId(request.getClinicId());
            clinicDoctors = clinicDoctors.stream()
                    .filter(d -> "APPROVED".equals(d.getStatus()))
                    .collect(Collectors.toList());
            
            if (clinicDoctors.isEmpty()) {
                throw new NotFoundException("No approved doctors found in this clinic");
            }
            
            // Chọn bác sĩ có ít appointment nhất trong tuần tới
            return selectBestDoctor(clinicDoctors);
        }
    }

    /**
     * Chọn bác sĩ tốt nhất (ít appointment nhất)
     */
    private Doctor selectBestDoctor(List<Doctor> doctors) {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);
        
        Doctor bestDoctor = doctors.get(0);
        long minAppointments = Long.MAX_VALUE;
        
        for (Doctor doctor : doctors) {
            List<Appointment> appointments = appointmentRepository.findByDoctorId(doctor.getId());
            long count = appointments.stream()
                    .filter(apt -> {
                        LocalDate aptDate = apt.getAppointmentTime().toLocalDate();
                        return !aptDate.isBefore(today) && !aptDate.isAfter(nextWeek) 
                                && !"CANCELLED".equals(apt.getStatus());
                    })
                    .count();
            
            if (count < minAppointments) {
                minAppointments = count;
                bestDoctor = doctor;
            }
        }
        
        return bestDoctor;
    }

    /**
     * Tìm slot rảnh của bác sĩ
     */
    private SlotInfo findAvailableSlot(Doctor doctor, RegisterAppointmentRequest request) {
        LocalDate startDate = LocalDate.now().plusDays(1); // Bắt đầu từ ngày mai
        if (request.getPreferredDate() != null && !request.getPreferredDate().isEmpty()) {
            try {
                LocalDate preferred = LocalDate.parse(request.getPreferredDate());
                if (!preferred.isBefore(LocalDate.now())) {
                    startDate = preferred;
                }
            } catch (Exception e) {
                // Invalid date format, use default
            }
        }

        int duration = request.getDurationMinutes() != null ? request.getDurationMinutes() : DEFAULT_DURATION_MINUTES;
        String preferredTimeRange = request.getPreferredTimeRange() != null ? request.getPreferredTimeRange() : "ANY";

        // Tìm trong MAX_SEARCH_DAYS ngày
        for (int i = 0; i < MAX_SEARCH_DAYS; i++) {
            LocalDate checkDate = startDate.plusDays(i);
            
            // Kiểm tra bác sĩ có làm việc ngày này không
            if (!isDoctorWorkingOnDate(doctor, checkDate)) {
                continue;
            }

            // Kiểm tra bác sĩ có nghỉ không
            if (isDoctorOnLeave(doctor, checkDate)) {
                continue;
            }

            // Tìm slot rảnh trong ngày
            SlotInfo slot = findAvailableSlotInDay(doctor, checkDate, duration, preferredTimeRange);
            if (slot != null) {
                return slot;
            }
        }

        return null; // Không tìm thấy slot
    }

    /**
     * Kiểm tra bác sĩ có làm việc ngày này không
     */
    private boolean isDoctorWorkingOnDate(Doctor doctor, LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int dayOfWeekValue = dayOfWeek.getValue(); // 1=Monday, 7=Sunday
        
        DoctorWorkingDays workingDay = workingDaysRepository.findByDoctorIdAndDayOfWeek(doctor.getId(), dayOfWeekValue);
        return workingDay != null && workingDay.getIsWorking();
    }

    /**
     * Kiểm tra bác sĩ có nghỉ không
     */
    private boolean isDoctorOnLeave(Doctor doctor, LocalDate date) {
        List<DoctorLeaveRequest> approvedLeaves = leaveRequestRepository
                .findByDoctorIdAndStatus(doctor.getId(), "APPROVED");
        
        for (DoctorLeaveRequest leave : approvedLeaves) {
            if (!date.isBefore(leave.getStartDate()) && !date.isAfter(leave.getEndDate())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tìm slot rảnh trong một ngày
     */
    private SlotInfo findAvailableSlotInDay(Doctor doctor, LocalDate date, int durationMinutes, String preferredTimeRange) {
        // Lấy giờ làm việc mặc định của bác sĩ
        LocalTime defaultStart = doctor.getDefaultStartTime() != null ? 
                doctor.getDefaultStartTime() : LocalTime.of(8, 0);
        LocalTime defaultEnd = doctor.getDefaultEndTime() != null ? 
                doctor.getDefaultEndTime() : LocalTime.of(17, 0);

        // Xác định time range cần tìm
        LocalTime rangeStart, rangeEnd;
        switch (preferredTimeRange.toUpperCase()) {
            case "MORNING":
                rangeStart = LocalTime.of(8, 0);
                rangeEnd = LocalTime.of(12, 0);
                break;
            case "AFTERNOON":
                rangeStart = LocalTime.of(12, 0);
                rangeEnd = LocalTime.of(17, 0);
                break;
            case "EVENING":
                rangeStart = LocalTime.of(17, 0);
                rangeEnd = LocalTime.of(20, 0);
                break;
            default: // ANY
                rangeStart = defaultStart;
                rangeEnd = defaultEnd;
        }

        // Lấy tất cả appointments của bác sĩ trong ngày
        List<Appointment> dayAppointments = appointmentRepository.findByDoctorId(doctor.getId());
        dayAppointments = dayAppointments.stream()
                .filter(apt -> apt.getAppointmentTime().toLocalDate().equals(date))
                .filter(apt -> !"CANCELLED".equals(apt.getStatus()))
                .collect(Collectors.toList());

        // Tạo danh sách busy slots
        List<TimeSlot> busySlots = new ArrayList<>();
        for (Appointment apt : dayAppointments) {
            LocalDateTime aptStart = apt.getAppointmentTime();
            LocalDateTime aptEnd;
            if (apt.getSchedule() != null) {
                aptEnd = LocalDateTime.of(apt.getSchedule().getWorkDate(), apt.getSchedule().getEndTime());
            } else {
                aptEnd = aptStart.plusMinutes(durationMinutes);
            }
            busySlots.add(new TimeSlot(aptStart.toLocalTime(), aptEnd.toLocalTime()));
        }

        // Tìm slot rảnh (mỗi slot 60 phút)
        LocalTime currentTime = rangeStart;
        while (currentTime.plusMinutes(durationMinutes).isBefore(rangeEnd) || 
               currentTime.plusMinutes(durationMinutes).equals(rangeEnd)) {
            
            LocalTime slotEnd = currentTime.plusMinutes(durationMinutes);
            
            // Kiểm tra slot có overlap với busy slots không
            boolean isAvailable = true;
            for (TimeSlot busy : busySlots) {
                if (isTimeOverlap(currentTime, slotEnd, busy.start, busy.end)) {
                    isAvailable = false;
                    break;
                }
            }

            if (isAvailable) {
                // Tìm thấy slot rảnh
                return new SlotInfo(date, currentTime, slotEnd);
            }

            // Chuyển sang slot tiếp theo (mỗi 30 phút)
            currentTime = currentTime.plusMinutes(30);
        }

        return null; // Không tìm thấy slot trong ngày
    }

    /**
     * Kiểm tra 2 khoảng thời gian có overlap không
     */
    private boolean isTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    /**
     * Tạo schedule
     */
    private DoctorSchedule createSchedule(Doctor doctor, Clinic clinic, SlotInfo slot, Integer durationMinutes) {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctor(doctor);
        schedule.setClinic(clinic);
        schedule.setWorkDate(slot.date);
        schedule.setStartTime(slot.startTime);
        schedule.setEndTime(slot.endTime);
        schedule.setStatus("HOLD"); // HOLD cho đến khi thanh toán xong
        return scheduleRepository.save(schedule);
    }

    /**
     * Tạo appointment
     */
    private Appointment createAppointment(User patient, Doctor doctor, Clinic clinic, 
                                          DoctorSchedule schedule, RegisterAppointmentRequest request) {
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setClinic(clinic);
        appointment.setSchedule(schedule);
        appointment.setAppointmentTime(LocalDateTime.of(schedule.getWorkDate(), schedule.getStartTime()));
        appointment.setStatus("PENDING"); // PENDING cho đến khi thanh toán xong
        appointment.setAge(request.getAge());
        appointment.setGender(request.getGender());
        appointment.setSymptoms(request.getSymptoms());
        appointment.setDepositAmount(50000.0);
        return appointmentRepository.save(appointment);
    }

    /**
     * Inner class để lưu thông tin slot
     */
    public static class SlotInfo {
        public LocalDate date;
        public LocalTime startTime;
        public LocalTime endTime;

        public SlotInfo(LocalDate date, LocalTime startTime, LocalTime endTime) {
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    /**
     * Inner class để lưu kết quả
     */
    public static class AppointmentScheduleResult {
        public Appointment appointment;
        public DoctorSchedule schedule;
        public SlotInfo slotInfo;

        public AppointmentScheduleResult(Appointment appointment, DoctorSchedule schedule, SlotInfo slotInfo) {
            this.appointment = appointment;
            this.schedule = schedule;
            this.slotInfo = slotInfo;
        }
    }

    /**
     * Inner class để lưu time slot
     */
    private static class TimeSlot {
        LocalTime start;
        LocalTime end;

        TimeSlot(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }
    }
}
