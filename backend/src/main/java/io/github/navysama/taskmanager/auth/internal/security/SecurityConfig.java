package io.github.navysama.taskmanager.auth.internal.security;

import io.github.navysama.taskmanager.auth.internal.config.CookieProperties;
import io.github.navysama.taskmanager.auth.internal.config.CorsProperties;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, ProblemDetailSecurityHandler problems, CookieProperties cookies, CorsProperties cors)
            throws Exception {
        http.csrf(csrf -> csrf.spa()
                        .csrfTokenRepository(csrfTokenRepository(cookies))
                        .ignoringRequestMatchers(ClientRequests::isNonBrowserClient))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .cors(corsConfig -> corsConfig.configurationSource(corsConfigurationSource(cors)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers(HttpMethod.POST, ClientRequests.PUBLIC_AUTH_ENDPOINTS)
                                .permitAll()
                                .requestMatchers(
                                        "/v3/api-docs/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/actuator/health/**",
                                        "/error")
                                .permitAll()
                                .anyRequest()
                                .authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .bearerTokenResolver(new CookieOrHeaderBearerTokenResolver())
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems)
                        .jwt(Customizer.withDefaults()))
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(problems).accessDeniedHandler(problems));
        return http.build();
    }

    /** The XSRF-TOKEN cookie must be readable by JavaScript (double-submit pattern), hence not HttpOnly. */
    private static CookieCsrfTokenRepository csrfTokenRepository(CookieProperties cookies) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie ->
                cookie.sameSite("Strict").secure(cookies.cookieSecure()).path("/"));
        return repository;
    }

    /** Empty allow-list by default: the web app is served from the same origin as the API. */
    private static CorsConfigurationSource corsConfigurationSource(CorsProperties cors) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(cors.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-XSRF-TOKEN", "X-Client-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
