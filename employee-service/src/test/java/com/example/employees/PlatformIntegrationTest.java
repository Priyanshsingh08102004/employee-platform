package com.example.employees;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Tests the Java side of the Java <-> .NET integration. Uses its own in-memory database name so it
// never shares rows with EmployeeApiTest, and points the payroll URL at a closed port so the
// "payroll service is down" behaviour is deterministic on any machine.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:platformtest",
        "app.internal.api-key=test-key",
        "app.payroll.base-url=http://localhost:1"
})
@AutoConfigureMockMvc
class PlatformIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void internalEndpointRejectsMissingKey() throws Exception {
        mvc.perform(get("/api/internal/employees")).andExpect(status().isUnauthorized());
    }

    @Test
    void internalEndpointRejectsWrongKey() throws Exception {
        mvc.perform(get("/api/internal/employees").header("X-API-KEY", "nope"))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void internalEndpointReturnsEmployeesForCorrectKey() throws Exception {
        mvc.perform(get("/api/internal/employees").header("X-API-KEY", "test-key"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    @WithMockUser(username = "staff", roles = "USER")
    void payrollGatewayIsAdminOnly() throws Exception {
        mvc.perform(get("/api/payroll/runs")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "boss", roles = "ADMIN")
    void payrollGatewayReturns503WhenPayrollServiceIsDown() throws Exception {
        mvc.perform(get("/api/payroll/runs")).andExpect(status().isServiceUnavailable());
    }
}
