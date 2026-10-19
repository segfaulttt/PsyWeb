
package com.psyweb.auth.web.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.psyweb.client.domain.Client;
import com.psyweb.client.repository.ClientRepository;
import com.psyweb.specialist.repository.SpecialistRepository;
import com.psyweb.testsupport.PostgreSQLIntegrationTest;
import com.psyweb.user.domain.User;
import com.psyweb.user.domain.UserRole;
import com.psyweb.user.domain.UserStatus;
import com.psyweb.user.repository.UserRepository;

@AutoConfigureMockMvc
class AuthRegistrationIntegrationTest
        extends PostgreSQLIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Test
    void shouldRegisterClientThroughHttpAndPersistProfile()
            throws Exception {

        String email = "http-registration-"
                + UUID.randomUUID()
                + "@example.com";

        String password = "StrongPassword123!";

        String requestJson = """
                {
                  "email": "%s",
                  "password": "%s",
                  "firstName": "Anna",
                  "lastName": "Smith"
                }
                """.formatted(email, password);

        MvcResult result = mockMvc.perform(
                        post("/api/auth/register/client")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        long userId = response.get("userId").asLong();

        User user = userRepository.findById(userId)
                .orElseThrow();

        Client client = clientRepository.findById(userId)
                .orElseThrow();

        assertEquals(email, user.getEmail());
        assertEquals(UserRole.CLIENT, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());

        assertEquals(userId, client.getId());
        assertEquals("Anna", client.getFirstName());
        assertEquals("Smith", client.getLastName());

        assertNotEquals(password, user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("$argon2id$"));

        assertFalse(specialistRepository.existsById(userId));
    }
}