package com.example.employees.controller;

import com.example.employees.dto.EmployeeResponseDTO;
import com.example.employees.service.EmployeeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Service-to-service endpoint. Browsers never call this - the .NET payroll service does, sending a
 * shared secret in the X-API-KEY header instead of a user session. SecurityConfig lets the path
 * through the login wall, and this controller does the real check itself.
 */
@RestController
@RequestMapping("/api/internal")
public class InternalEmployeeController {

    private final EmployeeService service;
    private final byte[] expectedKey;

    public InternalEmployeeController(EmployeeService service,
                                      @Value("${app.internal.api-key}") String apiKey) {
        this.service = service;
        this.expectedKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/employees")
    public ResponseEntity<?> employees(@RequestHeader(value = "X-API-KEY", required = false) String key) {
        // MessageDigest.isEqual compares in constant time, so response timing can't be used to guess the key
        if (key == null || !MessageDigest.isEqual(expectedKey, key.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Missing or invalid API key"));
        }
        Page<EmployeeResponseDTO> page = service.findAll(null, null, PageRequest.of(0, 1000, Sort.by("name")));
        return ResponseEntity.ok(page.getContent());
    }
}
