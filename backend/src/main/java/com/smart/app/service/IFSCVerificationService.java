package com.smart.app.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class IFSCVerificationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean verifyIFSC(String ifscCode) {
        // Basic IFSC format validation
        if (ifscCode == null || ifscCode.trim().length() != 11) {
            return false;
        }
        
        // Clean and uppercase the IFSC code
        ifscCode = ifscCode.trim().toUpperCase();
        
        // IFSC format: First 4 chars are alphabets, 5th is 0, last 6 are alphanumeric
        String pattern = "^[A-Z]{4}0[A-Z0-9]{6}$";
        if (!ifscCode.matches(pattern)) {
            return false;
        }
        
        // Skip API call for now, just return true for valid format
        return true;
    }

    public String getBankName(String ifscCode) {
        // Return default bank names for common IFSC prefixes
        if (ifscCode == null) return null;
        
        ifscCode = ifscCode.trim().toUpperCase();
        
        if (ifscCode.startsWith("SBIN")) return "State Bank of India";
        if (ifscCode.startsWith("HDFC")) return "HDFC Bank";
        if (ifscCode.startsWith("ICIC")) return "ICICI Bank";
        if (ifscCode.startsWith("AXIS")) return "Axis Bank";
        if (ifscCode.startsWith("PUNB")) return "Punjab National Bank";
        if (ifscCode.startsWith("CNRB")) return "Canara Bank";
        if (ifscCode.startsWith("UBIN")) return "Union Bank of India";
        if (ifscCode.startsWith("BARB")) return "Bank of Baroda";
        
        return "Unknown Bank";
    }
}