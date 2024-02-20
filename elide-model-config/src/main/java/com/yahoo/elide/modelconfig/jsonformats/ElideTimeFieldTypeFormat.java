/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.networknt.schema.ExecutionContext;
import com.networknt.schema.Format;

import java.util.regex.Pattern;

/**
 * Format specifier for {@code elideTimeFieldType} format attribute.
 * <p>
 * This specifier will check if a string instance is {@code Time}.
 * </p>
 */
public class ElideTimeFieldTypeFormat implements Format {
    private static final Pattern TIME_FIELD_TYPE_PATTERN = Pattern.compile("^(?i)(Time)$");

    public static final String NAME = "elideTimeFieldType";
    public static final String ERROR_MESSAGE_DESCRIPTION = "{0}: does not match the elideTimeFieldType pattern must be "
                    + "[Time] for any time dimension.";

    @Override
    public boolean matches(ExecutionContext executionContext, String value) {
        if (!TIME_FIELD_TYPE_PATTERN.matcher(value).matches()) {
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
