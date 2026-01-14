# Hướng Dẫn Demo VNPAY - Demo Guide

## 🎯 Mục Đích

Hướng dẫn chi tiết để demo thanh toán VNPAY cho appointment booking.

---

## 📋 Checklist Chuẩn Bị

### ✅ Bước 1: Đăng Ký VNPAY Sandbox

1. **Truy cập**: https://sandbox.vnpayment.vn/
2. **Đăng ký tài khoản** (nếu chưa có)
3. **Đăng nhập** vào Sandbox Portal

### ✅ Bước 2: Tạo Website/App

1. Vào **"Quản lý Website/App"**
2. Click **"Thêm mới Website/App"**
3. Điền thông tin:
   ```
   Tên Website/App: Medinova Clinic Platform
   URL Website: http://localhost:8080
   IPN URL: http://localhost:8080/api/payments/vnpay/callback
   Return URL: http://localhost:3000/payment/success
   ```
4. **Lưu lại** các thông tin:
   - **TmnCode**: `YOUR_TMN_CODE`
   - **HashSecret**: `YOUR_HASH_SECRET`

### ✅ Bước 3: Cấu Hình Backend

1. **Cập nhật `application.properties`**:
   ```properties
   vnpay.tmn.code=YOUR_TMN_CODE
   vnpay.hash.secret=YOUR_HASH_SECRET
   vnpay.payment.url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
   vnpay.return.url=http://localhost:3000/payment/success
   vnpay.api.url=https://sandbox.vnpayment.vn/merchant_webapi/merchant.html
   ```

2. **Chạy migration** (nếu chưa chạy):
   ```bash
   cd Medinova-BE
   psql -U medinova_user -d medinova -f migration_add_payment_system.sql
   ```

3. **Restart backend**:
   ```bash
   mvn spring-boot:run
   ```

### ✅ Bước 4: Cấu Hình Frontend (Tùy chọn)

1. Tạo trang `/payment/success` để hiển thị kết quả thanh toán
2. Xử lý redirect từ VNPAY về frontend

---

## 🎮 Demo Flow

### 1. Tạo Appointment

```bash
POST /api/appointments
Authorization: Bearer <patient_token>

{
  "doctorId": 1,
  "clinicId": 1,
  "appointmentTime": "2025-02-15T10:00:00",
  "age": 35,
  "gender": "MALE",
  "symptoms": "Đau đầu"
}
```

**Kết quả**: Appointment được tạo, Payment tự động được tạo với status `PENDING`

---

### 2. Lấy Payment ID

```bash
GET /api/payments/appointments/{appointmentId}
Authorization: Bearer <patient_token>
```

**Kết quả**: 
```json
{
  "id": 1,
  "status": "PENDING",
  "amount": 50000.0,
  "appointmentId": 1
}
```

---

### 3. Tạo VNPAY Payment URL

```bash
POST /api/payments/{paymentId}/vnpay/create
Authorization: Bearer <patient_token>
```

**Kết quả**:
```json
{
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "paymentId": 1,
  "amount": 50000.0,
  "orderInfo": "Deposit for appointment #1"
}
```

**Frontend**: Redirect user đến `paymentUrl`

---

### 4. Thanh Toán Trên VNPAY

1. User được redirect đến VNPAY
2. Nhập thông tin thẻ test:
   ```
   Số thẻ: 9704198526191432198
   Tên chủ thẻ: NGUYEN VAN A
   Ngày hết hạn: 12/25
   CVV: 123
   ```
3. Click **"Thanh toán"**

---

### 5. VNPAY Callback (Tự Động)

VNPAY sẽ gọi:
```
POST /api/payments/vnpay/callback
```

Backend tự động:
- Xác thực signature
- Cập nhật payment status: `PENDING` → `PAID`
- Lưu transaction ID

---

### 6. VNPAY Return (Redirect về Frontend)

VNPAY redirect về:
```
GET /api/payments/vnpay/return?vnp_ResponseCode=00&...
```

**Kết quả**:
```json
{
  "success": true,
  "paymentId": 1,
  "transactionId": "12345678",
  "message": "Payment successful"
}
```

**Frontend**: Hiển thị trang success và redirect về appointment page

---

### 7. Confirm Appointment

```bash
PUT /api/appointments/{appointmentId}/confirm
Authorization: Bearer <patient_token>
```

**Kết quả**: Appointment status: `PENDING` → `PENDING` (chờ doctor confirm)

---

## 🧪 Test Cases

### Test Case 1: Thanh Toán Thành Công ✅

1. Tạo appointment
2. Tạo VNPAY payment URL
3. Thanh toán với test card thành công
4. Kiểm tra payment status = `PAID`
5. Confirm appointment → Thành công

### Test Case 2: Thanh Toán Thất Bại ❌

1. Tạo appointment
2. Tạo VNPAY payment URL
3. Thanh toán với test card thất bại (hoặc cancel)
4. Kiểm tra payment status = `FAILED`
5. Confirm appointment → Lỗi: "Payment deposit required"

### Test Case 3: Hủy Appointment → Refund

1. Tạo appointment và thanh toán thành công
2. Hủy appointment
3. Kiểm tra payment status = `REFUNDED`

---

## 🔧 Troubleshooting

### Lỗi: "Invalid signature"

**Nguyên nhân**: HashSecret không đúng hoặc params không khớp

**Giải pháp**:
1. Kiểm tra `vnpay.hash.secret` trong `application.properties`
2. Đảm bảo params được sắp xếp đúng thứ tự
3. Kiểm tra encoding (UTF-8)

### Lỗi: "Payment not found"

**Nguyên nhân**: Payment ID không tồn tại

**Giải pháp**:
1. Kiểm tra payment đã được tạo chưa
2. Kiểm tra `vnp_TxnRef` trong VNPAY response

### Lỗi: Callback không được gọi

**Nguyên nhân**: IPN URL không đúng hoặc server không accessible

**Giải pháp**:
1. Kiểm tra IPN URL trong VNPAY portal
2. Sử dụng ngrok để expose localhost (cho demo):
   ```bash
   ngrok http 8080
   ```
3. Cập nhật IPN URL với ngrok URL

---

## 🌐 Expose Localhost với Ngrok (Cho Demo)

### Cài Đặt Ngrok

```bash
# Download từ https://ngrok.com/
# Hoặc với Homebrew (Mac):
brew install ngrok

# Hoặc với Chocolatey (Windows):
choco install ngrok
```

### Chạy Ngrok

```bash
ngrok http 8080
```

**Kết quả**:
```
Forwarding  https://abc123.ngrok.io -> http://localhost:8080
```

### Cập Nhật VNPAY Config

1. **IPN URL**: `https://abc123.ngrok.io/api/payments/vnpay/callback`
2. **Return URL**: `https://abc123.ngrok.io/api/payments/vnpay/return`

### Cập Nhật application.properties

```properties
vnpay.return.url=https://abc123.ngrok.io/api/payments/vnpay/return
```

---

## 📊 Test Cards (VNPAY Sandbox)

### Thẻ Thành Công ✅
```
Số thẻ: 9704198526191432198
Tên: NGUYEN VAN A
Ngày hết hạn: 12/25
CVV: 123
OTP: 123456
```

### Thẻ Thất Bại ❌
```
Số thẻ: 9704198526191432199
Tên: NGUYEN VAN A
Ngày hết hạn: 12/25
CVV: 123
```

---

## 🎯 Demo Script

### 1. Setup
```bash
# 1. Chạy database
docker-compose up -d postgres

# 2. Chạy backend
cd Medinova-BE
mvn spring-boot:run

# 3. (Tùy chọn) Expose với ngrok
ngrok http 8080
```

### 2. Demo Flow

1. **Đăng nhập** (Patient)
2. **Tạo appointment**
3. **Xem payment** → Status: PENDING
4. **Tạo VNPAY URL** → Redirect đến VNPAY
5. **Thanh toán** với test card
6. **Kiểm tra payment** → Status: PAID
7. **Confirm appointment** → Thành công
8. **Kiểm tra appointment** → Status: PENDING (chờ doctor)

---

## 📝 Notes

1. **Sandbox chỉ dùng để test** - Không có tiền thật
2. **HashSecret phải bảo mật** - Không commit vào Git
3. **IPN URL phải accessible** - Sử dụng ngrok cho localhost
4. **Test kỹ callback** - Đảm bảo xử lý đúng các trường hợp

---

## 🚀 Production Checklist

- [ ] Đăng ký tài khoản VNPAY Production
- [ ] Cập nhật config với production URLs
- [ ] Cấu hình IP whitelist
- [ ] Sử dụng HTTPS cho Return URL và IPN URL
- [ ] Test kỹ trước khi go-live
- [ ] Setup monitoring và logging
- [ ] Backup và recovery plan

---

## 📚 Tài Liệu Tham Khảo

- VNPAY Sandbox: https://sandbox.vnpayment.vn/
- VNPAY Documentation: https://sandbox.vnpayment.vn/apis/
- Ngrok: https://ngrok.com/
