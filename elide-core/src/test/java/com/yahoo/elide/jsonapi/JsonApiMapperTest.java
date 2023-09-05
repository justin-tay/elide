/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Resource;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Test for JsonApiMapper.
 */
class JsonApiMapperTest {

    @Test
    void writeJsonApiDocumentStream() throws IOException {
        JsonApiMapper mapper = new JsonApiMapper();
        Resource resource = new Resource("books", "1");
        JsonApiDocument document = new JsonApiDocument(new Data<Resource>(resource));
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            mapper.writeJsonApiDocument(document, outputStream);
            String actual = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            String expected = """
                    {"data":{"type":"books","id":"1"}}""";
            assertEquals(expected, actual);
        }
    }

    @Test
    void writeJsonApiDocument() throws IOException {
        JsonApiMapper mapper = new JsonApiMapper();
        Resource resource = new Resource("books", "1");
        JsonApiDocument document = new JsonApiDocument(new Data<Resource>(resource));
        String actual = mapper.writeJsonApiDocument(document);
        String expected = """
                {"data":{"type":"books","id":"1"}}""";
        assertEquals(expected, actual);

        JsonApiDocument read = mapper.readJsonApiDocument(expected);
        assertEquals(document, read);
    }
}
