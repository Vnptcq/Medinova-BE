# Luồng Đặt Lịch Khám - Từ Góc Độ Bệnh Nhân

## 🎯 Tổng Quan

Luồng đặt lịch khám hiện tại có **5 bước chính** (đã đơn giản hóa):

```
Bước 1: Patient tạo appointment
    ↓
Bước 2: Patient thanh toán cọc 50,000 VND ← BẮT BUỘC (chống abuse)
    ↓
Bước 3: Patient xác nhận (trong 5 phút) ← QUAN TRỌNG
    ↓
Bước 4: Appointment TỰ ĐỘNG CONFIRMED ✅
    └─> Sẵn sàng cho ngày khám
```

**🎉 Không cần chờ bác sĩ xác nhận nữa!**

**Lý do**: 
- Bệnh nhân đã chọn từ lịch rảnh của bác sĩ → Bác sĩ đã available
- Bệnh nhân đã đặt cọc 50k → Xác nhận nhu cầu thực sự

## 🛡️ Bảo Mật Chống Abuse

**Vấn đề**: Hacker hoặc đối thủ có thể tạo nhiều tài khoản và đặt lịch liên tục để làm sập hệ thống.

**Giải pháp**: Yêu cầu bệnh nhân **cọc 50,000 VND** mỗi lần đặt lịch để xác nhận nhu cầu thực sự.

- ✅ Chỉ những người thực sự có nhu cầu mới thanh toán
- ✅ Giảm thiểu spam và abuse
- ✅ Tự động hoàn tiền khi hủy appointment

---

## 📝 Luồng Chi Tiết

### **BƯỚC 1: Patient Tạo Appointment**

**Endpoint**: `POST /api/appointments`

**Request Body**:
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

**Điều gì xảy ra**:

1. ✅ **Validation**:
   - Kiểm tra user đã đăng nhập (JWT token)
   - Kiểm tra user có role PATIENT
   - Kiểm tra doctor tồn tại
   - Kiểm tra clinic tồn tại
   - Kiểm tra doctor có thuộc clinic không
   - Kiểm tra doctor không đang nghỉ (approved leave)
   - Kiểm tra không có appointment trùng thời gian

2. ✅ **Tạo Schedule với status HOLD**:
   - Tạo `DoctorSchedule` mới
   - `status = "HOLD"`
   - `holdExpiresAt = now + 5 phút`
   - Slot được **giữ trong 5 phút**

3. ✅ **Tạo Appointment với status PENDING**:
   - `status = "PENDING"`
   - Lưu thông tin: age, gender, symptoms
   - Liên kết với schedule vừa tạo

**Kết quả**:
- ✅ Appointment được tạo thành công
- ✅ **Payment tự động được tạo** với status `PENDING` (50,000 VND)
- ⚠️ **Status: PENDING**
- ⚠️ **Schedule Status: HOLD** (hết hạn sau 5 phút)
- ⚠️ **Slot chỉ được giữ 5 phút** - cần thanh toán và xác nhận ngay!

---

### **BƯỚC 2: Patient Thanh Toán Cọc 50,000 VND** 💰

**Endpoint**: `POST /api/payments/{paymentId}/process`

**Request Body**:
```json
{
  "appointmentId": 1,
  "paymentMethod": "BANK_TRANSFER",
  "transactionId": "TXN123456789",
  "paymentGateway": "VNPAY",
  "notes": "Payment via VNPAY"
}
```

**Điều gì xảy ra**:

1. ✅ **Validation**:
   - Kiểm tra payment thuộc về patient hiện tại
   - Kiểm tra payment status = PENDING
   - Kiểm tra appointment ID khớp

2. ✅ **Cập nhật Payment**:
   - Payment: `PENDING` → `PAID`
   - Lưu transaction ID và payment gateway
   - Ghi nhận thời gian thanh toán

**Kết quả**:
- ✅ Payment chuyển sang PAID
- ✅ Sẵn sàng để confirm appointment

**⚠️ LƯU Ý QUAN TRỌNG**:
- **Phải thanh toán cọc 50,000 VND** trước khi có thể confirm appointment
- Đây là biện pháp chống abuse - chỉ người thực sự có nhu cầu mới thanh toán
- Tiền sẽ được hoàn lại nếu hủy appointment (trước khi khám)

---

### **BƯỚC 3: Patient Xác Nhận Appointment (TRONG 5 PHÚT)**

**Endpoint**: `PUT /api/appointments/{id}/confirm`

**Request Body** (optional - có thể cập nhật thông tin):
```json
{
  "age": 35,
  "gender": "MALE",
  "symptoms": "Đau đầu và sốt 3 ngày"
}
```

**Điều gì xảy ra**:

1. ✅ **Validation**:
   - Kiểm tra appointment thuộc về patient hiện tại
   - Kiểm tra appointment status = PENDING
   - Kiểm tra schedule status = HOLD
   - ⚠️ **Kiểm tra HOLD chưa hết hạn** (< 5 phút)
   - ⚠️ **⚠️ QUAN TRỌNG: Kiểm tra payment đã được thanh toán chưa** (phải PAID)

2. ✅ **Cập nhật**:
   - Schedule: `HOLD` → `BOOKED`
   - Xóa `holdExpiresAt` (không còn hết hạn)
   - Appointment vẫn giữ status `PENDING` (chờ doctor confirm)

**Kết quả**:
- ✅ Schedule chuyển sang BOOKED (slot được giữ vĩnh viễn)
- ⚠️ Appointment vẫn PENDING (chờ doctor xác nhận)
- ✅ Có thể cập nhật thông tin bệnh nhân (age, gender, symptoms)

**⚠️ LƯU Ý QUAN TRỌNG**:
- ⚠️ **Phải thanh toán cọc 50,000 VND trước** khi confirm
- Nếu không confirm trong 5 phút → Slot tự động bị giải phóng
- Appointment sẽ bị xóa tự động sau 5 phút
- Payment sẽ bị hủy nếu appointment bị xóa
- Phải tạo lại appointment mới

---

### **BƯỚC 4: Appointment Tự Động CONFIRMED** ✅

**🎉 Sau khi patient confirm (đã thanh toán), appointment tự động chuyển sang `CONFIRMED`!**

**Không cần chờ bác sĩ xác nhận** vì:
- ✅ Bệnh nhân đã chọn từ lịch rảnh của bác sĩ → Bác sĩ đã available
- ✅ Bệnh nhân đã đặt cọc 50k → Xác nhận nhu cầu thực sự

**Kết quả**: 
- ✅ Appointment status: `CONFIRMED`
- ✅ Schedule status: `BOOKED`
- ✅ Sẵn sàng cho ngày khám

**Lưu ý**: Bác sĩ vẫn có thể reject appointment trong trường hợp đặc biệt (ví dụ: bác sĩ bị ốm), nhưng đây không phải luồng chính.

---

### **BƯỚC 5: Đến Ngày Khám**

Sau khi appointment được CONFIRMED, luồng khám bệnh:

1. **CONFIRMED** → Patient đến phòng khám
2. **CHECKED_IN** → Staff/Doctor check-in patient
3. **IN_PROGRESS** → Doctor bắt đầu khám
4. **REVIEW** → Doctor hoàn thành khám, chờ patient review
5. **COMPLETED** → Sau khi patient review xong

---

## ⏱️ Timeline Quan Trọng

```
T+0:00    Patient tạo appointment
          └─> Schedule: HOLD (expires in 5 phút)
          └─> Appointment: PENDING
          └─> Payment: PENDING (50,000 VND)

T+0:02    ✅ Patient thanh toán cọc
          └─> Payment: PAID

T+0:05    ⚠️ Nếu chưa confirm → Slot tự động giải phóng
          └─> Appointment bị xóa
          └─> Payment: CANCELLED

T+0:05    ✅ Patient confirm (trong 5 phút, sau khi đã thanh toán)
          └─> Schedule: BOOKED (vĩnh viễn)
          └─> Appointment: CONFIRMED ✅ (TỰ ĐỘNG)
          └─> Sẵn sàng cho ngày khám

**Không còn bước chờ bác sĩ xác nhận!**
```

---

## 🔄 Các Trường Hợp Đặc Biệt

### **1. HOLD Hết Hạn (Sau 5 Phút)**

**Điều gì xảy ra**:
- Scheduled task chạy mỗi 1 phút
- Tự động xóa HOLD schedules đã hết hạn
- Xóa appointment PENDING liên kết

**Patient phải làm gì**:
- ❌ Không thể confirm nữa
- ✅ Phải tạo appointment mới

### **2. Doctor Reject (Trường Hợp Đặc Biệt)**

**Khi nào**: Bác sĩ có thể reject CONFIRMED appointment trong trường hợp đặc biệt:
- Bác sĩ bị ốm đột xuất
- Lịch khám không phù hợp
- Lý do khác

**Điều gì xảy ra**:
- Appointment: `CONFIRMED` → `REJECTED`
- Schedule: `BLOCKED`
- Payment: `PAID` → `REFUNDED` (tự động hoàn tiền)

**Patient phải làm gì**:
- ✅ Có thể tạo appointment mới với thời gian khác
- ✅ Tiền cọc được hoàn lại tự động

**Lưu ý**: Đây là trường hợp đặc biệt, không phải luồng chính. Thông thường appointment sẽ tự động CONFIRMED sau khi patient confirm.

### **4. Patient Hủy**

**Endpoint**: `PUT /api/appointments/{id}/status`

**Request**:
```json
{
  "status": "CANCELLED"
}
```

**Điều gì xảy ra**:
- Appointment: `*` → `CANCELLED`
- Schedule: `BLOCKED`
- Slot không còn available
- ✅ **Payment tự động được REFUNDED** (nếu đã thanh toán)

**Lưu ý**:
- Patient chỉ có thể hủy appointment của mình
- Không thể hủy nếu đã COMPLETED
- **Tiền cọc sẽ được hoàn lại tự động** khi hủy appointment

---

## 📊 Status Flow Từ Góc Độ Patient (Đã Cập Nhật)

```
[Patient tạo]
    ↓
PENDING (Schedule: HOLD - 5 phút, Payment: PENDING)
    ├─→ [Patient thanh toán]
    │       └─> Payment: PAID
    │
    ├─→ [Patient confirm trong 5 phút, sau khi đã thanh toán]
    │       ↓
    │   CONFIRMED ✅ (TỰ ĐỘNG - Schedule: BOOKED)
    │       ├─→ CHECKED_IN (Đến phòng khám)
    │       ├─→ CANCELLED (Patient hủy)
    │       ├─→ CANCELLED_BY_DOCTOR (Doctor hủy)
    │       └─→ REJECTED (Doctor reject - trường hợp đặc biệt) ❌
    │
    └─→ [Không confirm trong 5 phút]
            ↓
        Appointment bị xóa tự động ❌

CONFIRMED
    ├─→ CHECKED_IN (Đến phòng khám)
    ├─→ CANCELLED (Patient hủy)
    ├─→ CANCELLED_BY_DOCTOR (Doctor hủy)
    └─→ REJECTED (Doctor reject - trường hợp đặc biệt)

CHECKED_IN → IN_PROGRESS → REVIEW → COMPLETED
```

---

## 🎯 Tóm Tắt Luồng Cho Patient

### **Quy Trình Đơn Giản**:

1. **Chọn bác sĩ và thời gian** → Gọi API `POST /api/appointments`
2. **Thanh toán cọc 50,000 VND** → Gọi API `POST /api/payments/{paymentId}/vnpay/create` ⚠️ BẮT BUỘC
3. **Xác nhận ngay (trong 5 phút)** → Gọi API `PUT /api/appointments/{id}/confirm`
4. ✅ **Appointment tự động CONFIRMED** → Sẵn sàng cho ngày khám!

### **Các Lưu Ý**:

- ⚠️ **Phải thanh toán cọc 50,000 VND trước** khi confirm appointment
- ⚠️ **Phải confirm trong 5 phút** sau khi tạo, nếu không slot sẽ bị giải phóng
- ✅ **Appointment tự động CONFIRMED** sau khi patient confirm (đã thanh toán) - Không cần chờ bác sĩ
- ✅ **Có thể hủy** appointment trước khi khám (trừ COMPLETED) → Tiền cọc được hoàn lại tự động
- ⚠️ **Bác sĩ vẫn có thể reject** trong trường hợp đặc biệt → Tiền cọc được hoàn lại

---

## 🔍 API Endpoints Cho Patient

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/appointments` | Tạo appointment (HOLD 5 phút, tự động tạo payment) |
| POST | `/api/payments/appointments/{appointmentId}` | Tạo payment cho appointment |
| POST | `/api/payments/{paymentId}/process` | Thanh toán cọc 50,000 VND |
| GET | `/api/payments/appointments/{appointmentId}` | Xem payment của appointment |
| GET | `/api/payments/my-payments` | Xem tất cả payments của mình |
| PUT | `/api/appointments/{id}/confirm` | Xác nhận appointment (sau khi đã thanh toán) |
| GET | `/api/appointments/my-appointments` | Xem appointments của mình |
| GET | `/api/appointments/doctors/{id}/busy-schedules` | Xem lịch bận của doctor (public) |
| PUT | `/api/appointments/{id}/status` | Hủy appointment (status = CANCELLED, tự động refund) |

---

## ❓ Câu Hỏi Thường Gặp

### **Q: Tại sao phải cọc 50,000 VND?**
A: Đây là biện pháp chống abuse và spam. Chỉ những người thực sự có nhu cầu mới thanh toán. Tiền sẽ được hoàn lại nếu hủy appointment (trước khi khám).

### **Q: Có cần chờ bác sĩ xác nhận không?**
A: **Không!** Sau khi bạn confirm appointment (đã thanh toán cọc), appointment sẽ tự động chuyển sang CONFIRMED. Không cần chờ bác sĩ xác nhận vì:
- Bạn đã chọn từ lịch rảnh của bác sĩ → Bác sĩ đã available
- Bạn đã đặt cọc 50k → Xác nhận nhu cầu thực sự

### **Q: Nếu quên confirm trong 5 phút thì sao?**
A: Slot sẽ tự động bị giải phóng, appointment bị xóa. Payment sẽ bị hủy. Phải tạo appointment mới và thanh toán lại.

### **Q: Tiền cọc có được hoàn lại không?**
A: Có, tiền cọc sẽ được hoàn lại tự động trong các trường hợp:
- Patient hủy appointment (trước khi khám)
- Doctor reject appointment (trường hợp đặc biệt, ví dụ: bác sĩ bị ốm)
- Doctor hủy appointment

### **Q: Có thể đổi lịch không?**
A: Hiện tại chưa có tính năng reschedule. Phải hủy appointment cũ và tạo appointment mới.

### **Q: Có thể xem lịch bận của doctor không?**
A: Có, dùng endpoint public `GET /api/appointments/doctors/{id}/busy-schedules` để xem các slot đã bận.

### **Q: Appointment có thể bị từ chối không?**
A: Có, nhưng đây là trường hợp đặc biệt. Bác sĩ có thể reject CONFIRMED appointment trong trường hợp đặc biệt (ví dụ: bác sĩ bị ốm đột xuất). Trong trường hợp này, tiền cọc sẽ được hoàn lại tự động.
