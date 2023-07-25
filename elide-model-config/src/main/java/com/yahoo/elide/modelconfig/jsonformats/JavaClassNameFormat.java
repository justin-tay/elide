/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.networknt.schema.format.AbstractFormat;

import java.util.regex.Pattern;

/**
 * Format specifier for {@code javaClassName} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid JAVA Class Name.
 * </p>
 */
public class JavaClassNameFormat extends AbstractFormat {
    private static final String ID_PATTERN = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
    public static final Pattern CLASS_NAME_FORMAT_PATTERN = Pattern.compile(ID_PATTERN + "(\\." + ID_PATTERN + ")*");

    public static final String FORMAT_NAME = "javaClassName";
    public static final String ERROR_MESSAGE_DESCRIPTION = "is not a valid Java class name.";

    public JavaClassNameFormat() {
        super(FORMAT_NAME, ERROR_MESSAGE_DESCRIPTION);
    }

    @Override
    public boolean matches(String value) {
        if (!CLASS_NAME_FORMAT_PATTERN.matcher(value).matches()) {
            return false;
        }
        return true;
    }
}
