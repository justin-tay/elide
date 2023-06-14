/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.ElideResponseBodyMapper;
import com.yahoo.elide.ElideStreamingBody;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;

/**
 * GraphQL body mapper.
 */
public class GraphQLBodyMapper implements ElideResponseBodyMapper {
    private final ObjectMapper objectMapper;

    public GraphQLBodyMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object map(Object body) throws Exception {
        if (body == null) {
            return null;
        }
        if (body instanceof String value) {
            return value;
        }
        return new ElideStreamingBody() {
            @Override
            public void writeTo(OutputStream outputStream) throws IOException {
                objectMapper.writeValue(outputStream, body);
            }
        };
    }
}
