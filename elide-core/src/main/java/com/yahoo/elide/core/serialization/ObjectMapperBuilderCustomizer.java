/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.serialization;

/**
 * Customize the {@link ObjectMapperBuilder}.
 */
@FunctionalInterface
public interface ObjectMapperBuilderCustomizer {
    void customize(ObjectMapperBuilder builder);
}
