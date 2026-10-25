package com.example.employees.controller;

import com.example.employees.dto.SubmissionCorrectionDTO;
import com.example.employees.model.EmployeeSubmission;
import com.example.employees.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class SubmissionController {

    private final SubmissionService service;

    public SubmissionController(SubmissionService service) {
        this.service = service;
    }

    // PUBLIC - no login required (see SecurityConfig: POST /api/applications is permitAll)
    @PostMapping
    public ResponseEntity<EmployeeSubmission> submit(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            @RequestParam String jobTitle,
            @RequestParam BigDecimal expectedSalary,
            @RequestParam Long departmentId,
            @RequestParam(required = false) MultipartFile resume) {
        EmployeeSubmission saved = service.submit(name, email, phone, jobTitle, expectedSalary, departmentId, resume);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Manager (Admin) review queue
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<EmployeeSubmission> list(@RequestParam(defaultValue = "PENDING") String status) {
        return service.findByStatus(status);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeSubmission update(@PathVariable Long id, @Valid @RequestBody SubmissionCorrectionDTO dto) {
        return service.update(id, dto);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeSubmission approve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("note") : null;
        return service.approve(id, note);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeSubmission reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return service.reject(id, reason);
    }
}
