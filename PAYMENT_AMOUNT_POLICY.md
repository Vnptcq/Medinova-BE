# Chính Sách Số Tiền Thanh Toán Cọc

## 📋 Tổng Quan

Hệ thống chấp nhận thanh toán cọc với các quy tắc sau:

### **1. Số Tiền Tối Thiểu**

- ✅ **Chấp nhận**: Số tiền >= **100,000 VND** (một trăm nghìn đồng)
- ❌ **Từ chối**: Số tiền < 100,000 VND

**Lý do**: Để đảm bảo tính nghiêm túc của việc đặt lịch và tránh abuse.

---

### **2. Số Tiền Yêu Cầu**

- **Số tiền yêu cầu**: 50,000 VND (năm mươi nghìn đồng)
- **Số tiền tối thiểu chấp nhận**: 100,000 VND (một trăm nghìn đồng)

**Lưu ý**: 
- Nếu chuyển đúng 50,000 VND → **KHÔNG được chấp nhận** (phải >= 100,000 VND)
- Nếu chuyển >= 100,000 VND → **Được chấp nhận**

---

## 💰 Các Trường Hợp

### **Trường Hợp 1: Chuyển Đúng 100,000 VND** ✅

- **Kết quả**: Thanh toán được chấp nhận
- **Appointment**: Tự động CONFIRMED
- **Số lần đặt lịch**: 1 lần

---

### **Trường Hợp 2: Chuyển Thừa (> 100,000 VND)** ⚠️

**Ví dụ**: Chuyển 150,000 VND hoặc 200,000 VND

- **Kết quả**: Thanh toán được chấp nhận
- **Appointment**: Tự động CONFIRMED
- **Số tiền thừa**: Được lưu trong log
- **Xử lý**: Admin sẽ xem log và xử lý sau

**Log sẽ ghi**:
```
⚠️ PAYMENT EXCESS - Payment ID: 1, Patient ID: 1, Expected: 50000 VND, Actual: 150000 VND, Excess: 100000 VND. Admin should review transaction log.
```

**Admin có thể**:
- Xem log chi tiết trong hệ thống
- Liên hệ với bệnh nhân để hoàn tiền thừa (nếu cần)
- Hoặc giữ lại làm credit cho lần đặt lịch tiếp theo

---

### **Trường Hợp 3: Chuyển Thiếu (< 100,000 VND)** ❌

**Ví dụ**: Chuyển 50,000 VND hoặc 80,000 VND

- **Kết quả**: Thanh toán **KHÔNG được chấp nhận**
- **Appointment**: Vẫn ở trạng thái PENDING
- **Xử lý**: Sử dụng công thức tính số lần đặt lịch

**Công thức**:
```
Số lần đặt lịch khám được xác nhận = Tổng số tiền đặt cọc trong 7 ngày / 100,000 VND
```

**Ví dụ**:
- Bệnh nhân chuyển 50,000 VND → Không đủ 1 lần (0.5 lần)
- Bệnh nhân chuyển thêm 50,000 VND nữa trong 7 ngày → Tổng = 100,000 VND → Đủ 1 lần
- Bệnh nhân chuyển 150,000 VND → Đủ 1.5 lần (làm tròn = 1 lần)
- Bệnh nhân chuyển 250,000 VND → Đủ 2.5 lần (làm tròn = 2 lần)

---

## 📊 Tính Số Lần Đặt Lịch (Khi Chuyển Thiếu)

### **Công Thức**

```
Số lần đặt lịch = floor(Tổng số tiền đặt cọc trong 7 ngày / 100,000 VND)
```

**⚠️ QUAN TRỌNG**: **PHẢI** làm tròn **XUỐNG** (floor), **KHÔNG** được làm tròn lên (ceiling).

**Lý do bảo mật kinh tế**:
- ❌ Nếu làm tròn **LÊN**: 50,000 VND → 1 lần → **LỖ HỔNG KINH TẾ!**
- ❌ Nếu làm tròn **LÊN**: 150,000 VND → 2 lần → **LỖ HỔNG KINH TẾ!**
- ✅ Làm tròn **XUỐNG**: 150,000 VND → 1 lần → **ĐÚNG, BẢO VỆ HỆ THỐNG**

**Ví dụ**:
- 150,000 VND / 100,000 = 1.5 → Làm tròn xuống = **1 lần**
- 250,000 VND / 100,000 = 2.5 → Làm tròn xuống = **2 lần**
- 199,999 VND / 100,000 = 1.99999 → Làm tròn xuống = **1 lần**

### **Ví Dụ Cụ Thể**

| Tổng tiền trong 7 ngày | Số lần đặt lịch | Ghi chú |
|------------------------|-----------------|---------|
| 50,000 VND | 0 | Chưa đủ 1 lần |
| 100,000 VND | 1 | Đủ 1 lần |
| 150,000 VND | 1 | Đủ 1 lần (làm tròn XUỐNG: 1.5 → 1) |
| 200,000 VND | 2 | Đủ 2 lần |
| 250,000 VND | 2 | Đủ 2 lần (làm tròn XUỐNG: 2.5 → 2) |
| 300,000 VND | 3 | Đủ 3 lần |
| 350,000 VND | 3 | Đủ 3 lần (làm tròn XUỐNG: 3.5 → 3) |

### **API Endpoint**

**GET** `/api/payments/patients/{patientId}/confirmed-appointments-count`

**Response**:
```json
{
  "patientId": 1,
  "confirmedAppointmentsCount": 2,
  "formula": "Total deposit amount in last 7 days / 100,000 VND",
  "note": "This is used when patient transfers insufficient amount. Each 100,000 VND = 1 confirmed appointment."
}
```

---

## 🔍 Logging Cho Admin

Hệ thống tự động ghi log chi tiết cho admin:

### **1. Log Thanh Toán Thành Công**

```
💰 PAYMENT CONFIRMED - Payment ID: 1, Expected: 50000 VND, Actual: 100000 VND
```

### **2. Log Thanh Toán Thừa**

```
⚠️ PAYMENT EXCESS - Payment ID: 1, Patient ID: 1, Expected: 50000 VND, Actual: 150000 VND, Excess: 100000 VND. Admin should review transaction log.
```

### **3. Log Thanh Toán Thiếu**

```
⚠️ PAYMENT REJECTED - Amount too low! Payment ID: 1, Expected: 50000 VND, Received: 50000 VND (Minimum: 100000 VND)
```

### **4. Log Tính Số Lần Đặt Lịch**

```
📊 APPOINTMENT CREDIT CALCULATION - Patient ID: 1, Total paid in 7 days: 250000 VND, Confirmed appointments: 2
```

---

## 📋 API Endpoints Cho Admin

### **1. Xem Payments Của Patient Trong 7 Ngày**

**GET** `/api/payments/patients/{patientId}/payments-last-7-days`

**Response**: Danh sách tất cả payments đã thanh toán trong 7 ngày

**Dùng để**:
- Kiểm tra số tiền thừa
- Xem lịch sử thanh toán
- Xử lý khiếu nại

---

### **2. Tính Số Lần Đặt Lịch**

**GET** `/api/payments/patients/{patientId}/confirmed-appointments-count`

**Response**: Số lần đặt lịch được xác nhận dựa trên tổng tiền

**Dùng để**:
- Xác nhận số lần đặt lịch khi bệnh nhân chuyển thiếu
- Tính toán credit cho bệnh nhân

---

## ⚠️ Cảnh Báo Cho Người Dùng

Hệ thống tự động hiển thị cảnh báo khi tạo VNPAY payment URL:

```
⚠️ QUAN TRỌNG - VUI LÒNG ĐỌC KỸ:

1. TÊN TÀI KHOẢN: Bạn BẮT BUỘC phải sử dụng tên tài khoản ĐÚNG với tên tài khoản ngân hàng của bạn. 
   Nếu tên không khớp, giao dịch có thể bị từ chối hoặc không được xác nhận.

2. SỐ TIỀN: Số tiền tối thiểu để chấp nhận là 100,000 VND. 
   Nếu bạn chuyển thừa, admin sẽ xem log và xử lý sau.

3. CHUYỂN THIẾU: Nếu chuyển thiếu (< 100,000 VND), giao dịch sẽ KHÔNG được xác nhận. 
   Số lần đặt lịch khám được xác nhận = Tổng số tiền đặt cọc trong 7 ngày / 100,000 VND.

4. VẤN ĐỀ KHÁC: Mọi vấn đề liên quan đến chuyển khoản nhầm, chuyển thừa, hoặc chuyển thiếu, 
   vui lòng liên hệ admin để được hỗ trợ. Admin có thể xem log chi tiết để kiểm tra.
```

---

## 📞 Liên Hệ Hỗ Trợ

Nếu bạn gặp vấn đề:

1. **Kiểm tra log** trong hệ thống (Admin)
2. **Liên hệ hotline**: 1900-xxxx
3. **Email hỗ trợ**: support@medinova.com

Admin sẽ xem log chi tiết để kiểm tra và xử lý.

---

## ✅ Tóm Tắt

- ✅ **Chấp nhận**: >= 100,000 VND
- ⚠️ **Chuyển thừa**: Admin xem log và xử lý sau
- ❌ **Chuyển thiếu**: Tính số lần đặt lịch = Tổng tiền trong 7 ngày / 100,000 VND
- 📊 **Logging**: Tự động ghi log chi tiết cho admin
- 🔍 **Admin tools**: API để xem payments và tính số lần đặt lịch
