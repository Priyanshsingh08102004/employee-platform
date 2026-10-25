package com.example.employees.scheduler;

import com.example.employees.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmployeeReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmployeeReportScheduler.class);

    private final EmployeeRepository repo;

    public EmployeeReportScheduler(EmployeeRepository repo) {
        this.repo = repo;
    }

    // Runs every 60 seconds so you can actually see it fire during a short demo.
    // In a real system you would use a cron expression instead, for example:
    //   @Scheduled(cron = "0 0 8 * * *")   // every day at 8:00 AM
    @Scheduled(fixedRate = 60000)
    public void logDailySummary() {
        long total = repo.count();
        log.info(">> Scheduled report: {} total employee record(s) currently in the database", total);
    }
}
