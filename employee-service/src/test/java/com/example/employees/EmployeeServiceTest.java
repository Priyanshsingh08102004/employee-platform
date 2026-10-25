package com.example.employees;

import com.example.employees.dto.EmployeeRequestDTO;
import com.example.employees.exception.ResourceNotFoundException;
import com.example.employees.repository.DepartmentRepository;
import com.example.employees.repository.EmployeeRepository;
import com.example.employees.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Unlike EmployeeApiTest, this never starts Spring or touches a real database -
// the repositories are faked with Mockito, so it runs in milliseconds and tests
// the service's own logic (like "what happens when the department doesn't exist") in isolation.
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock EmployeeRepository employeeRepository;
    @Mock DepartmentRepository departmentRepository;
    @InjectMocks EmployeeService employeeService;

    @Test
    void createThrowsWhenDepartmentMissing() {
        EmployeeRequestDTO dto = new EmployeeRequestDTO();
        dto.setName("Jane Doe");
        dto.setEmail("jane@example.com");
        dto.setJobTitle("Analyst");
        dto.setSalary(new BigDecimal("600000"));
        dto.setDepartmentId(99L);

        when(employeeRepository.existsByEmailIgnoreCaseAndDeletedFalse("jane@example.com")).thenReturn(false);
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.create(dto));
        verify(employeeRepository, never()).save(any());
    }
}
