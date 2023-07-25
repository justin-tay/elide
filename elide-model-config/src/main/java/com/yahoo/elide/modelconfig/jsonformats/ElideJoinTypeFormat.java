/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.networknt.schema.format.AbstractFormat;

import java.util.regex.Pattern;

/**
 * Format specifier for {@code elideJoinType} format attribute.
 * <p>
 * This specifier will check if a string instance is one of {@code left, inner, full, cross}.
 * </p>
 */
public class ElideJoinTypeFormat extends AbstractFormat {
    private static final Pattern JOIN_TYPE_PATTERN = Pattern.compile("^(?i)(left|inner|full|cross)$");

    public static final String NAME = "elideJoinType";
    public static final String ERROR_MESSAGE_DESCRIPTION =
                    "must be one of [left, inner, full, cross].";

    public ElideJoinTypeFormat() {
        super(NAME, ERROR_MESSAGE_DESCRIPTION);
    }

    @Override
    public boolean matches(String value) {
        if (!JOIN_TYPE_PATTERN.matcher(value).matches()) {
            return false;
        }
        return true;
    }
}
