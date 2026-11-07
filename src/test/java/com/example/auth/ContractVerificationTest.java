package com.example.auth;

import com.example.auth.dto.SignInRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
public class ContractVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Verify Contract YAML definition and execute dynamic matcher validation against /ms1/signin")
    void testContractDefinitionAndDynamicMatcher() throws Exception {
        // Step 1: Verify Contract YAML file exists under contracts resource directory
        ClassPathResource contractResource = new ClassPathResource("contracts/ms1/signin.yml");
        if (!contractResource.exists()) {
            contractResource = new ClassPathResource("contracts/signin.yml");
        }
        assertTrue(contractResource.exists(), "Contract YAML file must exist under contracts resource directory");

        // Parse YAML contract
        Yaml yaml = new Yaml();
        Map<String, Object> contract;
        try (InputStream is = contractResource.getInputStream()) {
            contract = yaml.load(is);
        }
        assertNotNull(contract, "Contract content cannot be null");

        // Step 2: Validate Request specification in Contract
        @SuppressWarnings("unchecked")
        Map<String, Object> requestSpec = (Map<String, Object>) contract.get("request");
        assertNotNull(requestSpec, "Contract must define request");
        assertEquals("POST", requestSpec.get("method"), "Request method must be POST");
        assertEquals("/ms1/signin", requestSpec.get("url"), "Request URL target must be /ms1/signin");

        @SuppressWarnings("unchecked")
        Map<String, Object> requestHeaders = (Map<String, Object>) requestSpec.get("headers");
        assertNotNull(requestHeaders, "Request headers must be defined");
        assertEquals("application/json", requestHeaders.get("Content-Type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> requestBody = (Map<String, Object>) requestSpec.get("body");
        assertNotNull(requestBody, "Request body must be defined");
        assertTrue(requestBody.containsKey("username"), "Request body must contain username");
        assertTrue(requestBody.containsKey("password"), "Request body must contain password");

        // Step 3: Validate Response specification and Dynamic Matcher in Contract
        @SuppressWarnings("unchecked")
        Map<String, Object> responseSpec = (Map<String, Object>) contract.get("response");
        assertNotNull(responseSpec, "Contract must define response");
        assertEquals(200, ((Number) responseSpec.get("status")).intValue(), "Expected response status must be 200");

        @SuppressWarnings("unchecked")
        Map<String, Object> matchers = (Map<String, Object>) responseSpec.get("matchers");
        assertNotNull(matchers, "Response must define matchers");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bodyMatchers = (List<Map<String, Object>>) matchers.get("body");
        assertNotNull(bodyMatchers, "Response matchers must include body matchers");

        // Find matcher targeting $.token
        Map<String, Object> tokenMatcher = bodyMatchers.stream()
                .filter(m -> "$.token".equals(m.get("path")))
                .findFirst()
                .orElse(null);

        assertNotNull(tokenMatcher, "A matcher for $.token must be configured");
        assertEquals("by_regex", tokenMatcher.get("type"), "Matcher type must be by_regex");

        String regexPattern = (String) tokenMatcher.get("value");
        assertNotNull(regexPattern, "Dynamic matcher regex value must not be null");

        // Execute actual endpoint call with request payload specified in contract
        SignInRequest signInRequest = new SignInRequest(
                (String) requestBody.get("username"),
                (String) requestBody.get("password")
        );

        MvcResult result = mockMvc.perform(post((String) requestSpec.get("url"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus(), "Actual response status must be 200");

        String responseContent = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> actualResponse = objectMapper.readValue(responseContent, Map.class);

        assertTrue(actualResponse.containsKey("token"), "Response must contain 'token' field");
        String actualToken = (String) actualResponse.get("token");

        // Verify that the dynamic token is non-empty
        assertNotNull(actualToken, "Dynamic token cannot be null");
        assertFalse(actualToken.trim().isEmpty(), "Dynamic token must be non-empty");

        // Verify that the dynamic token matches the regular expression configured in the contract
        Pattern pattern = Pattern.compile(regexPattern);
        assertTrue(pattern.matcher(actualToken).matches(),
                "Actual dynamic token must match contract regex pattern: " + regexPattern);
    }
}
