/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.networknt.schema.ExecutionContext;
import com.networknt.schema.Format;

/**
 * Format specifier for {@code elideJdbcUrl} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid JDBC url.
 * </p>
 */
public class ElideJDBCUrlFormat implements Format {

    public static final String NAME = "elideJdbcUrl";
    public static final String ERROR_MESSAGE_DESCRIPTION = "{0}: does not match the elideJdbcUrl"
            + " pattern must start with ''jdbc:''.";

    @Override
    public boolean matches(ExecutionContext executionContext, String value) {
        if (!value.startsWith("jdbc:")) {
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getMessageKey() {
        return ERROR_MESSAGE_DESCRIPTION;
    }
}
