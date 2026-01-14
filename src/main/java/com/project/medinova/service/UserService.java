package com.project.medinova.service;

import com.project.medinova.dto.UpdateUserRoleRequest;
import com.project.medinova.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    List<User> getAllUsers();
    Page<User> getAllUsers(Pageable pageable);
    User getUserById(Long id);
    User updateUserRole(Long id, UpdateUserRoleRequest request);
}

