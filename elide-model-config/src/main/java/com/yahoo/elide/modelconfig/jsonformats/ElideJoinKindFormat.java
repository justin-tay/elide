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
 * Format specifier for {@code elideJoinKind} format attribute.
 * <p>
 * This specifier will check if a string instance is one of {@code ToOne, ToMany}.
 * </p>
 */
public class ElideJoinKindFormat implements Format {
    private static final Pattern JOIN_KIND_PATTERN = Pattern.compile("^(?i)(ToOne|ToMany)$");

    public static final String NAME = "elideJoinKind";
    public static final String ERROR_MESSAGE_DESCRIPTION = "{0}: does not match the elideJoinKind"
            + " pattern must be one of [ToOne, ToMany].";

    @Override
    public boolean matches(ExecutionContext executionContext, String value) {
        if (!JOIN_KIND_PATTERN.matcher(value).matches()) {
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
