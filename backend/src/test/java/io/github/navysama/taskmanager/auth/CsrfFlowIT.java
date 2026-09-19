package io.github.navysama.taskmanager.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.navysama.taskmanager.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Exercises the real browser CSRF handshake, without spring-security-test's {@code csrf()} helper. That helper swaps
 * the CSRF token repository of the shared application context for a test double, hence a fresh context here.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CsrfFlowIT extends IntegrationTest {

    @Test
    void firstGet_shouldProvideXsrfCookie_usableByTheNextMutation() throws Exception {
        MvcResult bootstrap = mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        Cookie xsrf = bootstrap.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf).isNotNull();
        assertThat(xsrf.isHttpOnly()).isFalse();

        mockMvc.perform(post("/api/auth/register")
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(uniqueEmail(), PASSWORD)))
                .andExpect(status().isCreated());
    }
}
