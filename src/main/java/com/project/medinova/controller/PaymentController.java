package com.project.medinova.controller;

import com.project.medinova.dto.*;
import com.project.medinova.service.PaymentService;
import com.project.medinova.service.VNPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment", description = "Payment management APIs for appointment deposits")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private static final long MINIMUM_ACCEPTED_AMOUNT = 100000L; // 100,000 VND

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private VNPayService vnPayService;

    @PostMapping("/appointments/{appointmentId}")
    @Operation(summary = "Create payment for appointment", description = "Create a payment record for appointment deposit (50,000 VND). Patient only.")
    public ResponseEntity<PaymentResponse> createPaymentForAppointment(@PathVariable Long appointmentId) {
        PaymentResponse response = paymentService.createPaymentForAppointment(appointmentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{paymentId}/process")
    @Operation(summary = "Process payment", description = "Process/confirm payment deposit. Patient only.")
    public ResponseEntity<PaymentResponse> processPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(paymentId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/confirm")
    @Operation(summary = "Confirm payment from gateway", description = "Confirm payment from payment gateway callback. Internal use.")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody ProcessPaymentRequest request) {
        PaymentResponse response = paymentService.confirmPayment(paymentId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Refund payment", description = "Refund payment when appointment is cancelled. Patient/Admin only.")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody RefundPaymentRequest request) {
        PaymentResponse response = paymentService.refundPayment(paymentId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID", description = "Get payment details by ID")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long paymentId) {
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-payments")
    @Operation(summary = "Get my payments", description = "Get all payments of current user. Patient only.")
    public ResponseEntity<List<PaymentResponse>> getMyPayments() {
        List<PaymentResponse> responses = paymentService.getMyPayments();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/appointments/{appointmentId}")
    @Operation(summary = "Get payment by appointment ID", description = "Get payment details by appointment ID")
    public ResponseEntity<PaymentResponse> getPaymentByAppointmentId(@PathVariable Long appointmentId) {
        PaymentResponse response = paymentService.getPaymentByAppointmentId(appointmentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/vnpay/create")
    @Operation(summary = "Create VNPAY payment URL", description = "Create VNPAY payment URL for deposit payment. Patient only.")
    public ResponseEntity<VNPayPaymentResponse> createVNPayPaymentUrl(
            @PathVariable Long paymentId,
            HttpServletRequest request) {
        PaymentResponse payment = paymentService.getPaymentById(paymentId);
        
        // Lấy IP address từ request
        String ipAddress = getClientIpAddress(request);
        
        // Tạo nội dung chuyển khoản: "Tên bệnh nhân - Giờ đặt lịch - Chuyển khoản"
        String orderInfo = paymentService.generateTransferContent(paymentId);
        
        String paymentUrl = vnPayService.createPaymentUrl(paymentId, payment.getAmount().longValue(), orderInfo);
        
        VNPayPaymentResponse response = new VNPayPaymentResponse();
        response.setPaymentUrl(paymentUrl);
        response.setPaymentId(paymentId);
        response.setAmount(payment.getAmount());
        response.setOrderInfo(orderInfo);
        
        // ⚠️ QUAN TRỌNG: Warning message về tên tài khoản và số tiền
        response.setWarningMessage(
            "⚠️ QUAN TRỌNG - VUI LÒNG ĐỌC KỸ:\n\n" +
            "1. TÊN TÀI KHOẢN: Bạn BẮT BUỘC phải sử dụng tên tài khoản ĐÚNG với tên tài khoản ngân hàng của bạn. " +
            "Nếu tên không khớp, giao dịch có thể bị từ chối hoặc không được xác nhận.\n\n" +
            "2. SỐ TIỀN: Số tiền tối thiểu để chấp nhận là 100,000 VND. " +
            "Nếu bạn chuyển thừa, admin sẽ xem log và xử lý sau.\n\n" +
            "3. CHUYỂN THIẾU: Nếu chuyển thiếu (< 100,000 VND), giao dịch sẽ KHÔNG được xác nhận. " +
            "Số lần đặt lịch khám được xác nhận = Tổng số tiền đặt cọc trong 7 ngày / 100,000 VND.\n\n" +
            "4. VẤN ĐỀ KHÁC: Mọi vấn đề liên quan đến chuyển khoản nhầm, chuyển thừa, hoặc chuyển thiếu, " +
            "vui lòng liên hệ admin để được hỗ trợ. Admin có thể xem log chi tiết để kiểm tra."
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/vnpay/callback")
    @Operation(summary = "VNPAY callback", description = "Handle callback from VNPAY after payment. This endpoint is called by VNPAY server.")
    public ResponseEntity<String> vnpayCallback(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            params.put(paramName, request.getParameter(paramName));
        }
        
        // Xác thực callback
        if (!vnPayService.verifyPayment(params)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }
        
        // Lấy payment ID
        Long paymentId = vnPayService.getPaymentIdFromResponse(params);
        if (paymentId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payment ID");
        }
        
        // Kiểm tra thanh toán thành công
        boolean isSuccess = vnPayService.isPaymentSuccess(params);
        String transactionId = vnPayService.getTransactionId(params);
        String errorMessage = vnPayService.getErrorMessage(params);
        
        // ⚠️ QUAN TRỌNG: Xác nhận số tiền từ VNPAY callback
        Long callbackAmount = vnPayService.getAmountFromResponse(params);
        PaymentResponse payment = paymentService.getPaymentById(paymentId);
        
        if (callbackAmount != null && payment != null) {
            // Log chi tiết cho admin
            logger.info("💰 PAYMENT CALLBACK - Payment ID: {}, Patient ID: {}, Expected: {} VND, Received: {} VND", 
                    paymentId, payment.getPatientId(), payment.getAmount(), callbackAmount);
            
            // Kiểm tra số tiền: Chấp nhận nếu >= 100,000 VND
            if (callbackAmount < MINIMUM_ACCEPTED_AMOUNT) {
                logger.warn("⚠️ PAYMENT REJECTED - Amount too low! Payment ID: {}, Expected: {} VND, Received: {} VND (Minimum: {} VND)", 
                        paymentId, payment.getAmount(), callbackAmount, MINIMUM_ACCEPTED_AMOUNT);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Payment amount is too low. Minimum accepted: " + MINIMUM_ACCEPTED_AMOUNT + " VND");
            }
            
            // Nếu số tiền > số tiền yêu cầu, log warning cho admin
            if (callbackAmount > payment.getAmount().longValue()) {
                logger.warn("⚠️ PAYMENT EXCESS - Payment ID: {}, Patient ID: {}, Expected: {} VND, Received: {} VND (Excess: {} VND). Admin should review transaction log.", 
                        paymentId, payment.getPatientId(), payment.getAmount(), callbackAmount, callbackAmount - payment.getAmount().longValue());
            }
        }
        
        // Cập nhật payment
        try {
            if (isSuccess) {
                ProcessPaymentRequest processRequest = new ProcessPaymentRequest();
                processRequest.setTransactionId(transactionId);
                processRequest.setGatewayResponse("VNPAY_SUCCESS");
                // Lưu số tiền thực tế nhận được từ callback
                if (callbackAmount != null) {
                    processRequest.setActualAmount(callbackAmount.doubleValue());
                }
                paymentService.confirmPayment(paymentId, processRequest);
            } else {
                // Cập nhật payment failed - use the payment variable already fetched above
                CreatePaymentRequest updateRequest = new CreatePaymentRequest();
                updateRequest.setAppointmentId(payment.getAppointmentId());
                updateRequest.setPaymentMethod("VNPAY");
                updateRequest.setPaymentGateway("VNPAY");
                updateRequest.setTransactionId(transactionId);
                updateRequest.setNotes("Payment failed: " + errorMessage);
                // Note: Cần thêm method để update payment status to FAILED
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing payment: " + e.getMessage());
        }
        
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/vnpay/return")
    @Operation(summary = "VNPAY return URL", description = "Handle return from VNPAY after payment. Redirects to frontend.")
    public ResponseEntity<Map<String, Object>> vnpayReturn(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            params.put(paramName, request.getParameter(paramName));
        }
        
        Map<String, Object> response = new HashMap<>();
        
        // Xác thực
        if (!vnPayService.verifyPayment(params)) {
            response.put("success", false);
            response.put("message", "Invalid signature");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Lấy payment ID
        Long paymentId = vnPayService.getPaymentIdFromResponse(params);
        if (paymentId == null) {
            response.put("success", false);
            response.put("message", "Invalid payment ID");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Kiểm tra thanh toán
        boolean isSuccess = vnPayService.isPaymentSuccess(params);
        String transactionId = vnPayService.getTransactionId(params);
        
        response.put("success", isSuccess);
        response.put("paymentId", paymentId);
        response.put("transactionId", transactionId);
        response.put("message", isSuccess ? "Payment successful" : "Payment failed");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patients/{patientId}/payments-last-7-days")
    @Operation(summary = "Get patient payments in last 7 days (Admin only)", 
               description = "Get all payments of a patient in the last 7 days for admin review. Used to check for excess payments or issues.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponse>> getPatientPaymentsInLast7Days(@PathVariable Long patientId) {
        List<PaymentResponse> payments = paymentService.getPatientPaymentsInLast7Days(patientId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/patients/{patientId}/confirmed-appointments-count")
    @Operation(summary = "Calculate confirmed appointments from deposits (Admin only)", 
               description = "Calculate number of confirmed appointments based on total deposit amount in last 7 days. Formula: Total amount / 100,000 VND")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> calculateConfirmedAppointmentsFromDeposits(@PathVariable Long patientId) {
        int count = paymentService.calculateConfirmedAppointmentsFromDeposits(patientId);
        Map<String, Object> response = new HashMap<>();
        response.put("patientId", patientId);
        response.put("confirmedAppointmentsCount", count);
        response.put("formula", "Total deposit amount in last 7 days / 100,000 VND");
        response.put("note", "This is used when patient transfers insufficient amount. Each 100,000 VND = 1 confirmed appointment.");
        return ResponseEntity.ok(response);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
