/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.networknt.schema.format.AbstractFormat;

import java.util.regex.Pattern;

/**
 * Format specifier for {@code elideJoinKind} format attribute.
 * <p>
 * This specifier will check if a string instance is one of {@code ToOne, ToMany}.
 * </p>
 */
public class ElideJoinKindFormat extends AbstractFormat {
    private static final Pattern JOIN_KIND_PATTERN = Pattern.compile("^(?i)(ToOne|ToMany)$");

    public static final String NAME = "elideJoinKind";
    public static final String ERROR_MESSAGE_DESCRIPTION = "must be one of [ToOne, ToMany].";

    public ElideJoinKindFormat() {
        super(NAME, ERROR_MESSAGE_DESCRIPTION);
    }

    @Override
    public boolean matches(String value) {
        if (!JOIN_KIND_PATTERN.matcher(value).matches()) {
            return false;
        }
        return true;
    }
}
