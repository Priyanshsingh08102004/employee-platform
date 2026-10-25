package com.example.employees;

import com.example.employees.model.Department;
import com.example.employees.repository.DepartmentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DepartmentSeeder implements CommandLineRunner {

    private final DepartmentRepository repo;

    public DepartmentSeeder(DepartmentRepository repo) { this.repo = repo; }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;
        repo.save(new Department("Engineering", "Builds and maintains our products"));
        repo.save(new Department("Design", "Product and UX design"));
        repo.save(new Department("HR", "Hiring and people operations"));
        repo.save(new Department("Sales", "Customer acquisition and accounts"));
    }
}
