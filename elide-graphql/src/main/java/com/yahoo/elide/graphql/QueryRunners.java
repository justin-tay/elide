/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.Elide;
import com.yahoo.elide.RefreshableElide;

import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.SimpleDataFetcherExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Maps API version to a GraphQL query runner.  This class is hot reloadable and must be restricted to a single
 * access method.
 */
public class QueryRunners {
    private final Map<String, QueryRunner> runners;

    /**
     * Constructor.
     * @param runners the runners.
     */
    public QueryRunners(Map<String, QueryRunner> runners) {
        this.runners = runners;
    }

    /**
     * Constructor.
     *
     * @param elide the elide
     * @param optionalDataFetcherExceptionHandler the optional data fetched exception handler
     */
    public QueryRunners(Elide elide, Optional<DataFetcherExceptionHandler> optionalDataFetcherExceptionHandler) {
        this.runners = new HashMap<>();
        for (String apiVersion : elide.getElideSettings().getEntityDictionary().getApiVersions()) {
            this.runners.put(apiVersion, new QueryRunner(elide, apiVersion,
                    optionalDataFetcherExceptionHandler.orElseGet(SimpleDataFetcherExceptionHandler::new)));
        }
    }

    /**
     * Constructor.
     *
     * @param refreshableElide the elide
     * @param exceptionHandler the data fetcher exception handler
     */
    public QueryRunners(RefreshableElide refreshableElide, DataFetcherExceptionHandler exceptionHandler) {
        this(refreshableElide.getElide(), Optional.of(exceptionHandler));
    }

    /**
     * Gets a runner for a given API version.  This is the ONLY access method for this class to
     * eliminate state issues across reloads.
     * @param apiVersion The api version.
     * @return The associated query runner.
     */
    public QueryRunner getRunner(String apiVersion) {
        return runners.get(apiVersion);
    }
}
