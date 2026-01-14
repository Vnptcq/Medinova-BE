# ⚠️ CẢNH BÁO QUAN TRỌNG - Thanh Toán VNPAY

## 🚨 LƯU Ý BẮT BUỘC KHI THANH TOÁN

### **1. Tên Tài Khoản Phải Đúng**

**⚠️ QUAN TRỌNG**: Khi thanh toán qua VNPAY, bạn **BẮT BUỘC** phải sử dụng tên tài khoản **ĐÚNG** với tên tài khoản ngân hàng của bạn.

**Hậu quả nếu tên không khớp**:
- ❌ Giao dịch có thể bị từ chối
- ❌ Thanh toán không được xác nhận
- ❌ Tiền có thể bị hoàn lại nhưng mất thời gian
- ❌ Appointment có thể bị hủy do thanh toán thất bại

**Ví dụ**:
- ✅ **ĐÚNG**: Tài khoản ngân hàng là "NGUYEN VAN A" → Nhập "NGUYEN VAN A"
- ❌ **SAI**: Tài khoản ngân hàng là "NGUYEN VAN A" → Nhập "NGUYỄN VĂN A" (có dấu)
- ❌ **SAI**: Tài khoản ngân hàng là "NGUYEN VAN A" → Nhập "NGUYEN VAN B" (sai tên)

---

### **2. Số Tiền Phải Chính Xác**

**⚠️ QUAN TRỌNG**: Số tiền cọc là **50,000 VND** (năm mươi nghìn đồng).

**Hệ thống sẽ tự động kiểm tra**:
- ✅ Số tiền từ VNPAY callback phải khớp với số tiền trong hệ thống
- ✅ Nếu không khớp, hệ thống sẽ ghi log cảnh báo
- ✅ Giao dịch vẫn có thể được xác nhận nếu sai lệch nhỏ (do làm tròn)

**Lưu ý**:
- VNPAY trả về số tiền đã nhân 100 (ví dụ: 5000000 = 50,000 VND)
- Hệ thống tự động chia cho 100 để lấy số tiền thực tế

---

### **3. Thông Tin VNPAY Callback**

VNPAY callback **KHÔNG** trả về:
- ❌ Tên chủ thẻ/tài khoản (vì lý do bảo mật)
- ❌ Số tài khoản (vì lý do bảo mật)

VNPAY callback **CÓ** trả về:
- ✅ `vnp_Amount`: Số tiền (đã nhân 100)
- ✅ `vnp_TransactionNo`: Mã giao dịch VNPAY
- ✅ `vnp_ResponseCode`: Mã phản hồi (00 = thành công)
- ✅ `vnp_BankCode`: Mã ngân hàng
- ✅ `vnp_CardType`: Loại thẻ (nếu có)

**Vì vậy**, hệ thống **KHÔNG THỂ** tự động xác nhận tên tài khoản. Người dùng **PHẢI** tự đảm bảo nhập đúng tên.

---

## 📋 Checklist Trước Khi Thanh Toán

Trước khi xác nhận thanh toán trên VNPAY, vui lòng kiểm tra:

- [ ] **Tên tài khoản** phải ĐÚNG với tên trong tài khoản ngân hàng
- [ ] **Số tiền** là 50,000 VND (năm mươi nghìn đồng)
- [ ] **Thông tin thẻ/tài khoản** chính xác
- [ ] **OTP** (nếu có) đã sẵn sàng

---

## 🔍 Xác Nhận Sau Khi Thanh Toán

Sau khi thanh toán thành công, hệ thống sẽ:

1. ✅ **Xác nhận số tiền** từ VNPAY callback
2. ✅ **Lưu transaction ID** từ VNPAY
3. ✅ **Cập nhật payment status**: `PENDING` → `PAID`
4. ✅ **Cập nhật appointment status**: `PENDING` → `CONFIRMED`
5. ✅ **Gửi email** thông báo lịch khám

**Nếu có bất kỳ sai lệch nào**, hệ thống sẽ:
- ⚠️ Ghi log cảnh báo
- ⚠️ Vẫn tiếp tục xử lý (nếu sai lệch nhỏ)
- ❌ Từ chối giao dịch (nếu sai lệch lớn)

---

## 📞 Liên Hệ Hỗ Trợ

Nếu bạn gặp vấn đề với thanh toán:

1. **Kiểm tra email** thông báo từ hệ thống
2. **Kiểm tra payment status** trong tài khoản
3. **Liên hệ hotline**: 1900-xxxx
4. **Email hỗ trợ**: support@medinova.com

---

## ⚠️ TÓM TẮT

**QUAN TRỌNG NHẤT**: 
- ✅ **Tên tài khoản PHẢI ĐÚNG** với tài khoản ngân hàng của bạn
- ✅ **Số tiền PHẢI ĐÚNG**: 50,000 VND
- ✅ **Kiểm tra kỹ** trước khi xác nhận thanh toán

**Hệ thống sẽ tự động xác nhận số tiền, nhưng KHÔNG THỂ xác nhận tên tài khoản. Bạn phải tự đảm bảo nhập đúng!**
