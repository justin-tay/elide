/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import java.util.function.Consumer;

/**
 * Elide Error Response.
 * <p>
 * Builder example:
 * <pre><code>
 * ElideErrorResponse.status(400)
 *     .errors(errors -> errors.error(error -> error.message(message)))
 *     .build();
 * </code></pre>
 *
 * @param <T> the body type
 */
public class ElideErrorResponse<T> {
    /**
     * The HTTP status code.
     */
    private final int status;

    /**
     * The body.
     */
    private final T body;

    public ElideErrorResponse(int status, T body) {
        this.status = status;
        this.body = body;
    }

    /**
     * Returns the HTTP status code of the response.
     *
     * @return the HTTP status code of the response
     */
    public int getStatus() {
        return this.status;
    }

    /**
     * Returns the body of the response.
     *
     * @return the body of the response
     */
    public T getBody() {
        return this.body;
    }

    /**
     * Returns the body of the response if it is of the appropriate type.
     *
     * @param <V> the expected type of the response
     * @param clazz the expected class of the response
     * @return the body of the response
     */
    public <V> V getBody(Class<V> clazz) {
        if (clazz.isInstance(this.body)) {
            return clazz.cast(this.body);
        }
        return null;
    }

    /**
     * Builds a response with this HTTP status code.
     *
     * @param status the HTTP status code
     * @return the builder
     */
    public static ElideErrorResponseBuilder status(int status) {
        return new ElideErrorResponseBuilder(status);
    }

    /**
     * Builder for building a @{link ElideErrorResponse}.
     */
    public static class ElideErrorResponseBuilder {
        private int status;

        public ElideErrorResponseBuilder(int status) {
            this.status = status;
        }

        /**
         * Sets the body of the response.
         *
         * @param <T> the body type
         * @param body the body
         * @return the response
         */
        public <T> ElideErrorResponse<T> body(T body) {
            return new ElideErrorResponse<>(status, body);
        }

        /**
         * Sets the body of the response to {@link ElideErrors}.
         *
         * @param errors to customize
         * @return the response
         */
        public ElideErrorResponse<ElideErrors> errors(Consumer<ElideErrors.ElideErrorsBuilder> errors) {
            ElideErrors.ElideErrorsBuilder builder = ElideErrors.builder();
            errors.accept(builder);
            return body(builder.build());
        }
    }
}
