/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideStreamingBody;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Test for ResponseEntityConverter.
 */
class ResponseEntityConverterTest {

    @Test
    void convertNull() {
        ResponseEntityConverter converter = new ResponseEntityConverter(body -> body);
        ResponseEntity<?> response = converter.convert(ElideResponse.status(403).build());
        assertNull(response.getBody());
        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void convertString() {
        ResponseEntityConverter converter = new ResponseEntityConverter(body -> body);
        ResponseEntity<?> response = converter.convert(ElideResponse.ok("test"));
        assertEquals("test", response.getBody());
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void convertObject() {
        ResponseEntityConverter converter = new ResponseEntityConverter(body -> body.toString());
        ResponseEntity<?> response = converter.convert(ElideResponse.ok(1));
        assertEquals("1", response.getBody());
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void convertStreaming() throws IOException {
        ResponseEntityConverter converter = new ResponseEntityConverter(body -> body);
        ResponseEntity<?> response = converter.convert(ElideResponse.ok(new ElideStreamingBody() {
            @Override
            public void writeTo(OutputStream outputStream) throws IOException {
                outputStream.write("test".getBytes());
            }
        }));
        assertInstanceOf(StreamingResponseBody.class, response.getBody());
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (response.getBody() instanceof StreamingResponseBody streamingResponseBody) {
                streamingResponseBody.writeTo(outputStream);
            }
            String result = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            assertEquals("test", result);
        }
    }

    @Test
    void convertFailure() {
        ResponseEntityConverter converter = new ResponseEntityConverter(body -> {
            throw new IllegalArgumentException("failed");
        });
        ResponseEntity<?> response = converter.convert(ElideResponse.ok(1));
        assertEquals(500, response.getStatusCode().value());
        assertNull(response.getBody());
    }
}
