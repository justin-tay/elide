/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.atomicOperation;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.atomicOperations;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.links;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.ref;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.test.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.yahoo.elide.jsonapi.JsonApi;
import com.yahoo.elide.standalone.ElideStandalone;
import com.yahoo.elide.standalone.config.ElideStandaloneSettings;
import com.yahoo.elide.test.jsonapi.elements.AtomicOperationCode;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import jakarta.ws.rs.core.MediaType;

/**
 * Tests ElideStandalone starts and works.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ElideStandaloneTest {
    protected ElideStandalone elide;
    protected ElideStandaloneSettings settings;

    @BeforeAll
    public void init() throws Exception {
        settings = new ElideStandaloneTestSettings();

        elide = new ElideStandalone(settings);
        elide.start(false);
    }

    @AfterAll
    public void shutdown() throws Exception {
        elide.stop();
    }

    @Test
    public void testJsonAPIPost() {
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(
                datum(
                    resource(
                        type("post"),
                        id("1"),
                        attributes(
                            attr("content", "This is my first post. woot."),
                            attr("date", "2019-01-01T00:00Z")
                        )
                    )
                )
            )
            .post("/api/post")
            .then()
            .statusCode(HttpStatus.SC_CREATED);

        // Test the Dynamic Generated Analytical Model is accessible
        given()
            .when()
            .get("/api/postView")
            .then()
            .statusCode(200)
            .body(equalTo(
                    data(
                            resource(
                                    type("postView"),
                                    id("0"),
                                    attributes(
                                            attr("content", "This is my first post. woot.")
                                    ),
                                    links(
                                            attr("self", "https://elide.io/api/postView/0")
                                    )
                            )
                    ).toJSON()
                )
            );

        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .delete("/api/v1/post/1")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void testJsonAPIAtomicOperations() {
        given()
            .contentType(JsonApi.AtomicOperations.MEDIA_TYPE)
            .accept(JsonApi.AtomicOperations.MEDIA_TYPE)
            .body(
                    atomicOperations(
                            atomicOperation(AtomicOperationCode.add, "/post",
                                    datum(resource(
                                            type("post"),
                                            id("10"),
                                            attributes(
                                                    attr("content", "This is my second post. woot.")
                                            )
                                    ))
                            )
                    )
            )
            .post("/api/v1/operations")
            .then()
            .statusCode(HttpStatus.SC_OK);

        given()
            .when()
            .get("/api/v1/post/10")
            .then()
            .statusCode(200)
            .body(equalTo(
                    datum(
                            resource(
                                    type("post"),
                                    id("10"),
                                    attributes(
                                            attr("abusiveContent", false),
                                            attr("content", "This is my second post. woot."),
                                            attr("date", null)
                                    ),
                                    links(
                                            attr("self", "https://elide.io/api/v1/post/10")
                                    )
                            )
                    ).toJSON()
                )
            );

        given()
            .contentType(JsonApi.AtomicOperations.MEDIA_TYPE)
            .accept(JsonApi.AtomicOperations.MEDIA_TYPE)
            .body(
                atomicOperations(
                        atomicOperation(AtomicOperationCode.remove,
                                ref(type("post"), id("10")))
                        )
                )
            .post("/api/v1/operations")
            .then()
            .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testVersionedJsonAPIPost() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .header("ApiVersion", "1.0")
                .body(
                        datum(
                                resource(
                                        type("post"),
                                        id("2"),
                                        attributes(
                                                attr("text", "This is my first post. woot."),
                                                attr("date", "2019-01-01T00:00Z")
                                        )
                                )
                        )
                )
                .post("/api/post")
                .then()
                .statusCode(HttpStatus.SC_CREATED);
    }

    @Test
    public void testForbiddenJsonAPIPost() {
        given()
            .contentType(JSONAPI_CONTENT_TYPE)
            .accept(JSONAPI_CONTENT_TYPE)
            .body(
                datum(
                    resource(
                        type("post"),
                        id("2"),
                        attributes(
                            attr("content", "This is my first post. woot."),
                            attr("date", "2019-01-01T00:00Z"),
                            attr("abusiveContent", true)
                        )
                    )
                )
            )
            .post("/api/post")
            .then()
            .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void testMetricsServlet() throws Exception {
        given()
                .when()
                .get("/stats/metrics")
                .then()
                .statusCode(200)
                .body("meters", hasKey("io.dropwizard.metrics.servlet.InstrumentedFilter.responseCodes.ok"));
    }

    @Test
    public void testHealthCheckServlet() throws Exception {
            given()
                .when()
                .get("/stats/healthcheck")
                .then()
                .statusCode(501); //Returns 'Not Implemented' if there are no Health Checks Registered
    }

    @Test
    public void testApiDocsEndpoint() throws Exception {
        given()
                .when()
                .get("/api-docs")
                .then()
                .statusCode(200);
    }

    @Test
    public void apiDocsDocumentTest() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/api-docs")
                .then()
                .statusCode(200)
                .body("tags.name", containsInAnyOrder("post", "argument", "metric",
                        "dimension", "column", "table", "asyncQuery",
                        "timeDimensionGrain", "timeDimension", "postView", "namespace", "tableSource"));
    }

    @Test
    public void apiDocsDocumentVersionTest() {
        ExtractableResponse<Response> v0 = given()
                .accept(ContentType.JSON)
                .when()
                .get("/api-docs")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract();

        ExtractableResponse<Response> v1 = given()
                .accept(ContentType.JSON)
                .header("ApiVersion", "1.0")
                .when()
                .get("/api-docs")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract();
        assertNotEquals(v0.asString(), v1.asString());
        assertEquals("", v0.path("info.version"));
        assertEquals("1.0", v1.path("info.version"));

        given()
                .accept(ContentType.JSON)
                .header("ApiVersion", "2.0")
                .when()
                .get("/api-docs/doc/test")
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testAsyncApiEndpoint() throws InterruptedException {
        //Create Async Request
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .body(
                        data(
                                resource(
                                        type("asyncQuery"),
                                        id("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"),
                                        attributes(
                                                attr("query", "/post"),
                                                attr("queryType", "JSONAPI_V1_0"),
                                                attr("status", "QUEUED")
                                        )
                                )
                        ).toJSON())
                .when()
                .post("/api/asyncQuery").asString();

        int i = 0;
        while (i < 1000) {
            Thread.sleep(10);
            Response response = given()
                    .accept("application/vnd.api+json")
                    .get("/api/asyncQuery/ba31ca4e-ed8f-4be0-a0f3-12088fa9263d");

            String outputResponse = response.jsonPath().getString("data.attributes.status");

            // If Async Query is created and completed
            if (outputResponse.equals("COMPLETE")) {

                // Validate AsyncQuery Response
                response
                        .then()
                        .statusCode(com.yahoo.elide.core.exceptions.HttpStatus.SC_OK)
                        .body("data.id", equalTo("ba31ca4e-ed8f-4be0-a0f3-12088fa9263d"))
                        .body("data.type", equalTo("asyncQuery"))
                        .body("data.attributes.queryType", equalTo("JSONAPI_V1_0"))
                        .body("data.attributes.status", equalTo("COMPLETE"))
                        .body("data.attributes.result.contentLength", notNullValue())
                        .body("data.attributes.result.responseBody", equalTo(
                            data(
                                resource(
                                    type("post"),
                                    id("2"),
                                    attributes(
                                        attr("abusiveContent", false),
                                        attr("content", "This is my first post. woot."),
                                        attr("date", "2019-01-01T00:00Z")
                                    ),
                                    links(
                                        attr("self", "https://elide.io/api/post/2")
                                    )
                                )
                            ).toJSON()
                        ));

                // Validate GraphQL Response
                String responseGraphQL = given()
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body("{\"query\":\"{ asyncQuery(ids: [\\\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263d\\\"]) "
                                + "{ edges { node { id queryType status result "
                                + "{ responseBody httpStatus  contentLength } } } } }\","
                                + "\"variables\":null}")
                        .post("/graphql/api/")
                        .asString();

                String expectedResponse = "{\"data\":{\"asyncQuery\":{\"edges\":[{\"node\":{\"id\":\"ba31ca4e-ed8f-4be0-a0f3-12088fa9263d\",\"queryType\":\"JSONAPI_V1_0\",\"status\":\"COMPLETE\",\"result\":{\"responseBody\":\"{\\\"data\\\":[{\\\"type\\\":\\\"post\\\",\\\"id\\\":\\\"2\\\",\\\"attributes\\\":{\\\"abusiveContent\\\":false,\\\"content\\\":\\\"This is my first post. woot.\\\",\\\"date\\\":\\\"2019-01-01T00:00Z\\\"},\\\"links\\\":{\\\"self\\\":\\\"https://elide.io/api/post/2\\\"}}]}\",\"httpStatus\":200,\"contentLength\":188}}}]}}}";
                assertEquals(expectedResponse, responseGraphQL);
                break;
            }
            assertEquals("PROCESSING", outputResponse, "Async Query has failed.");
            i++;
            assertNotEquals(1000, i, "Async Query not completed.");
        }
    }

    // Resource disabled by default.
    @Test
    public void exportResourceDisabledTest() {
        // elide-standalone returns different error message when export resource is not initialized
        // vs when it could not find a matching id to export.
        // Jetty seems to have a different behavior than spring-framework for non-existent resources.
        // Post call to a non-existent resource returns 405 in jetty, spring-framework returns 404.
        // Spring-framework behaved similar to Jetty,
        // but was changed with https://github.com/spring-projects/spring-boot/issues/4876.
        int queryId = 1;
        when()
                .get("/export/" + queryId)
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body(containsString(" Not Found"))
                .body(not(containsString(queryId + " Not Found")));
    }

    @Test
    public void metaDataTest() {
        given()
                .accept("application/vnd.api+json")
                .get("/api/namespace/default") //"default" namespace added by Agg Store
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("data.attributes.name", equalTo("default"))
                .body("data.attributes.friendlyName", equalTo("default"));
    }

    @Test
    public void testVerboseErrors() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("post"),
                                        id("1"),
                                        attributes(
                                                attr("content", "This is my first post. woot."),
                                                attr("date", "Invalid")
                                        )
                                )
                        )
                )
                .post("/api/post")
                .then()
                .body("errors.detail[0]", equalTo("Invalid value: Invalid\nDate strings must be formatted as yyyy-MM-dd&#39;T&#39;HH:mm&#39;Z&#39;"))
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }
}
