/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.controllers;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.core.exceptions.InvalidApiVersionException;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.graphql.GraphQLBodyMapper;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.graphql.QueryRunners;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.spring.config.ElideConfigProperties;
import com.yahoo.elide.spring.http.ResponseEntityConverter;
import com.yahoo.elide.spring.security.AuthenticationUser;
import com.yahoo.elide.utils.HeaderUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Spring rest controller for Elide GraphQL.
 */
@Slf4j
@RestController
@RequestMapping(value = "${elide.graphql.path}")
public class GraphqlController {

    private final Elide elide;
    private final ElideConfigProperties settings;
    private final QueryRunners runners;
    private final ObjectMapper mapper;
    private final HeaderUtils.HeaderProcessor headerProcessor;
    private final ResponseEntityConverter responseEntityConverter;

    private static final String JSON_CONTENT_TYPE = "application/json";

    public GraphqlController(
            Elide elide,
            QueryRunners runners,
            JsonApiMapper jsonApiMapper,
            HeaderUtils.HeaderProcessor headerProcessor,
            ElideConfigProperties settings) {
        log.debug("Started ~~");
        this.elide = elide;
        this.runners = runners;
        this.settings = settings;
        this.headerProcessor = headerProcessor;
        this.mapper = elide.getMapper().getObjectMapper();
        this.responseEntityConverter = new ResponseEntityConverter(new GraphQLBodyMapper(this.mapper));
    }

    /**
     * Single entry point for GraphQL requests.
     *
     * @param requestHeaders request headers
     * @param graphQLDocument post data as json document
     * @param principal The user principal
     * @return response
     */
    @PostMapping(value = {"/**", ""}, consumes = JSON_CONTENT_TYPE, produces = JSON_CONTENT_TYPE)
    public Callable<ResponseEntity<?>> post(@RequestHeader HttpHeaders requestHeaders,
                                                 @RequestBody String graphQLDocument, Authentication principal) {
        final User user = new AuthenticationUser(principal);
        final String apiVersion = HeaderUtils.resolveApiVersion(requestHeaders);
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final QueryRunner runner = runners.getRunner(apiVersion);
        final String baseUrl = getBaseUrlEndpoint();

        return new Callable<ResponseEntity<?>>() {
            @Override
            public ResponseEntity<?> call() throws Exception {
                ElideResponse<?> response;

                if (runner == null) {
                    response = QueryRunner.handleRuntimeException(elide,
                            new InvalidApiVersionException("Invalid API Version"), false);
                } else {
                    response = runner.run(baseUrl, graphQLDocument, user, UUID.randomUUID(), requestHeadersCleaned);
                }
                return responseEntityConverter.convert(response);
            }
        };
    }

    protected String getBaseUrlEndpoint() {
        String baseUrl = settings.getBaseUrl();

        if (StringUtils.isEmpty(baseUrl)) {
            baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        }
        return baseUrl;
    }
}
