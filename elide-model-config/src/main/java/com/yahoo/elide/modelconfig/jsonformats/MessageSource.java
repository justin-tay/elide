/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Resolves locale specific messages.
 */
@FunctionalInterface
public interface MessageSource {
    String getMessage(String key, Supplier<String> defaultMessageSupplier, Locale locale, Object... args);


    default String getMessage(String key, String defaultMessage, Locale locale, Object... args) {
        return getMessage(key, defaultMessage::toString, locale, args);
    }

    default String getMessage(String key, Locale locale, Object... args) {
        return getMessage(key, (Supplier<String>) null, locale, args);
    }
}
