package io.github.navysama.taskmanager.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.navysama.taskmanager.auth.internal.security.AuthCookies;
import io.github.navysama.taskmanager.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class AuthApiIT extends IntegrationTest {

    private static final String MOBILE = "mobile";

    @Nested
    class Register {

        @Test
        void register_shouldSetHttpOnlyCookiesAndReturnUserOnly_whenWebClient() throws Exception {
            String email = uniqueEmail();

            MvcResult result = mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials(email, PASSWORD)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.user.email").value(email))
                    .andExpect(jsonPath("$.user.id").isNumber())
                    .andExpect(jsonPath("$.accessToken").doesNotExist())
                    .andExpect(jsonPath("$.refreshToken").doesNotExist())
                    .andReturn();

            List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
            assertThat(setCookies)
                    .anySatisfy(cookie -> assertThat(cookie)
                            .startsWith(AuthCookies.ACCESS_TOKEN + "=")
                            .contains("HttpOnly", "SameSite=Strict", "Path=/api;"))
                    .anySatisfy(cookie -> assertThat(cookie)
                            .startsWith(AuthCookies.REFRESH_TOKEN + "=")
                            .contains("HttpOnly", "SameSite=Strict", "Path=/api/auth"));
        }

        @Test
        void register_shouldReturnTokensInBodyAndNoCookie_whenMobileClient() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .header("X-Client-Type", MOBILE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials(uniqueEmail(), PASSWORD)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").isString())
                    .andExpect(jsonPath("$.refreshToken").isString())
                    .andExpect(jsonPath("$.expiresIn").value(900))
                    .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        }

        @Test
        void register_shouldReturn409_whenEmailAlreadyUsedWithDifferentCase() throws Exception {
            String email = uniqueEmail();
            mockMvc.perform(post("/api/auth/register")
                            .header("X-Client-Type", MOBILE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials(email, PASSWORD)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/auth/register")
                            .header("X-Client-Type", MOBILE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials("  " + email.toUpperCase() + " ", PASSWORD)))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"));
        }

        @Test
        void register_shouldReturn400WithFieldErrors_whenBodyIsInvalid() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .header("X-Client-Type", MOBILE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials("not-an-email", "short")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
                    .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists());
        }

        @Test
        void register_shouldReturn400_whenJsonIsMalformed() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .header("X-Client-Type", MOBILE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{not json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        }
    }

    @Nested
    class Login {

        @Test
        void login_shouldReturnSameGeneric401_whenPasswordWrongOrEmailUnknown() throws Exception {
            String email = uniqueEmail();
            mockMvc.perform(post("/api/auth/register")
                    .header("X-Client-Type", MOBILE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(credentials(email, PASSWORD)));

            String wrongPassword = mockMvc.perform(post("/api/auth/login")
                            .header("X-Client-Type", MOBILE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials(email, "wrong-password-42")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            String unknownEmail = mockMvc.perform(post("/api/auth/login")
                            .header("X-Client-Type", MOBILE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials(uniqueEmail(), PASSWORD)))
                    .andExpect(status().isUnauthorized())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertThat(JsonPath.<String>read(unknownEmail, "$.detail"))
                    .isEqualTo(JsonPath.<String>read(wrongPassword, "$.detail"));
        }

        @Test
        void login_shouldSucceed_whenStaleAccessCookieIsPresent() throws Exception {
            String email = uniqueEmail();
            mockMvc.perform(post("/api/auth/register")
                    .header("X-Client-Type", MOBILE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(credentials(email, PASSWORD)));

            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .cookie(new Cookie(AuthCookies.ACCESS_TOKEN, "expired-or-garbage"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials(email, PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.email").value(email));
        }
    }

    @Nested
    class Csrf {

        @Test
        void webMutation_shouldReturn403_whenCsrfTokenIsMissing() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials(uniqueEmail(), PASSWORD)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
        }
    }

    @Nested
    class Me {

        @Test
        void me_shouldReturnUser_whenAccessCookieIsValid() throws Exception {
            WebSession session = registerWebUser();

            mockMvc.perform(get("/api/auth/me").cookie(session.accessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").isString());
        }

        @Test
        void me_shouldReturnUser_whenBearerTokenIsValid() throws Exception {
            String accessToken = registerMobileUser();

            mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNumber());
        }

        @Test
        void me_shouldReturn401Problem_whenAnonymousOrTokenInvalid() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

            mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }
    }

    @Nested
    class RefreshAndLogout {

        @Test
        void refresh_shouldRotateTokens_andRevokeEverySession_whenAnOldTokenIsReused() throws Exception {
            WebSession session = registerWebUser();

            MvcResult rotated = mockMvc.perform(
                            post("/api/auth/refresh").with(csrf()).cookie(session.refreshToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.email").isString())
                    .andReturn();
            Cookie newRefresh = rotated.getResponse().getCookie(AuthCookies.REFRESH_TOKEN);
            assertThat(newRefresh).isNotNull();
            assertThat(newRefresh.getValue())
                    .isNotEqualTo(session.refreshToken().getValue());

            // Replaying the consumed token is treated as theft...
            mockMvc.perform(post("/api/auth/refresh").with(csrf()).cookie(session.refreshToken()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                    .andExpect(header().stringValues(
                                    HttpHeaders.SET_COOKIE,
                                    org.hamcrest.Matchers.hasItem(
                                            org.hamcrest.Matchers.startsWith(AuthCookies.REFRESH_TOKEN + "=;"))));

            // ...so even the legitimately rotated token no longer works.
            mockMvc.perform(post("/api/auth/refresh").with(csrf()).cookie(newRefresh))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void refresh_shouldRotateTokens_whenMobileSendsTokenInBody() throws Exception {
            MvcResult registered = mockMvc.perform(post("/api/auth/register")
                            .header("X-Client-Type", MOBILE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials(uniqueEmail(), PASSWORD)))
                    .andReturn();
            String refreshToken = JsonPath.read(registered.getResponse().getContentAsString(), "$.refreshToken");

            mockMvc.perform(post("/api/auth/refresh")
                            .header("X-Client-Type", MOBILE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isString())
                    .andExpect(jsonPath("$.refreshToken").value(org.hamcrest.Matchers.not(refreshToken)));
        }

        @Test
        void refresh_shouldReturn401_whenNoTokenIsSent() throws Exception {
            mockMvc.perform(post("/api/auth/refresh").with(csrf()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        }

        @Test
        void logout_shouldRevokeRefreshTokenAndClearCookies() throws Exception {
            WebSession session = registerWebUser();

            mockMvc.perform(post("/api/auth/logout").with(csrf()).cookie(session.refreshToken()))
                    .andExpect(status().isNoContent())
                    .andExpect(header().stringValues(
                                    HttpHeaders.SET_COOKIE,
                                    org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("Max-Age=0"))));

            mockMvc.perform(post("/api/auth/refresh").with(csrf()).cookie(session.refreshToken()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void logout_shouldBeIdempotent_whenNoSessionExists() throws Exception {
            mockMvc.perform(post("/api/auth/logout").with(csrf())).andExpect(status().isNoContent());
        }
    }
}
