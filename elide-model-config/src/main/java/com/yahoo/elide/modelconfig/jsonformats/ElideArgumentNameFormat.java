/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.networknt.schema.format.AbstractFormat;

/**
 * Format specifier for {@code elideArgumentName} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid Elide Argument Name.
 * </p>
 */
public class ElideArgumentNameFormat extends AbstractFormat {

    public static final String NAME = "elideArgumentName";
    public static final String ERROR_MESSAGE_DESCRIPTION = "must start with an alphabetic character and can include"
            + " alphabets, numbers and '_' only and cannot be 'grain'.";

    public ElideArgumentNameFormat() {
        super(NAME, ERROR_MESSAGE_DESCRIPTION);
    }

    @Override
    public boolean matches(String value) {
        if (!ElideNameFormat.NAME_FORMAT_REGEX.matcher(value).matches()) {
            return false;
        }

        if (value.equalsIgnoreCase("grain")) {
            return false;
        }
        return true;
    }
}
