/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import com.yahoo.elide.ElideError;
import com.yahoo.elide.jsonapi.models.JsonApiError;
import com.yahoo.elide.jsonapi.models.JsonApiError.JsonApiErrorBuilder;
import com.yahoo.elide.jsonapi.models.JsonApiError.Links;
import com.yahoo.elide.jsonapi.models.JsonApiError.Source;

import org.owasp.encoder.Encode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Default {@link JsonApiErrorMapper}.
 */
public class DefaultJsonApiErrorMapper implements JsonApiErrorMapper {

    @Override
    public JsonApiError toJsonApiError(ElideError error) {
        JsonApiErrorBuilder jsonApiError = JsonApiError.builder();
        if (error.getMessage() != null) {
            jsonApiError.detail(Encode.forHtml(error.getMessage()));
        }
        if (error.getAttributes() != null && !error.getAttributes().isEmpty()) {
            Map<String, Object> meta = new LinkedHashMap<>(error.getAttributes());
            attribute("id", meta, value -> {
                jsonApiError.id(value.toString());
                return true;
            });
            attribute("status", meta, value -> {
                jsonApiError.status(value.toString());
                return true;
            });
            attribute("code", meta, value -> {
                jsonApiError.code(value.toString());
                return true;
            });
            attribute("title", meta, value -> {
                jsonApiError.title(value.toString());
                return true;
            });
            attribute("source", meta, value -> {
                if (value instanceof Source source) {
                    jsonApiError.source(source);
                    return true;
                }
                return false;
            });
            attribute("links", meta, value -> {
                if (value instanceof Links links) {
                    jsonApiError.links(links);
                    return true;
                }
                return false;
            });
            if (!meta.isEmpty()) {
                jsonApiError.meta(meta);
            }
        }
        return jsonApiError.build();
    }

    protected void attribute(String key, Map<String, Object> map, Predicate<Object> processor) {
        if (map.containsKey(key) && processor.test(map.get(key))) {
            map.remove(key);
        }
    }
}
