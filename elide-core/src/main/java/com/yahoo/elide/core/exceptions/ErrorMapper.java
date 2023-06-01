/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import javax.annotation.Nullable;

/**
 * The ErrorMapper allows mapping an Exception of your choice into more meaningful
 * ErrorResponseException to improve your error response to the client.
 */
@FunctionalInterface
public interface ErrorMapper {
    /**
     * Map the exception.
     *
     * @param origin any Exception not caught by default
     * @return a mapped ErrorResponseException or null if you do not want to map this error
     */
    @Nullable ErrorResponseException map(Exception origin);
}
