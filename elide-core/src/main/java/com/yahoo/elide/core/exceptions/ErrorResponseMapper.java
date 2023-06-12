/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.yahoo.elide.ElideErrorResponse;

import javax.annotation.Nullable;

/**
 * Maps a exception to an {@link ElideErrorResponse}.
 *
 * @param <T> response body type
 */
@FunctionalInterface
public interface ErrorResponseMapper<T> {
    /**
     * Map the exception to an {@link ElideErrorResponse}.
     *
     * @param exception the exception to map.
     * @param errorContext the error context
     * @return the mapped ElideErrorResponse or null if you do not want to map this error
     */
    @Nullable ElideErrorResponse<T> map(Exception exception, ErrorContext errorContext);
}
