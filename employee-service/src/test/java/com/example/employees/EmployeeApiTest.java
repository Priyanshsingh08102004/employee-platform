package com.example.employees;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb")
@AutoConfigureMockMvc
class EmployeeApiTest {

    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username = "tester", roles = "ADMIN") // simulates a logged-in admin without a real login form
    void crudFlow() throws Exception {
        // Seeders run on context start: departments 1-4, employees 1-5, so the new row gets id 6
        String json = """
            {"name":"Test User","email":"test.user@example.com","jobTitle":"Tester",
             "salary":500000,"joinDate":"2024-01-15","departmentId":1}""";

        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(json))
           .andExpect(status().isCreated());
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(json))
           .andExpect(status().isConflict());
        mvc.perform(get("/api/employees")).andExpect(status().isOk());
        mvc.perform(put("/api/employees/6").contentType(MediaType.APPLICATION_JSON)
           .content(json.replace("test.user", "changed.user"))).andExpect(status().isOk());
        mvc.perform(delete("/api/employees/6")).andExpect(status().isNoContent());
        mvc.perform(get("/api/employees/6")).andExpect(status().isNotFound());
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content("{}"))
           .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "staff", roles = "USER") // a non-admin account
    void nonAdminCannotDelete() throws Exception {
        mvc.perform(delete("/api/employees/1")).andExpect(status().isForbidden());
    }
}
