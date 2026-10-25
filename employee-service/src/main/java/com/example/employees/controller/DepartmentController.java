package com.example.employees.controller;

import com.example.employees.model.Department;
import com.example.employees.repository.DepartmentRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentRepository repo;

    public DepartmentController(DepartmentRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Department> list() {
        return repo.findAll();
    }

    // Only admins can add new departments - everyone can read them (for the dropdown)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody Department department) {
        if (repo.existsByNameIgnoreCase(department.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("A department with this name already exists");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(department));
    }
}
