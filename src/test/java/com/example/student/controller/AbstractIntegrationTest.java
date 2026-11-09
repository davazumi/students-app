package com.example.student.controller;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Общая основа для интеграционных тестов: поднимается всё приложение
 * с профилем "demo" (база H2 в памяти) и тестовыми пользователями из AppInitializer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
abstract class AbstractIntegrationTest {

    protected static final String ADMIN = "admin@example.com";
    protected static final String TEACHER = "teacher@example.com";
    protected static final String SUPER_ADMIN = "superadmin@example.com";

    @Autowired
    protected MockMvc mockMvc;

    /** Логинится через API и возвращает заголовок "Bearer <токен>". */
    protected String bearer(String email) throws Exception {
        String password = switch (email) {
            case ADMIN -> "admin";
            case TEACHER -> "teacher";
            case SUPER_ADMIN -> "superAdmin";
            default -> throw new IllegalArgumentException(email);
        };
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }
}
