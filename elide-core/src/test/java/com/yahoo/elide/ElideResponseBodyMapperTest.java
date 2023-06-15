/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Test for ElideResponseBodyMapper.
 */
class ElideResponseBodyMapperTest {

    @Test
    void identity() throws Exception {
        ElideResponseBodyMapper<String, String> mapper = ElideResponseBodyMapper.identity();
        String result = mapper.map("test");
        assertEquals("test", result);
    }
}
