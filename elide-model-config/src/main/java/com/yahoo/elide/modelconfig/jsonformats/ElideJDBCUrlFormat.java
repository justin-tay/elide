/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.networknt.schema.format.AbstractFormat;

/**
 * Format specifier for {@code elideJdbcUrl} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid JDBC url.
 * </p>
 */
public class ElideJDBCUrlFormat extends AbstractFormat {

    public static final String NAME = "elideJdbcUrl";
    public static final String ERROR_MESSAGE_DESCRIPTION = "must start with 'jdbc:'.";

    public ElideJDBCUrlFormat() {
        super(NAME, ERROR_MESSAGE_DESCRIPTION);
    }

    @Override
    public boolean matches(String value) {
        if (!value.startsWith("jdbc:")) {
            return false;
        }
        return true;
    }
}
