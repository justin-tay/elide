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
 * Format specifier for {@code javaClassNameWithExt} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid JAVA Class Name
 * with {@code .class} extension.
 * </p>
 */
public class JavaClassNameWithExtFormat implements Format {
    private static final Pattern CLASS_NAME_FORMAT_PATTERN = Pattern
            .compile("^(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)+class$");

    public static final String NAME = "javaClassNameWithExt";
    public static final String ERROR_MESSAGE_DESCRIPTION = "{0}: does not match the javaClassNameWithExt pattern"
            + " is not a valid Java class name with .class extension.";

    @Override
    public boolean matches(ExecutionContext executionContext, String value) {
        if (!CLASS_NAME_FORMAT_PATTERN.matcher(value).matches()) {
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
