# Hướng Dẫn Generate QR Code Cho Thanh Toán VNPAY

## 🤔 Câu Hỏi: Generate QR Code Ở Đâu?

Có **2 cách** để xử lý QR code với VNPAY:

---

## 📋 **CÁCH 1: VNPAY Tự Hiển Thị QR Code (Hiện Tại - Đơn Giản Nhất)**

### **Flow:**

```
1. FE gọi API: POST /api/payments/{paymentId}/vnpay/create
   ↓
2. BE trả về paymentUrl
   ↓
3. FE redirect user đến paymentUrl
   ↓
4. VNPAY tự động hiển thị QR code trên trang của họ
   ↓
5. User quét QR code → Thanh toán
```

### **Ưu điểm:**
- ✅ **Đơn giản**: Không cần generate QR code
- ✅ **Bảo mật**: VNPAY tự xử lý
- ✅ **UI/UX tốt**: VNPAY có UI chuyên nghiệp
- ✅ **Không cần thêm dependency**: FE không cần library QR code

### **Nhược điểm:**
- ❌ User phải rời khỏi trang của bạn
- ❌ Không custom được UI QR code

### **Code Frontend (Hiện Tại):**

```javascript
// Frontend code
const response = await fetch(`/api/payments/${paymentId}/vnpay/create`, {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const data = await response.json();
// Redirect user đến VNPAY
window.location.href = data.paymentUrl;
```

---

## 📋 **CÁCH 2: FE Generate QR Code Từ Payment URL (Đề Xuất)**

### **Flow:**

```
1. FE gọi API: POST /api/payments/{paymentId}/vnpay/create
   ↓
2. BE trả về paymentUrl
   ↓
3. FE generate QR code từ paymentUrl (dùng library như qrcode.react, react-qr-code)
   ↓
4. FE hiển thị QR code trên trang của bạn
   ↓
5. User quét QR code → Redirect đến VNPAY → Thanh toán
```

### **Ưu điểm:**
- ✅ **User ở lại trang**: Không cần rời khỏi ứng dụng
- ✅ **Custom UI**: Có thể thiết kế UI đẹp hơn
- ✅ **Trải nghiệm tốt hơn**: User có thể xem thông tin payment ngay trên trang

### **Nhược điểm:**
- ❌ Cần thêm dependency (QR code library)
- ❌ Cần xử lý thêm code

### **Code Frontend (Với QR Code):**

```bash
# Install QR code library
npm install qrcode.react
# hoặc
npm install react-qr-code
```

```typescript
// Frontend code với QR code
import { QRCodeSVG } from 'qrcode.react';
// hoặc
import QRCode from 'react-qr-code';

const PaymentPage = () => {
  const [paymentUrl, setPaymentUrl] = useState<string | null>(null);
  
  const createPayment = async () => {
    const response = await fetch(`/api/payments/${paymentId}/vnpay/create`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    const data = await response.json();
    setPaymentUrl(data.paymentUrl);
  };
  
  return (
    <div>
      {paymentUrl && (
        <>
          <QRCodeSVG value={paymentUrl} size={256} />
          {/* hoặc */}
          <QRCode value={paymentUrl} size={256} />
          
          <p>Hoặc click để thanh toán:</p>
          <a href={paymentUrl}>Thanh toán qua VNPAY</a>
        </>
      )}
    </div>
  );
};
```

---

## 🎯 **Đề Xuất: CÁCH 2 (FE Generate QR Code)**

### **Lý Do:**

1. **Trải nghiệm tốt hơn**: User không cần rời khỏi trang
2. **UI/UX tốt hơn**: Có thể thiết kế trang payment đẹp
3. **Linh hoạt**: Có thể hiển thị thêm thông tin (số tiền, orderInfo, warning message)

### **Implementation:**

#### **1. Backend (Đã có sẵn):**

Backend đã trả về `paymentUrl` trong response:

```json
{
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "paymentId": 1,
  "amount": 50000.0,
  "orderInfo": "NGUYEN VAN A - 15/02/2025 10:00 - Chuyen khoan",
  "warningMessage": "⚠️ QUAN TRỌNG - VUI LÒNG ĐỌC KỸ:..."
}
```

#### **2. Frontend (Cần implement):**

**Bước 1**: Install QR code library

```bash
cd Medinova-FE
npm install qrcode.react
# hoặc
npm install react-qr-code
```

**Bước 2**: Tạo component Payment với QR code

```typescript
// src/app/payment/[paymentId]/page.tsx
'use client';

import { useState, useEffect } from 'react';
import { QRCodeSVG } from 'qrcode.react';

export default function PaymentPage({ params }: { params: { paymentId: string } }) {
  const [paymentData, setPaymentData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    const createPaymentUrl = async () => {
      try {
        const token = localStorage.getItem('token');
        const response = await fetch(`/api/payments/${params.paymentId}/vnpay/create`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });
        
        const data = await response.json();
        setPaymentData(data);
      } catch (error) {
        console.error('Error creating payment URL:', error);
      } finally {
        setLoading(false);
      }
    };
    
    createPaymentUrl();
  }, [params.paymentId]);
  
  if (loading) {
    return <div>Đang tạo mã thanh toán...</div>;
  }
  
  if (!paymentData) {
    return <div>Lỗi khi tạo mã thanh toán</div>;
  }
  
  return (
    <div className="payment-page">
      <h1>Thanh Toán Cọc Đặt Lịch</h1>
      
      {/* Warning Message */}
      {paymentData.warningMessage && (
        <div className="warning-box">
          <pre>{paymentData.warningMessage}</pre>
        </div>
      )}
      
      {/* QR Code */}
      <div className="qr-code-container">
        <h2>Quét mã QR để thanh toán</h2>
        <QRCodeSVG 
          value={paymentData.paymentUrl} 
          size={300}
          level="M"
          includeMargin={true}
        />
        <p className="amount">Số tiền: {paymentData.amount.toLocaleString('vi-VN')} VND</p>
        <p className="order-info">{paymentData.orderInfo}</p>
      </div>
      
      {/* Alternative: Direct Link */}
      <div className="alternative-payment">
        <p>Hoặc click để thanh toán trực tiếp:</p>
        <a 
          href={paymentData.paymentUrl} 
          className="payment-button"
          target="_blank"
        >
          Thanh Toán Qua VNPAY
        </a>
      </div>
    </div>
  );
}
```

---

## 📊 So Sánh 2 Cách

| Tiêu chí | Cách 1: VNPAY Hiển Thị | Cách 2: FE Generate QR |
|----------|------------------------|------------------------|
| **Độ phức tạp** | ⭐ Rất đơn giản | ⭐⭐ Đơn giản |
| **User Experience** | ⭐⭐ Trung bình | ⭐⭐⭐ Tốt |
| **Custom UI** | ❌ Không | ✅ Có |
| **Dependency** | ✅ Không cần | ❌ Cần library QR code |
| **Bảo mật** | ✅ VNPAY xử lý | ✅ VNPAY vẫn xử lý |
| **Đề xuất** | Cho prototype | ✅ **Cho production** |

---

## 🎯 Kết Luận

### **Hiện Tại (Cách 1):**
- ✅ Backend đã trả về `paymentUrl`
- ✅ Frontend chỉ cần redirect
- ✅ Đơn giản, nhanh

### **Đề Xuất (Cách 2):**
- ✅ Frontend generate QR code từ `paymentUrl`
- ✅ User có thể quét QR ngay trên trang
- ✅ Trải nghiệm tốt hơn
- ✅ Vẫn có option redirect trực tiếp

---

## 📝 Next Steps

1. **Backend**: ✅ Đã sẵn sàng (trả về `paymentUrl`)
2. **Frontend**: 
   - [ ] Install QR code library
   - [ ] Tạo payment page với QR code
   - [ ] Hiển thị warning message
   - [ ] Hiển thị thông tin payment

---

## 📚 QR Code Libraries Cho React/Next.js

1. **qrcode.react** (Đề xuất)
   ```bash
   npm install qrcode.react
   ```
   - ✅ Lightweight
   - ✅ TypeScript support
   - ✅ SSR compatible

2. **react-qr-code**
   ```bash
   npm install react-qr-code
   ```
   - ✅ Simple API
   - ✅ Customizable

3. **qrcode.js** (Vanilla JS)
   ```bash
   npm install qrcode
   ```
   - ✅ More control
   - ⚠️ Cần xử lý canvas manually

---

## 🔍 Backend Response Hiện Tại

Backend đã trả về đầy đủ thông tin:

```json
{
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "paymentId": 1,
  "amount": 50000.0,
  "orderInfo": "NGUYEN VAN A - 15/02/2025 10:00 - Chuyen khoan",
  "warningMessage": "⚠️ QUAN TRỌNG - VUI LÒNG ĐỌC KỸ:\n\n..."
}
```

Frontend có thể:
- Generate QR code từ `paymentUrl`
- Hiển thị `orderInfo` (nội dung chuyển khoản)
- Hiển thị `warningMessage` (cảnh báo quan trọng)
- Redirect user đến `paymentUrl` nếu cần
