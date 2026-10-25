package com.example.employees;

import com.example.employees.model.User;
import com.example.employees.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class UserSeeder implements CommandLineRunner {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public UserSeeder(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;
        repo.save(new User("admin", encoder.encode("admin123"), "ADMIN"));
        repo.save(new User("manager", encoder.encode("manager123"), "USER"));
        System.out.println(">> Default logins created:");
        System.out.println("   ADMIN  -> username: admin   / password: admin123   (can delete employees + add departments)");
        System.out.println("   USER   -> username: manager / password: manager123 (can view, add, edit only)");
    }
}
