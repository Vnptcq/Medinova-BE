# Tài Liệu Backend - Medinova

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Kiến Trúc](#kiến-trúc)
3. [Cấu Trúc Thư Mục](#cấu-trúc-thư-mục)
4. [Các Module Chính](#các-module-chính)
5. [API Endpoints](#api-endpoints)
6. [Database Schema](#database-schema)
7. [Security & Authentication](#security--authentication)
8. [Configuration](#configuration)
9. [Testing](#testing)

---

## 🎯 Tổng Quan

Backend của Medinova được xây dựng bằng **Spring Boot 4.0.1** với **Java 21**, sử dụng:
- **Spring Data JPA** cho database operations
- **Spring Security** + **JWT** cho authentication
- **PostgreSQL** làm database
- **SpringDoc OpenAPI** cho API documentation
- **Maven** làm build tool

---

## 🏗️ Kiến Trúc

### Layered Architecture

```
┌─────────────────────────────────────┐
│      Controllers (REST API)         │
│   - AuthController                  │
│   - UserController                  │
│   - DoctorController                │
│   - AppointmentController           │
│   - ...                             │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Services (Business Logic)      │
│   - AuthService                     │
│   - UserService                     │
│   - DoctorService                   │
│   - AppointmentService             │
│   - ...                             │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Repositories (Data Access)     │
│   - UserRepository                  │
│   - DoctorRepository                │
│   - AppointmentRepository           │
│   - ...                             │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Entities (Domain Model)        │
│   - User                            │
│   - Doctor                          │
│   - Appointment                     │
│   - ...                             │
└─────────────────────────────────────┘
```

---

## 📁 Cấu Trúc Thư Mục

```
src/main/java/com/project/medinova/
├── MedinovaApplication.java          # Main application class
│
├── config/                           # Configuration classes
│   ├── SecurityConfig.java           # Spring Security configuration
│   ├── JwtAuthenticationFilter.java  # JWT filter
│   ├── JwtTokenProvider.java         # JWT token utilities
│   ├── OpenApiConfig.java            # OpenAPI/Swagger config
│   └── DataInitializer.java          # Data initialization
│
├── controller/                       # REST Controllers
│   ├── AuthController.java
│   ├── UserController.java
│   ├── UserProfileController.java
│   ├── DoctorController.java
│   ├── ClinicController.java
│   ├── AppointmentController.java
│   ├── EmergencyController.java
│   ├── AmbulanceController.java
│   ├── AmbulanceBookingController.java
│   ├── LeaveRequestController.java
│   ├── BloodTestController.java
│   ├── PharmacyController.java
│   ├── SurgeryController.java
│   ├── PostController.java
│   ├── ReviewController.java
│   ├── RankingController.java
│   ├── DashboardController.java
│   └── PublicController.java
│
├── service/                          # Business logic layer
│   ├── AuthService.java
│   ├── UserService.java
│   ├── UserProfileService.java
│   ├── DoctorService.java
│   ├── ClinicService.java
│   ├── AppointmentService.java
│   ├── AppointmentSchedulerService.java
│   ├── EmergencyService.java
│   ├── AmbulanceService.java
│   ├── AmbulanceBookingService.java
│   ├── LeaveRequestService.java
│   ├── BloodTestService.java
│   ├── PharmacyService.java
│   ├── SurgeryService.java
│   ├── PostService.java
│   ├── ReviewService.java
│   ├── RankingService.java
│   ├── DashboardService.java
│   ├── PublicService.java
│   └── CustomUserDetailsService.java
│
├── repository/                       # Data access layer
│   ├── UserRepository.java
│   ├── UserProfileRepository.java
│   ├── DoctorRepository.java
│   ├── ClinicRepository.java
│   ├── AppointmentRepository.java
│   ├── DoctorScheduleRepository.java
│   ├── EmergencyRepository.java
│   ├── EmergencyAssignmentRepository.java
│   ├── AmbulanceRepository.java
│   ├── AmbulanceBookingRepository.java
│   ├── LeaveRequestRepository.java
│   ├── BloodTestRepository.java
│   ├── PharmacyOrderRepository.java
│   ├── PharmacyOrderItemRepository.java
│   ├── SurgeryConsultationRepository.java
│   ├── PostRepository.java
│   ├── PostCommentRepository.java
│   ├── DoctorReviewRepository.java
│   ├── PatientMedicalHistoryRepository.java
│   ├── MedicalRecordRepository.java
│   └── DoctorWorkingDaysRepository.java
│
├── entity/                           # JPA Entities
│   ├── User.java
│   ├── UserProfile.java
│   ├── Doctor.java
│   ├── Clinic.java
│   ├── Appointment.java
│   ├── DoctorSchedule.java
│   ├── DoctorWorkingDays.java
│   ├── Emergency.java
│   ├── EmergencyAssignment.java
│   ├── Ambulance.java
│   ├── AmbulanceBooking.java
│   ├── DoctorLeaveRequest.java
│   ├── BloodTest.java
│   ├── PharmacyOrder.java
│   ├── PharmacyOrderItem.java
│   ├── SurgeryConsultation.java
│   ├── MedicalRecord.java
│   ├── PatientMedicalHistory.java
│   ├── Post.java
│   ├── PostComment.java
│   └── DoctorReview.java
│
├── dto/                              # Data Transfer Objects
│   ├── AuthRequest.java
│   ├── AuthResponse.java
│   ├── RegisterRequest.java
│   ├── CreateDoctorRequest.java
│   ├── UpdateDoctorRequest.java
│   ├── CreateAppointmentRequest.java
│   ├── AppointmentResponse.java
│   └── ...
│
└── exception/                        # Exception handling
    ├── GlobalExceptionHandler.java
    ├── NotFoundException.java
    ├── BadRequestException.java
    ├── UnauthorizedException.java
    └── ForbiddenException.java
```

---

## 🔧 Các Module Chính

### 1. Authentication Module (`AuthController`, `AuthService`)

**Chức năng:**
- Đăng ký người dùng mới (PATIENT)
- Đăng nhập và nhận JWT token
- Đăng xuất
- Validate JWT token
- Lấy thông tin user hiện tại

**Endpoints:**
- `POST /api/auth/register` - Đăng ký
- `POST /api/auth/login` - Đăng nhập
- `POST /api/auth/logout` - Đăng xuất
- `GET /api/auth/me` - Lấy thông tin user hiện tại
- `POST /api/auth/validate-token` - Validate token

### 2. User Management Module (`UserController`, `UserService`)

**Chức năng:**
- Quản lý danh sách users (ADMIN only)
- Cập nhật role của user (ADMIN only)
- Xem thông tin user

**Endpoints:**
- `GET /api/users` - Lấy tất cả users (ADMIN)
- `GET /api/users/{id}` - Lấy user theo ID (ADMIN)
- `PUT /api/users/{id}/role` - Cập nhật role (ADMIN)

### 3. User Profile Module (`UserProfileController`, `UserProfileService`)

**Chức năng:**
- Quản lý profile của user
- Quản lý lịch sử y tế (Medical History)

**Endpoints:**
- `GET /api/profile` - Lấy profile hiện tại
- `PUT /api/profile` - Cập nhật profile
- `GET /api/profile/medical-history` - Lấy lịch sử y tế
- `PUT /api/profile/medical-history` - Cập nhật lịch sử y tế

### 4. Doctor Management Module (`DoctorController`, `DoctorService`)

**Chức năng:**
- Tạo profile bác sĩ (ADMIN only)
- Tìm kiếm bác sĩ (public)
- Quản lý thông tin bác sĩ
- Phê duyệt/từ chối bác sĩ (ADMIN only)

**Endpoints:**
- `POST /api/doctors` - Tạo bác sĩ (ADMIN)
- `GET /api/doctors` - Lấy tất cả bác sĩ (pagination)
- `GET /api/doctors/{id}` - Lấy bác sĩ theo ID
- `GET /api/doctors/search` - Tìm kiếm bác sĩ (public)
- `GET /api/doctors/clinic/{clinicId}` - Lấy bác sĩ theo clinic
- `GET /api/doctors/specialization/{specialization}` - Lấy bác sĩ theo chuyên khoa
- `GET /api/doctors/pending` - Lấy bác sĩ chờ phê duyệt (ADMIN)
- `PUT /api/doctors/{id}` - Cập nhật bác sĩ (ADMIN/DOCTOR)
- `PUT /api/doctors/{id}/status` - Cập nhật status (ADMIN)
- `DELETE /api/doctors/{id}` - Xóa bác sĩ (ADMIN)

### 5. Clinic Management Module (`ClinicController`, `ClinicService`)

**Chức năng:**
- Quản lý phòng khám (ADMIN only)
- CRUD operations cho clinics

**Endpoints:**
- `POST /api/clinics` - Tạo clinic (ADMIN)
- `GET /api/clinics` - Lấy tất cả clinics
- `GET /api/clinics/{id}` - Lấy clinic theo ID
- `PUT /api/clinics/{id}` - Cập nhật clinic (ADMIN)
- `DELETE /api/clinics/{id}` - Xóa clinic (ADMIN)

### 6. Appointment Management Module (`AppointmentController`, `AppointmentService`)

**Chức năng:**
- Đặt lịch khám với bác sĩ (PATIENT)
- Quản lý lịch hẹn
- Xác nhận lịch hẹn (trong 5 phút)
- Hủy lịch hẹn
- Xem lịch bận của bác sĩ (public)

**Endpoints:**
- `POST /api/appointments` - Tạo appointment (PATIENT) - Hold slot 5 phút
- `GET /api/appointments/my-appointments` - Lấy appointments của user hiện tại
- `GET /api/appointments` - Lấy appointments với pagination (filter by doctorId)
- `GET /api/appointments/today` - Lấy appointments hôm nay
- `GET /api/appointments/doctors/{doctorId}/busy-schedules` - Lấy lịch bận (public)
- `PUT /api/appointments/{id}/confirm` - Xác nhận appointment (PATIENT)
- `PUT /api/appointments/{id}/status` - Cập nhật status (PATIENT)

**Quy trình đặt lịch:**
1. Patient tạo appointment → Tạo DoctorSchedule với status HOLD
2. Slot được giữ trong 5 phút
3. Patient xác nhận trong 5 phút → Status chuyển thành CONFIRMED, schedule chuyển thành BOOKED
4. Nếu không xác nhận trong 5 phút → Slot tự động được giải phóng

### 7. Emergency Management Module (`EmergencyController`, `EmergencyService`)

**Chức năng:**
- Tạo yêu cầu khẩn cấp
- Phân công bác sĩ và xe cứu thương
- Cập nhật trạng thái emergency

**Endpoints:**
- `POST /api/emergencies` - Tạo emergency request
- `GET /api/emergencies` - Lấy danh sách emergencies
- `GET /api/emergencies/{id}` - Lấy emergency theo ID
- `PUT /api/emergencies/{id}/status` - Cập nhật status
- `POST /api/emergencies/{id}/assign` - Phân công bác sĩ và xe cứu thương

### 8. Ambulance Management Module (`AmbulanceController`, `AmbulanceService`)

**Chức năng:**
- Quản lý xe cứu thương
- Cập nhật vị trí và trạng thái xe

**Endpoints:**
- `POST /api/ambulances` - Tạo xe cứu thương (ADMIN)
- `GET /api/ambulances` - Lấy tất cả xe cứu thương
- `GET /api/ambulances/{id}` - Lấy xe theo ID
- `PUT /api/ambulances/{id}` - Cập nhật thông tin (ADMIN)
- `PUT /api/ambulances/{id}/status` - Cập nhật status
- `PUT /api/ambulances/{id}/location` - Cập nhật vị trí

### 9. Leave Request Module (`LeaveRequestController`, `LeaveRequestService`)

**Chức năng:**
- Bác sĩ tạo yêu cầu nghỉ phép
- Admin phê duyệt/từ chối

**Endpoints:**
- `POST /api/leave-requests` - Tạo yêu cầu nghỉ phép (DOCTOR)
- `GET /api/leave-requests` - Lấy danh sách (DOCTOR: của mình, ADMIN: tất cả)
- `PUT /api/leave-requests/{id}/status` - Cập nhật status (ADMIN)

### 10. Blood Test Module (`BloodTestController`, `BloodTestService`)

**Chức năng:**
- Tạo yêu cầu xét nghiệm máu (PATIENT)
- Quản lý lịch xét nghiệm
- Upload kết quả xét nghiệm

**Endpoints:**
- `POST /api/blood-tests` - Tạo yêu cầu xét nghiệm (PATIENT)
- `GET /api/blood-tests/{id}` - Lấy test theo ID
- `GET /api/blood-tests/my-tests` - Lấy tests của tôi (PATIENT)
- `GET /api/blood-tests` - Lấy tất cả tests (ADMIN/DOCTOR, filter by status)
- `GET /api/blood-tests/clinics/{clinicId}` - Lấy tests theo clinic (ADMIN/DOCTOR)
- `PUT /api/blood-tests/{id}/status` - Cập nhật status (ADMIN/DOCTOR)
- `PUT /api/blood-tests/{id}/result` - Upload kết quả (ADMIN/DOCTOR)

**Test Types:**
- Complete Blood Count (CBC): $50
- Blood Glucose Test: $30
- Lipid Panel: $60
- Liver Function Test: $70
- Thyroid Function Test: $80
- Vitamin D Test: $90

**Status Flow:**
```
PENDING → SCHEDULED → COMPLETED
                ↓
            CANCELLED
```

### 11. Pharmacy Order Module (`PharmacyController`, `PharmacyService`)

**Chức năng:**
- Tạo đơn hàng dược phẩm (PATIENT)
- Quản lý đơn hàng
- Link với appointment hoặc upload prescription

**Endpoints:**
- `POST /api/pharmacy-orders` - Tạo đơn hàng (PATIENT)
- `GET /api/pharmacy-orders/{id}` - Lấy đơn hàng theo ID
- `GET /api/pharmacy-orders/my-orders` - Lấy đơn hàng của tôi (PATIENT)
- `GET /api/pharmacy-orders` - Lấy tất cả đơn hàng (ADMIN/DOCTOR, filter by status)
- `GET /api/pharmacy-orders/clinics/{clinicId}` - Lấy đơn hàng theo clinic (ADMIN/DOCTOR)
- `PUT /api/pharmacy-orders/{id}/status` - Cập nhật status (ADMIN/DOCTOR)

**Status Flow:**
```
PENDING → PROCESSING → READY → OUT_FOR_DELIVERY → DELIVERED
                ↓
            CANCELLED
```

### 12. Surgery Consultation Module (`SurgeryController`, `SurgeryService`)

**Chức năng:**
- Tạo yêu cầu tư vấn phẫu thuật (PATIENT)
- Phân công bác sĩ
- Quản lý consultation notes

**Endpoints:**
- `POST /api/surgery-consultations` - Tạo yêu cầu tư vấn (PATIENT)
- `GET /api/surgery-consultations/{id}` - Lấy consultation theo ID
- `GET /api/surgery-consultations/my-consultations` - Lấy consultations của tôi (PATIENT)
- `GET /api/surgery-consultations` - Lấy tất cả consultations (ADMIN/DOCTOR, filter by status)
- `GET /api/surgery-consultations/doctors/{doctorId}` - Lấy consultations theo doctor (ADMIN/DOCTOR)
- `PUT /api/surgery-consultations/{id}/assign-doctor` - Phân công bác sĩ (ADMIN/DOCTOR)
- `PUT /api/surgery-consultations/{id}/status` - Cập nhật status (ADMIN/DOCTOR)
- `PUT /api/surgery-consultations/{id}/notes` - Cập nhật notes (ADMIN/DOCTOR)

### 13. Ambulance Booking Module (`AmbulanceBookingController`, `AmbulanceBookingService`)

**Chức năng:**
- Đặt xe cứu thương (tất cả authenticated users)
- Tự động tìm xe gần nhất
- Tracking vị trí và trạng thái

**Endpoints:**
- `POST /api/ambulance-bookings` - Tạo booking (authenticated)
- `GET /api/ambulance-bookings/{id}` - Lấy booking theo ID
- `GET /api/ambulance-bookings/my-bookings` - Lấy bookings của tôi
- `GET /api/ambulance-bookings` - Lấy tất cả bookings (ADMIN/DOCTOR, filter by status)
- `GET /api/ambulance-bookings/ambulances/{ambulanceId}` - Lấy bookings theo ambulance (ADMIN/DOCTOR)
- `PUT /api/ambulance-bookings/{id}/status` - Cập nhật status (ADMIN/DOCTOR)
- `PUT /api/ambulance-bookings/{id}/assign-ambulance` - Phân công xe (ADMIN/DOCTOR)

**Status Flow:**
```
PENDING → ASSIGNED → IN_TRANSIT → ARRIVED → COMPLETED
                ↓
            CANCELLED
```

### 14. Post/Blog Module (`PostController`, `PostService`)

**Chức năng:**
- Quản lý blog posts (ADMIN)
- Public posts cho homepage
- Draft posts

**Endpoints:**
- `POST /api/posts` - Tạo post (ADMIN)
- `GET /api/posts/{id}` - Lấy post theo ID (public nếu published)
- `GET /api/posts/published` - Lấy published posts (public)
- `GET /api/posts` - Lấy tất cả posts (ADMIN, pagination, filter by status)
- `GET /api/posts/my-posts` - Lấy posts của tôi
- `PUT /api/posts/{id}` - Cập nhật post (author hoặc ADMIN)
- `DELETE /api/posts/{id}` - Xóa post (author hoặc ADMIN)

### 15. Review Module (`ReviewController`, `ReviewService`)

**Chức năng:**
- Tạo review cho bác sĩ (PATIENT)
- Mỗi patient chỉ review một lần cho mỗi doctor
- Public reviews

**Endpoints:**
- `POST /api/reviews` - Tạo review (PATIENT)
- `GET /api/reviews/{id}` - Lấy review theo ID (public)
- `GET /api/reviews/doctors/{doctorId}` - Lấy reviews theo doctor (public)
- `GET /api/reviews/my-reviews` - Lấy reviews của tôi (PATIENT)
- `DELETE /api/reviews/{id}` - Xóa review (author hoặc ADMIN)

### 16. Ranking Module (`RankingController`, `RankingService`)

**Chức năng:**
- Xếp hạng bác sĩ theo rating, reviews, appointments
- Xếp hạng phòng khám theo appointments, doctors, ratings

**Endpoints:**
- `GET /api/ranking/doctors` - Lấy ranking bác sĩ (ADMIN, limit: 1-100)
- `GET /api/ranking/clinics` - Lấy ranking phòng khám (ADMIN, limit: 1-100)

### 17. Dashboard Module (`DashboardController`, `DashboardService`)

**Chức năng:**
- Thống kê cho admin dashboard
- Thống kê cho doctor dashboard

**Endpoints:**
- `GET /api/dashboard/admin` - Lấy stats admin (ADMIN)
- `GET /api/dashboard/doctor` - Lấy stats doctor (DOCTOR)

### 18. Public Module (`PublicController`, `PublicService`)

**Chức năng:**
- Public stats cho homepage
- Featured doctors, clinics, recent posts

**Endpoints:**
- `GET /api/public/stats` - Lấy public stats (public, không cần auth)

---

## 🌐 API Endpoints

### Base URL
```
http://localhost:8080/api
```

### Authentication
Tất cả các endpoint (trừ public) yêu cầu JWT token trong header:
```
Authorization: Bearer <token>
```

### Response Format

**Success Response:**
```json
{
  "data": {...},
  "message": "Success"
}
```

**Error Response:**
```json
{
  "message": "Error message",
  "timestamp": "2024-01-01T00:00:00"
}
```

### Status Codes
- `200 OK` - Success
- `201 Created` - Resource created
- `204 No Content` - Success, no content
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Invalid/missing token
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

---

## 🗄️ Database Schema

### Core Entities

#### User
- `id` (Long, PK)
- `email` (String, unique)
- `passwordHash` (String)
- `fullName` (String)
- `phone` (String)
- `role` (String: PATIENT | DOCTOR | ADMIN)
- `status` (String)
- `createdAt` (LocalDateTime)

#### Doctor
- `id` (Long, PK)
- `user_id` (FK → User)
- `clinic_id` (FK → Clinic)
- `specialization` (String)
- `experienceYears` (Integer)
- `bio` (Text)
- `defaultStartTime` (LocalTime)
- `defaultEndTime` (LocalTime)
- `status` (String: PENDING | APPROVED | REJECTED)

#### Clinic
- `id` (Long, PK)
- `name` (String)
- `address` (Text)
- `phone` (String)
- `email` (String)
- `description` (Text)

#### Appointment
- `id` (Long, PK)
- `patient_id` (FK → User)
- `doctor_id` (FK → Doctor)
- `clinic_id` (FK → Clinic)
- `schedule_id` (FK → DoctorSchedule, unique)
- `appointmentTime` (LocalDateTime)
- `status` (String: PENDING | CONFIRMED | COMPLETED | CANCELLED)
- `age` (Integer)
- `gender` (String: MALE | FEMALE | OTHER)
- `symptoms` (Text)
- `createdAt` (LocalDateTime)

#### DoctorSchedule
- `id` (Long, PK)
- `doctor_id` (FK → Doctor)
- `startTime` (LocalDateTime)
- `endTime` (LocalDateTime)
- `status` (String: HOLD | BOOKED | AVAILABLE | CANCELLED)

#### Emergency
- `id` (Long, PK)
- `patient_id` (FK → User, nullable)
- `clinic_id` (FK → Clinic)
- `patientLat` (Double)
- `patientLng` (Double)
- `patientAddress` (Text)
- `patientName` (String)
- `patientPhone` (String)
- `description` (Text)
- `status` (String: PENDING | DISPATCHED | IN_TRANSIT | ARRIVED | COMPLETED | CANCELLED)
- `priority` (String: LOW | MEDIUM | HIGH | CRITICAL)
- `createdAt` (LocalDateTime)
- `dispatchedAt` (LocalDateTime)
- `arrivedAt` (LocalDateTime)
- `completedAt` (LocalDateTime)

#### Ambulance
- `id` (Long, PK)
- `clinic_id` (FK → Clinic)
- `licensePlate` (String, unique)
- `driverName` (String)
- `driverPhone` (String)
- `status` (String: AVAILABLE | DISPATCHED | IN_TRANSIT | MAINTENANCE)
- `currentLat` (Double)
- `currentLng` (Double)
- `lastUpdatedAt` (LocalDateTime)

#### DoctorLeaveRequest
- `id` (Long, PK)
- `doctor_id` (FK → Doctor)
- `startDate` (LocalDate)
- `endDate` (LocalDate)
- `reason` (Text)
- `status` (String: PENDING | APPROVED | REJECTED)

#### BloodTest
- `id` (Long, PK)
- `patient_id` (FK → User)
- `clinic_id` (FK → Clinic)
- `testType` (String: CBC | BLOOD_GLUCOSE | LIPID_PANEL | LIVER_FUNCTION | THYROID_FUNCTION | VITAMIN_D)
- `testDate` (LocalDate)
- `testTime` (LocalTime)
- `status` (String: PENDING | SCHEDULED | COMPLETED | CANCELLED)
- `resultFileUrl` (String, nullable)
- `notes` (Text, nullable)
- `price` (Double)
- `createdAt` (LocalDateTime)

#### PharmacyOrder
- `id` (Long, PK)
- `patient_id` (FK → User)
- `clinic_id` (FK → Clinic)
- `appointment_id` (FK → Appointment, nullable)
- `prescriptionFileUrl` (String, nullable)
- `deliveryAddress` (Text)
- `deliveryLat` (Double)
- `deliveryLng` (Double)
- `status` (String: PENDING | PROCESSING | READY | OUT_FOR_DELIVERY | DELIVERED | CANCELLED)
- `totalPrice` (Double)
- `createdAt` (LocalDateTime)
- `deliveredAt` (LocalDateTime, nullable)

#### PharmacyOrderItem
- `id` (Long, PK)
- `order_id` (FK → PharmacyOrder)
- `medicationName` (String)
- `quantity` (Integer)
- `unitPrice` (Double)
- `totalPrice` (Double)

#### SurgeryConsultation
- `id` (Long, PK)
- `patient_id` (FK → User)
- `doctor_id` (FK → Doctor, nullable)
- `clinic_id` (FK → Clinic)
- `surgeryType` (String)
- `description` (Text)
- `preferredDate` (LocalDate, nullable)
- `status` (String: PENDING | CONSULTED | SCHEDULED | COMPLETED | CANCELLED)
- `doctorNotes` (Text, nullable)
- `createdAt` (LocalDateTime)

#### AmbulanceBooking
- `id` (Long, PK)
- `patient_id` (FK → User, nullable)
- `ambulance_id` (FK → Ambulance, nullable)
- `clinic_id` (FK → Clinic)
- `pickupLat` (Double)
- `pickupLng` (Double)
- `pickupAddress` (Text)
- `destinationLat` (Double, nullable)
- `destinationLng` (Double, nullable)
- `destinationAddress` (Text, nullable)
- `patientName` (String, nullable)
- `patientPhone` (String, nullable)
- `status` (String: PENDING | ASSIGNED | IN_TRANSIT | ARRIVED | COMPLETED | CANCELLED)
- `estimatedTime` (Integer, nullable)
- `distanceKm` (Double, nullable)
- `notes` (Text, nullable)
- `createdAt` (LocalDateTime)
- `assignedAt` (LocalDateTime, nullable)
- `arrivedAt` (LocalDateTime, nullable)

#### Post
- `id` (Long, PK)
- `author_id` (FK → User)
- `title` (String)
- `content` (Text)
- `imageUrl` (String, nullable)
- `status` (String: DRAFT | PUBLISHED)
- `createdAt` (LocalDateTime)
- `updatedAt` (LocalDateTime)

#### PostComment
- `id` (Long, PK)
- `post_id` (FK → Post)
- `author_id` (FK → User)
- `content` (Text)
- `createdAt` (LocalDateTime)

#### DoctorReview
- `id` (Long, PK)
- `patient_id` (FK → User)
- `doctor_id` (FK → Doctor)
- `rating` (Integer: 1-5)
- `comment` (Text, nullable)
- `createdAt` (LocalDateTime)

#### UserProfile
- `id` (Long, PK)
- `user_id` (FK → User, unique)
- `dateOfBirth` (LocalDate, nullable)
- `gender` (String: MALE | FEMALE | OTHER, nullable)
- `address` (Text, nullable)
- `city` (String, nullable)
- `country` (String, nullable)
- `profileImageUrl` (String, nullable)

#### PatientMedicalHistory
- `id` (Long, PK)
- `user_id` (FK → User, unique)
- `bloodType` (String, nullable)
- `allergies` (Text, nullable)
- `chronicConditions` (Text, nullable)
- `medications` (Text, nullable)
- `previousSurgeries` (Text, nullable)
- `familyHistory` (Text, nullable)
- `updatedAt` (LocalDateTime)

#### MedicalRecord
- `id` (Long, PK)
- `appointment_id` (FK → Appointment, nullable)
- `patient_id` (FK → User)
- `doctor_id` (FK → Doctor, nullable)
- `diagnosis` (Text, nullable)
- `prescription` (Text, nullable)
- `notes` (Text, nullable)
- `createdAt` (LocalDateTime)

#### DoctorWorkingDays
- `id` (Long, PK)
- `doctor_id` (FK → Doctor)
- `dayOfWeek` (String: MONDAY | TUESDAY | WEDNESDAY | THURSDAY | FRIDAY | SATURDAY | SUNDAY)
- `isWorking` (Boolean)

#### EmergencyAssignment
- `id` (Long, PK)
- `emergency_id` (FK → Emergency)
- `doctor_id` (FK → Doctor, nullable)
- `ambulance_id` (FK → Ambulance, nullable)
- `assignedAt` (LocalDateTime)

### Relationships

```
User (1) ──< (N) Doctor
User (1) ──< (N) Appointment (patient)
User (1) ──< (1) UserProfile
User (1) ──< (1) PatientMedicalHistory
User (1) ──< (N) BloodTest
User (1) ──< (N) PharmacyOrder
User (1) ──< (N) SurgeryConsultation
User (1) ──< (N) AmbulanceBooking
User (1) ──< (N) Post (author)
User (1) ──< (N) PostComment (author)
User (1) ──< (N) DoctorReview (patient)
User (1) ──< (N) MedicalRecord (patient)

Doctor (1) ──< (N) Appointment
Doctor (1) ──< (N) DoctorSchedule
Doctor (1) ──< (N) DoctorLeaveRequest
Doctor (1) ──< (N) DoctorWorkingDays
Doctor (1) ──< (N) SurgeryConsultation
Doctor (1) ──< (N) EmergencyAssignment
Doctor (1) ──< (N) DoctorReview
Doctor (1) ──< (N) MedicalRecord
Doctor (N) ──> (1) Clinic

Clinic (1) ──< (N) Doctor
Clinic (1) ──< (N) Appointment
Clinic (1) ──< (N) Emergency
Clinic (1) ──< (N) Ambulance
Clinic (1) ──< (N) BloodTest
Clinic (1) ──< (N) PharmacyOrder
Clinic (1) ──< (N) SurgeryConsultation
Clinic (1) ──< (N) AmbulanceBooking

Appointment (1) ──> (1) DoctorSchedule
Appointment (1) ──< (N) PharmacyOrder
Appointment (1) ──< (N) MedicalRecord

Emergency (1) ──< (N) EmergencyAssignment

Ambulance (1) ──< (N) EmergencyAssignment
Ambulance (1) ──< (N) AmbulanceBooking

Post (1) ──< (N) PostComment

PharmacyOrder (1) ──< (N) PharmacyOrderItem
```

---

## 🔐 Security & Authentication

### JWT Authentication Flow

1. **Login/Register** → Nhận JWT token
2. **Request với token** → Header: `Authorization: Bearer <token>`
3. **JwtAuthenticationFilter** → Validate token
4. **SecurityContext** → Set authentication

### JWT Configuration
- **Secret Key**: Cấu hình trong `application.properties`
- **Expiration**: 24 giờ (86400000ms)
- **Algorithm**: HS256

### Security Configuration

**Public Endpoints (Không Cần Đăng Nhập):**
- `/api/auth/**` - Đăng nhập, đăng ký, validate token
- `/swagger-ui/**` - Swagger UI documentation
- `/v3/api-docs/**` - OpenAPI documentation

**Protected Endpoints (Yêu Cầu Đăng Nhập):**
- **Tất cả các endpoint khác** đều yêu cầu authentication
- Guest không thể xem thông tin bác sĩ, đặt lịch, xem bài viết, reviews mà không đăng nhập
- Xem chi tiết tại [SECURITY_POLICY.md](./SECURITY_POLICY.md)

**Role-based Access:**
- `@PreAuthorize("hasRole('ADMIN')")` - Chỉ ADMIN
- `@PreAuthorize("hasRole('DOCTOR')")` - Chỉ DOCTOR
- `@PreAuthorize("hasRole('PATIENT')")` - Chỉ PATIENT
- `@PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")` - ADMIN hoặc DOCTOR

### Password Encoding
- Sử dụng **BCrypt** để hash passwords
- Password được hash khi đăng ký và so sánh khi đăng nhập

---

## ⚙️ Configuration

### application.properties

```properties
# Application
spring.application.name=medinova

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/medinova
spring.datasource.username=medinova_user
spring.datasource.password=medinova_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# JWT
jwt.secret=your-256-bit-secret-key-for-jwt-token-generation-minimum-32-characters-long-secure-key
jwt.expiration=86400000

# Swagger/OpenAPI
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

### Docker Compose

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: medinova
      POSTGRES_USER: medinova_user
      POSTGRES_PASSWORD: medinova_password
    ports:
      - "5432:5432"
```

---

## 🧪 Testing

### Chạy Tests

```bash
# Tất cả tests
./mvnw test

# Test cụ thể
./mvnw test -Dtest=AuthControllerTest

# Test method cụ thể
./mvnw test -Dtest=AuthControllerTest#testLogin_Success
```

### Test Coverage

Các test classes hiện có:
- `AuthControllerTest` - Test authentication APIs
- `ClinicControllerTest` - Test clinic management APIs
- `UserControllerTest` - Test user management APIs
- `UserProfileControllerTest` - Test profile APIs

Xem chi tiết tại: [TEST_GUIDE.md](./TEST_GUIDE.md)

---

## 📝 Best Practices

1. **Exception Handling**: Sử dụng `GlobalExceptionHandler` để xử lý exceptions tập trung
2. **DTO Pattern**: Sử dụng DTOs để tách biệt API layer và domain model
3. **Validation**: Sử dụng Jakarta Validation annotations
4. **Pagination**: Sử dụng Spring Data pagination cho list endpoints
5. **Security**: Luôn validate permissions trong service layer
6. **Logging**: Sử dụng SLF4J cho logging

---

## 🚀 Deployment

### Build JAR

```bash
./mvnw clean package
```

### Run JAR

```bash
java -jar target/medinova-0.0.1-SNAPSHOT.jar
```

### Production Considerations

1. **JWT Secret**: Thay đổi secret key mạnh hơn
2. **Database**: Sử dụng connection pooling
3. **CORS**: Hạn chế allowed origins
4. **HTTPS**: Sử dụng HTTPS trong production
5. **Logging**: Cấu hình logging level phù hợp
6. **Monitoring**: Thêm health checks và metrics

---

## 📚 Tài Liệu Tham Khảo

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [Spring Data JPA Documentation](https://spring.io/projects/spring-data-jpa)
- [JWT.io](https://jwt.io/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

---

## 🤝 Đóng Góp

Khi thêm tính năng mới:
1. Tạo entity trong `entity/`
2. Tạo repository trong `repository/`
3. Tạo service trong `service/`
4. Tạo controller trong `controller/`
5. Tạo DTOs trong `dto/`
6. Viết tests trong `test/`

