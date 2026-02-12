package com.smart.app.config;

import com.smart.app.model.Role;
import com.smart.app.model.User;
import com.smart.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create or update admin user
        var existingAdmin = userRepository.findByEmail("admin@smart.com");
        if (existingAdmin.isPresent()) {
            // Update existing admin name and add security question if missing
            User admin = existingAdmin.get();
            admin.setName("Admin");
            if (admin.getSecurityQuestion() == null || admin.getSecurityQuestion().isEmpty()) {
                admin.setSecurityQuestion("What is your favorite color?");
                admin.setSecurityAnswer(passwordEncoder.encode("blue"));
            }
            userRepository.save(admin);
            System.out.println("Admin user updated: admin@smart.com");
        } else {
            // Create new admin user
            User admin = User.builder()
                    .name("Admin")
                    .email("admin@smart.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .phone("9999999999")
                    .securityQuestion("What is your favorite color?")
                    .securityAnswer(passwordEncoder.encode("blue"))
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            userRepository.save(admin);
            System.out.println("Admin user created: admin@smart.com / admin123");
        }
    }
}