/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;

import java.util.function.Consumer;

/**
 * Elide Error Response.
 * <p>
 * Builder example:
 * <pre><code>
 * ElideErrorResponse.builder()
 *     .responseCode(200)
 *     .errors(errors -> errors.error(error -> error.message(message)))
 *     .build();
 * </code></pre>
 */
@Getter
public class ElideErrorResponse {
    private final int responseCode;

    /**
     * The errors will be used to generate the body if not present. This is not
     * specific to either JSON API or GraphQL and will later be mapped to the
     * JsonApiError and GraphQLError.
     */
    private final ElideErrors errors;

    /**
     * The body if present will be used.
     */
    private final JsonNode body;

    public ElideErrorResponse(int responseCode, ElideErrors errors) {
        this.responseCode = responseCode;
        this.errors = errors;
        this.body = null;
    }

    public ElideErrorResponse(int responseCode, JsonNode body) {
        this.responseCode = responseCode;
        this.body = body;
        this.errors = null;
    }


    public static ElideErrorResponseBuilder builder() {
        return new ElideErrorResponseBuilder();
    }

    public static class ElideErrorResponseBuilder {
        private int responseCode;
        private ElideErrors errors;
        private JsonNode body;

        public ElideErrorResponseBuilder responseCode(int responseCode) {
            this.responseCode = responseCode;
            return this;
        }

        public ElideErrorResponseBuilder body(JsonNode body) {
            this.body = body;
            return this;
        }

        public ElideErrorResponseBuilder errors(Consumer<ElideErrors.ElideErrorsBuilder> errors) {
            ElideErrors.ElideErrorsBuilder builder = ElideErrors.builder();
            errors.accept(builder);
            return errors(builder.build());
        }

        public ElideErrorResponseBuilder errors(ElideErrors errors) {
            this.errors = errors;
            return this;
        }

        public ElideErrorResponse build() {
            if (this.body != null) {
                return new ElideErrorResponse(this.responseCode, this.body);
            }
            return new ElideErrorResponse(this.responseCode, this.errors);
        }
    }
}
