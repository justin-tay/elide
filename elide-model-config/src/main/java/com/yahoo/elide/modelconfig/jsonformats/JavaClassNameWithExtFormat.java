/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.networknt.schema.format.AbstractFormat;

import java.util.regex.Pattern;

/**
 * Format specifier for {@code javaClassNameWithExt} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid JAVA Class Name with {@code .class} extension.
 * </p>
 */
public class JavaClassNameWithExtFormat extends AbstractFormat {
    private static final Pattern CLASS_NAME_FORMAT_PATTERN =
            Pattern.compile("^(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)+class$");

    public static final String NAME = "javaClassNameWithExt";
    public static final String ERROR_MESSAGE_DESCRIPTION = "is not a valid Java class name with .class extension.";

    public JavaClassNameWithExtFormat() {
        super(NAME, ERROR_MESSAGE_DESCRIPTION);
    }

    @Override
    public boolean matches(String value) {
        if (!CLASS_NAME_FORMAT_PATTERN.matcher(value).matches()) {
            return false;
        }
        return true;
    }
}
