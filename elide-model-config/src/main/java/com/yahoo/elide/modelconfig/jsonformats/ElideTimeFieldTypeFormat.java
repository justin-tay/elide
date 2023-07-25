/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.networknt.schema.format.AbstractFormat;

import java.util.regex.Pattern;

/**
 * Format specifier for {@code elideTimeFieldType} format attribute.
 * <p>
 * This specifier will check if a string instance is {@code Time}.
 * </p>
 */
public class ElideTimeFieldTypeFormat extends AbstractFormat {
    private static final Pattern TIME_FIELD_TYPE_PATTERN = Pattern.compile("^(?i)(Time)$");

    public static final String NAME = "elideTimeFieldType";
    public static final String ERROR_MESSAGE_DESCRIPTION = "must be "
                    + "[Time] for any time dimension.";

    public ElideTimeFieldTypeFormat() {
        super(NAME, ERROR_MESSAGE_DESCRIPTION);
    }

    @Override
    public boolean matches(String value) {
        if (!TIME_FIELD_TYPE_PATTERN.matcher(value).matches()) {
            return false;
        }
        return true;
    }
}
