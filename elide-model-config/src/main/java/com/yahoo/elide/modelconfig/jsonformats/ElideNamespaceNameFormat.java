/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.networknt.schema.format.AbstractFormat;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Format specifier for {@code elideNamespaceName} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid Elide Name.
 * </p>
 */
public class ElideNamespaceNameFormat extends AbstractFormat {
    public static final Pattern NAME_FORMAT_REGEX = ElideNameFormat.NAME_FORMAT_REGEX;

    public static final String NAME = "elideNamespaceName";
    public static final String ERROR_MESSAGE_DESCRIPTION =
                    "must start with an alphabetic character and can include "
                    + "alphabets, numbers and '_' only and must not clash with the 'default' namespace.";
    public static final String DEFAULT_NAME = "default";

    public ElideNamespaceNameFormat() {
        super(NAME, ERROR_MESSAGE_DESCRIPTION);
    }

    @Override
    public boolean matches(String value) {
        if (!NAME_FORMAT_REGEX.matcher(value).matches()) {
            return false;
        }

        if (!value.equals(DEFAULT_NAME) && value.toLowerCase(Locale.ENGLISH).equals(DEFAULT_NAME)) {
            return false;
        }
        return true;
    }
}
