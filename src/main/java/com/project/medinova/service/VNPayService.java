package com.project.medinova.service;

import com.project.medinova.config.VNPayConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VNPayService {

    @Autowired
    private VNPayConfig vnPayConfig;

    /**
     * Tạo payment URL để redirect đến VNPAY
     * 
     * @param paymentId Payment ID
     * @param amount Số tiền (đã là VND, không cần nhân 100)
     * @param orderInfo Nội dung chuyển khoản (sẽ hiển thị khi quét QR code)
     */
    public String createPaymentUrl(Long paymentId, Long amount, String orderInfo) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TmnCode = vnPayConfig.getTmnCode();
        String vnp_Amount = String.valueOf(amount * 100); // VNPAY yêu cầu amount * 100
        String vnp_CurrCode = "VND";
        String vnp_TxnRef = String.valueOf(paymentId);
        String vnp_OrderInfo = orderInfo;
        String vnp_OrderType = "other";
        String vnp_Locale = "vn";
        String vnp_ReturnUrl = vnPayConfig.getReturnUrl();
        String vnp_IpAddr = "127.0.0.1"; // Sẽ lấy từ request trong controller
        String vnp_CreateDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", vnp_Amount);
        vnp_Params.put("vnp_CurrCode", vnp_CurrCode);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", vnp_OrderType);
        vnp_Params.put("vnp_Locale", vnp_Locale);
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        // Sắp xếp params theo thứ tự alphabet
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        
        String queryUrl = query.toString();
        String vnp_SecureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        
        String paymentUrl = vnPayConfig.getPaymentUrl() + "?" + queryUrl;
        return paymentUrl;
    }

    /**
     * Xác thực callback từ VNPAY
     */
    public boolean verifyPayment(Map<String, String> params) {
        String vnp_SecureHash = params.get("vnp_SecureHash");
        if (vnp_SecureHash == null || vnp_SecureHash.isEmpty()) {
            return false;
        }
        
        // Loại bỏ vnp_SecureHash khỏi params để tính hash
        params.remove("vnp_SecureHash");
        
        // Sắp xếp params
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }
        
        String calculatedHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        return calculatedHash.equals(vnp_SecureHash);
    }

    /**
     * Tính HMAC SHA512
     */
    private String hmacSHA512(String key, String data) {
        try {
            Mac hmacSHA512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmacSHA512.init(secretKey);
            byte[] hashBytes = hmacSHA512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculating HMAC SHA512", e);
        }
    }

    /**
     * Lấy payment ID từ VNPAY response
     */
    public Long getPaymentIdFromResponse(Map<String, String> params) {
        String vnp_TxnRef = params.get("vnp_TxnRef");
        if (vnp_TxnRef == null || vnp_TxnRef.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(vnp_TxnRef);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Kiểm tra thanh toán thành công
     */
    public boolean isPaymentSuccess(Map<String, String> params) {
        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        return "00".equals(vnp_ResponseCode);
    }

    /**
     * Lấy transaction ID từ VNPAY
     */
    public String getTransactionId(Map<String, String> params) {
        return params.get("vnp_TransactionNo");
    }

    /**
     * Lấy số tiền từ VNPAY callback (đã được nhân 100)
     * Cần chia cho 100 để lấy số tiền thực tế
     */
    public Long getAmountFromResponse(Map<String, String> params) {
        String vnp_Amount = params.get("vnp_Amount");
        if (vnp_Amount == null || vnp_Amount.isEmpty()) {
            return null;
        }
        try {
            // VNPAY trả về amount * 100, cần chia cho 100
            return Long.parseLong(vnp_Amount) / 100;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Lấy tên ngân hàng từ VNPAY callback (nếu có)
     */
    public String getBankCode(Map<String, String> params) {
        return params.get("vnp_BankCode");
    }

    /**
     * Lấy thông tin thẻ từ VNPAY callback (nếu có)
     * Lưu ý: VNPAY không trả về tên chủ thẻ trong callback vì lý do bảo mật
     */
    public String getCardType(Map<String, String> params) {
        return params.get("vnp_CardType");
    }

    /**
     * Lấy thông tin lỗi (nếu có)
     */
    public String getErrorMessage(Map<String, String> params) {
        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        String vnp_TransactionStatus = params.get("vnp_TransactionStatus");
        
        if ("00".equals(vnp_ResponseCode) && "00".equals(vnp_TransactionStatus)) {
            return null; // Success
        }
        
        // Map response codes to error messages
        Map<String, String> errorMessages = new HashMap<>();
        errorMessages.put("07", "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).");
        errorMessages.put("09", "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking");
        errorMessages.put("10", "Xác thực giao dịch không thành công do: Quá 3 lần nhập sai mật khẩu, thẻ/tài khoản bị khóa.");
        errorMessages.put("11", "Đã hết hạn chờ thanh toán. Vui lòng thử lại.");
        errorMessages.put("12", "Thẻ/Tài khoản bị khóa.");
        errorMessages.put("13", "Nhập sai mật khẩu xác thực giao dịch (OTP).");
        errorMessages.put("51", "Tài khoản không đủ số dư để thực hiện giao dịch.");
        errorMessages.put("65", "Tài khoản đã vượt quá hạn mức giao dịch trong ngày.");
        errorMessages.put("75", "Ngân hàng thanh toán đang bảo trì.");
        errorMessages.put("79", "Nhập sai mật khẩu đăng nhập Internet Banking.");
        errorMessages.put("99", "Lỗi không xác định.");
        
        return errorMessages.getOrDefault(vnp_ResponseCode, "Lỗi không xác định. Mã lỗi: " + vnp_ResponseCode);
    }
}
