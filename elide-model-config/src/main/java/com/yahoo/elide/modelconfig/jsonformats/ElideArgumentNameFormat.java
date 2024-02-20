/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.networknt.schema.ExecutionContext;
import com.networknt.schema.Format;

/**
 * Format specifier for {@code elideArgumentName} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid Elide Argument Name.
 * </p>
 */
public class ElideArgumentNameFormat implements Format {

    public static final String NAME = "elideArgumentName";
    public static final String ERROR_MESSAGE_DESCRIPTION = "{0}: does not match the elideArgumentName pattern"
            + " must start with an alphabetic character and can include"
            + " alphabets, numbers and ''_'' only and cannot be ''grain''.";

    @Override
    public boolean matches(ExecutionContext executionContext, String value) {
        if (!ElideNameFormat.NAME_FORMAT_REGEX.matcher(value).matches()) {
            return false;
        }

        if (value.equalsIgnoreCase("grain")) {
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
