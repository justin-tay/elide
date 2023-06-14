/*
 * Copyright 2019, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.controllers;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.RefreshableElide;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.jsonapi.JsonApi;
import com.yahoo.elide.jsonapi.JsonApiBodyMapper;
import com.yahoo.elide.spring.config.ElideConfigProperties;
import com.yahoo.elide.spring.http.ResponseEntityConverter;
import com.yahoo.elide.spring.security.AuthenticationUser;
import com.yahoo.elide.utils.HeaderUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MultivaluedHashMap;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Spring rest controller for Elide JSON-API.
 * Based on 'https://github.com/illyasviel/elide-spring-boot/'
 */
@Slf4j
@RestController
@RequestMapping(value = "${elide.json-api.path}")
public class JsonApiController {

    private final Elide elide;
    private final ElideConfigProperties settings;
    private final HeaderUtils.HeaderProcessor headerProcessor;
    private final ResponseEntityConverter responseEntityConverter;

    public static final String JSON_API_CONTENT_TYPE = JsonApi.MEDIA_TYPE;
    public static final String JSON_API_PATCH_CONTENT_TYPE = JsonApi.JsonPatch.MEDIA_TYPE;
    public static final String JSON_API_ATOMIC_OPERATIONS_CONTENT_TYPE = JsonApi.AtomicOperations.MEDIA_TYPE;

    public JsonApiController(RefreshableElide refreshableElide, ElideConfigProperties settings) {
        log.debug("Started ~~");
        this.settings = settings;
        this.elide = refreshableElide.getElide();
        this.headerProcessor = elide.getElideSettings().getHeaderProcessor();
        this.responseEntityConverter = new ResponseEntityConverter(new JsonApiBodyMapper(this.elide.getMapper()));
    }

    private <K, V> MultivaluedHashMap<K, V> convert(MultiValueMap<K, V> springMVMap) {
        MultivaluedHashMap<K, V> convertedMap = new MultivaluedHashMap<>(springMVMap.size());
        springMVMap.forEach(convertedMap::put);
        return convertedMap;
    }

    @GetMapping(value = "/**", produces = JsonApi.MEDIA_TYPE)
    public Callable<ResponseEntity<?>> elideGet(@RequestHeader HttpHeaders requestHeaders,
                                                     @RequestParam MultiValueMap<String, String> allRequestParams,
                                                     HttpServletRequest request, Authentication authentication) {
        final String apiVersion = HeaderUtils.resolveApiVersion(requestHeaders);
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final String pathname = getJsonApiPath(request, settings.getJsonApi().getPath());
        final User user = new AuthenticationUser(authentication);
        final String baseUrl = getBaseUrlEndpoint();

        return new Callable<ResponseEntity<?>>() {
            @Override
            public ResponseEntity<?> call() throws Exception {
                ElideResponse<?> response = elide.get(baseUrl, pathname,
                        convert(allRequestParams), requestHeadersCleaned,
                        user, apiVersion, UUID.randomUUID());
                return responseEntityConverter.convert(response);
            }
        };
    }

    @PostMapping(value = "/**", consumes = JsonApi.MEDIA_TYPE, produces = JsonApi.MEDIA_TYPE)
    public Callable<ResponseEntity<?>> elidePost(@RequestHeader HttpHeaders requestHeaders,
                                                      @RequestParam MultiValueMap<String, String> allRequestParams,
                                                      @RequestBody String body,
                                                      HttpServletRequest request, Authentication authentication) {
        final String apiVersion = HeaderUtils.resolveApiVersion(requestHeaders);
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final String pathname = getJsonApiPath(request, settings.getJsonApi().getPath());
        final User user = new AuthenticationUser(authentication);
        final String baseUrl = getBaseUrlEndpoint();

        return new Callable<ResponseEntity<?>>() {
            @Override
            public ResponseEntity<?> call() throws Exception {
                ElideResponse<?> response = elide.post(baseUrl, pathname, body, convert(allRequestParams),
                        requestHeadersCleaned, user, apiVersion, UUID.randomUUID());
                return responseEntityConverter.convert(response);
            }
        };
    }

    @PatchMapping(
            value = "/**",
            consumes = { JsonApi.MEDIA_TYPE, JsonApi.JsonPatch.MEDIA_TYPE },
            produces = JsonApi.MEDIA_TYPE
    )
    public Callable<ResponseEntity<?>> elidePatch(@RequestHeader HttpHeaders requestHeaders,
                                                       @RequestParam MultiValueMap<String, String> allRequestParams,
                                                       @RequestBody String body,
                                                       HttpServletRequest request, Authentication authentication) {
        final String apiVersion = HeaderUtils.resolveApiVersion(requestHeaders);
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final String pathname = getJsonApiPath(request, settings.getJsonApi().getPath());
        final User user = new AuthenticationUser(authentication);
        final String baseUrl = getBaseUrlEndpoint();

        return new Callable<ResponseEntity<?>>() {
            @Override
            public ResponseEntity<?> call() throws Exception {
                ElideResponse<?> response = elide
                        .patch(baseUrl, request.getContentType(), request.getContentType(), pathname, body,
                               convert(allRequestParams), requestHeadersCleaned, user, apiVersion,
                               UUID.randomUUID());
                return responseEntityConverter.convert(response);
            }
        };
    }

    @DeleteMapping(value = "/**", produces = JsonApi.MEDIA_TYPE)
    public Callable<ResponseEntity<?>> elideDelete(@RequestHeader HttpHeaders requestHeaders,
                                                        @RequestParam MultiValueMap<String, String> allRequestParams,
                                                        HttpServletRequest request,
                                                        Authentication authentication) {
        final String apiVersion = HeaderUtils.resolveApiVersion(requestHeaders);
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final String pathname = getJsonApiPath(request, settings.getJsonApi().getPath());
        final User user = new AuthenticationUser(authentication);
        final String baseUrl = getBaseUrlEndpoint();

        return new Callable<ResponseEntity<?>>() {
            @Override
            public ResponseEntity<?> call() throws Exception {
                ElideResponse<?> response = elide.delete(baseUrl, pathname, null,
                        convert(allRequestParams), requestHeadersCleaned,
                        user, apiVersion, UUID.randomUUID());
                return responseEntityConverter.convert(response);
            }
        };
    }

    @DeleteMapping(value = "/**", consumes = JsonApi.MEDIA_TYPE)
    public Callable<ResponseEntity<?>> elideDeleteRelation(
            @RequestHeader HttpHeaders requestHeaders,
            @RequestParam MultiValueMap<String, String> allRequestParams,
            @RequestBody String body,
            HttpServletRequest request,
            Authentication authentication) {
        final String apiVersion = HeaderUtils.resolveApiVersion(requestHeaders);
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final String pathname = getJsonApiPath(request, settings.getJsonApi().getPath());
        final User user = new AuthenticationUser(authentication);
        final String baseUrl = getBaseUrlEndpoint();

        return new Callable<ResponseEntity<?>>() {
            @Override
            public ResponseEntity<?> call() throws Exception {
                ElideResponse<?> response = elide
                        .delete(baseUrl, pathname, body, convert(allRequestParams),
                                requestHeadersCleaned, user, apiVersion, UUID.randomUUID());
                return responseEntityConverter.convert(response);
            }
        };
    }

    @PostMapping(
            value = "/operations",
            consumes = JsonApi.AtomicOperations.MEDIA_TYPE,
            produces = JsonApi.AtomicOperations.MEDIA_TYPE
    )
    public Callable<ResponseEntity<?>> elideOperations(@RequestHeader HttpHeaders requestHeaders,
                                                       @RequestParam MultiValueMap<String, String> allRequestParams,
                                                       @RequestBody String body,
                                                       HttpServletRequest request, Authentication authentication) {
        final String apiVersion = HeaderUtils.resolveApiVersion(requestHeaders);
        final Map<String, List<String>> requestHeadersCleaned = headerProcessor.process(requestHeaders);
        final String pathname = getJsonApiPath(request, settings.getJsonApi().getPath());
        final User user = new AuthenticationUser(authentication);
        final String baseUrl = getBaseUrlEndpoint();

        return new Callable<ResponseEntity<?>>() {
            @Override
            public ResponseEntity<?> call() throws Exception {
                ElideResponse<?> response = elide
                        .operations(baseUrl, request.getContentType(), request.getContentType(), pathname, body,
                               convert(allRequestParams), requestHeadersCleaned, user, apiVersion,
                               UUID.randomUUID());
                return responseEntityConverter.convert(response);
            }
        };
    }

    private String getJsonApiPath(HttpServletRequest request, String prefix) {
        String pathname = (String) request
                .getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        return pathname.replaceFirst(prefix, "");
    }

    protected String getBaseUrlEndpoint() {
        String baseUrl = elide.getElideSettings().getBaseUrl();

        if (StringUtils.isEmpty(baseUrl)) {
            baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        }

        return baseUrl;
    }
}
