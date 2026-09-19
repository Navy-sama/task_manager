package io.github.navysama.taskmanager.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import io.github.navysama.taskmanager.auth.internal.security.AuthCookies;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Base class of API integration tests: full application context, real security filter chain, Flyway-migrated
 * database. Tests never assume an empty database: every test creates its own user with a unique e-mail.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    protected static final String PASSWORD = "correct-horse-42";

    @Autowired
    protected MockMvc mockMvc;

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    protected static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    /** Registers a user in web mode and returns its session cookies. */
    protected WebSession registerWebUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(uniqueEmail(), PASSWORD)))
                .andReturn();
        return new WebSession(
                result.getResponse().getCookie(AuthCookies.ACCESS_TOKEN),
                result.getResponse().getCookie(AuthCookies.REFRESH_TOKEN));
    }

    /** Registers a user in mobile mode and returns its access token. */
    protected String registerMobileUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .header("X-Client-Type", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(uniqueEmail(), PASSWORD)))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    protected record WebSession(Cookie accessToken, Cookie refreshToken) {}
}
