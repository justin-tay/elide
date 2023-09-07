/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import com.yahoo.elide.ElideResponseBodyMapper;
import com.yahoo.elide.ElideStreamingBody;

import java.io.IOException;
import java.io.OutputStream;

/**
 * JsonApi body mapper.
 */
public class JsonApiBodyMapper implements ElideResponseBodyMapper<Object, Object> {
    private final JsonApiMapper jsonApiMapper;

    public JsonApiBodyMapper(JsonApiMapper jsonApiMapper) {
        this.jsonApiMapper = jsonApiMapper;
    }

    @Override
    public Object map(Object body) {
        if (body == null) {
            return null;
        }
        if (body instanceof String value) {
            return value;
        }
        return new ElideStreamingBody() {
            @Override
            public void writeTo(OutputStream outputStream) throws IOException {
                jsonApiMapper.writeJsonApiDocument(body, outputStream);
            }
        };
    }
}
