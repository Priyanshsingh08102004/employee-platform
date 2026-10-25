package com.example.employees.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class PayrollClientConfig {

    // A pre-configured HTTP client for the .NET payroll service. Timeouts matter: without them,
    // a hung payroll service would hang every request thread that calls it.
    @Bean
    public RestClient payrollRestClient(@Value("${app.payroll.base-url}") String baseUrl,
                                        @Value("${app.internal.api-key}") String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-API-KEY", apiKey)
                .requestFactory(factory)
                .build();
    }
}
