/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.networknt.schema.format.AbstractFormat;

import java.util.regex.Pattern;

/**
 * Format specifier for {@code elideGrainType} format attribute.
 * <p>
 * This specifier will check if a string instance is one of
 * {@code Second, Minute, Hour, Day, IsoWeek, Week, Month, Quarter, Year}.
 * </p>
 */
public class ElideGrainTypeFormat extends AbstractFormat {
    private static final Pattern GRAIN_TYPE_PATTERN =
            Pattern.compile("^(?i)(Second|Minute|Hour|Day|IsoWeek|Week|Month|Quarter|Year)$");

    public static final String NAME = "elideGrainType";
    public static final String ERROR_MESSAGE_DESCRIPTION = "must be one of "
            + "[Second, Minute, Hour, Day, IsoWeek, Week, Month, Quarter, Year].";

    public ElideGrainTypeFormat() {
        super(NAME, ERROR_MESSAGE_DESCRIPTION);
    }

    @Override
    public boolean matches(String value) {
        if (!GRAIN_TYPE_PATTERN.matcher(value).matches()) {
            return false;
        }
        return true;
    }
}
