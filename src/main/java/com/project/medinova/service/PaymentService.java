package com.project.medinova.service;

import com.project.medinova.dto.CreatePaymentRequest;
import com.project.medinova.dto.PaymentResponse;
import com.project.medinova.dto.ProcessPaymentRequest;
import com.project.medinova.dto.RefundPaymentRequest;
import com.project.medinova.entity.Appointment;
import com.project.medinova.entity.Payment;
import com.project.medinova.entity.User;
import com.project.medinova.exception.BadRequestException;
import com.project.medinova.exception.ForbiddenException;
import com.project.medinova.exception.NotFoundException;
import com.project.medinova.repository.AppointmentRepository;
import com.project.medinova.repository.PaymentRepository;
import com.project.medinova.repository.DoctorScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    
    private static final Double DEPOSIT_AMOUNT = 50000.0; // 50,000 VND - Số tiền cọc yêu cầu
    private static final Double MINIMUM_ACCEPTED_AMOUNT = 100000.0; // 100,000 VND - Số tiền tối thiểu để chấp nhận
    private static final Double APPOINTMENT_CREDIT_AMOUNT = 100000.0; // 100,000 VND - Số tiền để tính 1 lần đặt lịch

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorScheduleRepository scheduleRepository;

    @Autowired
    private AuthService authService;

    @Autowired(required = false)
    private com.project.medinova.service.EmailService emailService;

    /**
     * Tạo payment khi patient tạo appointment
     * Payment sẽ ở status PENDING, chờ patient thanh toán
     */
    public PaymentResponse createPaymentForAppointment(Long appointmentId) {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + appointmentId));

        // Kiểm tra appointment thuộc về patient hiện tại
        if (!appointment.getPatient().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only create payment for your own appointments");
        }

        // Kiểm tra appointment phải là PENDING
        if (!"PENDING".equals(appointment.getStatus())) {
            throw new BadRequestException("Payment can only be created for PENDING appointments");
        }

        // Kiểm tra đã có payment chưa
        if (paymentRepository.findByAppointmentId(appointmentId).isPresent()) {
            throw new BadRequestException("Payment already exists for this appointment");
        }

        // Tạo payment
        Payment payment = new Payment();
        payment.setPatient(currentUser);
        payment.setAppointment(appointment);
        payment.setAmount(DEPOSIT_AMOUNT);
        payment.setPaymentMethod("BANK_TRANSFER"); // Default, sẽ update sau
        payment.setStatus("PENDING");
        payment.setNotes("Deposit for appointment #" + appointmentId);

        Payment savedPayment = paymentRepository.save(payment);

        // Cập nhật appointment với deposit amount
        appointment.setDepositAmount(DEPOSIT_AMOUNT);
        appointmentRepository.save(appointment);

        return toPaymentResponse(savedPayment);
    }

    /**
     * Patient thanh toán cọc
     */
    public PaymentResponse processPayment(Long paymentId, CreatePaymentRequest request) {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found with id: " + paymentId));

        // Kiểm tra payment thuộc về patient hiện tại
        if (!payment.getPatient().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only process your own payments");
        }

        // Kiểm tra payment phải là PENDING
        if (!"PENDING".equals(payment.getStatus())) {
            throw new BadRequestException("Payment is not in PENDING status. Current status: " + payment.getStatus());
        }

        // Kiểm tra appointment ID khớp
        if (request.getAppointmentId() != null && !payment.getAppointment().getId().equals(request.getAppointmentId())) {
            throw new BadRequestException("Appointment ID does not match");
        }

        // Cập nhật payment
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionId(request.getTransactionId());
        payment.setPaymentGateway(request.getPaymentGateway() != null ? request.getPaymentGateway() : "MANUAL");
        payment.setStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());
        if (request.getNotes() != null) {
            payment.setNotes(request.getNotes());
        }

        Payment savedPayment = paymentRepository.save(payment);

        return toPaymentResponse(savedPayment);
    }

    /**
     * Xác nhận payment đã thanh toán (từ payment gateway callback)
     */
    public PaymentResponse confirmPayment(Long paymentId, ProcessPaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found with id: " + paymentId));

        if (!"PENDING".equals(payment.getStatus())) {
            throw new BadRequestException("Payment is not in PENDING status");
        }

        // Xác nhận payment đã thanh toán
        payment.setStatus("PAID");
        payment.setTransactionId(request.getTransactionId());
        payment.setPaidAt(LocalDateTime.now());
        
        // Lưu số tiền thực tế nhận được (nếu có trong request)
        if (request.getActualAmount() != null) {
            payment.setActualAmount(request.getActualAmount());
            logger.info("💰 PAYMENT CONFIRMED - Payment ID: {}, Expected: {} VND, Actual: {} VND", 
                    payment.getId(), payment.getAmount(), request.getActualAmount());
            
            // Log warning nếu số tiền thừa
            if (request.getActualAmount() > payment.getAmount()) {
                logger.warn("⚠️ PAYMENT EXCESS - Payment ID: {}, Patient ID: {}, Expected: {} VND, Actual: {} VND, Excess: {} VND. Admin should review transaction log.", 
                        payment.getId(), payment.getPatient().getId(), payment.getAmount(), 
                        request.getActualAmount(), request.getActualAmount() - payment.getAmount());
            }
        }
        
        if (request.getGatewayResponse() != null) {
            payment.setNotes(payment.getNotes() + " | Gateway: " + request.getGatewayResponse());
        }

        Payment savedPayment = paymentRepository.save(payment);

        // Cập nhật appointment status sang CONFIRMED và gửi email
        Appointment appointment = payment.getAppointment();
        if (appointment != null && "PENDING".equals(appointment.getStatus())) {
            appointment.setStatus("CONFIRMED");
            appointmentRepository.save(appointment);
            
            // Cập nhật schedule status sang BOOKED
            if (appointment.getSchedule() != null && scheduleRepository != null) {
                appointment.getSchedule().setStatus("BOOKED");
                scheduleRepository.save(appointment.getSchedule());
            }
            
            // Gửi email thông báo
            try {
                if (emailService != null) {
                    emailService.sendAppointmentConfirmationEmail(appointment);
                }
            } catch (Exception e) {
                System.err.println("Failed to send confirmation email: " + e.getMessage());
                // Không throw exception để không làm gián đoạn luồng
            }
        }

        return toPaymentResponse(savedPayment);
    }

    /**
     * Hoàn tiền khi hủy appointment
     */
    public PaymentResponse refundPayment(Long paymentId, RefundPaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found with id: " + paymentId));

        // Chỉ có thể refund payment đã PAID
        if (!"PAID".equals(payment.getStatus())) {
            throw new BadRequestException("Only PAID payments can be refunded. Current status: " + payment.getStatus());
        }

        // Kiểm tra appointment đã bị hủy chưa
        Appointment appointment = payment.getAppointment();
        if (appointment != null) {
            String status = appointment.getStatus();
            if (!status.contains("CANCELLED") && !"REJECTED".equals(status) && !"EXPIRED".equals(status)) {
                throw new BadRequestException("Cannot refund payment for appointment that is not cancelled/rejected/expired");
            }
        }

        // Refund payment
        payment.setStatus("REFUNDED");
        payment.setRefundedAt(LocalDateTime.now());
        payment.setRefundReason(request.getReason());
        if (request.getNotes() != null) {
            payment.setNotes(payment.getNotes() + " | Refund: " + request.getNotes());
        }

        Payment savedPayment = paymentRepository.save(payment);
        return toPaymentResponse(savedPayment);
    }

    /**
     * Lấy payment theo ID
     */
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found with id: " + paymentId));

        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Patient chỉ xem được payment của mình, ADMIN/DOCTOR xem được tất cả
        if ("PATIENT".equals(currentUser.getRole()) && !payment.getPatient().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only view your own payments");
        }

        return toPaymentResponse(payment);
    }

    /**
     * Lấy payments của patient hiện tại
     */
    public List<PaymentResponse> getMyPayments() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        List<Payment> payments = paymentRepository.findByPatientId(currentUser.getId());
        return payments.stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy payment theo appointment ID
     */
    public PaymentResponse getPaymentByAppointmentId(Long appointmentId) {
        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new NotFoundException("Payment not found for appointment id: " + appointmentId));

        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new ForbiddenException("User not authenticated");
        }

        // Patient chỉ xem được payment của mình
        if ("PATIENT".equals(currentUser.getRole()) && !payment.getPatient().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only view your own payments");
        }

        return toPaymentResponse(payment);
    }

    /**
     * Kiểm tra payment đã được thanh toán chưa
     */
    public boolean isPaymentPaid(Long appointmentId) {
        return paymentRepository.findByAppointmentId(appointmentId)
                .map(payment -> "PAID".equals(payment.getStatus()))
                .orElse(false);
    }

    /**
     * Refund payment theo appointment ID (helper method)
     */
    public PaymentResponse refundPaymentByAppointmentId(Long appointmentId, RefundPaymentRequest request) {
        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new NotFoundException("Payment not found for appointment id: " + appointmentId));
        
        return refundPayment(payment.getId(), request);
    }

    /**
     * Convert Payment entity to PaymentResponse
     */
    private PaymentResponse toPaymentResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setPatientId(payment.getPatient().getId());
        response.setPatientName(payment.getPatient().getFullName());
        if (payment.getAppointment() != null) {
            response.setAppointmentId(payment.getAppointment().getId());
        }
        response.setAmount(payment.getAmount());
        response.setActualAmount(payment.getActualAmount());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setStatus(payment.getStatus());
        response.setTransactionId(payment.getTransactionId());
        response.setPaymentGateway(payment.getPaymentGateway());
        response.setPaidAt(payment.getPaidAt());
        response.setRefundedAt(payment.getRefundedAt());
        response.setRefundReason(payment.getRefundReason());
        response.setNotes(payment.getNotes());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }

    /**
     * Tính số lần đặt lịch khám được xác nhận dựa trên tổng số tiền đặt cọc trong 7 ngày
     * Công thức: Tổng tiền đặt cọc trong 7 ngày / 100,000 VND
     * 
     * ⚠️ QUAN TRỌNG: PHẢI làm tròn XUỐNG (floor), KHÔNG được làm tròn lên (ceiling)
     * 
     * Lý do bảo mật kinh tế:
     * - Nếu làm tròn LÊN: 50,000 VND → 1 lần (thiệt hại cho hệ thống)
     * - Nếu làm tròn LÊN: 150,000 VND → 2 lần (thiệt hại cho hệ thống)
     * - Làm tròn XUỐNG: 150,000 VND → 1 lần (đúng, bảo vệ hệ thống)
     * 
     * Làm tròn lên sẽ tạo lỗ hổng kinh tế nghiêm trọng!
     */
    public int calculateConfirmedAppointmentsFromDeposits(Long patientId) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime now = LocalDateTime.now();
        
        // Lấy tổng số tiền đã thanh toán trong 7 ngày
        Double totalPaidAmount = paymentRepository.getTotalPaidAmountByPatientInDateRange(patientId, sevenDaysAgo, now);
        
        if (totalPaidAmount == null || totalPaidAmount < APPOINTMENT_CREDIT_AMOUNT) {
            return 0;
        }
        
        // Tính số lần đặt lịch: Tổng tiền / 100,000 VND (làm tròn XUỐNG - BẮT BUỘC)
        // Ví dụ: 150,000 VND / 100,000 = 1.5 → floor = 1 lần ✅
        // Ví dụ: 250,000 VND / 100,000 = 2.5 → floor = 2 lần ✅
        // ⚠️ KHÔNG được dùng Math.ceil() - sẽ tạo lỗ hổng kinh tế!
        int confirmedAppointments = (int) Math.floor(totalPaidAmount / APPOINTMENT_CREDIT_AMOUNT);
        
        logger.info("📊 APPOINTMENT CREDIT CALCULATION - Patient ID: {}, Total paid in 7 days: {} VND, Confirmed appointments: {} (rounded DOWN for security)", 
                patientId, totalPaidAmount, confirmedAppointments);
        
        return confirmedAppointments;
    }

    /**
     * Lấy danh sách payments của patient trong 7 ngày (cho admin review)
     */
    public List<PaymentResponse> getPatientPaymentsInLast7Days(Long patientId) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime now = LocalDateTime.now();
        
        List<Payment> payments = paymentRepository.findByPatientIdAndStatusAndCreatedAtBetween(
                patientId, "PAID", sevenDaysAgo, now);
        
        return payments.stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Tạo nội dung chuyển khoản cho QR code
     * Format: "Tên bệnh nhân - Giờ đặt lịch - Chuyển khoản"
     * 
     * Ví dụ: "NGUYEN VAN A - 15/02/2025 10:00 - Chuyển khoản"
     * 
     * Lưu ý: VNPAY có giới hạn độ dài orderInfo (thường là 255 ký tự)
     * Nếu tên quá dài, sẽ rút ngắn để đảm bảo không vượt quá giới hạn
     */
    public String generateTransferContent(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found with id: " + paymentId));
        
        // Lấy tên bệnh nhân (loại bỏ dấu và chuyển thành chữ hoa để dễ đọc khi quét QR)
        String patientName = payment.getPatient().getFullName();
        if (patientName == null || patientName.trim().isEmpty()) {
            patientName = "BENH NHAN";
        }
        // Chuyển thành chữ hoa và loại bỏ khoảng trắng thừa
        patientName = patientName.toUpperCase().trim();
        // Rút ngắn nếu quá dài (giữ lại tối đa 50 ký tự)
        if (patientName.length() > 50) {
            patientName = patientName.substring(0, 47) + "...";
        }
        
        // Lấy giờ đặt lịch từ appointment
        String appointmentTimeStr = "N/A";
        if (payment.getAppointment() != null) {
            java.time.LocalDateTime appointmentTime = payment.getAppointment().getAppointmentTime();
            if (appointmentTime != null) {
                // Format: "dd/MM/yyyy HH:mm"
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                appointmentTimeStr = appointmentTime.format(formatter);
            }
        }
        
        // Format: "Tên bệnh nhân - Giờ đặt lịch - Chuyển khoản"
        String transferContent = String.format("%s - %s - Chuyen khoan", patientName, appointmentTimeStr);
        
        // Giới hạn độ dài tối đa 255 ký tự (giới hạn của VNPAY)
        if (transferContent.length() > 255) {
            // Rút ngắn tên bệnh nhân nếu cần
            int maxNameLength = 255 - appointmentTimeStr.length() - " -  - Chuyen khoan".length();
            if (maxNameLength > 0) {
                patientName = patientName.substring(0, Math.min(maxNameLength, patientName.length()));
                transferContent = String.format("%s - %s - Chuyen khoan", patientName, appointmentTimeStr);
            } else {
                // Nếu vẫn quá dài, chỉ lấy phần cần thiết
                transferContent = transferContent.substring(0, 255);
            }
        }
        
        logger.info("📝 TRANSFER CONTENT GENERATED - Payment ID: {}, Content: {} (Length: {})", 
                paymentId, transferContent, transferContent.length());
        
        return transferContent;
    }
}
