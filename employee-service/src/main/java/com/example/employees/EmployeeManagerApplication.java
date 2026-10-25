package com.example.employees;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing     // turns on automatic createdAt / updatedAt timestamps
@EnableCaching          // turns on @Cacheable / @CacheEvict support
@EnableScheduling       // turns on @Scheduled background jobs
public class EmployeeManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmployeeManagerApplication.class, args);
    }
}
