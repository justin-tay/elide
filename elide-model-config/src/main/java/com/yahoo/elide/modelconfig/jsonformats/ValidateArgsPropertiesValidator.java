/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.networknt.schema.ExecutionContext;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationContext;
import com.networknt.schema.ValidationMessage;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Defines custom Keyword Validator for {@code validateArgumentProperties}.
 * <p>
 * This validator checks not both {@code tableSource} and {@code values} property is defined for any argument.
 * </p>
 */
public class ValidateArgsPropertiesValidator extends BaseJsonValidator {

    public static final String KEYWORD = "validateArgumentProperties";
    public static final String ATMOST_ONE_KEY = "validateArgumentProperties.error.atmostOne";
    public static final String ATMOST_ONE_MSG =
                    "{0}: tableSource and values cannot both be defined for an argument. Choose One or None.";

    public ValidateArgsPropertiesValidator(MessageSource messageSource, String schemaPath, JsonNode schemaNode,
            JsonSchema parentSchema, ValidationContext validationContext) {
        super(KEYWORD, messageSource, schemaPath, schemaNode, parentSchema, validationContext);
    }

    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode,
            String at) {
        Set<ValidationMessage> messages = new LinkedHashSet<>();
        JsonNode instance = node;
        Set<String> fields = Sets.newHashSet(instance.fieldNames());

        if (fields.contains("values") && fields.contains("tableSource")) {
            messages.add(constructValidationMessage(ATMOST_ONE_KEY, ATMOST_ONE_MSG, at, schemaPath));
        }
        return messages;
    }
}
