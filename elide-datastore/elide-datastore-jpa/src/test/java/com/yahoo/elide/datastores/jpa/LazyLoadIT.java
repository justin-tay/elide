/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jpa;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.reset;

import com.yahoo.elide.core.datastore.DataStoreTransaction;

import example.LazyGroup;
import example.LazyProduct;
import example.LazyVersion;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;


/**
 * Verifies Lazy Loading.
 */
public class LazyLoadIT extends JPQLIntegrationTest {

    @Override
    protected boolean delegateToInMemoryStore() {
        return true;
    }

    @BeforeEach
    public void setUp() throws IOException {
        reset(logger);

        try (DataStoreTransaction tx = dataStore.beginTransaction()) {
            LazyGroup group1 = new LazyGroup();
            group1.setDescription("com.yahoo.elide");
            tx.createObject(group1, null);

            LazyProduct product1 = new LazyProduct();
            product1.setDescription("elide-core");
            tx.createObject(product1, null);

            LazyProduct product2 = new LazyProduct();
            product2.setDescription("elide-graphql");
            tx.createObject(product2, null);

            LazyVersion version1 = new LazyVersion();
            version1.setDescription("1.0.0");
            tx.createObject(version1, null);

            LazyVersion version2 = new LazyVersion();
            version2.setDescription("2.0.0");
            tx.createObject(version2, null);


            product1.setGroup(group1);
            product2.setGroup(group1);
            version1.setProduct(product1);
            version2.setProduct(product2);
            tx.commit(null);
        }
    }

    @Test
    public void testLoadRootCollection() {
        given()
                .when().get("/lazyVersion?include=product")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("included[0].relationships.group.data.id", equalTo("1"));

        verifyLoggingStatements(
                "SELECT example_LazyVersion FROM example.LazyVersion AS example_LazyVersion LEFT JOIN FETCH example_LazyVersion.product"
        );
    }
}
