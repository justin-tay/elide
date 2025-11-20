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

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "elide")
public interface ElideConfig {
    interface JsonApiConfig {
        /**
         * The base URL path prefix for Elide JSON-API service endpoints.
         * This is appended to the basePath.
         */
        @WithDefault(JSONAPI_BASE)
        String path();
    }

    interface GraphQlConfig {
        /**
         * The base URL path prefix for Elide GraphQL service endpoints.
         * This is appended to the basePath.
         */
        @WithDefault(GRAPHQL_BASE)
        String path();
    }

    interface ApiDocsConfig {
        /**
         * The base URL path prefix for the Elide Swagger document.
         * This is appended to the basePath.
         */
        @WithDefault(SWAGGER_BASE)
        String path();
    }

    /**
     * JSON-API configuration.
     *
     * @return the configuration
     */
    JsonApiConfig jsonApi();

    /**
     * GraphQL configuration.
     *
     * @return the configuration
     */
    GraphQlConfig graphql();

    /**
     * API Docs configuration.
     *
     * @return the configuration
     */
    ApiDocsConfig apiDocs();

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
}
