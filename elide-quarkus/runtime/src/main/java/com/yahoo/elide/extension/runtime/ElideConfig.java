/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.extension.runtime;

import static com.yahoo.elide.extension.runtime.ElideResourceBuilder.GRAPHQL_BASE;
import static com.yahoo.elide.extension.runtime.ElideResourceBuilder.JSONAPI_BASE;
import static com.yahoo.elide.extension.runtime.ElideResourceBuilder.SWAGGER_BASE;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "elide")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ElideConfig {

    /**
     * Default page size if client doesn't request any.
     */
    @WithDefault("100")
    int defaultPageSize();

    /**
     * Maximum page size that can be requested by a client.
     */
    @WithDefault("10000")
    int defaultMaxPageSize();

    /**
     * Turns on verbose errors in HTTP responses.
     */
    @WithDefault("true")
    boolean verboseErrors();

    /**
     * The base URL path prefix for Elide JSON-API service endpoints.
     * This is appended to the basePath.
     */
    @WithDefault(JSONAPI_BASE)
    String baseJsonapi();

    /**
     * The base URL path prefix for Elide GraphQL service endpoints.
     * This is appended to the basePath.
     */
    @WithDefault(GRAPHQL_BASE)
    String baseGraphql();

    /**
     * The base URL path prefix for the Elide Swagger document.
     * This is appended to the basePath.
     */
    @WithDefault(SWAGGER_BASE)
    String baseSwagger();
}
