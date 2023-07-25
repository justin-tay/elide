/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.Locale;

class ResourceBundleMessageSourceTest {

    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource("messages", "validation-messages");

    @Test
    void messageFallback() {
        String message = messageSource.getMessage("unknown.key", Locale.getDefault());
        assertEquals("unknown.key", message);
    }

    @Test
    void messageDefaultSupplier() {
        String message = messageSource.getMessage("unknown.key", "default", Locale.getDefault());
        assertEquals("default", message);
    }

    @Test
    void messageDefaultSupplierArguments() {
        String message = messageSource.getMessage("unknown.key", "An error {0}", Locale.getDefault(), "argument");
        assertEquals("An error argument", message);
    }

    @Test
    void messageDefault() {
        String message = messageSource.getMessage("validateDimensionProperties.error.atmostOne", Locale.getDefault());
        assertEquals("english", message);
    }

    @Test
    void messageUnknown() {
        String message = messageSource.getMessage("validateDimensionProperties.error.atmostOne", Locale.SIMPLIFIED_CHINESE);
        assertEquals("english", message);
    }

    @Test
    void messageFrench() {
        String message = messageSource.getMessage("validateDimensionProperties.error.atmostOne", Locale.FRANCE);
        assertEquals("french", message);
    }

    @Test
    void validationMessageDefault() {
        String message = messageSource.getMessage("validateDimensionProperties.error.additional", Locale.getDefault());
        assertEquals("english", message);
    }

    @Test
    void validationMessageFrench() {
        String message = messageSource.getMessage("validateDimensionProperties.error.additional", Locale.FRANCE);
        assertEquals("french", message);
    }
}
