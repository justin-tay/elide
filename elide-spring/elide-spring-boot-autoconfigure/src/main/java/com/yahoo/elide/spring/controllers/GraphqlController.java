/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.controllers;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.core.exceptions.InvalidApiVersionException;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.core.request.route.RouteResolver;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.graphql.GraphQLBodyMapper;
import com.yahoo.elide.graphql.QueryRunner;
import com.yahoo.elide.graphql.QueryRunners;
import com.yahoo.elide.spring.config.ElideConfigProperties;
import com.yahoo.elide.spring.http.ResponseEntityConverter;
import com.yahoo.elide.spring.security.AuthenticationUser;
import com.yahoo.elide.utils.HeaderProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

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
    private final HeaderProcessor headerProcessor;
    private final RouteResolver routeResolver;
    private final ResponseEntityConverter responseEntityConverter;

    private static final String JSON_CONTENT_TYPE = "application/json";

    public GraphqlController(
            Elide elide,
            QueryRunners runners,
            ObjectMapper objectMapper,
            HeaderProcessor headerProcessor,
            ElideConfigProperties settings,
            RouteResolver routeResolver) {
        log.debug("Started ~~");
        this.elide = elide;
        this.runners = runners;
        this.settings = settings;
        this.headerProcessor = headerProcessor;
        this.mapper = objectMapper;
        this.routeResolver = routeResolver;
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
                                                 @RequestParam MultiValueMap<String, String> allRequestParams,
                                                 @RequestBody String graphQLDocument, HttpServletRequest request,
                                                 Authentication principal) {
        final User user = new AuthenticationUser(principal);
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final String prefix = settings.getGraphql().getPath();
        final String baseUrl = getBaseUrl(prefix);
        final String pathname = getPath(request, prefix);
        Route route = routeResolver.resolve(JSON_CONTENT_TYPE, baseUrl, pathname, requestHeaders, allRequestParams);

        final QueryRunner runner = runners.getRunner(route.getApiVersion());

        return new Callable<ResponseEntity<?>>() {
            @Override
            public ResponseEntity<?> call() throws Exception {
                ElideResponse<?> response;

                if (runner == null) {
                    response = QueryRunner.handleRuntimeException(elide,
                            new InvalidApiVersionException("Invalid API Version"), false);
                } else {
                    response = runner.run(route.getBaseUrl(), graphQLDocument, user, UUID.randomUUID(),
                            requestHeadersCleaned);
                }
                return responseEntityConverter.convert(response);
            }
        };
    }

    private String getPath(HttpServletRequest request, String prefix) {
        String pathname = (String) request
                .getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        return pathname.replaceFirst(prefix, "");
    }

    protected String getBaseUrl(String prefix) {
        String baseUrl = this.settings.getBaseUrl();

        if (StringUtils.isEmpty(baseUrl)) {
            baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        }

        if (prefix.length() > 1) {
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1) + prefix;
            } else {
                baseUrl = baseUrl + prefix;
            }
        }

        return baseUrl;
    }
}
