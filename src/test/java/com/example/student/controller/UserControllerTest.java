package com.example.student.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends AbstractIntegrationTest {

    private static final String URL = "/api/v1/users";

    @Test
    void superAdminSeesAllUsers() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(SUPER_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.responses[*].email", hasItem(ADMIN)));
    }

    @Test
    void superAdminCanCreateUpdateAndDeleteUser() throws Exception {
        String superAdmin = bearer(SUPER_ADMIN);

        String created = mockMvc.perform(post(URL + "/save")
                        .header(HttpHeaders.AUTHORIZATION, superAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newteacher@example.com\",\"password\":\"secret\",\"roles\":[\"TEACHER\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newteacher@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("TEACHER"))
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(created, "$.id");

        // новый пользователь может войти
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newteacher@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put(URL + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, superAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newteacher@example.com\",\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, superAdmin))
                .andExpect(status().isOk());

        String list = mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, superAdmin))
                .andReturn().getResponse().getContentAsString();
        List<String> emails = JsonPath.read(list, "$.responses[*].email");
        assertThat(emails).doesNotContain("newteacher@example.com");
    }

    @Test
    void adminHasNoAccessToUsers() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(ADMIN)))
                .andExpect(status().isForbidden());
    }
}
