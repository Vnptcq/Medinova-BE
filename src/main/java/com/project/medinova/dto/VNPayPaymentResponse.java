package com.project.medinova.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "VNPAY payment URL response")
public class VNPayPaymentResponse {
    
    @Schema(description = "Payment URL to redirect to VNPAY", example = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...")
    private String paymentUrl;
    
    @Schema(description = "Payment ID", example = "1")
    private Long paymentId;
    
    @Schema(description = "Amount in VND", example = "50000.0")
    private Double amount;
    
    @Schema(description = "Order info", example = "Deposit for appointment #1")
    private String orderInfo;
    
    @Schema(description = "⚠️ QUAN TRỌNG: Warning message về tên tài khoản khi thanh toán", 
            example = "⚠️ QUAN TRỌNG: Khi thanh toán qua VNPAY, bạn BẮT BUỘC phải sử dụng tên tài khoản ĐÚNG với tên tài khoản ngân hàng của bạn.")
    private String warningMessage;
}
