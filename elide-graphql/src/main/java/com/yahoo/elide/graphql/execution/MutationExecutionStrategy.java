/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.execution;

import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.graphql.GraphQLRequestScope;

import com.google.common.collect.Maps;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.execution.AsyncSerialExecutionStrategy;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.ExecutionContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class MutationExecutionStrategy extends AsyncSerialExecutionStrategy {
    public MutationExecutionStrategy() {
        super();
    }

    public MutationExecutionStrategy(DataFetcherExceptionHandler exceptionHandler) {
        super(exceptionHandler);
    }

    @Override
    protected BiConsumer<List<Object>, Throwable> handleResults(ExecutionContext executionContext,
            List<String> fieldNames, CompletableFuture<ExecutionResult> overallResult) {
        return (List<Object> results, Throwable exception) -> {
            if (exception != null) {
                handleNonNullException(executionContext, overallResult, exception);
                return;
            }
            Map<String, Object> resolvedValuesByField = Maps.newLinkedHashMapWithExpectedSize(fieldNames.size());
            int ix = 0;
            for (Object result : results) {
                String fieldName = fieldNames.get(ix++);
                resolvedValuesByField.put(fieldName, result);
            }
            try {
                GraphQLRequestScope requestScope = (GraphQLRequestScope) executionContext.getExecutionInput()
                        .getLocalContext();
                DataStoreTransaction tx = requestScope.getTransaction();
                tx.preCommit(requestScope);
                requestScope.getPermissionExecutor().executeCommitChecks();
                if (executionContext.getErrors().isEmpty()) {
                    requestScope.saveOrCreateObjects();
                }
                tx.flush(requestScope);
            } catch (Throwable e) {
                handleNonNullException(executionContext, overallResult, e);
                return;
            }
            overallResult.complete(new ExecutionResultImpl(resolvedValuesByField, executionContext.getErrors()));
        };
    }
}
