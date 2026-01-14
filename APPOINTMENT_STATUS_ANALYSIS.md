# Phân Tích Trạng Thái Appointment - Status Analysis

## 📊 Trạng Thái Hiện Tại

### Danh Sách Trạng Thái (12 trạng thái)

1. **PENDING** - Chờ xác nhận (sau khi patient confirm, chờ doctor)
2. **CONFIRMED** - Đã xác nhận, sẵn sàng khám
3. **CHECKED_IN** - Đã check-in tại phòng khám
4. **IN_PROGRESS** - Đang khám bệnh
5. **REVIEW** - Đã khám xong, chờ patient review
6. **COMPLETED** - Hoàn thành (sau review)
7. **CANCELLED** - Hủy (chung, không rõ ai hủy)
8. **CANCELLED_BY_PATIENT** - Hủy bởi patient
9. **CANCELLED_BY_DOCTOR** - Hủy bởi doctor
10. **REJECTED** - Bị từ chối bởi doctor
11. **EXPIRED** - Hết hạn (timeout sau 2 giờ)
12. **NO_SHOW** - Không đến khám (patient không đến)

---

## 🔍 Phân Tích Chi Tiết

### ✅ Trạng Thái Đã Đầy Đủ

#### 1. **Luồng Đặt Lịch**
- ✅ PENDING - Chờ doctor xác nhận
- ✅ CONFIRMED - Đã xác nhận
- ✅ REJECTED - Bị từ chối
- ✅ EXPIRED - Hết hạn

#### 2. **Luồng Khám Bệnh**
- ✅ CONFIRMED - Sẵn sàng khám
- ✅ CHECKED_IN - Đã check-in
- ✅ IN_PROGRESS - Đang khám
- ✅ REVIEW - Chờ review
- ✅ COMPLETED - Hoàn thành

#### 3. **Luồng Hủy**
- ✅ CANCELLED - Hủy chung
- ✅ CANCELLED_BY_PATIENT - Hủy bởi patient
- ✅ CANCELLED_BY_DOCTOR - Hủy bởi doctor
- ✅ NO_SHOW - Không đến khám

---

## ⚠️ Trạng Thái Có Thể Bổ Sung

### 1. **PAYMENT_PENDING** ⚠️ QUAN TRỌNG

**Vấn đề**: Hiện tại khi tạo appointment, status là PENDING nhưng thực tế có 2 giai đoạn:
- PENDING (chưa thanh toán) - Chờ thanh toán cọc
- PENDING (đã thanh toán) - Chờ doctor xác nhận

**Giải pháp**: Tách thành 2 trạng thái:
- `PAYMENT_PENDING` - Chờ thanh toán cọc (50,000 VND)
- `PENDING` - Đã thanh toán, chờ doctor xác nhận

**Lợi ích**:
- Phân biệt rõ ràng giữa chờ thanh toán và chờ doctor
- Dễ dàng filter và query
- Báo cáo chính xác hơn

**Luồng mới**:
```
PAYMENT_PENDING → (thanh toán) → PENDING → (doctor confirm) → CONFIRMED
```

---

### 2. **PAYMENT_FAILED** ⚠️ QUAN TRỌNG

**Vấn đề**: Nếu patient thanh toán thất bại, hiện tại không có trạng thái để đánh dấu.

**Giải pháp**: Thêm trạng thái `PAYMENT_FAILED`

**Luồng**:
```
PAYMENT_PENDING → (thanh toán thất bại) → PAYMENT_FAILED
PAYMENT_FAILED → (thử lại) → PAYMENT_PENDING
```

**Lợi ích**:
- Theo dõi được các lần thanh toán thất bại
- Có thể gửi thông báo nhắc nhở
- Thống kê tỷ lệ thanh toán thành công

---

### 3. **RESCHEDULED** / **RESCHEDULED_BY_PATIENT** / **RESCHEDULED_BY_DOCTOR**

**Vấn đề**: Hiện tại chưa có tính năng đổi lịch (reschedule), nhưng có thể cần trong tương lai.

**Giải pháp**: Thêm trạng thái để đánh dấu appointment đã được đổi lịch

**Luồng**:
```
CONFIRMED → (đổi lịch) → RESCHEDULED → CONFIRMED (với thời gian mới)
```

**Lợi ích**:
- Theo dõi lịch sử đổi lịch
- Phân biệt appointment mới và appointment đổi lịch
- Thống kê số lần đổi lịch

**Lưu ý**: Cần thêm tính năng reschedule trước khi thêm trạng thái này.

---

### 4. **WAITING** (Tùy chọn)

**Vấn đề**: Sau khi CHECKED_IN, có thể có thời gian chờ trước khi IN_PROGRESS.

**Giải pháp**: Thêm trạng thái `WAITING` giữa CHECKED_IN và IN_PROGRESS

**Luồng**:
```
CHECKED_IN → WAITING → IN_PROGRESS
```

**Lợi ích**:
- Theo dõi thời gian chờ của patient
- Thống kê thời gian chờ trung bình
- Cải thiện trải nghiệm patient

**Lưu ý**: Có thể không cần thiết nếu không có phòng chờ hoặc thời gian chờ ngắn.

---

### 5. **ON_HOLD** (Tùy chọn)

**Vấn đề**: Có thể có trường hợp cần tạm hoãn appointment (ví dụ: chờ kết quả xét nghiệm).

**Giải pháp**: Thêm trạng thái `ON_HOLD`

**Luồng**:
```
IN_PROGRESS → ON_HOLD → IN_PROGRESS (tiếp tục)
CONFIRMED → ON_HOLD → CONFIRMED (hoãn trước khi khám)
```

**Lợi ích**:
- Xử lý các trường hợp đặc biệt
- Theo dõi lý do hoãn

**Lưu ý**: Có thể không cần thiết nếu ít trường hợp đặc biệt.

---

## 📋 Đề Xuất Bổ Sung

### ⚠️ **QUAN TRỌNG - Nên Bổ Sung Ngay**

1. **PAYMENT_PENDING** - Tách rõ trạng thái chờ thanh toán
2. **PAYMENT_FAILED** - Đánh dấu thanh toán thất bại

### 💡 **Tùy Chọn - Có Thể Bổ Sung Sau**

3. **RESCHEDULED** - Khi có tính năng đổi lịch
4. **WAITING** - Nếu cần theo dõi thời gian chờ
5. **ON_HOLD** - Nếu có nhiều trường hợp đặc biệt

---

## 🔄 Luồng Trạng Thái Đề Xuất (Sau Khi Bổ Sung)

### Luồng Đặt Lịch (Có Payment)

```
[Patient tạo appointment]
    ↓
PAYMENT_PENDING (chờ thanh toán cọc)
    ├─→ PAYMENT_FAILED (thanh toán thất bại)
    │       └─→ PAYMENT_PENDING (thử lại)
    │
    └─→ PENDING (đã thanh toán, chờ doctor)
            ├─→ CONFIRMED (doctor confirm) ✅
            ├─→ REJECTED (doctor reject) ❌
            └─→ EXPIRED (timeout 2 giờ) ⏰
```

### Luồng Khám Bệnh

```
CONFIRMED
    ├─→ CHECKED_IN (đến phòng khám)
    │       └─→ WAITING (chờ khám) [Tùy chọn]
    │               └─→ IN_PROGRESS (bắt đầu khám)
    │                       └─→ REVIEW (hoàn thành khám)
    │                               └─→ COMPLETED (sau review) ✅
    │
    ├─→ NO_SHOW (không đến khám) ❌
    ├─→ CANCELLED_BY_PATIENT (patient hủy) ❌
    └─→ CANCELLED_BY_DOCTOR (doctor hủy) ❌
```

### Luồng Đổi Lịch (Tương Lai)

```
CONFIRMED → RESCHEDULED → CONFIRMED (với thời gian mới)
```

---

## 📊 Bảng So Sánh

| Trạng Thái | Hiện Tại | Đề Xuất | Mức Độ Ưu Tiên |
|------------|----------|---------|----------------|
| PAYMENT_PENDING | ❌ (dùng PENDING) | ✅ Thêm | 🔴 CAO |
| PAYMENT_FAILED | ❌ | ✅ Thêm | 🔴 CAO |
| RESCHEDULED | ❌ | ⚠️ Thêm khi có tính năng | 🟡 TRUNG BÌNH |
| WAITING | ❌ | ⚠️ Tùy chọn | 🟢 THẤP |
| ON_HOLD | ❌ | ⚠️ Tùy chọn | 🟢 THẤP |

---

## 🎯 Kết Luận

### Trạng Thái Hiện Tại: **Đã Đầy Đủ Cho Luồng Cơ Bản**

Tuy nhiên, với việc thêm hệ thống payment, nên bổ sung:

1. ✅ **PAYMENT_PENDING** - Tách rõ trạng thái chờ thanh toán
2. ✅ **PAYMENT_FAILED** - Đánh dấu thanh toán thất bại

Các trạng thái khác (RESCHEDULED, WAITING, ON_HOLD) có thể bổ sung sau khi cần thiết.

---

## 🔧 Implementation Plan

### Phase 1: Bổ Sung Trạng Thái Payment (Ưu Tiên Cao)

1. Thêm `PAYMENT_PENDING` và `PAYMENT_FAILED` vào enum/validation
2. Cập nhật `AppointmentService.createAppointment()` - set status = PAYMENT_PENDING
3. Cập nhật `PaymentService.processPayment()` - chuyển PAYMENT_PENDING → PENDING khi thanh toán thành công
4. Cập nhật `PaymentService.processPayment()` - chuyển PAYMENT_PENDING → PAYMENT_FAILED khi thanh toán thất bại
5. Cập nhật validation trong `AppointmentService.confirmAppointment()` - chỉ cho phép PENDING (đã thanh toán)
6. Cập nhật documentation

### Phase 2: Bổ Sung Trạng Thái Khác (Khi Cần)

- RESCHEDULED: Khi implement tính năng reschedule
- WAITING: Nếu cần theo dõi thời gian chờ
- ON_HOLD: Nếu có nhiều trường hợp đặc biệt
