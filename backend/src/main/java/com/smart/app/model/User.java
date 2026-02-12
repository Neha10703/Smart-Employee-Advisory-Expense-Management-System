package com.smart.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String bankName; // For BANK role
    private String ifscCode; // For BANK role

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phone;
    private String profilePhoto;
    private String address;
    private String occupation;
    private String securityQuestion;
    private String securityAnswer;
    private Double monthlyBudget;
    private Double budgetAlertThreshold;
    private Boolean emailNotifications = true;
    private Boolean pushNotifications = true;
    private Boolean budgetAlerts = true;
    private Boolean twoFactorEnabled = false;
    private String subscriptionPlan = "FREE";
    private Boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role role;

    private LocalDateTime createdAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        return email; // Use email as username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled != null ? enabled : true;
    }
}
