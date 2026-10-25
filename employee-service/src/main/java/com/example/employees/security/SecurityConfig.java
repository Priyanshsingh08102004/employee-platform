package com.example.employees.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity(prePostEnabled = true) // turns on @PreAuthorize("hasRole(...)") on controller methods
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Simplified for this learning project: CSRF is off so the existing
            // fetch()-based POST/PUT/DELETE calls in app.js keep working without
            // extra token handling. A production app would enable CSRF properly
            // and send the token with every state-changing request instead.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/login.html", "/register.html", "/login", "/api/auth/register",
                        "/style.css", "/auth.js", "/favicon.ico",
                        "/h2-console/**",
                        "/swagger-ui/**", "/v3/api-docs/**",
                        "/actuator/health",
                        "/apply.html", "/apply.js", "/chatbot.js",
                        "/api/chatbot/ask",
                        "/api/internal/**"   // service-to-service: protected by X-API-KEY inside the controller
                ).permitAll()
                // Public job applicants aren't logged in, but still need to pick a department
                // and submit their application - these two are opened up by HTTP method only,
                // so GET /api/departments and POST /api/applications stay public while every
                // other employee/department/application endpoint stays behind login.
                .requestMatchers(HttpMethod.GET, "/api/departments").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/applications").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/index.html", true)
                .failureUrl("/login.html?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.html?logout")
                .permitAll()
            )
            // Needed so the H2 console (also loaded in a frame) still displays
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));

        return http.build();
    }
}
