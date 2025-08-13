package ru.denis5726.criteriaarticle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.ZonedDateTime;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "currentZonedDateTimeProvider")
public class JpaConfig {

    @Bean
    DateTimeProvider currentZonedDateTimeProvider() {
        return () -> Optional.of(ZonedDateTime.now());
    }
}
