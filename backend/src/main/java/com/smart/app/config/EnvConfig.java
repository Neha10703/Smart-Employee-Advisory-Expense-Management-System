package com.smart.app.config;

import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

@Configuration
public class EnvConfig {

    @PostConstruct
    public void loadEnvFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("=") && !line.startsWith("#")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        System.setProperty(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("No .env file found or error reading it: " + e.getMessage());
        }
    }
}