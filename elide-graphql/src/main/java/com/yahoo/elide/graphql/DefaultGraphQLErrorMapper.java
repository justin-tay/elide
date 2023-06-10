/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.ElideError;

import graphql.GraphQLError;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default {@link GraphQLErrorMapper}.
 */
public class DefaultGraphQLErrorMapper implements GraphQLErrorMapper {

    @Override
    public GraphQLError toGraphQLError(ElideError error) {
        com.yahoo.elide.graphql.models.GraphQLError.GraphQLErrorBuilder graphqlError =
                com.yahoo.elide.graphql.models.GraphQLError.builder();
        if (error.getMessage() != null) {
            graphqlError.message(error.getMessage()); // The serializer will encode the message
        }
        if (error.getAttributes() != null && !error.getAttributes().isEmpty()) {
            Map<String, Object> extensions = new LinkedHashMap<>(error.getAttributes());
            if (!extensions.isEmpty()) {
                graphqlError.extensions(extensions);
            }
        }
        return graphqlError.build();
    }
}
