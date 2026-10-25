package com.example.employees.controller;

import com.example.employees.model.User;
import com.example.employees.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    public AuthController(UserRepository userRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody User incoming) {
        if (userRepo.existsByUsernameIgnoreCase(incoming.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "That username is already taken"));
        }
        // Self-registration always creates a plain USER - nobody can grant themselves ADMIN this way
        User user = new User(incoming.getUsername(), encoder.encode(incoming.getPassword()), "USER");
        userRepo.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Account created"));
    }

    // Lets the frontend ask "who am I, and am I an admin?" so it can hide admin-only buttons
    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        if (auth == null) {
            return Map.of("authenticated", false);
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return Map.of(
                "authenticated", true,
                "username", auth.getName(),
                "isAdmin", isAdmin
        );
    }
}
