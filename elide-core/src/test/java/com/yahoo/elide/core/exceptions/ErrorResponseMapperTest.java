/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideErrors;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.TestDictionary;
import com.yahoo.elide.core.lifecycle.FieldTestModel;
import com.yahoo.elide.core.lifecycle.LegacyTestModel;
import com.yahoo.elide.core.lifecycle.PropertyTestModel;
import com.yahoo.elide.core.type.ClassType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Tests the error mapping logic.
 */
class ErrorResponseMapperTest {

    private final String baseUrl = "http://localhost:8080/api/v1";
    private static final ErrorResponseMapper MOCK_ERROR_MAPPER = mock(ErrorResponseMapper.class);
    private static final Exception EXPECTED_EXCEPTION = new IllegalStateException("EXPECTED_EXCEPTION");
    private static final ErrorResponseException MAPPED_EXCEPTION = new ErrorResponseException(
            422,
            "MAPPED_EXCEPTION",
            ElideErrors.builder()
                    .error(error -> error.attribute("code", "SOME_ERROR"))
                    .build()
    );
    private EntityDictionary dictionary;

    ErrorResponseMapperTest() throws Exception {
        dictionary = TestDictionary.getTestDictionary();
        dictionary.bindEntity(FieldTestModel.class);
        dictionary.bindEntity(PropertyTestModel.class);
        dictionary.bindEntity(LegacyTestModel.class);
    }

    @AfterEach
    public void afterEach() {
        reset(MOCK_ERROR_MAPPER);
    }

    @Test
    void testElideRuntimeExceptionNoErrorMapper() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, null);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);
        doThrow(EXPECTED_EXCEPTION).when(tx).preCommit(any());

        RuntimeException result = assertThrows(RuntimeException.class, () -> elide.post(baseUrl, "/testModel", body, null, NO_VERSION));
        assertEquals(EXPECTED_EXCEPTION, result);

        verify(tx).close();
    }

    @Test
    void testElideIOExceptionNoErrorMapper() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, null);

        //Invalid JSON
        String body = "{\"data\": {\"type\":\"testModel\"\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);

        ElideResponse<String> response = elide.post(baseUrl, "/testModel", body, null, NO_VERSION);
        assertEquals(400, response.getStatus());
        assertEquals(
                "{\"errors\":[{\"detail\":\"Unexpected character (&#39;&#34;&#39; (code 34)): was expecting comma to separate Object entries\\n at [Source: (String)&#34;{&#34;data&#34;: {&#34;type&#34;:&#34;testModel&#34;&#34;id&#34;:&#34;1&#34;,&#34;attributes&#34;: {&#34;field&#34;:&#34;Foo&#34;}}}&#34;; line: 1, column: 30]\"}]}",
                response.getBody());

        verify(tx).close();
    }

    @Test
    void testElideRuntimeExceptionWithErrorMapperUnmapped() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_ERROR_MAPPER);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);
        doThrow(EXPECTED_EXCEPTION).when(tx).preCommit(any());

        RuntimeException result = assertThrows(RuntimeException.class, () -> elide.post(baseUrl, "/testModel", body, null, NO_VERSION));
        assertEquals(EXPECTED_EXCEPTION, result);

        verify(tx).close();
    }

    @Test
    void testElideIOExceptionWithErrorMapperUnmapped() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_ERROR_MAPPER);

        //Invalid JSON
        String body = "{\"data\": {\"type\":\"testModel\"\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);

        ElideResponse<String> response = elide.post(baseUrl, "/testModel", body, null, NO_VERSION);
        assertEquals(400, response.getStatus());
        assertEquals(
                "{\"errors\":[{\"detail\":\"Unexpected character (&#39;&#34;&#39; (code 34)): was expecting comma to separate Object entries\\n at [Source: (String)&#34;{&#34;data&#34;: {&#34;type&#34;:&#34;testModel&#34;&#34;id&#34;:&#34;1&#34;,&#34;attributes&#34;: {&#34;field&#34;:&#34;Foo&#34;}}}&#34;; line: 1, column: 30]\"}]}",
                response.getBody());

        verify(tx).close();
    }

    @Test
    void testElideRuntimeExceptionWithErrorMapperMapped() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_ERROR_MAPPER);

        String body = "{\"data\": {\"type\":\"testModel\",\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);
        doThrow(EXPECTED_EXCEPTION).when(tx).preCommit(any());
        when(MOCK_ERROR_MAPPER.map(eq(EXPECTED_EXCEPTION), any())).thenReturn(MAPPED_EXCEPTION.getErrorResponse());

        ElideResponse<String> response = elide.post(baseUrl, "/testModel", body, null, NO_VERSION);
        assertEquals(422, response.getStatus());
        assertEquals(
                "{\"errors\":[{\"code\":\"SOME_ERROR\"}]}",
                response.getBody());

        verify(tx).close();
    }

    @Test
    void testElideIOExceptionWithErrorMapperMapped() throws Exception {
        DataStore store = mock(DataStore.class);
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        FieldTestModel mockModel = mock(FieldTestModel.class);

        Elide elide = getElide(store, dictionary, MOCK_ERROR_MAPPER);

        //Invalid JSON:
        String body = "{\"data\": {\"type\":\"testModel\"\"id\":\"1\",\"attributes\": {\"field\":\"Foo\"}}}";

        when(store.beginTransaction()).thenReturn(tx);
        when(tx.createNewObject(eq(ClassType.of(FieldTestModel.class)), any())).thenReturn(mockModel);

        when(MOCK_ERROR_MAPPER.map(isA(IOException.class), any())).thenReturn(MAPPED_EXCEPTION.getErrorResponse());

        ElideResponse<String> response = elide.post(baseUrl, "/testModel", body, null, NO_VERSION);
        assertEquals(422, response.getStatus());
        assertEquals(
                "{\"errors\":[{\"code\":\"SOME_ERROR\"}]}",
                response.getBody());

        verify(tx).close();
    }

    private Elide getElide(DataStore dataStore, EntityDictionary dictionary, ErrorResponseMapper errorMapper) {
        ElideSettings settings = getElideSettings(dataStore, dictionary, errorMapper);
        return new Elide(settings, new TransactionRegistry(), settings.getDictionary().getScanner(), false);
    }

    private ElideSettings getElideSettings(DataStore dataStore, EntityDictionary dictionary, ErrorResponseMapper errorMapper) {
        return new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withErrorResponseMapper(errorMapper)
                .withVerboseErrors()
                .build();
    }
}
