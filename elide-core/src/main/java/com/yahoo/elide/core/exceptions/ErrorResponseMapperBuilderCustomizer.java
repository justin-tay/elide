/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

/**
 * Customize for {@link ErrorResponseMapperBuilder}.
 */
@FunctionalInterface
public interface ErrorResponseMapperBuilderCustomizer {
    /**
     * Customize the {@link ErrorResponseMapperBuilder}.
     *
     * @param errorResponseMapperBuilder the builder
     */
    void customize(ErrorResponseMapperBuilder errorResponseMapperBuilder);
}
