package com.example.auth;

import com.example.auth.dto.SignInRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Step 2 & 3: POST /ms1/signin returns 200 and dynamic token matching non-empty regex")
    void testSignInEndpointMatchesContractSpecification() throws Exception {
        SignInRequest request = new SignInRequest("user", "password123");

        mockMvc.perform(post("/ms1/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/json")))
                // Step 3: Validate dynamic token field accepts any non-empty string using regex matcher
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.token").value(matchesPattern("^\\s*\\S.*$")))
                // Also verify that the token conforms to a valid 3-part JWT structure
                .andExpect(jsonPath("$.token").value(matchesPattern("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$")));
    }

    @Test
    @DisplayName("Verify token is dynamic across multiple requests")
    void testDynamicTokenGeneration() throws Exception {
        SignInRequest request = new SignInRequest("user", "password123");

        String response1 = mockMvc.perform(post("/ms1/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String response2 = mockMvc.perform(post("/ms1/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Ensure tokens are dynamically generated and distinct
        org.junit.jupiter.api.Assertions.assertNotEquals(response1, response2);
    }
}
