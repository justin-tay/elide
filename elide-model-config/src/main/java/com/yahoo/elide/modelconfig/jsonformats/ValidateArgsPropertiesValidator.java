/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.networknt.schema.BaseJsonValidator;
import com.networknt.schema.ErrorMessageType;
import com.networknt.schema.ExecutionContext;
import com.networknt.schema.JsonNodePath;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.Keyword;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.ValidationContext;
import com.networknt.schema.ValidationMessage;

import java.util.Collections;
import java.util.Set;

/**
 * Defines custom Keyword Validator for {@code validateArgumentProperties}.
 * <p>
 * This validator checks not both {@code tableSource} and {@code values}
 * property is defined for any argument.
 * </p>
 */
public class ValidateArgsPropertiesValidator extends BaseJsonValidator {

    public static final String KEYWORD = "validateArgumentProperties";
    public static final String ATMOST_ONE_KEY = "validateArgumentProperties.error.atmostOne";
    public static final String ATMOST_ONE_MSG = "{0}: tableSource and values cannot both be defined for an argument. "
            + "Choose One or None.";

    public static final ErrorMessageType ERROR_MESSAGE_TYPE = new ErrorMessageType() {
        @Override
        public String getErrorCode() {
            return "3001";
        }
    };

    public ValidateArgsPropertiesValidator(Keyword keyword, SchemaLocation schemaLocation, JsonNodePath evaluationPath,
            JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) {
        super(schemaLocation, evaluationPath, schemaNode, parentSchema, ERROR_MESSAGE_TYPE, keyword, validationContext,
                false);
    }

    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode,
            JsonNodePath instanceLocation) {
        JsonNode instance = node;
        Set<String> fields = Sets.newHashSet(instance.fieldNames());

        if (fields.contains("values") && fields.contains("tableSource")) {
            return Collections.singleton(
                    message().messageKey(ATMOST_ONE_MSG).instanceLocation(instanceLocation).instanceNode(node).build());
        }
        return Collections.emptySet();
    }
}
