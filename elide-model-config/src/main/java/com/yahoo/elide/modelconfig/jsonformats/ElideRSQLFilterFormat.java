/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import static com.yahoo.elide.core.filter.dialect.RSQLFilterDialect.getDefaultOperatorsWithIsnull;

import com.networknt.schema.format.AbstractFormat;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;

/**
 * Format specifier for {@code elideRSQLFilter} format attribute.
 * <p>
 * This specifier will check if a string instance is a valid RSQL filter.
 * </p>
 */
public class ElideRSQLFilterFormat extends AbstractFormat {

    public static final String NAME = "elideRSQLFilter";
    public static final String ERROR_MESSAGE_DESCRIPTION = "is not a valid RSQL filter expression. Please visit page "
                    + "https://elide.io/pages/guide/v5/11-graphql.html#operators for samples.";

    public ElideRSQLFilterFormat() {
        super(NAME, ERROR_MESSAGE_DESCRIPTION);
    }

    @Override
    public boolean matches(String value) {
        try {
            new RSQLParser(getDefaultOperatorsWithIsnull()).parse(value);
        } catch (RSQLParserException e) {
            return false;
        }
        return true;
    }
}
