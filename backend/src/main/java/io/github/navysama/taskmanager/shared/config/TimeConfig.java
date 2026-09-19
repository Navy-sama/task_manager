package io.github.navysama.taskmanager.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TimeConfig {

    /** Single UTC clock injected wherever "now" is needed, so time-dependent logic stays testable. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
