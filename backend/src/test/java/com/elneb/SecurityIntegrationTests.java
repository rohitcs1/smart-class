package com.elneb;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTests {
    @Autowired MockMvc mvc;

    @Test
    void protectedReportsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/reports/summary")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedCampusRoutesRequireAuthentication() throws Exception {
        mvc.perform(get("/api/campus/institution/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
    }
}
