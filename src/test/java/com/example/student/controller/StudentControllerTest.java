package com.example.student.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentControllerTest extends AbstractIntegrationTest {

    private static final String URL = "/api/v1/student";

    @Test
    void teacherCanSeeStudentList() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(TEACHER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount", greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.response[0].name").exists());
    }

    @Test
    void adminCanCreateReadUpdateAndDeleteStudent() throws Exception {
        String admin = bearer(ADMIN);

        // создание
        String created = mockMvc.perform(post(URL + "/save")
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Anna","surname":"Testova","phone_number":"+79990001122","email":"anna@test.ru"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Anna"))
                .andExpect(jsonPath("$.surname").value("Testova"))
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(created, "$.id");

        // чтение полной информации
        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("+79990001122"))
                .andExpect(jsonPath("$.email").value("anna@test.ru"));

        mockMvc.perform(get(URL + "/info/" + id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Anna"));

        // изменение только телефона: email должен остаться прежним
        mockMvc.perform(put(URL + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"+70000000000\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.phoneNumber").value("+70000000000"))
                .andExpect(jsonPath("$.email").value("anna@test.ru"));

        // изменение email
        mockMvc.perform(put(URL + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@test.ru\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.email").value("new@test.ru"));

        // удаление
        mockMvc.perform(delete(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk());
        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownStudentReturns404WithMessage() throws Exception {
        mockMvc.perform(get(URL + "/999999").header(HttpHeaders.AUTHORIZATION, bearer(ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void updatingUnknownStudentReturns404() throws Exception {
        mockMvc.perform(put(URL + "/999999")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@test.ru\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void teacherCannotCreateOrDeleteStudents() throws Exception {
        String teacher = bearer(TEACHER);
        mockMvc.perform(post(URL + "/save")
                        .header(HttpHeaders.AUTHORIZATION, teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"surname\":\"Y\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(URL + "/1").header(HttpHeaders.AUTHORIZATION, teacher))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdminCannotSeeStudents() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(SUPER_ADMIN)))
                .andExpect(status().isForbidden());
    }
}
