package com.example.employees.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

/**
 * The browser only ever talks to THIS service. It checks the logged-in session (ADMIN only), then
 * calls the .NET payroll service server-to-server with the shared API key. That keeps the payroll
 * service off the public internet and means it needs no login system of its own.
 */
@RestController
@RequestMapping("/api/payroll")
@PreAuthorize("hasRole('ADMIN')")
public class PayrollGatewayController {

    private final RestClient payroll;

    public PayrollGatewayController(RestClient payrollRestClient) {
        this.payroll = payrollRestClient;
    }

    @PostMapping("/runs")
    public ResponseEntity<String> run(@RequestParam String month) {
        return forward(() -> payroll.post()
                .uri("/api/payroll/runs?month={month}", month)
                .retrieve().toEntity(String.class));
    }

    @GetMapping("/runs")
    public ResponseEntity<String> list() {
        return forward(() -> payroll.get().uri("/api/payroll/runs").retrieve().toEntity(String.class));
    }

    @GetMapping("/runs/{id}")
    public ResponseEntity<String> one(@PathVariable long id) {
        return forward(() -> payroll.get().uri("/api/payroll/runs/{id}", id).retrieve().toEntity(String.class));
    }

    private ResponseEntity<String> forward(Supplier<ResponseEntity<String>> call) {
        try {
            ResponseEntity<String> r = call.get();
            return ResponseEntity.status(r.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON).body(r.getBody());
        } catch (RestClientResponseException ex) {
            // The payroll service answered with an error (400/409/502...) - pass it through unchanged
            return ResponseEntity.status(ex.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON).body(ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            // Could not reach it at all (down, DNS failure, timeout) - degrade gracefully instead of a 500 stack trace
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\":\"Payroll service is unavailable right now. Try again shortly.\"}");
        }
    }
}
