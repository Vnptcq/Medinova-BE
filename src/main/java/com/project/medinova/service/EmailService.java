package com.project.medinova.service;

import com.project.medinova.entity.Appointment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:medinova@example.com}")
    private String fromEmail;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    /**
     * Gửi email thông báo lịch khám đã được xác nhận
     */
    public void sendAppointmentConfirmationEmail(Appointment appointment) {
        if (!emailEnabled || mailSender == null) {
            System.out.println("Email service not configured. Skipping email send.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(appointment.getPatient().getEmail());
            message.setSubject("Xác nhận lịch khám - Medinova Clinic");
            
            String emailBody = buildAppointmentConfirmationEmailBody(appointment);
            message.setText(emailBody);
            
            mailSender.send(message);
            System.out.println("Appointment confirmation email sent to: " + appointment.getPatient().getEmail());
        } catch (Exception e) {
            System.err.println("Failed to send appointment confirmation email: " + e.getMessage());
            // Không throw exception để không làm gián đoạn luồng chính
        }
    }

    /**
     * Gửi email thông báo lịch khám đã được hủy
     */
    public void sendAppointmentCancellationEmail(Appointment appointment, String reason) {
        if (!emailEnabled || mailSender == null) {
            System.out.println("Email service not configured. Skipping email send.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(appointment.getPatient().getEmail());
            message.setSubject("Thông báo hủy lịch khám - Medinova Clinic");
            
            String emailBody = buildAppointmentCancellationEmailBody(appointment, reason);
            message.setText(emailBody);
            
            mailSender.send(message);
            System.out.println("Appointment cancellation email sent to: " + appointment.getPatient().getEmail());
        } catch (Exception e) {
            System.err.println("Failed to send appointment cancellation email: " + e.getMessage());
        }
    }

    /**
     * Gửi email thông báo lịch khám đã được đổi (nếu có tính năng reschedule)
     */
    public void sendAppointmentRescheduleEmail(Appointment appointment, String oldTime) {
        if (!emailEnabled || mailSender == null) {
            System.out.println("Email service not configured. Skipping email send.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(appointment.getPatient().getEmail());
            message.setSubject("Thông báo đổi lịch khám - Medinova Clinic");
            
            String emailBody = buildAppointmentRescheduleEmailBody(appointment, oldTime);
            message.setText(emailBody);
            
            mailSender.send(message);
            System.out.println("Appointment reschedule email sent to: " + appointment.getPatient().getEmail());
        } catch (Exception e) {
            System.err.println("Failed to send appointment reschedule email: " + e.getMessage());
        }
    }

    private String buildAppointmentConfirmationEmailBody(Appointment appointment) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        
        StringBuilder body = new StringBuilder();
        body.append("Kính chào ").append(appointment.getPatient().getFullName()).append(",\n\n");
        body.append("Chúng tôi xin thông báo lịch khám của bạn đã được xác nhận:\n\n");
        body.append("📅 THÔNG TIN LỊCH KHÁM:\n");
        body.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        body.append("Bác sĩ: ").append(appointment.getDoctor().getUser().getFullName()).append("\n");
        body.append("Phòng khám: ").append(appointment.getClinic().getName()).append("\n");
        body.append("Ngày khám: ").append(appointment.getAppointmentTime().format(dateFormatter)).append("\n");
        body.append("Giờ khám: ").append(appointment.getAppointmentTime().format(timeFormatter)).append("\n");
        body.append("Mã lịch hẹn: #").append(appointment.getId()).append("\n");
        if (appointment.getSymptoms() != null && !appointment.getSymptoms().isEmpty()) {
            body.append("Triệu chứng: ").append(appointment.getSymptoms()).append("\n");
        }
        body.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        body.append("💰 THÔNG TIN THANH TOÁN:\n");
        body.append("Số tiền cọc: 50,000 VND (đã thanh toán)\n\n");
        body.append("📝 LƯU Ý:\n");
        body.append("- Vui lòng đến đúng giờ hẹn\n");
        body.append("- Mang theo CMND/CCCD để xác nhận danh tính\n");
        body.append("- Nếu cần hủy lịch, vui lòng liên hệ trước 24 giờ\n");
        body.append("- Tiền cọc sẽ được hoàn lại nếu hủy trước 24 giờ\n\n");
        body.append("Trân trọng,\n");
        body.append("Medinova Clinic\n");
        body.append("Hotline: 1900-xxxx\n");
        body.append("Email: support@medinova.com");
        
        return body.toString();
    }

    private String buildAppointmentCancellationEmailBody(Appointment appointment, String reason) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        
        StringBuilder body = new StringBuilder();
        body.append("Kính chào ").append(appointment.getPatient().getFullName()).append(",\n\n");
        body.append("Chúng tôi xin thông báo lịch khám của bạn đã bị hủy:\n\n");
        body.append("📅 THÔNG TIN LỊCH KHÁM ĐÃ HỦY:\n");
        body.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        body.append("Bác sĩ: ").append(appointment.getDoctor().getUser().getFullName()).append("\n");
        body.append("Phòng khám: ").append(appointment.getClinic().getName()).append("\n");
        body.append("Ngày khám: ").append(appointment.getAppointmentTime().format(dateFormatter)).append("\n");
        body.append("Giờ khám: ").append(appointment.getAppointmentTime().format(timeFormatter)).append("\n");
        body.append("Mã lịch hẹn: #").append(appointment.getId()).append("\n");
        if (reason != null && !reason.isEmpty()) {
            body.append("Lý do: ").append(reason).append("\n");
        }
        body.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        body.append("💰 HOÀN TIỀN:\n");
        body.append("Số tiền cọc 50,000 VND sẽ được hoàn lại vào tài khoản của bạn trong vòng 3-5 ngày làm việc.\n\n");
        body.append("Nếu bạn muốn đặt lịch khám mới, vui lòng truy cập hệ thống hoặc liên hệ hotline.\n\n");
        body.append("Trân trọng,\n");
        body.append("Medinova Clinic\n");
        body.append("Hotline: 1900-xxxx\n");
        body.append("Email: support@medinova.com");
        
        return body.toString();
    }

    private String buildAppointmentRescheduleEmailBody(Appointment appointment, String oldTime) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        
        StringBuilder body = new StringBuilder();
        body.append("Kính chào ").append(appointment.getPatient().getFullName()).append(",\n\n");
        body.append("Chúng tôi xin thông báo lịch khám của bạn đã được đổi:\n\n");
        body.append("📅 THÔNG TIN LỊCH KHÁM MỚI:\n");
        body.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        body.append("Bác sĩ: ").append(appointment.getDoctor().getUser().getFullName()).append("\n");
        body.append("Phòng khám: ").append(appointment.getClinic().getName()).append("\n");
        body.append("Lịch cũ: ").append(oldTime).append("\n");
        body.append("Lịch mới: ").append(appointment.getAppointmentTime().format(dateFormatter))
                .append(" lúc ").append(appointment.getAppointmentTime().format(timeFormatter)).append("\n");
        body.append("Mã lịch hẹn: #").append(appointment.getId()).append("\n");
        body.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        body.append("Vui lòng đến đúng giờ hẹn mới.\n\n");
        body.append("Trân trọng,\n");
        body.append("Medinova Clinic");
        
        return body.toString();
    }
}
