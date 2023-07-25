/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.networknt.schema.format.AbstractFormat;

import java.util.regex.Pattern;

/**
 * Format specifier for {@code elideCardiality} format attribute.
 * <p>
 * This specifier will check if a string instance is one of {@code Tiny, Small, Medium, Large, Huge}.
 * </p>
 */
public class ElideCardinalityFormat extends AbstractFormat {
    private static final Pattern CARDINALITY_PATTERN = Pattern.compile("^(?i)(Tiny|Small|Medium|Large|Huge)$");

    public static final String NAME = "elideCardiality";
    public static final String ERROR_MESSAGE_DESCRIPTION = "must be one of "
                    + "[Tiny, Small, Medium, Large, Huge].";

    public ElideCardinalityFormat() {
        super(NAME, ERROR_MESSAGE_DESCRIPTION);
    }

    @Override
    public boolean matches(String value) {
        if (!CARDINALITY_PATTERN.matcher(value).matches()) {
            return false;
        }
        return true;
    }
}
