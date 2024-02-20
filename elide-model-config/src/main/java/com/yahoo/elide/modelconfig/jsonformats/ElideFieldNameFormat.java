/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.networknt.schema.ExecutionContext;
import com.networknt.schema.Format;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Format specifier for {@code elideFieldName} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid Elide Field Name.
 * </p>
 */
public class ElideFieldNameFormat implements Format {
    private static final Pattern FIELD_NAME_FORMAT_REGEX = Pattern.compile("^[a-z][0-9A-Za-z_]*$");
    private static final Set<String> RESERVED_KEYWORDS_FOR_COLUMN_NAME = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    static {
        RESERVED_KEYWORDS_FOR_COLUMN_NAME.add("id");
        RESERVED_KEYWORDS_FOR_COLUMN_NAME.add("sql");
    }

    public static final String NAME = "elideFieldName";
    public static final String NAME_KEY = "elideFieldName.error.name";
    public static final String ERROR_MESSAGE_DESCRIPTION = "{0}: does not match the elideFieldName pattern"
            + " must start with "
                    + "lower case alphabet and can include alphabets, numbers and ''_'' only and "
                    + "cannot be one of " + RESERVED_KEYWORDS_FOR_COLUMN_NAME;

    @Override
    public boolean matches(ExecutionContext executionContext, String value) {
        if (!FIELD_NAME_FORMAT_REGEX.matcher(value).matches()) {
            return false;
        }

        if (RESERVED_KEYWORDS_FOR_COLUMN_NAME.contains(value)) {
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
