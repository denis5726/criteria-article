package ru.denis5726.criteriaarticle.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.ZonedDateTime;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "currentZonedDateTimeProvider")
public class JpaConfig implements FunctionContributor {
    public static final String BOOL_OR = "bool_or";

    // Бин для получения текущего времени типа ZonedDateTime
    // для сущностей с аудитом AuditingEntityListener.class и полем с аннотацией @CreatedDate
    @Bean
    DateTimeProvider currentZonedDateTimeProvider() {
        return () -> Optional.of(ZonedDateTime.now());
    }

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        // Регистрация функции bool_or в реестре функций
        functionContributions.getFunctionRegistry().register(
                BOOL_OR,
                // StandardSQLFunction это самый простой способ описать SQL-функцию,
                // нужно просто указать её название и возвращаемый тип,
                // без более продвинутых вещей, например валидации аргументов
                new StandardSQLFunction(
                        BOOL_OR,
                        StandardBasicTypes.BOOLEAN
                )
        );
    }
}
