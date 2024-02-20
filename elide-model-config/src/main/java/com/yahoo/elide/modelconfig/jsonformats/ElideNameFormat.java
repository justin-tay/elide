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
 * Format specifier for {@code elideName} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid Elide Name.
 * </p>
 */
public class ElideNameFormat implements Format {
    public static final Pattern NAME_FORMAT_REGEX = Pattern.compile("^[A-Za-z][0-9A-Za-z_]*$");

    public static final String NAME = "elideName";
    public static final String ERROR_MESSAGE_DESCRIPTION =
                    "{0}: does not match the elideName pattern must start with an alphabetic character and can include "
                    + "alphabets, numbers and ''_'' only.";

    @Override
    public boolean matches(ExecutionContext executionContext, String value) {
        if (!NAME_FORMAT_REGEX.matcher(value).matches()) {
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
