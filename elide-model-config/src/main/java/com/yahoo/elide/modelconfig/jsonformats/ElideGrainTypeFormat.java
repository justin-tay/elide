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
 * Format specifier for {@code elideGrainType} format attribute.
 * <p>
 * This specifier will check if a string instance is one of
 * {@code Second, Minute, Hour, Day, IsoWeek, Week, Month, Quarter, Year}.
 * </p>
 */
public class ElideGrainTypeFormat implements Format {
    private static final Pattern GRAIN_TYPE_PATTERN =
            Pattern.compile("^(?i)(Second|Minute|Hour|Day|IsoWeek|Week|Month|Quarter|Year)$");

    public static final String NAME = "elideGrainType";
    public static final String ERROR_MESSAGE_DESCRIPTION = "{0}: does not match the elideGrainType pattern"
            + " must be one of "
            + "[Second, Minute, Hour, Day, IsoWeek, Week, Month, Quarter, Year].";

    @Override
    public boolean matches(ExecutionContext executionContext, String value) {
        if (!GRAIN_TYPE_PATTERN.matcher(value).matches()) {
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
