package com.example.employees;

import com.example.employees.model.Department;
import com.example.employees.model.Employee;
import com.example.employees.repository.DepartmentRepository;
import com.example.employees.repository.EmployeeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@Order(2) // runs after DepartmentSeeder, which must create departments first
public class EmployeeSeeder implements CommandLineRunner {

    private final EmployeeRepository repo;
    private final DepartmentRepository deptRepo;

    public EmployeeSeeder(EmployeeRepository repo, DepartmentRepository deptRepo) {
        this.repo = repo;
        this.deptRepo = deptRepo;
    }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;

        Department engineering = deptRepo.findByNameIgnoreCase("Engineering").orElseThrow();
        Department design = deptRepo.findByNameIgnoreCase("Design").orElseThrow();
        Department hr = deptRepo.findByNameIgnoreCase("HR").orElseThrow();
        Department sales = deptRepo.findByNameIgnoreCase("Sales").orElseThrow();

        repo.save(make("Aarav Sharma", "aarav.sharma@example.com", engineering, "Backend Developer", "1250000", "2022-03-14"));
        repo.save(make("Priya Verma", "priya.verma@example.com", design, "UX Designer", "980000", "2021-07-01"));
        repo.save(make("Rohan Gupta", "rohan.gupta@example.com", engineering, "Tech Lead", "2100000", "2019-11-20"));
        repo.save(make("Sneha Iyer", "sneha.iyer@example.com", hr, "HR Manager", "1100000", "2020-01-06"));
        repo.save(make("Vikram Singh", "vikram.singh@example.com", sales, "Account Executive", "900000", "2023-05-22"));
    }

    private Employee make(String name, String email, Department dept, String title, String salary, String join) {
        Employee e = new Employee();
        e.setName(name); e.setEmail(email); e.setDepartment(dept);
        e.setJobTitle(title); e.setSalary(new BigDecimal(salary));
        e.setJoinDate(LocalDate.parse(join));
        return e;
    }
}
