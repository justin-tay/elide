/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.yahoo.elide.ElideErrorResponse;

import javax.annotation.Nullable;

/**
 * Maps a exception to an ElideErrorResponse.
 */
@FunctionalInterface
public interface ErrorResponseMapper {
    /**
     * Map the exception.
     *
     * @param exception the exception to map.
     * @param verbose if the response should be verbose
     * @return the mapped ElideErrorResponse or null if you do not want to map this error
     */
    @Nullable ElideErrorResponse map(Exception exception, ErrorContext errorContext);
}
