/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.yahoo.elide.ElideErrorResponse;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Exception describing error caused from JSON API Atomic Extension request.
 */
public class JsonApiAtomicOperationsException extends HttpStatusException {
    private static final long serialVersionUID = 1L;
    private final ElideErrorResponse response;

    public JsonApiAtomicOperationsException(int status, final JsonNode errorNode) {
        super(status, "");
        response = ElideErrorResponse.builder().responseCode(status).body(errorNode).build();
    }

    @Override
    public ElideErrorResponse getErrorResponse() {
        return response;
    }

    @Override
    public ElideErrorResponse getVerboseErrorResponse() {
        return getErrorResponse();
    }
}
