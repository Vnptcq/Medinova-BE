# Luồng Đăng Ký Lịch Hẹn - Hướng Dẫn Chi Tiết Cho Bệnh Nhân

## 🎯 Tổng Quan

Luồng đăng ký lịch hẹn gồm **5 bước chính** (đã đơn giản hóa):

```
1. Đăng nhập/Đăng ký
2. Tìm kiếm và chọn bác sĩ (từ lịch rảnh)
3. Tạo appointment (giữ slot 5 phút)
4. Thanh toán cọc 50,000 VND qua VNPAY
5. Xác nhận appointment (trong 5 phút)
   └─> TỰ ĐỘNG CONFIRMED ✅
   └─> Sẵn sàng cho ngày khám
```

**🎉 Không cần chờ bác sĩ xác nhận nữa!**

---

## 📝 Luồng Chi Tiết Từng Bước

### **BƯỚC 1: Đăng Nhập/Đăng Ký** 🔐

**Yêu cầu**: Bệnh nhân phải có tài khoản để sử dụng dịch vụ

#### 1.1. Đăng Ký (Nếu chưa có tài khoản)

**Endpoint**: `POST /api/auth/register`

**Request**:
```json
{
  "email": "patient@example.com",
  "password": "password123",
  "fullName": "Nguyễn Văn A",
  "phone": "0123456789",
  "role": "PATIENT"
}
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "patient@example.com",
    "fullName": "Nguyễn Văn A",
    "role": "PATIENT"
  }
}
```

**Lưu token** để sử dụng cho các request sau.

#### 1.2. Đăng Nhập (Nếu đã có tài khoản)

**Endpoint**: `POST /api/auth/login`

**Request**:
```json
{
  "email": "patient@example.com",
  "password": "password123"
}
```

**Response**: Tương tự như đăng ký

---

### **BƯỚC 2: Tìm Kiếm và Chọn Bác Sĩ** 🔍

**Endpoint**: `GET /api/doctors/search?q={tên_bác_sĩ}&clinicId={id_phòng_khám}`

**Headers**:
```
Authorization: Bearer <token>
```

**Request Example**:
```bash
GET /api/doctors/search?q=Nguyễn&clinicId=1&page=0&size=10
```

**Response**:
```json
{
  "content": [
    {
      "id": 1,
      "user": {
        "fullName": "BS. Nguyễn Văn B",
        "email": "doctor@example.com"
      },
      "department": "CARDIOLOGY",
      "clinic": {
        "id": 1,
        "name": "Phòng khám Đa khoa Medinova"
      },
      "status": "APPROVED"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

**Lưu lại**: `doctorId` và `clinicId` để tạo appointment

---

### **BƯỚC 3: Tạo Appointment (Giữ Slot 5 Phút)** ⏰

**Endpoint**: `POST /api/appointments`

**Headers**:
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request**:
```json
{
  "doctorId": 1,
  "clinicId": 1,
  "appointmentTime": "2025-02-15T10:00:00",
  "durationMinutes": 60,
  "age": 35,
  "gender": "MALE",
  "symptoms": "Đau đầu và sốt 3 ngày"
}
```

**Response**:
```json
{
  "id": 1,
  "patientId": 1,
  "doctorId": 1,
  "clinicId": 1,
  "appointmentTime": "2025-02-15T10:00:00",
  "status": "PENDING",
  "scheduleStatus": "HOLD",
  "age": 35,
  "gender": "MALE",
  "symptoms": "Đau đầu và sốt 3 ngày",
  "createdAt": "2025-01-15T10:00:00"
}
```

**⚠️ QUAN TRỌNG**:
- Appointment được tạo với status `PENDING`
- Schedule status là `HOLD` (giữ slot 5 phút)
- **Payment tự động được tạo** với status `PENDING` (50,000 VND)
- **Phải thanh toán và confirm trong 5 phút**, nếu không slot sẽ bị giải phóng

---

### **BƯỚC 4: Thanh Toán Cọc 50,000 VND qua VNPAY** 💰

#### 4.1. Lấy Payment ID

**Endpoint**: `GET /api/payments/appointments/{appointmentId}`

**Headers**:
```
Authorization: Bearer <token>
```

**Request Example**:
```bash
GET /api/payments/appointments/1
```

**Response**:
```json
{
  "id": 1,
  "appointmentId": 1,
  "amount": 50000.0,
  "status": "PENDING",
  "paymentMethod": "BANK_TRANSFER",
  "createdAt": "2025-01-15T10:00:00"
}
```

**Lưu lại**: `paymentId` = 1

#### 4.2. Tạo VNPAY Payment URL

**Endpoint**: `POST /api/payments/{paymentId}/vnpay/create`

**Headers**:
```
Authorization: Bearer <token>
```

**Request Example**:
```bash
POST /api/payments/1/vnpay/create
```

**Response**:
```json
{
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=5000000&vnp_Command=pay&...",
  "paymentId": 1,
  "amount": 50000.0,
  "orderInfo": "Deposit for appointment #1"
}
```

#### 4.3. Redirect Đến VNPAY

**Frontend**: Redirect user đến `paymentUrl` từ response trên

```javascript
// Frontend code example
const response = await fetch('/api/payments/1/vnpay/create', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
const data = await response.json();
window.location.href = data.paymentUrl; // Redirect đến VNPAY
```

#### 4.4. Thanh Toán Trên VNPAY

User sẽ thấy trang thanh toán VNPAY:
1. Nhập thông tin thẻ (hoặc chọn phương thức khác)
2. Xác nhận thanh toán
3. Nhập OTP (nếu cần)

**Test Card (Sandbox)**:
```
Số thẻ: 9704198526191432198
Tên: NGUYEN VAN A
Ngày hết hạn: 12/25
CVV: 123
OTP: 123456
```

#### 4.5. VNPAY Callback (Tự Động)

VNPAY sẽ tự động gọi:
```
POST /api/payments/vnpay/callback
```

Backend tự động:
- Xác thực signature
- Cập nhật payment: `PENDING` → `PAID`
- Lưu transaction ID

#### 4.6. VNPAY Return (Redirect về Frontend)

VNPAY redirect về:
```
GET /api/payments/vnpay/return?vnp_ResponseCode=00&vnp_TransactionNo=12345678&...
```

**Response**:
```json
{
  "success": true,
  "paymentId": 1,
  "transactionId": "12345678",
  "message": "Payment successful"
}
```

**Frontend**: Hiển thị thông báo thành công và chuyển sang bước tiếp theo

---

### **BƯỚC 5: Xác Nhận Appointment (Trong 5 Phút)** ✅

**⚠️ QUAN TRỌNG**: Phải confirm trong 5 phút sau khi tạo appointment

**Endpoint**: `PUT /api/appointments/{appointmentId}/confirm`

**Headers**:
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request** (optional - có thể cập nhật thông tin):
```json
{
  "age": 35,
  "gender": "MALE",
  "symptoms": "Đau đầu và sốt 3 ngày"
}
```

**Response**:
```json
{
  "id": 1,
  "status": "CONFIRMED",  // ✅ Tự động CONFIRMED
  "scheduleStatus": "BOOKED",
  "message": "Appointment confirmed successfully"
}
```

**Điều gì xảy ra**:
- ✅ Schedule: `HOLD` → `BOOKED` (slot được giữ vĩnh viễn)
- ✅ **Appointment: `PENDING` → `CONFIRMED`** (TỰ ĐỘNG) 🎉
- ✅ Payment đã `PAID` (đã kiểm tra ở bước trước)
- ✅ **Sẵn sàng cho ngày khám** - Không cần chờ bác sĩ xác nhận

**⚠️ LƯU Ý**:
- Nếu chưa thanh toán → Lỗi: "Payment deposit (50,000 VND) is required"
- Nếu quá 5 phút → Lỗi: "Hold period has expired"
- Phải confirm trong 5 phút, nếu không slot sẽ bị giải phóng
- **Sau khi confirm, appointment tự động CONFIRMED** - Không cần chờ bác sĩ

---

### **BƯỚC 6: Appointment Tự Động CONFIRMED** ✅

**🎉 Sau khi patient confirm (đã thanh toán), appointment tự động chuyển sang `CONFIRMED`!**

**Không cần chờ bác sĩ xác nhận** vì:
- ✅ Bệnh nhân đã chọn từ lịch rảnh của bác sĩ → Bác sĩ đã available
- ✅ Bệnh nhân đã đặt cọc 50k → Xác nhận nhu cầu thực sự

#### 6.1. Kiểm Tra Trạng Thái

**Endpoint**: `GET /api/appointments/my-appointments`

**Headers**:
```
Authorization: Bearer <token>
```

**Response**:
```json
[
  {
    "id": 1,
    "doctorName": "BS. Nguyễn Văn B",
    "clinicName": "Phòng khám Đa khoa Medinova",
    "appointmentTime": "2025-02-15T10:00:00",
    "status": "CONFIRMED",  // ✅ Tự động CONFIRMED
    "createdAt": "2025-01-15T10:00:00"
  }
]
```

#### 6.2. Kết Quả

- ✅ Appointment status: `CONFIRMED`
- ✅ Sẵn sàng cho ngày khám
- ✅ Không cần chờ bác sĩ xác nhận

**Lưu ý**: Bác sĩ vẫn có thể reject appointment trong trường hợp đặc biệt (ví dụ: bác sĩ bị ốm), nhưng đây không phải luồng chính.

---

## ⏱️ Timeline Quan Trọng

```
T+0:00    Bước 1-3: Đăng nhập → Tìm bác sĩ → Tạo appointment
          └─> Appointment: PENDING
          └─> Schedule: HOLD (5 phút)
          └─> Payment: PENDING

T+0:01    Bước 4: Thanh toán cọc qua VNPAY
          └─> Payment: PAID

T+0:05    ⚠️ Nếu chưa confirm → Slot tự động giải phóng
          └─> Appointment bị xóa
          └─> Payment: CANCELLED

T+0:05    ✅ Bước 5: Confirm appointment (trong 5 phút, sau khi đã thanh toán)
          └─> Schedule: BOOKED (vĩnh viễn)
          └─> Appointment: CONFIRMED ✅ (TỰ ĐỘNG)
          └─> Sẵn sàng cho ngày khám

**Không còn bước chờ bác sĩ xác nhận!**
```

---

## 📊 Flow Diagram

```
[Đăng nhập/Đăng ký]
    ↓
[Tìm kiếm bác sĩ]
    ↓
[Tạo appointment]
    ├─> Payment tự động tạo (PENDING)
    └─> Schedule: HOLD (5 phút)
    ↓
[Thanh toán cọc VNPAY]
    ├─> Tạo VNPAY URL
    ├─> Redirect đến VNPAY
    ├─> Thanh toán
    └─> Payment: PAID
    ↓
[Xác nhận appointment] (trong 5 phút)
    ├─> Kiểm tra payment đã PAID
    ├─> Schedule: BOOKED
    └─> Appointment: PENDING (chờ bác sĩ)
    ↓
[Chờ bác sĩ xác nhận]
    ├─> CONFIRMED ✅
    ├─> REJECTED ❌ (hoàn tiền)
    └─> EXPIRED ⏰ (timeout 2 giờ, hoàn tiền)
```

---

## 🔄 Các Trường Hợp Đặc Biệt

### 1. Quên Thanh Toán Trong 5 Phút

**Điều gì xảy ra**:
- Slot tự động giải phóng sau 5 phút
- Appointment bị xóa
- Payment bị hủy

**Phải làm gì**:
- ❌ Không thể confirm nữa
- ✅ Phải tạo appointment mới và thanh toán lại

### 2. Thanh Toán Thất Bại

**Điều gì xảy ra**:
- Payment status: `FAILED`
- Appointment vẫn `PENDING` với schedule `HOLD`

**Phải làm gì**:
- ✅ Thử lại thanh toán (tạo VNPAY URL mới)
- ⚠️ Phải thanh toán thành công trước khi confirm

### 3. Quên Confirm Trong 5 Phút

**Điều gì xảy ra**:
- Slot tự động giải phóng
- Appointment bị xóa
- Payment bị hủy (nếu đã thanh toán, tiền sẽ được hoàn lại)

**Phải làm gì**:
- ✅ Tạo appointment mới và làm lại từ đầu

### 4. Bác Sĩ Từ Chối

**Điều gì xảy ra**:
- Appointment: `PENDING` → `REJECTED`
- Payment: `PAID` → `REFUNDED` (tự động hoàn tiền)
- Slot được giải phóng

**Phải làm gì**:
- ✅ Có thể đặt lại với bác sĩ/thời gian khác
- ✅ Tiền cọc sẽ được hoàn lại tự động

### 5. Bác Sĩ Chưa Confirm Sau 2 Giờ

**Điều gì xảy ra**:
- Appointment: `PENDING` → `EXPIRED`
- Payment: `PAID` → `REFUNDED` (tự động hoàn tiền)
- Slot được giải phóng

**Phải làm gì**:
- ✅ Có thể đặt lại appointment mới

---

## 📱 Frontend Implementation Guide

### Step-by-Step Code Example

```javascript
// 1. Đăng nhập
const loginResponse = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email, password })
});
const { token } = await loginResponse.json();

// 2. Tìm bác sĩ
const doctorsResponse = await fetch('/api/doctors/search?q=Nguyễn', {
  headers: { 'Authorization': `Bearer ${token}` }
});
const doctors = await doctorsResponse.json();

// 3. Tạo appointment
const appointmentResponse = await fetch('/api/appointments', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    doctorId: 1,
    clinicId: 1,
    appointmentTime: '2025-02-15T10:00:00',
    age: 35,
    gender: 'MALE',
    symptoms: 'Đau đầu'
  })
});
const appointment = await appointmentResponse.json();

// 4. Lấy payment
const paymentResponse = await fetch(`/api/payments/appointments/${appointment.id}`, {
  headers: { 'Authorization': `Bearer ${token}` }
});
const payment = await paymentResponse.json();

// 5. Tạo VNPAY URL
const vnpayResponse = await fetch(`/api/payments/${payment.id}/vnpay/create`, {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}` }
});
const { paymentUrl } = await vnpayResponse.json();

// 6. Redirect đến VNPAY
window.location.href = paymentUrl;

// 7. Sau khi thanh toán xong (từ VNPAY return URL)
// Frontend nhận kết quả và hiển thị thông báo

// 8. Confirm appointment
const confirmResponse = await fetch(`/api/appointments/${appointment.id}/confirm`, {
  method: 'PUT',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({})
});
const confirmedAppointment = await confirmResponse.json();

// 9. Kiểm tra trạng thái định kỳ
setInterval(async () => {
  const statusResponse = await fetch(`/api/appointments/${appointment.id}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const status = await statusResponse.json();
  
  if (status.status === 'CONFIRMED') {
    // Hiển thị thông báo thành công
    showSuccessNotification('Appointment confirmed!');
  } else if (status.status === 'REJECTED') {
    // Hiển thị thông báo từ chối
    showRejectionNotification('Appointment rejected');
  }
}, 30000); // Check mỗi 30 giây
```

---

## ✅ Checklist Cho Bệnh Nhân

- [ ] Đăng nhập/Đăng ký tài khoản
- [ ] Tìm kiếm và chọn bác sĩ phù hợp (từ lịch rảnh)
- [ ] Chọn thời gian khám
- [ ] Tạo appointment
- [ ] Thanh toán cọc 50,000 VND qua VNPAY
- [ ] Xác nhận appointment (trong 5 phút)
- [ ] ✅ **Appointment tự động CONFIRMED** - Sẵn sàng cho ngày khám!

**Không cần chờ bác sĩ xác nhận nữa!**

---

## 🎯 Tóm Tắt

**Luồng đơn giản**:
1. **Đăng nhập** → Lấy token
2. **Tìm bác sĩ** → Chọn từ lịch rảnh
3. **Tạo appointment** → Payment tự động tạo
4. **Thanh toán VNPAY** → Redirect đến VNPAY và thanh toán
5. **Confirm appointment** → Trong 5 phút, sau khi đã thanh toán
6. ✅ **Appointment tự động CONFIRMED** - Sẵn sàng cho ngày khám!

**Tổng thời gian**: ~5 phút (nếu làm nhanh)

**Lưu ý quan trọng**:
- ⚠️ Phải thanh toán trước khi confirm
- ⚠️ Phải confirm trong 5 phút
- ✅ **Appointment tự động CONFIRMED sau khi confirm** - Không cần chờ bác sĩ
- ✅ Tiền cọc được hoàn lại nếu hủy/reject
