/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.AbstractJsonValidator;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationContext;
import com.networknt.schema.ValidationMessage;

import java.text.MessageFormat;
import java.util.Locale;

public abstract class BaseJsonValidator extends AbstractJsonValidator {
    protected final String validatorType;
    protected final String schemaPath;
    protected final JsonNode schemaNode;
    protected final JsonSchema parentSchema;
    protected final ValidationContext validationContext;
    protected final Locale locale;
    protected final MessageSource messageSource;

    public BaseJsonValidator(String validatorType, MessageSource messageSource, String schemaPath, JsonNode schemaNode,
            JsonSchema parentSchema, ValidationContext validationContext) {
        this.validatorType = validatorType;
        this.schemaPath = schemaPath;
        this.schemaNode = schemaNode;
        this.parentSchema = parentSchema;
        this.validationContext = validationContext;
        this.messageSource = messageSource;
        this.locale = (validationContext != null && validationContext.getConfig() != null
                && validationContext.getConfig().getLocale() != null) ? validationContext.getConfig().getLocale()
                        : Locale.getDefault();
    }

    protected ValidationMessage constructValidationMessage(String messageKey, String message, String at,
            String... arguments) {
        String customMessage = messageSource.getMessage(messageKey, message, this.locale, (Object[]) arguments);
        return new ValidationMessage.Builder().code(messageKey)
                .path(at)
                .schemaPath(this.schemaPath)
                .arguments(arguments)
                .customMessage(customMessage)
                .format(NULL)
                .type(this.validatorType)
                .build();
    }

    private static final MessageFormat NULL = new MessageFormat("");
}
