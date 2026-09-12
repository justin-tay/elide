/*
 * Copyright 2026, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.tests;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.yahoo.elide.async.service.storageengine.ResultStorageEngine;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Regression test for the intermittent {@link ClientProtocolException} when downloading a table export.
 * <p>
 * By default Spring Security's {@code HeaderWriterFilter} writes its headers when the response is committed
 * and again in a {@code finally} block once the filter chain returns. The export endpoint returns a
 * {@code StreamingResponseBody}, which is flushed on an async thread, so the request thread can still be
 * adding headers to Tomcat's unsynchronized {@code MimeHeaders} while the async thread commits the response.
 * A header that has been counted but not yet named is then sent with an empty name, and the client fails
 * with {@code ProtocolException: Invalid header: : }. See
 * <a href="https://github.com/spring-projects/spring-security/issues/15510">spring-security#15510</a>.
 * <p>
 * {@link example.SecurityConfiguration} writes the headers eagerly to avoid this. Without that, this test
 * typically fails within a few hundred requests.
 */
public class ExportClientProtocolTest extends IntegrationTest {
    private static final int THREADS = 8;
    private static final int REQUESTS = 10_000;
    private static final long TIMEOUT_SECONDS = 120;

    @Autowired
    private ResultStorageEngine resultStorageEngine;

    @Test
    public void concurrentExportDownloadsHaveValidHeaders() throws InterruptedException {
        String tableExportId = UUID.randomUUID() + ".csv";
        resultStorageEngine.storeResults(tableExportId, outputStream -> {
            try {
                outputStream.write("\"name\"\n\"value\"\n".getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        // Reuse pooled keep-alive connections so a high request volume does not exhaust local ports.
        RestAssuredConfig config = pooledHttpClientConfig();
        AtomicReference<Exception> failure = new AtomicReference<>();
        AtomicInteger requests = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        boolean completed;
        try {
            for (int i = 0; i < THREADS; i++) {
                executor.execute(() -> {
                    while (failure.get() == null && requests.incrementAndGet() <= REQUESTS) {
                        try {
                            // Consume the body so the connection is returned to the pool.
                            given().config(config).when().get("/export/" + tableExportId).asByteArray();
                        } catch (Exception e) {
                            // RestAssured rethrows the checked ClientProtocolException undeclared.
                            failure.compareAndSet(null, e);
                        }
                    }
                });
            }
        } finally {
            executor.shutdown();
            completed = executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                executor.shutdownNow();
            }
        }
        assertTrue(completed, "Requests did not complete");

        Exception exception = failure.get();
        if (exception != null) {
            fail("Export download failed after " + Math.min(requests.get(), REQUESTS) + " requests", exception);
        }
    }

    @SuppressWarnings("deprecation")
    private static RestAssuredConfig pooledHttpClientConfig() {
        return RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig()
                .reuseHttpClientInstance()
                .httpClientFactory(() -> {
                    PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
                    connectionManager.setMaxTotal(THREADS);
                    connectionManager.setDefaultMaxPerRoute(THREADS);
                    return new DefaultHttpClient(connectionManager);
                }));
    }
}
