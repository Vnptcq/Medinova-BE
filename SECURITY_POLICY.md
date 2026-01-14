# Chính Sách Bảo Mật - Security Policy

## 🔐 Nguyên Tắc Bảo Mật

**Tất cả các chức năng yêu cầu đăng nhập** - Guest không thể truy cập bất kỳ thông tin nào mà không đăng nhập.

---

## ✅ Endpoints Công Khai (Public - Không Cần Đăng Nhập)

### 1. Authentication Endpoints
- `POST /api/auth/login` - Đăng nhập
- `POST /api/auth/register` - Đăng ký
- `POST /api/auth/validate-token` - Validate token
- `POST /api/auth/logout` - Đăng xuất

### 2. Swagger/OpenAPI Documentation
- `/swagger-ui/**` - Swagger UI
- `/v3/api-docs/**` - OpenAPI documentation
- `/swagger-resources/**` - Swagger resources

### 3. Root Path
- `/` - Redirect to Swagger UI

---

## 🔒 Endpoints Yêu Cầu Đăng Nhập (Protected)

### Tất cả các endpoint khác đều yêu cầu authentication:

#### 1. Doctor Management
- `GET /api/doctors` - Xem danh sách bác sĩ
- `GET /api/doctors/{id}` - Xem thông tin bác sĩ
- `GET /api/doctors/search` - Tìm kiếm bác sĩ
- `GET /api/doctors/clinic/{clinicId}` - Xem bác sĩ theo phòng khám
- `GET /api/doctors/department/{department}` - Xem bác sĩ theo khoa
- Tất cả các endpoint khác về doctor

#### 2. Appointment Management
- `POST /api/appointments` - Đặt lịch khám
- `GET /api/appointments/my-appointments` - Xem lịch hẹn của mình
- `GET /api/appointments/doctors/{doctorId}/busy-schedules` - Xem lịch bận của bác sĩ
- Tất cả các endpoint khác về appointment

#### 3. Payment Management
- `POST /api/payments/appointments/{appointmentId}` - Tạo payment
- `POST /api/payments/{paymentId}/process` - Thanh toán
- `GET /api/payments/my-payments` - Xem payments của mình
- Tất cả các endpoint khác về payment

#### 4. Post/Blog Management
- `GET /api/posts/published` - Xem bài viết đã publish
- `GET /api/posts/{id}` - Xem chi tiết bài viết
- Tất cả các endpoint khác về posts

#### 5. Review Management
- `GET /api/reviews/{id}` - Xem review
- `GET /api/reviews/doctors/{doctorId}` - Xem reviews của bác sĩ
- Tất cả các endpoint khác về reviews

#### 6. Public Stats
- `GET /api/public/stats` - Xem thống kê công khai

#### 7. Tất cả các module khác
- Clinic Management
- User Profile
- Emergency
- Ambulance
- Pharmacy
- Blood Test
- Surgery
- Dashboard
- etc.

---

## 🎯 Lý Do

### 1. Bảo Vệ Thông Tin Cá Nhân
- Thông tin bác sĩ, lịch khám, reviews là thông tin nhạy cảm
- Chỉ người dùng đã đăng nhập mới được xem

### 2. Chống Abuse
- Ngăn chặn việc spam, scrape dữ liệu
- Yêu cầu đăng nhập giúp theo dõi và quản lý người dùng

### 3. Quản Lý Người Dùng
- Dễ dàng theo dõi hành vi người dùng
- Có thể áp dụng rate limiting, blocking

### 4. Bảo Mật Dữ Liệu
- Giảm thiểu rủi ro lộ thông tin
- Kiểm soát truy cập tốt hơn

---

## 📋 Luồng Đăng Nhập

```
1. Guest truy cập hệ thống
   └─> Chỉ thấy trang đăng nhập/đăng ký

2. Guest đăng ký tài khoản
   └─> POST /api/auth/register
   └─> Nhận JWT token

3. Guest đăng nhập
   └─> POST /api/auth/login
   └─> Nhận JWT token

4. Sử dụng token để truy cập các chức năng
   └─> Header: Authorization: Bearer <token>
   └─> Có thể xem thông tin bác sĩ, đặt lịch, etc.
```

---

## 🔧 Implementation

### SecurityConfig.java
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/").permitAll()
    .requestMatchers("/api/auth/**").permitAll() // Chỉ auth endpoints
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll() // Swagger
    .anyRequest().authenticated() // Tất cả khác yêu cầu đăng nhập
)
```

### JwtAuthenticationFilter.java
```java
// Chỉ skip cho auth endpoints và Swagger
if (path.equals("/") || path.startsWith("/swagger-ui") || 
    path.startsWith("/v3/api-docs") || path.startsWith("/api/auth")) {
    filterChain.doFilter(request, response);
    return;
}
```

---

## ⚠️ Lưu Ý

1. **Frontend cần xử lý**:
   - Redirect về trang đăng nhập nếu chưa đăng nhập
   - Lưu JWT token sau khi đăng nhập
   - Gửi token trong header mỗi request

2. **Error Handling**:
   - 401 Unauthorized: Token không hợp lệ hoặc hết hạn
   - 403 Forbidden: Không có quyền truy cập
   - Frontend cần xử lý và redirect về login

3. **Token Expiration**:
   - Token hết hạn sau 24 giờ
   - Cần refresh token hoặc đăng nhập lại

---

## 📊 So Sánh Trước và Sau

### Trước (Có Public Endpoints)
- ✅ Guest có thể xem thông tin bác sĩ
- ✅ Guest có thể xem lịch bận
- ✅ Guest có thể xem bài viết, reviews
- ❌ Dễ bị abuse, scrape dữ liệu

### Sau (Yêu Cầu Đăng Nhập)
- ✅ Guest phải đăng nhập để xem bất kỳ thông tin nào
- ✅ Bảo vệ thông tin tốt hơn
- ✅ Quản lý người dùng dễ dàng hơn
- ✅ Chống abuse hiệu quả hơn

---

## 🎯 Kết Luận

**Tất cả các chức năng yêu cầu đăng nhập** - Đây là chính sách bảo mật nghiêm ngặt để:
- Bảo vệ thông tin cá nhân
- Chống abuse và spam
- Quản lý người dùng tốt hơn
- Tuân thủ các quy định về bảo mật dữ liệu
