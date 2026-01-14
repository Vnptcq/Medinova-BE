package com.project.medinova.service;

import com.project.medinova.entity.Appointment;
import com.project.medinova.entity.DoctorSchedule;
import com.project.medinova.repository.AppointmentRepository;
import com.project.medinova.repository.DoctorScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentSchedulerService.class);

    @Autowired
    private DoctorScheduleRepository scheduleRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    /**
     * Tự động release HOLD slots sau 5 phút
     * Chạy mỗi phút
     */
    @Scheduled(fixedRate = 60000) // 60 seconds = 1 minute
    @Transactional
    public void releaseExpiredHoldSlots() {
        LocalDateTime now = LocalDateTime.now();
        List<DoctorSchedule> expiredHolds = scheduleRepository.findByStatusAndHoldExpiresAtBefore("HOLD", now);
        
        for (DoctorSchedule schedule : expiredHolds) {
            logger.info("Releasing expired HOLD slot: scheduleId={}, expiredAt={}", 
                    schedule.getId(), schedule.getHoldExpiresAt());
            
            // Xóa appointment nếu có (cascade = CascadeType.ALL sẽ tự động xóa schedule)
            if (schedule.getAppointment() != null) {
                Appointment appointment = schedule.getAppointment();
                if ("PENDING".equals(appointment.getStatus())) {
                    appointmentRepository.delete(appointment);
                    logger.info("Deleted expired PENDING appointment: appointmentId={}, scheduleId={}", 
                            appointment.getId(), schedule.getId());
                } else {
                    // Nếu appointment không phải PENDING, chỉ xóa schedule
                    scheduleRepository.delete(schedule);
                    logger.info("Deleted expired HOLD schedule: scheduleId={}", schedule.getId());
                }
            } else {
                // Nếu không có appointment, chỉ xóa schedule
                scheduleRepository.delete(schedule);
                logger.info("Deleted expired HOLD schedule without appointment: scheduleId={}", schedule.getId());
            }
        }
        
        if (!expiredHolds.isEmpty()) {
            logger.info("Released {} expired HOLD slots", expiredHolds.size());
        }
    }

    /**
     * Tự động expire PENDING appointments sau timeout (nếu chưa thanh toán/confirm)
     * Chạy mỗi 10 phút
     * 
     * LƯU Ý: Với luồng mới, sau khi patient confirm (đã thanh toán), appointment tự động chuyển sang CONFIRMED.
     * Scheduled task này chỉ xử lý các PENDING appointments chưa được confirm (chưa thanh toán hoặc quá 5 phút).
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    @Transactional
    public void expirePendingAppointments() {
        // Timeout: 2 giờ - chỉ cho các PENDING appointments chưa được confirm
        // (Thực tế, các appointment này sẽ bị xóa sau 5 phút bởi releaseExpiredHoldSlots)
        LocalDateTime timeoutAgo = LocalDateTime.now().minusHours(2);
        List<Appointment> expiredPending = appointmentRepository.findByStatusAndCreatedAtBefore("PENDING", timeoutAgo);
        
        for (Appointment appointment : expiredPending) {
            // Chỉ xử lý các appointment có schedule HOLD (chưa được confirm)
            DoctorSchedule schedule = appointment.getSchedule();
            if (schedule != null && "HOLD".equals(schedule.getStatus())) {
                logger.info("Expiring PENDING appointment (not confirmed): appointmentId={}, createdAt={}", 
                        appointment.getId(), appointment.getCreatedAt());
                
                // Chuyển status sang EXPIRED
                appointment.setStatus("EXPIRED");
                appointmentRepository.save(appointment);
                
                // Release slot
                scheduleRepository.delete(schedule);
                logger.info("Released slot for expired appointment: appointmentId={}, scheduleId={}", 
                        appointment.getId(), schedule.getId());
            }
        }
        
        if (!expiredPending.isEmpty()) {
            logger.info("Expired {} PENDING appointments", expiredPending.size());
        }
    }
}


