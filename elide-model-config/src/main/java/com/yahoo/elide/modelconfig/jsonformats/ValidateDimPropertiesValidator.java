/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Defines custom Keyword Validator for {@code validateDimensionProperties}.
 * <p>
 * This validator checks neither additional properties are defined for any
 * dimension nor not both {@code tableSource} and {@code values} property is
 * defined for any dimension.
 * </p>
 */
public class ValidateDimPropertiesValidator extends BaseJsonValidator {

    public static final Set<String> COMMON_DIM_PROPERTIES = ImmutableSet.of("name", "friendlyName", "description",
            "category", "hidden", "readAccess", "definition", "cardinality", "tags", "type", "arguments",
            "filterTemplate");
    private static final Set<String> ADDITIONAL_DIM_PROPERTIES = ImmutableSet.of("values", "tableSource");

    public static final String KEYWORD = "validateDimensionProperties";
    public static final String ATMOST_ONE_KEY = "validateDimensionProperties.error.atmostOne";
    public static final String ATMOST_ONE_MSG = "{0}: tableSource and values cannot both be defined for a dimension. "
            + "Choose One or None.";
    public static final String ADDITIONAL_KEY = "validateDimensionProperties.error.additional";
    public static final String ADDITIONAL_MSG = "{0}: properties {1} are not allowed for dimensions.";

    public static final ErrorMessageType ERROR_MESSAGE_TYPE = new ErrorMessageType() {
        @Override
        public String getErrorCode() {
            return "3002";
        }
    };

    public ValidateDimPropertiesValidator(Keyword keyword, SchemaLocation schemaLocation, JsonNodePath evaluationPath,
            JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) {
        super(schemaLocation, evaluationPath, schemaNode, parentSchema, ERROR_MESSAGE_TYPE, keyword, validationContext,
                false);
    }

    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode,
            JsonNodePath instanceLocation) {
        Set<ValidationMessage> messages = new LinkedHashSet<>();
        JsonNode instance = node;
        Set<String> fields = Sets.newHashSet(instance.fieldNames());

        if (fields.contains("values") && fields.contains("tableSource")) {
            messages.add(
                    message().messageKey(ATMOST_ONE_MSG).instanceLocation(instanceLocation).instanceNode(node).build());
        }

        fields.removeAll(COMMON_DIM_PROPERTIES);
        fields.removeAll(ADDITIONAL_DIM_PROPERTIES);
        if (!fields.isEmpty()) {
            messages.add(message().messageKey(ADDITIONAL_MSG).instanceLocation(instanceLocation).instanceNode(node)
                    .arguments(fields.toString()).build());
        }
        return messages;
    }
}
