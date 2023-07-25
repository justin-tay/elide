/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.networknt.schema.format.AbstractFormat;

import java.util.regex.Pattern;

/**
 * Format specifier for {@code elideName} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid Elide Name.
 * </p>
 */
public class ElideNameFormat extends AbstractFormat {
    public static final Pattern NAME_FORMAT_REGEX = Pattern.compile("^[A-Za-z][0-9A-Za-z_]*$");

    public static final String NAME = "elideName";
    public static final String ERROR_MESSAGE_DESCRIPTION =
                    "must start with an alphabetic character and can include "
                    + "alphabets, numbers and '_' only.";

    public ElideNameFormat() {
        super(NAME, ERROR_MESSAGE_DESCRIPTION);
    }

    @Override
    public boolean matches(String value) {
        if (!NAME_FORMAT_REGEX.matcher(value).matches()) {
            return false;
        }
        return true;
    }
}
