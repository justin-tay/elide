/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import static com.yahoo.elide.modelconfig.jsonformats.ValidateDimPropertiesValidator.COMMON_DIM_PROPERTIES;

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

import java.util.Collections;
import java.util.Set;

/**
 * Defines custom Keyword Validator for {@code validateTimeDimensionProperties}.
 * <p>
 * This validator checks no additional properties are defined for any time
 * dimension.
 * </p>
 */
public class ValidateTimeDimPropertiesValidator extends BaseJsonValidator {

    private static final Set<String> ADDITIONAL_TIME_DIM_PROPERTIES = ImmutableSet.of("grains");

    public static final String KEYWORD = "validateTimeDimensionProperties";
    public static final String ADDITIONAL_KEY = "validateTimeDimensionProperties.error.additional";
    public static final String ADDITIONAL_MSG = "{0}: properties {1} are not allowed for time dimensions.";

    public static final ErrorMessageType ERROR_MESSAGE_TYPE = new ErrorMessageType() {
        @Override
        public String getErrorCode() {
            return "3003";
        }
    };

    public ValidateTimeDimPropertiesValidator(Keyword keyword, SchemaLocation schemaLocation,
            JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema,
            ValidationContext validationContext) {
        super(schemaLocation, evaluationPath, schemaNode, parentSchema, ERROR_MESSAGE_TYPE, keyword, validationContext,
                false);
    }

    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode,
            JsonNodePath instanceLocation) {
        JsonNode instance = node;
        Set<String> fields = Sets.newHashSet(instance.fieldNames());
        fields.removeAll(COMMON_DIM_PROPERTIES);
        fields.removeAll(ADDITIONAL_TIME_DIM_PROPERTIES);
        if (!fields.isEmpty()) {
            return Collections.singleton(message().instanceLocation(instanceLocation).instanceNode(node)
                    .messageKey(ADDITIONAL_MSG).arguments(fields.toString()).build());
        }
        return Collections.emptySet();
    }
}
