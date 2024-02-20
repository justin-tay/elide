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
 * Format specifier for {@code elideRole} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid Elide role.
 * </p>
 */
public class ElideRoleFormat implements Format {
    private static final Pattern ROLE_FORMAT_REGEX = Pattern.compile("^[A-Za-z][0-9A-Za-z. ]*$");

    public static final String NAME = "elideRole";
    public static final String ERROR_MESSAGE_DESCRIPTION =
            "{0}: does not match the elideRole pattern must start with an alphabetic character and can include "
                    + "alphabets, numbers, spaces and ''.'' only.";

    @Override
    public boolean matches(ExecutionContext executionContext, String value) {
        if (!ROLE_FORMAT_REGEX.matcher(value).matches()) {
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
