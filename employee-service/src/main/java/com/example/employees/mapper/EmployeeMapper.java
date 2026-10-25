package com.example.employees.mapper;

import com.example.employees.dto.EmployeeResponseDTO;
import com.example.employees.model.Employee;

public class EmployeeMapper {

    public static EmployeeResponseDTO toDTO(Employee e) {
        EmployeeResponseDTO dto = new EmployeeResponseDTO();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setEmail(e.getEmail());
        dto.setJobTitle(e.getJobTitle());
        dto.setSalary(e.getSalary());
        dto.setJoinDate(e.getJoinDate());
        if (e.getDepartment() != null) {
            dto.setDepartmentId(e.getDepartment().getId());
            dto.setDepartmentName(e.getDepartment().getName());
        }
        dto.setProfileImageUrl(e.getProfileImagePath() != null ? "/uploads/" + e.getProfileImagePath() : null);
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }
}
