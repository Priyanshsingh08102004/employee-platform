package com.example.employees.controller;

import com.example.employees.dto.EmployeeRequestDTO;
import com.example.employees.dto.EmployeeResponseDTO;
import com.example.employees.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    // READ (paginated, sortable, filterable)
    // Example: /api/employees?q=sharma&departmentId=1&page=0&size=10&sort=salary,desc
    @GetMapping
    public Page<EmployeeResponseDTO> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long departmentId,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return service.findAll(q, departmentId, pageable);
    }

    @GetMapping("/{id}")
    public EmployeeResponseDTO get(@PathVariable Long id) {
        return service.findById(id);
    }

    // CREATE
    @PostMapping
    public ResponseEntity<EmployeeResponseDTO> create(@Valid @RequestBody EmployeeRequestDTO dto) {
        EmployeeResponseDTO saved = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // UPDATE
    @PutMapping("/{id}")
    public EmployeeResponseDTO update(@PathVariable Long id, @Valid @RequestBody EmployeeRequestDTO dto) {
        return service.update(id, dto);
    }

    // DELETE (soft delete) - only ADMIN accounts are allowed to delete
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // Profile photo upload (multipart/form-data)
    @PostMapping("/{id}/photo")
    public EmployeeResponseDTO uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return service.uploadPhoto(id, file);
    }

    // CSV export of every active (non-deleted) employee
    @GetMapping(value = "/export/csv", produces = "text/csv")
    public ResponseEntity<String> exportCsv() {
        String csv = service.exportCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employees.csv")
                .body(csv);
    }
}
