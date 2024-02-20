/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;


import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.AbstractJsonValidator;
import com.networknt.schema.AbstractKeyword;
import com.networknt.schema.ExecutionContext;
import com.networknt.schema.JsonNodePath;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.JsonValidator;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.ValidationContext;
import com.networknt.schema.ValidationMessage;

import java.util.Collections;
import java.util.Set;

/**
 * Defines custom Keyword Validator for {@code validateArgumentProperties}.
 * <p>
 * This validator checks not both {@code tableSource} and {@code values} property is defined for any argument.
 * </p>
 */
public class ValidateArgsPropertiesKeyword extends AbstractKeyword {

    public static final String KEYWORD = "validateArgumentProperties";

    public ValidateArgsPropertiesKeyword() {
        super(KEYWORD);
    }

    @Override
    public JsonValidator newValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode,
            JsonSchema parentSchema, ValidationContext validationContext) throws JsonSchemaException, Exception {
        boolean validate = schemaNode.booleanValue();
        if (validate) {
            return new ValidateArgsPropertiesValidator(this, schemaLocation, evaluationPath, schemaNode, parentSchema,
                    validationContext);
        } else {
            return new AbstractJsonValidator(schemaLocation, evaluationPath, this, schemaNode) {
                @Override
                public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node,
                        JsonNode rootNode, JsonNodePath instanceLocation) {
                    return Collections.emptySet();
                }
            };
        }
    }
}
