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
 * Format specifier for {@code elideFieldType} format attribute.
 * <p>
 * This specifier will check if a string instance is one of {@code Integer, Decimal, Money, Text, Coordinate, Boolean}.
 * </p>
 */
public class ElideFieldTypeFormat implements Format {
    public static final Pattern FIELD_TYPE_PATTERN =
            Pattern.compile("^(?i)(Integer|Decimal|Money|Text|Coordinate|Boolean|Enum_Text|Enum_Ordinal)$");

    public static final String NAME = "elideFieldType";
    public static final String ERROR_MESSAGE_DESCRIPTION = "{0}: does not match the elideFieldType pattern"
            + " must be one of "
            + "[Integer, Decimal, Money, Text, Coordinate, Boolean, Enum_Text, Enum_Ordinal].";

    private final String name;

    public ElideFieldTypeFormat() {
        this.name = NAME;
    }

    public ElideFieldTypeFormat(String formatName) {
        this.name = formatName;
    }

    @Override
    public boolean matches(ExecutionContext executionContext, String value) {
        if (!FIELD_TYPE_PATTERN.matcher(value).matches()) {
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getMessageKey() {
        return ERROR_MESSAGE_DESCRIPTION;
    }
}
