/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig;

import com.yahoo.elide.modelconfig.jsonformats.ElideArgumentNameFormat;
import com.yahoo.elide.modelconfig.jsonformats.ElideCardinalityFormat;
import com.yahoo.elide.modelconfig.jsonformats.ElideFieldNameFormat;
import com.yahoo.elide.modelconfig.jsonformats.ElideFieldTypeFormat;
import com.yahoo.elide.modelconfig.jsonformats.ElideGrainTypeFormat;
import com.yahoo.elide.modelconfig.jsonformats.ElideJDBCUrlFormat;
import com.yahoo.elide.modelconfig.jsonformats.ElideJoinKindFormat;
import com.yahoo.elide.modelconfig.jsonformats.ElideJoinTypeFormat;
import com.yahoo.elide.modelconfig.jsonformats.ElideNameFormat;
import com.yahoo.elide.modelconfig.jsonformats.ElideNamespaceNameFormat;
import com.yahoo.elide.modelconfig.jsonformats.ElideRSQLFilterFormat;
import com.yahoo.elide.modelconfig.jsonformats.ElideRoleFormat;
import com.yahoo.elide.modelconfig.jsonformats.ElideTimeFieldTypeFormat;
import com.yahoo.elide.modelconfig.jsonformats.JavaClassNameFormat;
import com.yahoo.elide.modelconfig.jsonformats.JavaClassNameWithExtFormat;

import com.yahoo.elide.modelconfig.jsonformats.MessageSource;
import com.yahoo.elide.modelconfig.jsonformats.ResourceBundleMessageSource;
import com.yahoo.elide.modelconfig.jsonformats.ValidateArgsPropertiesKeyword;
import com.yahoo.elide.modelconfig.jsonformats.ValidateDimPropertiesKeyword;
import com.yahoo.elide.modelconfig.jsonformats.ValidateTimeDimPropertiesKeyword;
import com.networknt.schema.Format;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.NonValidationKeyword;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidatorTypeCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Elide {@link JsonMetaSchema}.
 */
public class ElideMetaSchema {
    public static final List<Format> FORMATS;

    static {
        List<Format> result = new ArrayList<>();
        result.add(new ElideArgumentNameFormat());
        result.add(new ElideCardinalityFormat());
        result.add(new ElideFieldNameFormat());
        result.add(new ElideFieldTypeFormat());
        result.add(new ElideGrainTypeFormat());
        result.add(new ElideJDBCUrlFormat());
        result.add(new ElideJoinKindFormat());
        result.add(new ElideJoinTypeFormat());
        result.add(new ElideNameFormat());
        result.add(new ElideNamespaceNameFormat());
        result.add(new ElideRoleFormat());
        result.add(new ElideRSQLFilterFormat());
        result.add(new ElideTimeFieldTypeFormat());
        result.add(new JavaClassNameFormat());
        result.add(new JavaClassNameWithExtFormat());
        FORMATS = result;
    }

    private static class Holder {
        static final JsonMetaSchema INSTANCE;
        static {
            String uri = SpecVersion.VersionFlag.V4.getId();
            String id = "$id";
            List<Format> builtInFormats = new ArrayList<>(JsonMetaSchema.COMMON_BUILTIN_FORMATS);

            MessageSource messageSource = new ResourceBundleMessageSource();

            INSTANCE = JsonMetaSchema.builder(uri)
                    .idKeyword(id)
                    .addFormats(builtInFormats)
                    .addFormats(FORMATS)
                    .addKeywords(ValidatorTypeCode.getNonFormatKeywords(SpecVersion.VersionFlag.V4))
                    // keywords that may validly exist, but have no validation aspect to them
                    .addKeywords(Arrays.asList(
                            new NonValidationKeyword("examples")
                    ))
                    // add your custom keyword
                    .addKeyword(new ValidateArgsPropertiesKeyword(messageSource))
                    .addKeyword(new ValidateDimPropertiesKeyword(messageSource))
                    .addKeyword(new ValidateTimeDimPropertiesKeyword(messageSource))
                    .build();

        }
    }

    public static JsonMetaSchema getInstance() {
        return Holder.INSTANCE;
    }
}
