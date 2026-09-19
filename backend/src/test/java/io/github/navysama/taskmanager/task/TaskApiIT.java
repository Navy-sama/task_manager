package io.github.navysama.taskmanager.task;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.navysama.taskmanager.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class TaskApiIT extends IntegrationTest {

    private String owner;

    @BeforeEach
    void signIn() throws Exception {
        owner = registerMobileUser();
    }

    @Test
    void create_shouldReturn201WithDefaultsAndLocation() throws Exception {
        mockMvc.perform(as(owner, post("/api/tasks")).content("{\"title\":\"  Write README  \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("/api/tasks/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Write README"))
                .andExpect(jsonPath("$.description").isEmpty())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.updatedAt").isString());
    }

    @Test
    void create_shouldWorkWithWebCookieSession() throws Exception {
        WebSession session = registerWebUser();

        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .cookie(session.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"From the browser\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void create_shouldReturn400WithFieldErrors_whenTitleMissingOrTooLong() throws Exception {
        mockMvc.perform(as(owner, post("/api/tasks")).content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("title"));

        mockMvc.perform(as(owner, post("/api/tasks")).content("{\"title\":\"" + "x".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400_whenStatusIsUnknown() throws Exception {
        mockMvc.perform(as(owner, post("/api/tasks")).content("{\"title\":\"A\",\"status\":\"LATER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void list_shouldFilterByStatusAndSearchCaseInsensitively() throws Exception {
        createTask(owner, "Buy milk", "at the store", "TODO");
        createTask(owner, "Call the BANK", null, "IN_PROGRESS");
        createTask(owner, "Pay rent", "bank transfer", "DONE");

        mockMvc.perform(as(owner, get("/api/tasks")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].title").value("Pay rent"));

        mockMvc.perform(as(owner, get("/api/tasks").param("status", "IN_PROGRESS")))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Call the BANK"));

        mockMvc.perform(as(owner, get("/api/tasks").param("q", "bank")))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(as(owner, get("/api/tasks").param("q", "bank").param("status", "DONE")))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Pay rent"));
    }

    @Test
    void list_shouldTreatSqlWildcardsLiterally() throws Exception {
        createTask(owner, "50% discount", null, "TODO");
        createTask(owner, "500 items", null, "TODO");

        mockMvc.perform(as(owner, get("/api/tasks").param("q", "50%")))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("50% discount"));
    }

    @Test
    void list_shouldPaginateAndClampOutOfRangeValues() throws Exception {
        for (int i = 0; i < 3; i++) {
            createTask(owner, "Task " + i, null, "TODO");
        }

        mockMvc.perform(as(owner, get("/api/tasks").param("size", "2").param("page", "1")))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(as(owner, get("/api/tasks").param("size", "5000").param("page", "-3")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void list_shouldReturn400_whenStatusParameterIsInvalid() throws Exception {
        mockMvc.perform(as(owner, get("/api/tasks").param("status", "LATER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void update_shouldReplaceFieldsAndRefreshUpdatedAt() throws Exception {
        long id = createTask(owner, "Draft", "old description", "TODO");

        mockMvc.perform(as(owner, put("/api/tasks/" + id)).content("{\"title\":\"Final\",\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Final"))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.description").isEmpty());
    }

    @Test
    void update_shouldReturn400_whenStatusIsMissing() throws Exception {
        long id = createTask(owner, "Draft", null, "TODO");

        mockMvc.perform(as(owner, put("/api/tasks/" + id)).content("{\"title\":\"Final\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
    }

    @Test
    void delete_shouldReturn204ThenTaskIsGone() throws Exception {
        long id = createTask(owner, "Temporary", null, "TODO");

        mockMvc.perform(as(owner, delete("/api/tasks/" + id))).andExpect(status().isNoContent());
        mockMvc.perform(as(owner, delete("/api/tasks/" + id)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
    }

    @Test
    void anotherUsersTask_shouldBeInvisibleAndReturn404_neverFound403() throws Exception {
        long id = createTask(owner, "Private plan", null, "TODO");
        String intruder = registerMobileUser();

        mockMvc.perform(as(intruder, get("/api/tasks")))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(as(intruder, put("/api/tasks/" + id)).content("{\"title\":\"Hacked\",\"status\":\"DONE\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
        mockMvc.perform(as(intruder, delete("/api/tasks/" + id))).andExpect(status().isNotFound());

        mockMvc.perform(as(owner, get("/api/tasks")))
                .andExpect(jsonPath("$.content[0].title").value("Private plan"));
    }

    @Test
    void tasks_shouldReturn401Problem_whenAnonymous() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void unknownRoute_shouldReturn404Problem() throws Exception {
        mockMvc.perform(as(owner, get("/api/nothing-here")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private MockHttpServletRequestBuilder as(String accessToken, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private long createTask(String accessToken, String title, String description, String status) throws Exception {
        String body = "{\"title\":\"" + title + "\",\"status\":\"" + status + "\""
                + (description == null ? "" : ",\"description\":\"" + description + "\"") + "}";
        String response = mockMvc.perform(as(accessToken, post("/api/tasks")).content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }
}
