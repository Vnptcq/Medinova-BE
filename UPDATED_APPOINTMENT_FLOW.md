# Luồng Đặt Lịch Khám - Đã Cập Nhật (Không Cần Bác Sĩ Xác Nhận)

## 🎯 Thay Đổi Chính

**Trước đây**: Patient confirm → PENDING (chờ doctor confirm) → CONFIRMED

**Bây giờ**: Patient confirm (đã thanh toán) → **Tự động CONFIRMED** ✅

---

## 💡 Lý Do Thay Đổi

1. **Bệnh nhân đã chọn từ lịch rảnh của bác sĩ** → Bác sĩ đã available
2. **Bệnh nhân đã đặt cọc 50k** → Xác nhận nhu cầu thực sự
3. **Không cần bác sĩ xác nhận thêm** → Đơn giản hóa luồng, tăng trải nghiệm người dùng

---

## 📝 Luồng Mới (Đơn Giản Hơn)

### **5 Bước Chính**:

```
1. Đăng nhập/Đăng ký
   ↓
2. Tìm kiếm và chọn bác sĩ (từ lịch rảnh)
   ↓
3. Tạo appointment (giữ slot 5 phút)
   ↓
4. Thanh toán cọc 50,000 VND qua VNPAY
   ↓
5. Xác nhận appointment (trong 5 phút)
   └─> TỰ ĐỘNG CONFIRMED ✅
   └─> Sẵn sàng cho ngày khám
```

---

## 🔄 Luồng Chi Tiết

### **BƯỚC 1-3: Tạo Appointment**

**Endpoint**: `POST /api/appointments`

**Kết quả**:
- Appointment: `PENDING`
- Schedule: `HOLD` (5 phút)
- Payment: `PENDING` (50,000 VND)

---

### **BƯỚC 4: Thanh Toán Cọc**

**Endpoints**:
1. `GET /api/payments/appointments/{appointmentId}` - Lấy payment
2. `POST /api/payments/{paymentId}/vnpay/create` - Tạo VNPAY URL
3. Redirect đến VNPAY và thanh toán
4. VNPAY callback tự động → Payment: `PAID`

---

### **BƯỚC 5: Xác Nhận Appointment**

**Endpoint**: `PUT /api/appointments/{appointmentId}/confirm`

**Điều gì xảy ra**:
1. ✅ Kiểm tra payment đã `PAID`
2. ✅ Schedule: `HOLD` → `BOOKED` (giữ vĩnh viễn)
3. ✅ **Appointment: `PENDING` → `CONFIRMED`** (TỰ ĐỘNG) 🎉
4. ✅ **Sẵn sàng cho ngày khám** - Không cần chờ bác sĩ xác nhận

**Kết quả**:
```json
{
  "id": 1,
  "status": "CONFIRMED",  // ✅ Tự động CONFIRMED
  "scheduleStatus": "BOOKED",
  "appointmentTime": "2025-02-15T10:00:00"
}
```

---

## ⏱️ Timeline Mới

```
T+0:00    Patient tạo appointment
          └─> Appointment: PENDING
          └─> Schedule: HOLD (5 phút)
          └─> Payment: PENDING

T+0:01    Patient thanh toán cọc
          └─> Payment: PAID

T+0:05    ⚠️ Nếu chưa confirm → Slot tự động giải phóng
          └─> Appointment bị xóa

T+0:05    ✅ Patient confirm (trong 5 phút, sau khi đã thanh toán)
          └─> Schedule: BOOKED (vĩnh viễn)
          └─> Appointment: CONFIRMED ✅ (TỰ ĐỘNG)
          └─> Sẵn sàng cho ngày khám
```

**Không còn bước chờ bác sĩ xác nhận!**

---

## 🔄 Status Flow Mới

```
[Patient tạo appointment]
    ↓
PENDING (Schedule: HOLD - 5 phút, Payment: PENDING)
    ├─→ [Patient thanh toán]
    │       └─> Payment: PAID
    │
    ├─→ [Patient confirm trong 5 phút]
    │       └─> Schedule: BOOKED
    │       └─> Appointment: CONFIRMED ✅ (TỰ ĐỘNG)
    │       └─> Sẵn sàng cho ngày khám
    │
    └─→ [Không confirm trong 5 phút]
            └─> Appointment bị xóa ❌

CONFIRMED
    ├─→ CHECKED_IN (Đến phòng khám)
    ├─→ CANCELLED (Patient hủy)
    └─→ REJECTED (Doctor reject - trường hợp đặc biệt)

CHECKED_IN → IN_PROGRESS → REVIEW → COMPLETED
```

---

## 🎯 Các Trường Hợp Đặc Biệt

### 1. Doctor Reject (Trường Hợp Đặc Biệt)

**Khi nào**: Bác sĩ có thể reject CONFIRMED appointment trong trường hợp đặc biệt:
- Bác sĩ bị ốm đột xuất
- Lịch khám không phù hợp
- Lý do khác

**Endpoint**: `POST /api/appointments/{id}/reject` (Doctor)

**Điều gì xảy ra**:
- Appointment: `CONFIRMED` → `REJECTED`
- Schedule: `BLOCKED`
- Payment: `PAID` → `REFUNDED` (tự động hoàn tiền)

**Lưu ý**: Đây là trường hợp đặc biệt, không phải luồng chính.

---

### 2. Patient Hủy

**Endpoint**: `PUT /api/appointments/{id}/status`

**Request**:
```json
{
  "status": "CANCELLED"
}
```

**Điều gì xảy ra**:
- Appointment: `CONFIRMED` → `CANCELLED`
- Schedule: `BLOCKED`
- Payment: `PAID` → `REFUNDED` (tự động hoàn tiền)

---

## 📊 So Sánh Trước và Sau

### Trước (Cần Bác Sĩ Xác Nhận)

```
Patient confirm → PENDING → [Chờ doctor] → CONFIRMED
                                    ↓
                              (Có thể bị reject/expire)
```

**Vấn đề**:
- Phải chờ bác sĩ xác nhận
- Có thể bị từ chối sau khi đã đặt cọc
- Timeout 2 giờ nếu bác sĩ chưa confirm

### Sau (Tự Động CONFIRMED)

```
Patient confirm → CONFIRMED ✅ (TỰ ĐỘNG)
```

**Lợi ích**:
- ✅ Đơn giản hơn, nhanh hơn
- ✅ Không cần chờ bác sĩ
- ✅ Trải nghiệm tốt hơn cho bệnh nhân
- ✅ Vẫn có cơ chế reject trong trường hợp đặc biệt

---

## ✅ Checklist Cho Bệnh Nhân (Đã Cập Nhật)

- [ ] Đăng nhập/Đăng ký tài khoản
- [ ] Tìm kiếm và chọn bác sĩ (từ lịch rảnh)
- [ ] Chọn thời gian khám
- [ ] Tạo appointment
- [ ] Thanh toán cọc 50,000 VND qua VNPAY
- [ ] Xác nhận appointment (trong 5 phút)
- [ ] ✅ **Appointment tự động CONFIRMED** - Sẵn sàng cho ngày khám!

**Không cần chờ bác sĩ xác nhận nữa!**

---

## 🎯 Tóm Tắt

**Luồng mới đơn giản hơn**:
1. **Đăng nhập** → Lấy token
2. **Tìm bác sĩ** → Chọn từ lịch rảnh
3. **Tạo appointment** → Payment tự động tạo
4. **Thanh toán VNPAY** → Payment PAID
5. **Confirm appointment** → **TỰ ĐỘNG CONFIRMED** ✅

**Tổng thời gian**: ~5 phút (nếu làm nhanh)

**Lưu ý**:
- ⚠️ Phải thanh toán trước khi confirm
- ⚠️ Phải confirm trong 5 phút
- ✅ Appointment tự động CONFIRMED sau khi confirm
- ✅ Không cần chờ bác sĩ xác nhận
- ✅ Tiền cọc được hoàn lại nếu hủy/reject
