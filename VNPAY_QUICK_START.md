# VNPAY Quick Start - Bắt Đầu Nhanh

## ⚡ 5 Bước Để Demo VNPAY

### 1️⃣ Đăng Ký VNPAY Sandbox (5 phút)

1. Truy cập: https://sandbox.vnpayment.vn/
2. Đăng ký/Đăng nhập
3. Tạo Website/App:
   - Tên: `Medinova Clinic`
   - IPN URL: `http://localhost:8080/api/payments/vnpay/callback`
   - Return URL: `http://localhost:3000/payment/success`
4. Copy **TmnCode** và **HashSecret**

### 2️⃣ Cấu Hình Backend (2 phút)

Cập nhật `application.properties`:
```properties
vnpay.tmn.code=YOUR_TMN_CODE
vnpay.hash.secret=YOUR_HASH_SECRET
vnpay.payment.url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.return.url=http://localhost:3000/payment/success
```

### 3️⃣ Chạy Migration (1 phút)

```bash
cd Medinova-BE
psql -U medinova_user -d medinova -f migration_add_payment_system.sql
```

### 4️⃣ Restart Backend (1 phút)

```bash
mvn spring-boot:run
```

### 5️⃣ Test Thanh Toán (2 phút)

1. Tạo appointment → Payment PENDING
2. Gọi API: `POST /api/payments/{paymentId}/vnpay/create`
3. Redirect đến VNPAY URL
4. Thanh toán với test card: `9704198526191432198`
5. Kiểm tra payment → Status: PAID ✅

---

## 🎯 Test Card

```
Số thẻ: 9704198526191432198
Tên: NGUYEN VAN A
Ngày hết hạn: 12/25
CVV: 123
OTP: 123456
```

---

## 📞 API Endpoints

- `POST /api/payments/{paymentId}/vnpay/create` - Tạo payment URL
- `POST /api/payments/vnpay/callback` - VNPAY callback (tự động)
- `GET /api/payments/vnpay/return` - Return URL

---

## ⚠️ Lưu Ý

- **Sandbox**: Chỉ để test, không có tiền thật
- **Localhost**: Cần ngrok để VNPAY gọi callback
- **HashSecret**: Không commit vào Git

---

## 🆘 Cần Giúp?

Xem chi tiết tại:
- [VNPAY_INTEGRATION_GUIDE.md](./VNPAY_INTEGRATION_GUIDE.md)
- [VNPAY_DEMO_GUIDE.md](./VNPAY_DEMO_GUIDE.md)
