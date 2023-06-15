/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideStreamingBody;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Test for ResponseConverter.
 */
class ResponseConverterTest {

    @Test
    void convertNull() {
        ResponseConverter converter = new ResponseConverter(body -> body);
        Response response = converter.convert(ElideResponse.status(403).build());
        assertNull(response.getEntity());
        assertEquals(403, response.getStatus());
    }

    @Test
    void convertString() {
        ResponseConverter converter = new ResponseConverter(body -> body);
        Response response = converter.convert(ElideResponse.ok("test"));
        assertEquals("test", response.getEntity());
        assertEquals(200, response.getStatus());
    }

    @Test
    void convertObject() {
        ResponseConverter converter = new ResponseConverter(body -> body.toString());
        Response response = converter.convert(ElideResponse.ok(1));
        assertEquals("1", response.getEntity());
        assertEquals(200, response.getStatus());
    }

    @Test
    void convertStreaming() throws IOException {
        ResponseConverter converter = new ResponseConverter(body -> body);
        Response response = converter.convert(ElideResponse.ok(new ElideStreamingBody() {
            @Override
            public void writeTo(OutputStream outputStream) throws IOException {
                outputStream.write("test".getBytes());
            }
        }));
        assertInstanceOf(StreamingOutput.class, response.getEntity());
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (response.getEntity() instanceof StreamingOutput streamingOutput) {
                streamingOutput.write(outputStream);
            }
            String result = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            assertEquals("test", result);
        }
    }

    @Test
    void convertFailure() {
        ResponseConverter converter = new ResponseConverter(body -> {
            throw new IllegalArgumentException("failed");
        });
        Response response = converter.convert(ElideResponse.ok(1));
        assertEquals(500, response.getStatus());
        assertNull(response.getEntity());
    }
}
