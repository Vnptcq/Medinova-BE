# Hướng Dẫn Tích Hợp VNPAY - VNPAY Integration Guide

## 🎯 Mục Đích

Tích hợp VNPAY để demo thanh toán cọc 50,000 VND cho appointment booking.

---

## 📋 Bước 1: Đăng Ký Tài Khoản VNPAY Sandbox

### 1.1. Đăng Ký Tài Khoản

1. Truy cập: https://sandbox.vnpayment.vn/
2. Đăng ký tài khoản mới (nếu chưa có)
3. Đăng nhập vào Sandbox Portal

### 1.2. Tạo Website/App

1. Vào **"Quản lý Website/App"**
2. Click **"Thêm mới Website/App"**
3. Điền thông tin:
   - **Tên Website/App**: Medinova Clinic Platform
   - **URL Website**: `http://localhost:8080` (hoặc domain của bạn)
   - **IPN URL**: `http://localhost:8080/api/payments/vnpay/callback` (callback URL)
   - **Return URL**: `http://localhost:3000/payment/success` (frontend success page)

### 1.3. Lấy Thông Tin Kết Nối

Sau khi tạo Website/App, bạn sẽ nhận được:

- **TmnCode** (Terminal Code): Mã website/merchant
- **HashSecret**: Secret key để tạo chữ ký
- **Payment URL**: 
  - Sandbox: `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html`
  - Production: `https://vnpayment.vn/paymentv2/vpcpay.html`

### 1.4. Lưu Thông Tin

Lưu lại các thông tin này để cấu hình trong `application.properties`:
```
VNPAY_TMN_CODE=YOUR_TMN_CODE
VNPAY_HASH_SECRET=YOUR_HASH_SECRET
VNPAY_PAYMENT_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:3000/payment/success
```

---

## 🔧 Bước 2: Cấu Hình Dự Án

### 2.1. Thêm Dependency (Nếu Cần)

VNPAY không cần SDK riêng, chỉ cần HTTP client (Spring Boot đã có sẵn).

### 2.2. Cấu Hình application.properties

Thêm vào `application.properties`:

```properties
# VNPAY Configuration
vnpay.tmn.code=YOUR_TMN_CODE
vnpay.hash.secret=YOUR_HASH_SECRET
vnpay.payment.url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.return.url=http://localhost:3000/payment/success
vnpay.api.url=https://sandbox.vnpayment.vn/merchant_webapi/merchant.html
```

---

## 💻 Bước 3: Tạo VNPAY Service

Tạo file `VNPayService.java` để xử lý:
- Tạo payment URL
- Xác thực callback từ VNPAY
- Xử lý kết quả thanh toán

---

## 🎮 Bước 4: Demo Flow

### 4.1. Luồng Thanh Toán

```
1. Patient tạo appointment
   └─> Payment được tạo (PENDING)

2. Patient click "Thanh toán"
   └─> Gọi API: POST /api/payments/{paymentId}/vnpay/create
   └─> Nhận payment URL từ VNPAY

3. Frontend redirect đến VNPAY
   └─> Patient thanh toán trên VNPAY

4. VNPAY callback về backend
   └─> POST /api/payments/vnpay/callback
   └─> Xác thực và cập nhật payment status

5. VNPAY redirect về frontend
   └─> GET /api/payments/vnpay/return
   └─> Frontend hiển thị kết quả
```

### 4.2. Test Cases

1. ✅ Tạo appointment → Payment PENDING
2. ✅ Tạo VNPAY payment URL → Redirect đến VNPAY
3. ✅ Thanh toán thành công → Payment PAID
4. ✅ Thanh toán thất bại → Payment FAILED
5. ✅ Callback từ VNPAY → Xác thực và cập nhật

---

## 🔐 Bước 5: Security

### 5.1. Hash Secret

- **KHÔNG** commit Hash Secret vào Git
- Sử dụng environment variables hoặc secrets management
- Trong production, sử dụng Vault hoặc AWS Secrets Manager

### 5.2. IP Whitelist (Production)

- Trong production, cấu hình IP whitelist trong VNPAY portal
- Chỉ cho phép IP của server backend

---

## 📝 Bước 6: Testing

### 6.1. Test Cards (Sandbox)

VNPAY Sandbox cung cấp test cards:
- **Thẻ thành công**: 9704198526191432198
- **Thẻ thất bại**: 9704198526191432199
- **CVV**: 123
- **Ngày hết hạn**: Bất kỳ ngày trong tương lai

### 6.2. Test Flow

1. Tạo appointment
2. Tạo payment URL
3. Sử dụng test card để thanh toán
4. Kiểm tra callback và payment status

---

## 🚀 Bước 7: Production

### 7.1. Chuyển Sang Production

1. Đăng ký tài khoản VNPAY Production
2. Cập nhật config:
   - `vnpay.payment.url=https://vnpayment.vn/paymentv2/vpcpay.html`
   - `vnpay.api.url=https://vnpayment.vn/merchant_webapi/merchant.html`
3. Cập nhật Return URL và IPN URL với domain thật
4. Test kỹ trước khi go-live

---

## 📚 Tài Liệu Tham Khảo

- VNPAY Documentation: https://sandbox.vnpayment.vn/apis/
- VNPAY Integration Guide: https://sandbox.vnpayment.vn/apis/docs/huong-dan-tich-hop/
- VNPAY Test Cards: https://sandbox.vnpayment.vn/apis/docs/test-cards/

---

## ⚠️ Lưu Ý

1. **Sandbox chỉ dùng để test** - Không có tiền thật
2. **Hash Secret phải bảo mật** - Không commit vào Git
3. **Return URL và IPN URL** phải là HTTPS trong production
4. **Test kỹ callback** - Đảm bảo xử lý đúng các trường hợp
