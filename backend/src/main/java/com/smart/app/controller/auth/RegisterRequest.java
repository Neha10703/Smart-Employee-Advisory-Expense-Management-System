package com.smart.app.controller.auth;

import com.smart.app.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String firstname;
    private String lastname;
    private String email;
    private String password;
    private String phone;
    private String securityQuestion;
    private String securityAnswer;
    private Role role;
}
