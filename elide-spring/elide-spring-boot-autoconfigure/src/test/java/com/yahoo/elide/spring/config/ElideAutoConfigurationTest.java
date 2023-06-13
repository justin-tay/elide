/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.yahoo.elide.ElideErrorResponse;
import com.yahoo.elide.ElideErrors;
import com.yahoo.elide.core.exceptions.ErrorContext;
import com.yahoo.elide.core.exceptions.ExceptionMapper;
import com.yahoo.elide.core.exceptions.ExceptionMapperRegistration;
import com.yahoo.elide.core.exceptions.ExceptionMappers;
import com.yahoo.elide.core.exceptions.ExceptionMappers.ExceptionMappersBuilder;
import com.yahoo.elide.core.exceptions.ExceptionMappersBuilderCustomizer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.ConstraintViolationException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for ElideAutoConfiguration.
 */
class ElideAutoConfigurationTest {
    private static final String TARGET_NAME_PREFIX = "scopedTarget.";
    private static final String SCOPE_REFRESH = "refresh";
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ElideAutoConfiguration.class, DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class, TransactionAutoConfiguration.class, RefreshAutoConfiguration.class));

    @Test
    void nonRefreshable() {
        Set<String> nonRefreshableBeans = new HashSet<>();

        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false", "elide.json-api.enabled=true",
                "elide.api-docs.enabled=true", "elide.graphql.enabled=true").run(context -> {
                    Arrays.stream(context.getBeanDefinitionNames()).forEach(beanDefinitionName -> {
                        if (context.getBeanFactory() instanceof BeanDefinitionRegistry beanDefinitionRegistry) {
                            BeanDefinition beanDefinition = beanDefinitionRegistry
                                    .getBeanDefinition(beanDefinitionName);
                            assertThat(beanDefinition.getScope()).isNotEqualTo(SCOPE_REFRESH);
                            nonRefreshableBeans.add(beanDefinitionName);
                        }
                    });
                });
        assertThat(nonRefreshableBeans).contains("refreshableElide", "graphqlController", "queryRunners",
                "apiDocsController", "apiDocsRegistrations", "jsonApiController");
    }

    enum RefreshableInput {
        GRAPHQL(new String[] { "elide.graphql.enabled=true" },
                new String[] { "refreshableElide", "graphqlController", "queryRunners" }),
        OPENAPI(new String[] { "elide.api-docs.enabled=true" },
                new String[] { "refreshableElide", "apiDocsController", "apiDocsRegistrations" }),
        JSONAPI(new String[] { "elide.json-api.enabled=true" },
                new String[] { "refreshableElide", "jsonApiController" });

        String[] propertyValues;
        String[] beanNames;

        RefreshableInput(String[] propertyValues, String[] beanNames) {
            this.propertyValues = propertyValues;
            this.beanNames = beanNames;
        }
    }

    @ParameterizedTest
    @EnumSource(RefreshableInput.class)
    void refreshable(RefreshableInput refreshableInput) {
        contextRunner.withPropertyValues(refreshableInput.propertyValues).run(context -> {

            Set<String> refreshableBeans = new HashSet<>();

            Arrays.stream(context.getBeanDefinitionNames()).forEach(beanDefinitionName -> {
                if (context.getBeanFactory() instanceof BeanDefinitionRegistry beanDefinitionRegistry) {
                    BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(beanDefinitionName);
                    if (SCOPE_REFRESH.equals(beanDefinition.getScope())) {
                        refreshableBeans.add(beanDefinitionName.replace(TARGET_NAME_PREFIX, ""));
                        assertThat(context.getBean(beanDefinitionName)).isNotNull();
                    }
                }
            });

            assertThat(refreshableBeans).contains(refreshableInput.beanNames);
        });
    }

    @RestController
    public static class UserGraphqlController {
    }

    @Configuration(proxyBeanMethods = false)
    public static class UserGraphqlControllerConfiguration {
        @Bean
        public UserGraphqlController graphqlController() {
            return new UserGraphqlController();
        }
    }

    @RestController
    public static class UserApiDocsController {
    }

    @Configuration(proxyBeanMethods = false)
    public static class UserApiDocsControllerConfiguration {
        @Bean
        public UserApiDocsController apiDocsController() {
            return new UserApiDocsController();
        }
    }

    @RestController
    public static class UserJsonApiController {
    }

    @Configuration(proxyBeanMethods = false)
    public static class UserJsonApiControllerConfiguration {
        @Bean
        public UserJsonApiController jsonApiController() {
            return new UserJsonApiController();
        }
    }

    enum OverrideControllerInput {
        GRAPHQL(new String[] { "elide.graphql.enabled=true" }, UserGraphqlControllerConfiguration.class,
                "graphqlController"),
        OPENAPI(new String[] { "elide.api-docs.enabled=true" }, UserApiDocsControllerConfiguration.class,
                "apiDocsController"),
        JSONAPI(new String[] { "elide.json-api.enabled=true" }, UserJsonApiControllerConfiguration.class,
                "jsonApiController");

        String[] propertyValues;
        Class<?> userConfiguration;
        String beanName;

        OverrideControllerInput(String[] propertyValues, Class<?> userConfiguration, String beanName) {
            this.propertyValues = propertyValues;
            this.userConfiguration = userConfiguration;
            this.beanName = beanName;
        }
    }

    @ParameterizedTest
    @EnumSource(OverrideControllerInput.class)
    void overrideController(OverrideControllerInput input) {
        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false").withPropertyValues(input.propertyValues)
                .withConfiguration(UserConfigurations.of(input.userConfiguration)).run(context -> {
                    if (context.getBeanFactory() instanceof BeanDefinitionRegistry beanDefinitionRegistry) {
                        BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(input.beanName);
                        assertThat(beanDefinition.getFactoryBeanName())
                                .endsWith(input.userConfiguration.getSimpleName());
                    }
                });
    }

    public static class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException, ElideErrors> {
        @Override
        public ElideErrorResponse<ElideErrors> toErrorResponse(ConstraintViolationException exception,
                ErrorContext errorContext) {
            return ElideErrorResponse.status(400)
                    .errors(errors -> errors.error(error -> error.message(exception.getMessage())));
        }
    }

    @Configuration
    public static class UserExceptionMapperConfiguration {
        @Bean
        public ConstraintViolationExceptionMapper constraintViolationExceptionMapper() {
            return new ConstraintViolationExceptionMapper();
        }
    }

    @Test
    void exceptionMappers() {
        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false")
                .withUserConfiguration(UserExceptionMapperConfiguration.class).run(context -> {
                    ExceptionMappers exceptionMappers = context.getBean(ExceptionMappersBuilder.class).build();
                    ElideErrorResponse<Object> errorResponse = exceptionMappers
                            .toErrorResponse(new IllegalArgumentException(), null);
                    assertThat(errorResponse).isNull();
                    errorResponse = exceptionMappers.toErrorResponse(new ConstraintViolationException("message", null),
                            null);
                    assertThat(errorResponse.getBody(ElideErrors.class).getErrors().get(0).getMessage())
                            .isEqualTo("message");
                });
    }

    @Configuration
    public static class UserExceptionMappersBuilderCustomizerConfiguration {
        @Bean
        public ExceptionMappersBuilderCustomizer exceptionMappersBuilderCustomizer() {
            return builder -> builder.registrations(registrations -> {
               // Add in front
                registrations.add(0, ExceptionMapperRegistration.builder().supported(ConstraintViolationException.class)
                        .exceptionMapper((exception, errorContext) -> ElideErrorResponse.status(200).build()).build());
            });
        }
    }

    @Test
    void exceptionMappersBuilderCustomizer() {
        contextRunner.withPropertyValues("spring.cloud.refresh.enabled=false")
                .withUserConfiguration(UserExceptionMapperConfiguration.class,
                        UserExceptionMappersBuilderCustomizerConfiguration.class)
                .run(context -> {
                    ExceptionMappers exceptionMappers = context.getBean(ExceptionMappersBuilder.class).build();
                    ElideErrorResponse<Object> errorResponse = exceptionMappers
                            .toErrorResponse(new IllegalArgumentException(), null);
                    assertThat(errorResponse).isNull();
                    errorResponse = exceptionMappers.toErrorResponse(new ConstraintViolationException("message", null),
                            null);
                    assertThat(errorResponse.getStatus()).isEqualTo(200);
                });
    }
}
