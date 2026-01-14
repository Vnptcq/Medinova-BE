package com.project.medinova.controller;

import com.project.medinova.dto.UpdateUserRoleRequest;
import com.project.medinova.dto.UserResponse;
import com.project.medinova.entity.User;
import com.project.medinova.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.project.medinova.dto.UpdateRoleRequest;
import com.project.medinova.entity.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@Tag(name = "User Management", description = "User management APIs (ADMIN only)")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Operation(summary = "Get all users", description = "Get list of all users with optional pagination (ADMIN only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN can access")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        
        if (page < 0) page = 0;
        if (size < 1) size = 10;
        if (size > 100) size = 100; // Limit max page size
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userService.getAllUsers(pageable);
        List<User> users = userPage.getContent();
        
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get user by ID", description = "Get user information by ID (ADMIN only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN can access"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @Operation(
            summary = "Update user role",
            description = "Update user role. When changing role to DOCTOR, a doctor record will be automatically created. If clinicId is provided, doctor will be assigned to that clinic. If clinicId is not provided, the first clinic in the database will be used as default. If role change fails, both role update and doctor creation will be rolled back. (ADMIN only)",
            tags = {"User Management"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User role updated successfully. If role is DOCTOR, doctor record is also created.",
                    content = @Content(schema = @Schema(implementation = User.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Only ADMIN can update role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found - User or Clinic not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request - Validation error, cannot change own role, or no clinic available when role is DOCTOR")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateRoleRequest request) {
        
        logger.info("📝 Updating role for user ID: {} to role: {}", userId, request.getRole());
        
        try {
            // Validate role
            String roleStr = request.getRole().toUpperCase();
            Role newRole;
            try {
                newRole = Role.valueOf(roleStr);
            } catch (IllegalArgumentException e) {
                logger.error("❌ Invalid role: {}", request.getRole());
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid role",
                    "message", "Role must be one of: ADMIN, DOCTOR, PATIENT, RECEPTIONIST, DRIVER",
                    "providedRole", request.getRole()
                ));
            }
            
            // Call service to update role
            UpdateUserRoleRequest updateRequest = new UpdateUserRoleRequest();
            updateRequest.setRole(roleStr);
            updateRequest.setClinicId(null);  // Will use default clinic if needed
            
            User updatedUser = userService.updateUserRole(userId, updateRequest);
            logger.info("✅ Role updated successfully for user ID: {} to {}", userId, newRole);
            
            // Convert User to UserResponse
            UserResponse response = new UserResponse(
                updatedUser.getId(),
                updatedUser.getEmail(),  // Using email as username
                updatedUser.getEmail(),
                Role.valueOf(updatedUser.getRole()),  // Convert String to Role enum
                updatedUser.getFullName(),
                updatedUser.getPhone(),
                updatedUser.getStatus()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error updating role for user ID: {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to update role",
                "message", e.getMessage()
            ));
        }
    }
}

