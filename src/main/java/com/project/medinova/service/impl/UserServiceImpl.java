package com.project.medinova.service.impl;

import com.project.medinova.dto.UpdateUserRoleRequest;
import com.project.medinova.entity.Clinic;
import com.project.medinova.entity.Department;
import com.project.medinova.entity.Doctor;
import com.project.medinova.entity.User;
import com.project.medinova.exception.BadRequestException;
import com.project.medinova.exception.NotFoundException;
import com.project.medinova.repository.ClinicRepository;
import com.project.medinova.repository.DoctorRepository;
import com.project.medinova.repository.UserRepository;
import com.project.medinova.service.AuthService;
import com.project.medinova.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ClinicRepository clinicRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }

    @Transactional(rollbackFor = Exception.class)
    public User updateUserRole(Long id, UpdateUserRoleRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        // Không cho phép thay đổi role của chính mình
        var currentUser = authService.getCurrentUser();
        if (currentUser.getId().equals(id)) {
            throw new BadRequestException("Cannot change your own role");
        }

        // Validate role
        String newRole = request.getRole().toUpperCase();
        if (!newRole.equals("PATIENT") && !newRole.equals("DOCTOR") && !newRole.equals("ADMIN")) {
            throw new BadRequestException("Invalid role. Must be PATIENT, DOCTOR, or ADMIN");
        }

        String oldRole = user.getRole();
        
        // Nếu chuyển thành DOCTOR, tự động lấy clinic đầu tiên nếu không có clinicId
        if ("DOCTOR".equals(newRole)) {
            Clinic clinic;
            
            if (request.getClinicId() != null) {
                // Nếu có clinicId, sử dụng clinic đó
                clinic = clinicRepository.findById(request.getClinicId())
                        .orElseThrow(() -> new NotFoundException("Clinic not found with id: " + request.getClinicId()));
            } else {
                // Nếu không có clinicId, lấy clinic đầu tiên trong database
                clinic = clinicRepository.findAll().stream()
                        .findFirst()
                        .orElseThrow(() -> new BadRequestException("No clinic available. Please create a clinic first."));
            }

            // Kiểm tra user đã là doctor chưa
            if (doctorRepository.findByUserId(id).isPresent()) {
                // User đã là doctor rồi, chỉ cần update role nếu cần
                user.setRole(newRole);
                return userRepository.save(user);
            }

            // Update role trước
            user.setRole(newRole);
            user = userRepository.save(user);

            // Tạo doctor record - nếu có lỗi sẽ rollback cả việc update role
            try {
                Doctor doctor = new Doctor();
                doctor.setUser(user);
                doctor.setClinic(clinic);
                doctor.setStatus("PENDING"); // Mặc định là PENDING khi admin chuyển role
                doctor.setDepartment(Department.GENERAL_MEDICINE); // Set default department (required field)
                // Các field khác có thể để null, admin có thể update sau
                doctorRepository.save(doctor);
            } catch (Exception e) {
                // Nếu có lỗi khi tạo doctor, rollback sẽ tự động xảy ra do @Transactional
                throw new BadRequestException("Failed to create doctor record: " + e.getMessage());
            }
        } else {
            // Nếu chuyển từ DOCTOR sang role khác, xóa doctor record nếu có
            if ("DOCTOR".equals(oldRole)) {
                doctorRepository.findByUserId(id).ifPresent(doctorRepository::delete);
            }
            
            // Update role
            user.setRole(newRole);
            user = userRepository.save(user);
        }

        return user;
    }
}