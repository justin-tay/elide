/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents an error that can later be mapped to the more specific
 * JsonApiError or GraphQLError.
 *
 * @see ElideErrors
 */
@Getter
public class ElideError {
    /**
     * The error message. For JSON-API this will be mapped to the details member and
     * for GraphQL this will be mapped to the message member.
     */
    private final String message;

    /**
     * Additional attributes about the error. For JSON-API this will be mapped to the
     * meta member and for GraphQL this will be mapped to the extensions member.
     */
    private final Map<String, Object> attributes;

    public ElideError(String message, Map<String, Object> attributes) {
        this.message = message;
        this.attributes = attributes;
    }

    public static ElideErrorBuilder builder() {
        return new ElideErrorBuilder();
    }

    public static class ElideErrorBuilder {
        private String message;
        private Map<String, Object> attributes = new LinkedHashMap<>();

        public ElideErrorBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ElideErrorBuilder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        public ElideErrorBuilder attributes(Consumer<Map<String, Object>> attributes) {
            attributes.accept(this.attributes);
            return this;
        }

        public ElideErrorBuilder attribute(String key, String value) {
            this.attributes.put(key, value);
            return this;
        }

        public ElideError build() {
            return new ElideError(this.message, this.attributes);
        }
    }
}
