# Hệ Thống Thanh Toán Cọc - Payment System

## 🎯 Mục Đích

Hệ thống thanh toán cọc được triển khai để **chống abuse và spam** trong việc đặt lịch khám.

### Vấn Đề
- Hacker hoặc đối thủ có thể tạo nhiều tài khoản và đặt lịch liên tục
- Guest có thể spam đặt lịch mà không có nhu cầu thực sự
- Điều này có thể làm sập hệ thống

### Giải Pháp
- Yêu cầu bệnh nhân **cọc 50,000 VND** mỗi lần đặt lịch
- Chỉ những người thực sự có nhu cầu mới thanh toán
- Tự động hoàn tiền khi hủy appointment

---

## 📊 Kiến Trúc

### Entities

1. **Payment**
   - Lưu thông tin thanh toán cọc
   - Liên kết 1-1 với Appointment
   - Status: PENDING → PAID → REFUNDED (nếu hủy)

2. **Appointment**
   - Thêm trường `depositAmount` (50,000 VND)
   - Liên kết với Payment qua `payment` field

### Luồng Thanh Toán

```
1. Patient tạo appointment
   └─> Payment tự động được tạo (status: PENDING)

2. Patient thanh toán cọc 50,000 VND
   └─> Payment: PENDING → PAID

3. Patient confirm appointment
   └─> Kiểm tra payment đã PAID
   └─> Nếu chưa PAID → Lỗi

4. Hủy appointment
   └─> Payment: PAID → REFUNDED (tự động)
```

---

## 🔌 API Endpoints

### Payment APIs

| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| POST | `/api/payments/appointments/{appointmentId}` | Tạo payment cho appointment | PATIENT |
| POST | `/api/payments/{paymentId}/process` | Thanh toán cọc | PATIENT |
| POST | `/api/payments/{paymentId}/confirm` | Xác nhận từ payment gateway | INTERNAL |
| POST | `/api/payments/{paymentId}/refund` | Hoàn tiền | PATIENT/ADMIN |
| GET | `/api/payments/{paymentId}` | Lấy payment theo ID | PATIENT/DOCTOR/ADMIN |
| GET | `/api/payments/my-payments` | Lấy payments của mình | PATIENT |
| GET | `/api/payments/appointments/{appointmentId}` | Lấy payment theo appointment | PATIENT/DOCTOR/ADMIN |

### Request/Response Examples

#### Tạo Payment (tự động khi tạo appointment)
```json
POST /api/payments/appointments/1
Response:
{
  "id": 1,
  "patientId": 1,
  "appointmentId": 1,
  "amount": 50000.0,
  "paymentMethod": "BANK_TRANSFER",
  "status": "PENDING",
  "createdAt": "2025-01-15T10:00:00"
}
```

#### Thanh Toán Cọc
```json
POST /api/payments/1/process
Request:
{
  "appointmentId": 1,
  "paymentMethod": "BANK_TRANSFER",
  "transactionId": "TXN123456789",
  "paymentGateway": "VNPAY",
  "notes": "Payment via VNPAY"
}
Response:
{
  "id": 1,
  "status": "PAID",
  "paidAt": "2025-01-15T10:05:00",
  "transactionId": "TXN123456789"
}
```

#### Hoàn Tiền
```json
POST /api/payments/1/refund
Request:
{
  "reason": "Appointment cancelled by patient",
  "notes": "Automatic refund"
}
Response:
{
  "id": 1,
  "status": "REFUNDED",
  "refundedAt": "2025-01-15T11:00:00",
  "refundReason": "Appointment cancelled by patient"
}
```

---

## 🔄 Tích Hợp Với Appointment Service

### Tự Động Tạo Payment

Khi patient tạo appointment, payment tự động được tạo:

```java
// AppointmentService.createAppointment()
Appointment savedAppointment = appointmentRepository.save(appointment);

// Tự động tạo payment
paymentService.createPaymentForAppointment(savedAppointment.getId());
```

### Yêu Cầu Payment Trước Khi Confirm

Khi patient confirm appointment, hệ thống kiểm tra payment đã PAID:

```java
// AppointmentService.confirmAppointment()
if (!paymentService.isPaymentPaid(appointmentId)) {
    throw new BadRequestException("Payment deposit (50,000 VND) is required before confirming appointment");
}
```

### Tự Động Refund Khi Hủy

Khi hủy appointment, payment tự động được refund:

```java
// AppointmentService.updateAppointmentStatus()
if ("CANCELLED".equals(request.getStatus())) {
    if (paymentService.isPaymentPaid(id)) {
        RefundPaymentRequest refundRequest = new RefundPaymentRequest();
        refundRequest.setReason("Appointment cancelled by patient");
        paymentService.refundPaymentByAppointmentId(id, refundRequest);
    }
}
```

---

## 💾 Database Schema

### Payments Table

```sql
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    appointment_id BIGINT UNIQUE,
    amount DOUBLE PRECISION NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    transaction_id VARCHAR(255),
    payment_gateway VARCHAR(50),
    paid_at TIMESTAMP,
    refunded_at TIMESTAMP,
    refund_reason TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_patient FOREIGN KEY (patient_id) REFERENCES users(id),
    CONSTRAINT fk_payment_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED', 'CANCELLED'))
);
```

### Appointments Table (Updated)

```sql
ALTER TABLE appointments 
ADD COLUMN deposit_amount DOUBLE PRECISION;
```

---

## 🚀 Migration

Chạy migration script:

```bash
psql -U medinova_user -d medinova -f migration_add_payment_system.sql
```

Hoặc sử dụng script có sẵn:

```bash
cd Medinova-BE
bash run_migration.sh migration_add_payment_system.sql
```

---

## ⚙️ Configuration

### Deposit Amount

Mặc định: **50,000 VND**

Có thể thay đổi trong `PaymentService`:

```java
private static final Double DEPOSIT_AMOUNT = 50000.0;
```

---

## 🔒 Security

1. **Authentication**: Tất cả endpoints yêu cầu JWT token
2. **Authorization**: 
   - Patient chỉ có thể xem/thanh toán payment của mình
   - Admin/Doctor có thể xem tất cả payments
3. **Validation**: 
   - Kiểm tra payment thuộc về patient hiện tại
   - Kiểm tra appointment ID khớp
   - Kiểm tra status hợp lệ

---

## 📝 Status Flow

```
PENDING (Payment được tạo)
    ↓
PAID (Patient thanh toán)
    ↓
REFUNDED (Nếu hủy appointment)
```

---

## 🧪 Testing

### Test Cases

1. ✅ Tạo appointment → Payment tự động được tạo (PENDING)
2. ✅ Thanh toán cọc → Payment chuyển sang PAID
3. ✅ Confirm appointment (chưa thanh toán) → Lỗi
4. ✅ Confirm appointment (đã thanh toán) → Thành công
5. ✅ Hủy appointment → Payment tự động REFUNDED
6. ✅ Doctor reject appointment → Payment tự động REFUNDED
7. ✅ Doctor cancel appointment → Payment tự động REFUNDED

---

## 📚 Tài Liệu Liên Quan

- [PATIENT_APPOINTMENT_FLOW.md](./PATIENT_APPOINTMENT_FLOW.md) - Luồng đặt lịch chi tiết
- [APPOINTMENT_FLOW_ANALYSIS.md](./APPOINTMENT_FLOW_ANALYSIS.md) - Phân tích luồng appointment
- [BE_DOCUMENTATION.md](./BE_DOCUMENTATION.md) - Tài liệu backend tổng quan
