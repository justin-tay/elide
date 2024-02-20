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
 * Format specifier for {@code elideCardinality} format attribute.
 * <p>
 * This specifier will check if a string instance is one of {@code Tiny, Small, Medium, Large, Huge}.
 * </p>
 */
public class ElideCardinalityFormat implements Format {
    private static final Pattern CARDINALITY_PATTERN = Pattern.compile("^(?i)(Tiny|Small|Medium|Large|Huge)$");

    public static final String NAME = "elideCardinality";
    public static final String ERROR_MESSAGE_DESCRIPTION = "{0}: does not match the elideCardinality"
            + " pattern must be one of "
                    + "[Tiny, Small, Medium, Large, Huge].";

    @Override
    public boolean matches(ExecutionContext executionContext, String value) {
        if (!CARDINALITY_PATTERN.matcher(value).matches()) {
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
