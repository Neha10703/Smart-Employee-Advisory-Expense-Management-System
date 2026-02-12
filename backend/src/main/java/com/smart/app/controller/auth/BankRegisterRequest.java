package com.smart.app.controller.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BankRegisterRequest {
    private String bankName;
    private String contactPerson;
    private String email;
    private String password;
    private String phone;
    private String address;
    private String ifscCode;
    private String securityQuestion;
    private String securityAnswer;
}