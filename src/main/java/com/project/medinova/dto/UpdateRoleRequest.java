package com.project.medinova.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateRoleRequest {
    
    @NotNull(message = "Role is required")
    private String role;
    
    public UpdateRoleRequest() {}
    
    public UpdateRoleRequest(String role) {
        this.role = role;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
}
