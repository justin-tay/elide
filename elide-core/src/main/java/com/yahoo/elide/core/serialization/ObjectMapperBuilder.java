/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;

/**
 * Used to build an ObjectMapper.
 *
 * @see com.fasterxml.jackson.databind.ObjectMapper
 */
public class ObjectMapperBuilder extends MapperBuilder<ObjectMapper, ObjectMapperBuilder> {
    public ObjectMapperBuilder(ObjectMapper mapper) {
        super(mapper);
    }

    public ObjectMapperBuilder() {
        this(new ObjectMapper());
    }
}
