/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

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

    private final Object body;

    public ElideErrorResponse(int responseCode, Object body) {
        this.responseCode = responseCode;
        this.body = body;
    }

    public <T> T getBody(Class<T> clazz) {
        if (clazz.isInstance(this.body)) {
            return clazz.cast(this.body);
        }
        return null;
    }

    public static ElideErrorResponseBuilder builder() {
        return new ElideErrorResponseBuilder();
    }

    public static class ElideErrorResponseBuilder {
        private int responseCode;
        private Object body;

        public ElideErrorResponseBuilder responseCode(int responseCode) {
            this.responseCode = responseCode;
            return this;
        }

        public ElideErrorResponseBuilder body(Object body) {
            this.body = body;
            return this;
        }

        public ElideErrorResponseBuilder errors(Consumer<ElideErrors.ElideErrorsBuilder> errors) {
            ElideErrors.ElideErrorsBuilder builder = ElideErrors.builder();
            errors.accept(builder);
            return body(builder.build());
        }

        public ElideErrorResponse build() {
            return new ElideErrorResponse(this.responseCode, this.body);
        }
    }
}
