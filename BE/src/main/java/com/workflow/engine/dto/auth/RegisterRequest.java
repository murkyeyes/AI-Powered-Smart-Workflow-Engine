package com.workflow.engine.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 20, message = "Username phải từ 3 đến 20 ký tự")
    private String username;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 6, max = 40, message = "Password phải từ 6 đến 40 ký tự")
    private String password;

    private String role;
}