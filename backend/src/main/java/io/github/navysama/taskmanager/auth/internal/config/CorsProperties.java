package io.github.navysama.taskmanager.auth.internal.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param allowedOrigins browser origins allowed to call the API cross-origin; empty means same-origin only
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream()
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toList();
    }
}
