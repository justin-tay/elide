/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.swagger.OpenApiDocument.Version;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * Test for OpenApiDocument.
 */
class OpenApiDocumentTest {

    @Test
    void jsonShouldConvertTo31() throws JsonMappingException, JsonProcessingException {
        OpenAPI openApi = new OpenAPI();
        String result = OpenApiDocument.of(openApi, Version.OPENAPI_3_1, OpenApiDocument.MediaType.APPLICATION_JSON);
        JsonNode node = Json31.mapper().readTree(result);
        assertEquals("3.1.0", node.get("openapi").asText());
    }

    @Test
    void jsonShouldNotConvertTo31() throws JsonMappingException, JsonProcessingException {
        OpenAPI openApi = new OpenAPI();
        String result = OpenApiDocument.of(openApi, Version.OPENAPI_3_0, OpenApiDocument.MediaType.APPLICATION_JSON);
        JsonNode node = Json.mapper().readTree(result);
        assertEquals("3.0.1", node.get("openapi").asText());
    }

    @Test
    void yamlShouldConvertTo31() throws JsonMappingException, JsonProcessingException {
        OpenAPI openApi = new OpenAPI();
        String result = OpenApiDocument.of(openApi, Version.OPENAPI_3_1, OpenApiDocument.MediaType.APPLICATION_YAML);
        JsonNode node = Yaml31.mapper().readTree(result);
        assertEquals("3.1.0", node.get("openapi").asText());
    }

    @Test
    void yamlShouldNotConvertTo31() throws JsonMappingException, JsonProcessingException {
        OpenAPI openApi = new OpenAPI();
        String result = OpenApiDocument.of(openApi, Version.OPENAPI_3_0, OpenApiDocument.MediaType.APPLICATION_YAML);
        JsonNode node = Yaml.mapper().readTree(result);
        assertEquals("3.0.1", node.get("openapi").asText());
    }
}
