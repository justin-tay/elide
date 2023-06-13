/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.ElideError;
import com.yahoo.elide.ElideErrorResponse;
import com.yahoo.elide.ElideErrors;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.core.exceptions.ExceptionHandlerSupport;
import com.yahoo.elide.core.exceptions.ExceptionMappers;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.graphql.models.GraphQLErrors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import graphql.GraphQLException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Default {@link GraphQLExceptionHandler}.
 */
@Slf4j
public class DefaultGraphQLExceptionHandler extends ExceptionHandlerSupport<GraphQLErrorContext>
        implements GraphQLExceptionHandler {
    protected ObjectMapper objectMapper;
    protected GraphQLErrorMapper graphqlErrorMapper;

    public DefaultGraphQLExceptionHandler(ExceptionMappers exceptionMappers, ObjectMapper objectMapper,
            GraphQLErrorMapper graphqlErrorMapper) {
        super(exceptionMappers);
        this.objectMapper = objectMapper;
        this.graphqlErrorMapper = graphqlErrorMapper;
    }

    @Override
    public ElideResponse<?> handleException(Throwable exception, GraphQLErrorContext errorContext) {
        return super.handleException(exception, errorContext);
    }

    @Override
    protected ElideResponse<?> handleRuntimeException(RuntimeException exception, GraphQLErrorContext errorContext) {
        if (exception instanceof GraphQLException e) {
            log.debug("GraphQLException", e);
            String body = e.getMessage();
            return ElideResponse.status(HttpStatus.SC_OK).body(body);
        }

        if (exception instanceof HttpStatusException e) {
            if (e instanceof ForbiddenAccessException forbiddenAccessException) {
                if (log.isDebugEnabled()) {
                    log.debug("{}", forbiddenAccessException.getLoggedMessage());
                }
            } else {
                log.debug("Caught HTTP status exception {}", e.getStatus(), e);
            }

            return buildResponse(new HttpStatusException(200, e.getMessage()) {
                @Override
                public int getStatus() {
                    return 200;
                }

                @Override
                public ElideErrorResponse<?> getErrorResponse() {
                    ElideErrorResponse<?> r = e.getErrorResponse();
                    return ElideErrorResponse.status(getStatus()).body(r.getBody());
                }

                @Override
                public ElideErrorResponse<?> getVerboseErrorResponse() {
                    ElideErrorResponse<?> r = e.getVerboseErrorResponse();
                    return ElideErrorResponse.status(getStatus()).body(r.getBody());
                }

                @Override
                public String getVerboseMessage() {
                    return e.getVerboseMessage();
                }

                @Override
                public String toString() {
                    return e.toString();
                }
            }, errorContext);
        }

        if (exception instanceof ConstraintViolationException e) {
            log.debug("Constraint violation exception caught", e);
            final GraphQLErrors.GraphQLErrorsBuilder errors = GraphQLErrors.builder();
            for (ConstraintViolation<?> constraintViolation : e.getConstraintViolations()) {
                errors.error(error -> {
                    error.message(constraintViolation.getMessage());
                    error.extension("code", constraintViolation.getConstraintDescriptor().getAnnotation()
                            .annotationType().getSimpleName());
                    error.extension("type",  "ConstraintViolation");
                    final String propertyPathString = constraintViolation.getPropertyPath().toString();
                    if (!propertyPathString.isEmpty()) {
                        error.extension("property",  propertyPathString);
                    }
                });
            }
            return buildResponse(HttpStatus.SC_OK, errors.build());
        }

        log.error("Error or exception uncaught by Elide", exception);
        throw exception;

    }

    @Override
    protected ElideResponse<?> handleNonRuntimeException(Exception exception, GraphQLErrorContext errorContext) {
        if (exception instanceof JsonProcessingException) {
            log.debug("Invalid json body provided to GraphQL", exception);
            return buildResponse(new InvalidEntityBodyException(errorContext.getGraphQLDocument()), errorContext);
        }

        if (exception instanceof IOException) {
            log.error("Uncaught IO Exception by Elide in GraphQL", exception);
            return buildResponse(new TransactionException(exception), errorContext);
        }

        log.error("Error or exception uncaught by Elide", exception);
        throw new RuntimeException(exception);
    }

    @Override
    protected ElideResponse<?> buildResponse(ElideErrorResponse<?> errorResponse) {
        if (errorResponse.getBody() instanceof ElideErrors errors) {
            GraphQLErrors.GraphQLErrorsBuilder builder = GraphQLErrors.builder();
            for (ElideError error : errors.getErrors()) {
                builder.error(graphqlErrorMapper.toGraphQLError(error));
            }
            return buildResponse(errorResponse.getStatus(), builder.build());
        }
        else {
            return buildResponse(errorResponse.getStatus(), errorResponse.getBody());
        }
    }

    @Override
    protected ElideResponse<?> buildResponse(int status, Object body) {
        try {
            return new ElideResponse<>(status, this.objectMapper.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            return new ElideResponse<>(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.toString());
        }
    }
}
