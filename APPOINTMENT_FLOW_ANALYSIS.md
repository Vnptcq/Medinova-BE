# Phân Tích Luồng Đặt Lịch Khám Hiện Tại

## 📋 Tổng Quan

Tính năng đặt lịch khám cho phép bệnh nhân (PATIENT) đặt lịch khám với bác sĩ (DOCTOR) tại phòng khám (CLINIC).

## 🏗️ Kiến Trúc

### Entities
- **Appointment**: Lịch hẹn khám
- **DoctorSchedule**: Lịch làm việc của bác sĩ (1-1 với Appointment)
- **Doctor**: Bác sĩ
- **Clinic**: Phòng khám
- **User**: Bệnh nhân

### Relationships
```
Appointment (1) ──< (1) DoctorSchedule
Appointment (N) ──< (1) User (Patient)
Appointment (N) ──< (1) Doctor
Appointment (N) ──< (1) Clinic
```

## 🔄 Luồng Hiện Tại

### 1. Tạo Appointment (POST /api/appointments)

**Actor**: PATIENT

**Quy trình**:
1. Patient gửi request với:
   - `doctorId`: ID bác sĩ
   - `clinicId`: ID phòng khám
   - `appointmentTime`: Thời gian hẹn
   - `durationMinutes`: Thời lượng (mặc định 60 phút)
   - `age`, `gender`, `symptoms`: Thông tin bệnh nhân (optional)

2. **Validation**:
   - ✅ Kiểm tra doctor tồn tại
   - ✅ Kiểm tra clinic tồn tại
   - ✅ Kiểm tra doctor có thuộc clinic không
   - ✅ Kiểm tra doctor không đang nghỉ (approved leave)
   - ✅ Kiểm tra không có appointment trùng thời gian (trừ CANCELLED và HOLD đã hết hạn)

3. **Tạo Schedule với status HOLD**:
   - Tạo `DoctorSchedule` với:
     - `status = "HOLD"`
     - `holdExpiresAt = now + 5 phút`
   - Slot được giữ trong 5 phút

4. **Tạo Appointment với status PENDING**:
   - `status = "PENDING"`
   - Liên kết với schedule vừa tạo

**Kết quả**: Appointment được tạo với status PENDING, schedule status HOLD

---

### 2. Xác Nhận Appointment (PUT /api/appointments/{id}/confirm)

**Actor**: PATIENT

**Quy trình**:
1. Patient xác nhận appointment trong vòng 5 phút
2. **Validation**:
   - ✅ Appointment phải là PENDING
   - ✅ Schedule phải là HOLD
   - ✅ HOLD chưa hết hạn (< 5 phút)

3. **Cập nhật**:
   - Schedule: `HOLD` → `BOOKED`
   - Xóa `holdExpiresAt`
   - Appointment vẫn giữ status `PENDING` (chờ doctor confirm)

**Kết quả**: Schedule chuyển sang BOOKED, appointment vẫn PENDING

---

### 3. Doctor Xác Nhận (POST /api/appointments/{id}/confirm)

**Actor**: DOCTOR

**Quy trình**:
1. Doctor xác nhận appointment PENDING
2. **Validation**:
   - ✅ Appointment phải là PENDING
   - ✅ Appointment được assign cho doctor hiện tại

3. **Cập nhật**:
   - Appointment: `PENDING` → `CONFIRMED`
   - Schedule: Đảm bảo `BOOKED` (nếu chưa)

**Kết quả**: Appointment chuyển sang CONFIRMED

---

### 4. Doctor Từ Chối (POST /api/appointments/{id}/reject)

**Actor**: DOCTOR

**Quy trình**:
1. Doctor từ chối appointment PENDING
2. **Validation**:
   - ✅ Appointment phải là PENDING
   - ✅ Appointment được assign cho doctor hiện tại

3. **Cập nhật**:
   - Appointment: `PENDING` → `REJECTED`
   - Schedule: Release slot (có thể xóa hoặc set BLOCKED)
   - Lưu `rejectionReason` (chỉ doctor/admin thấy)

**Kết quả**: Appointment bị từ chối, slot được giải phóng

---

### 5. Luồng Khám Bệnh

**Các bước**:
1. **CONFIRMED** → **CHECKED_IN** (PUT /api/appointments/{id}/check-in)
   - Bệnh nhân đến và check-in
   - Actor: DOCTOR hoặc ADMIN

2. **CHECKED_IN** → **IN_PROGRESS** (PUT /api/appointments/{id}/start)
   - Bác sĩ bắt đầu khám
   - Actor: DOCTOR

3. **IN_PROGRESS** → **REVIEW** (PUT /api/appointments/{id}/complete)
   - Bác sĩ hoàn thành khám, chuyển sang chờ review
   - Actor: DOCTOR

4. **REVIEW** → **COMPLETED** (PUT /api/appointments/{id}/status/doctor)
   - Sau khi patient review xong
   - Actor: DOCTOR

---

### 6. Hủy Appointment

**Patient hủy** (PUT /api/appointments/{id}/status):
- Status: `CANCELLED`
- Schedule: `BLOCKED`

**Doctor hủy** (POST /api/appointments/{id}/cancel):
- Status: `CANCELLED_BY_DOCTOR`
- Schedule: Release slot

---

## 📊 Status Flow

```
PENDING
  ├─→ CONFIRMED (Doctor confirm)
  ├─→ REJECTED (Doctor reject)
  ├─→ EXPIRED (Timeout sau 2 giờ)
  └─→ CANCELLED (Patient cancel)

CONFIRMED
  ├─→ CHECKED_IN (Check-in)
  ├─→ CANCELLED_BY_DOCTOR (Doctor cancel)
  └─→ CANCELLED (Patient cancel)

CHECKED_IN
  ├─→ IN_PROGRESS (Start consultation)
  └─→ CANCELLED (Cancel)

IN_PROGRESS
  ├─→ REVIEW (Complete consultation)
  └─→ CANCELLED (Cancel)

REVIEW
  └─→ COMPLETED (After review)
```

## ⚙️ Scheduled Tasks

### 1. Release Expired HOLD Slots
- **Frequency**: Mỗi 1 phút
- **Action**: Xóa HOLD schedules đã hết hạn (> 5 phút)
- **Service**: `AppointmentSchedulerService.releaseExpiredHoldSlots()`

### 2. Expire PENDING Appointments
- **Frequency**: Mỗi 10 phút
- **Action**: Chuyển PENDING appointments > 2 giờ sang EXPIRED
- **Service**: `AppointmentSchedulerService.expirePendingAppointments()`

## 🔍 Các Endpoint Chính

| Method | Endpoint | Actor | Mô tả |
|--------|----------|-------|-------|
| POST | `/api/appointments` | PATIENT | Tạo appointment (HOLD slot 5 phút) |
| PUT | `/api/appointments/{id}/confirm` | PATIENT | Xác nhận appointment (trong 5 phút) |
| POST | `/api/appointments/{id}/confirm` | DOCTOR | Doctor xác nhận PENDING appointment |
| POST | `/api/appointments/{id}/reject` | DOCTOR | Doctor từ chối PENDING appointment |
| POST | `/api/appointments/{id}/cancel` | DOCTOR | Doctor hủy CONFIRMED appointment |
| PUT | `/api/appointments/{id}/status` | PATIENT | Patient cập nhật status (chỉ CANCELLED) |
| PUT | `/api/appointments/{id}/status/doctor` | DOCTOR | Doctor cập nhật status |
| PUT | `/api/appointments/{id}/check-in` | DOCTOR/ADMIN | Check-in appointment |
| PUT | `/api/appointments/{id}/start` | DOCTOR | Bắt đầu khám |
| PUT | `/api/appointments/{id}/complete` | DOCTOR | Hoàn thành khám |
| GET | `/api/appointments/my-appointments` | PATIENT/DOCTOR | Lấy appointments của mình |
| GET | `/api/appointments/doctors/{id}/busy-schedules` | PUBLIC | Lấy lịch bận của doctor |

## ⚠️ Vấn Đề Hiện Tại Cần Xem Xét

1. **Luồng xác nhận phức tạp**:
   - Patient tạo → Patient confirm (trong 5 phút) → Doctor confirm
   - Có thể đơn giản hóa?

2. **HOLD slot 5 phút**:
   - Có đủ thời gian cho patient không?
   - Có cần thông báo khi sắp hết hạn?

3. **Validation overlap**:
   - Logic kiểm tra trùng lịch có đầy đủ không?
   - Có xử lý edge cases?

4. **Status transitions**:
   - Có đủ trạng thái không?
   - Có cần thêm trạng thái nào?

5. **Notification**:
   - Có thông báo cho doctor khi có appointment mới?
   - Có thông báo cho patient khi appointment được confirm/reject?

6. **Reschedule**:
   - Có hỗ trợ đổi lịch không?

## 💡 Gợi Ý Cải Thiện

1. **Đơn giản hóa luồng xác nhận**:
   - Option 1: Patient tạo → Tự động CONFIRMED (nếu slot available)
   - Option 2: Patient tạo → Doctor confirm (bỏ bước patient confirm)

2. **Thêm tính năng**:
   - Reschedule appointment
   - Reminder notifications
   - Auto-confirm cho appointments trong tương lai xa

3. **Cải thiện validation**:
   - Kiểm tra working hours của doctor
   - Kiểm tra clinic operating hours
   - Kiểm tra minimum advance booking time

4. **Thêm business rules**:
   - Maximum appointments per day cho doctor
   - Minimum time between appointments
   - Cancellation policy (có thể cancel trước X giờ)
