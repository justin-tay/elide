/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import com.yahoo.elide.core.exceptions.HttpStatus;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Converts {@link ElideResponse} to {@link Response}.
 */
public class ResponseConverter {
    private final ElideResponseBodyMapper responseBodyMapper;

    public ResponseConverter(ElideResponseBodyMapper responseBodyMapper) {
        this.responseBodyMapper = responseBodyMapper;
    }

    public Response convert(ElideResponse<?> elideResponse) {
        Object body = elideResponse.getBody();
        if (body == null) {
            return Response.status(elideResponse.getStatus()).build();
        }
        if (body instanceof String) {
            return Response.status(elideResponse.getStatus()).entity(body).build();
        }
        // Convert first
        try {
            Object mapped = this.responseBodyMapper.map(body);
            if (mapped instanceof ElideStreamingBody streamingBody) {
                mapped = new StreamingOutput() {
                    @Override
                    public void write(OutputStream output) throws IOException, WebApplicationException {
                        streamingBody.writeTo(output);
                    }
                };
            }
            return Response.status(elideResponse.getStatus()).entity(mapped).build();
        } catch (Exception e) {
            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.toString()).build();
        }
    }
}
