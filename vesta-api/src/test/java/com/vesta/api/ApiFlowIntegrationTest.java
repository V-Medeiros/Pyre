package com.vesta.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class ApiFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void registrationRefreshOwnershipAndFocusFlowWorkTogether() throws Exception {
        MvcResult firstRegistration = register("first-" + UUID.randomUUID() + "@example.com");
        String firstToken = body(firstRegistration).get("accessToken").asText();
        Cookie refresh = firstRegistration.getResponse().getCookie("vesta_refresh");
        Cookie csrf = firstRegistration.getResponse().getCookie("vesta_csrf");

        mvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refresh, csrf)
                        .header("X-CSRF-Token", csrf.getValue()))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("vesta_refresh", true))
                .andExpect(cookie().path("vesta_csrf", "/"));

        String taskId = "task_" + UUID.randomUUID();
        mvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new TaskRequest(taskId, "Write integration test"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(taskId));

        MvcResult secondRegistration = register("second-" + UUID.randomUUID() + "@example.com");
        String secondToken = body(secondRegistration).get("accessToken").asText();
        mvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("task_not_found"));

        String sessionId = "session_" + UUID.randomUUID();
        mvc.perform(post("/api/v1/focus-sessions")
                        .header("Authorization", "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new SessionRequest(sessionId, 5, taskId, "test-device"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RUNNING"));
        mvc.perform(post("/api/v1/focus-sessions/{id}/pause", sessionId)
                        .header("Authorization", "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));
        mvc.perform(post("/api/v1/focus-sessions/{id}/resume", sessionId)
                        .header("Authorization", "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));
        mvc.perform(post("/api/v1/focus-sessions/{id}/abandon", sessionId)
                        .header("Authorization", "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABANDONED"));
    }

    private MvcResult register(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegisterRequest(
                                email, "correct-horse-battery-staple", "Vesta Test",
                                "America/Sao_Paulo", "pt-BR", "integration-test"))))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("vesta_refresh"))
                .andExpect(cookie().exists("vesta_csrf"))
                .andReturn();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsByteArray());
    }

    private record RegisterRequest(String email, String password, String displayName,
                                   String timezone, String locale, String deviceName) {
    }

    private record TaskRequest(String id, String title) {
    }

    private record SessionRequest(String id, int durationMinutes, String taskId, String deviceId) {
    }
}

