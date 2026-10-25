package com.example.employees.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class SubmissionCorrectionDTO {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    private String phone;

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    @NotNull(message = "Expected salary is required")
    @Positive(message = "Expected salary must be greater than zero")
    private BigDecimal expectedSalary;

    @NotNull(message = "Department is required")
    private Long departmentId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public BigDecimal getExpectedSalary() { return expectedSalary; }
    public void setExpectedSalary(BigDecimal expectedSalary) { this.expectedSalary = expectedSalary; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
}
