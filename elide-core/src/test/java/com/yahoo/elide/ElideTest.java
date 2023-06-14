/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.TestDictionary;
import com.yahoo.elide.core.exceptions.ExceptionMappers;
import com.yahoo.elide.core.lifecycle.FieldTestModel;
import com.yahoo.elide.core.lifecycle.LegacyTestModel;
import com.yahoo.elide.core.lifecycle.PropertyTestModel;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.jsonapi.models.JsonApiError;
import com.yahoo.elide.jsonapi.models.JsonApiErrors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * Tests Elide.
 */
class ElideTest {

    private final String baseUrl = "http://localhost:8080/api/v1";
    private EntityDictionary dictionary;
    private ObjectMapper objectMapper = new ObjectMapper();

    ElideTest() throws Exception {
        dictionary = TestDictionary.getTestDictionary();
        dictionary.bindEntity(FieldTestModel.class);
        dictionary.bindEntity(PropertyTestModel.class);
        dictionary.bindEntity(LegacyTestModel.class);
    }


    @Test
    void constraintViolationException() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, null);

        String body = """
                {"data": {"type":"testModel","id":"1","attributes": {"field":"Foo"}}}""";

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        TestObject testObject = new TestObject();
        Set<ConstraintViolation<TestObject>> violations = validator.validate(testObject);
        ConstraintViolationException e = new ConstraintViolationException("message", violations);

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);
        doThrow(e).when(tx).preCommit(any());

        ElideResponse<?> response = elide.post(baseUrl, "/testModel", body, null, NO_VERSION);
        String responseBody = getBody(response);
        JsonApiErrors errorObjects = objectMapper.readValue(responseBody, JsonApiErrors.class);
        assertEquals(3, errorObjects.getErrors().size());
        for (JsonApiError errorObject : errorObjects.getErrors()) {
            Map<String, Object> meta = errorObject.getMeta();
            String expected;
            String actual = objectMapper.writeValueAsString(errorObject);
            switch (meta.get("property").toString()) {
            case "nestedTestObject.nestedNotNullField":
                expected = """
                        {"code":"NotNull","source":{"pointer":"/data/attributes/nestedTestObject/nestedNotNullField"},"detail":"must not be null","meta":{"type":"ConstraintViolation","property":"nestedTestObject.nestedNotNullField"}}""";
                assertEquals(expected, actual);
                break;
            case "notNullField":
                expected = """
                        {"code":"NotNull","source":{"pointer":"/data/attributes/notNullField"},"detail":"must not be null","meta":{"type":"ConstraintViolation","property":"notNullField"}}""";
                assertEquals(expected, actual);
                break;
            case "minField":
                expected = """
                        {"code":"Min","source":{"pointer":"/data/attributes/minField"},"detail":"must be greater than or equal to 5","meta":{"type":"ConstraintViolation","property":"minField"}}""";
                assertEquals(expected, actual);
                break;
            }
        }

        verify(tx).close();
    }

    private Elide getElide(DataStore dataStore, EntityDictionary dictionary, ExceptionMappers exceptionMappers) {
        ElideSettings settings = getElideSettings(dataStore, dictionary, exceptionMappers);
        return new Elide(settings, new TransactionRegistry(), settings.getDictionary().getScanner(), false);
    }

    private ElideSettings getElideSettings(DataStore dataStore, EntityDictionary dictionary, ExceptionMappers exceptionMappers) {
        return new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withExceptionMappers(exceptionMappers)
                .withVerboseErrors()
                .build();
    }

    public static class TestObject {
        public static class NestedTestObject {
            @NotNull
            private String nestedNotNullField;
        }

        @NotNull
        private String notNullField;

        @Min(5)
        private int minField = 1;

        @Valid
        private NestedTestObject nestedTestObject = new NestedTestObject();
    }

    protected String getBody(ElideResponse<?> response) throws JsonProcessingException {
        if (response.getBody() == null) {
            return null;
        }
        if (response.getBody() instanceof String value) {
            return value;
        }
        if (response.getBody() instanceof ElideStreamingBody streamingBody) {
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                streamingBody.writeTo(output);
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return this.objectMapper.writeValueAsString(response.getBody());
    }
}
