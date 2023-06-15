/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.http;

import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideResponseBodyMapper;
import com.yahoo.elide.ElideStreamingBody;
import com.yahoo.elide.core.exceptions.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Converts {@link ElideResponse} to {@link ResponseEntity}.
 *
 * @see com.yahoo.elide.spring.http.converter.StreamingResponseBodyHttpMessageConverter
 */
@Slf4j
public class ResponseEntityConverter {
    private final ElideResponseBodyMapper<Object, ?> responseBodyMapper;

    public ResponseEntityConverter(ElideResponseBodyMapper<Object, ?> responseBodyMapper) {
        this.responseBodyMapper = responseBodyMapper;
    }

    public ResponseEntity<?> convert(ElideResponse<?> elideResponse) {
        Object body = elideResponse.getBody();
        if (body == null) {
            return ResponseEntity.status(elideResponse.getStatus()).build();
        }
        if (body instanceof String) {
            return ResponseEntity.status(elideResponse.getStatus()).body(body);
        }
        // Convert first
        try {
            Object mapped = this.responseBodyMapper.map(body);
            if (mapped instanceof ElideStreamingBody streamingBody) {
                // This requires a custom HttpMessageConverter to process
                mapped = new StreamingResponseBody() {
                    @Override
                    public void writeTo(OutputStream outputStream) throws IOException {
                        streamingBody.writeTo(outputStream);
                    }
                };
            }
            return ResponseEntity.status(elideResponse.getStatus()).body(mapped);
        } catch (Exception e) {
            log.error("Caught {}", e.getClass().getSimpleName(), e);
            return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
        }
    }
}
