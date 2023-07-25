/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.networknt.schema.format.AbstractFormat;

import java.util.regex.Pattern;

/**
 * Format specifier for {@code elideFieldType} format attribute.
 * <p>
 * This specifier will check if a string instance is one of {@code Integer, Decimal, Money, Text, Coordinate, Boolean}.
 * </p>
 */
public class ElideFieldTypeFormat extends AbstractFormat {
    public static final Pattern FIELD_TYPE_PATTERN =
            Pattern.compile("^(?i)(Integer|Decimal|Money|Text|Coordinate|Boolean|Enum_Text|Enum_Ordinal)$");

    public static final String NAME = "elideFieldType";
    public static final String ERROR_MESSAGE_DESCRIPTION = "must be one of "
            + "[Integer, Decimal, Money, Text, Coordinate, Boolean, Enum_Text, Enum_Ordinal].";

    public ElideFieldTypeFormat() {
        this(NAME);
    }

    public ElideFieldTypeFormat(String formatName) {
        super(formatName, ERROR_MESSAGE_DESCRIPTION);
    }

    @Override
    public boolean matches(String value) {
        if (!FIELD_TYPE_PATTERN.matcher(value).matches()) {
            return false;
        }
        return true;
    }
}
