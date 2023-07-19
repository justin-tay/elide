/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils.coerce.converters;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts from {@link ZonedDateTime} to String.
 * <p>
 * By default this will
 * <li>Serialize without the zone id which is the same default as Jackson.
 * <li>Deserialize retaining the zone id/offset which is not same default as
 * Jackson which adjusts to the context timezone.
 */
public class ZonedDateTimeSerde implements Serde<String, ZonedDateTime> {

    private final DateTimeFormatter dateTimeFormatter;

    /**
     * Creates the ZonedDateTimeSerde using the
     * {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME} formatter.
     * <p>
     * This follows Jackson's default behavior where
     * SerializationFeature.WRITE_DATES_WITH_ZONE_ID is not enabled by default.
     */
    public ZonedDateTimeSerde() {
        this(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * Creates the ZonedDateTimeSerde.
     *
     * @param dateTimeFormatter the date time formatter to use for serializing and
     *                          deserializing
     * @see DateTimeFormatter#ISO_OFFSET_DATE_TIME
     * @see DateTimeFormatter#ISO_ZONED_DATE_TIME
     */
    public ZonedDateTimeSerde(DateTimeFormatter dateTimeFormatter) {
        this.dateTimeFormatter = dateTimeFormatter;
    }

    @Override
    public ZonedDateTime deserialize(String val) {
        if (val == null || val.isBlank()) {
            return null;
        }
        try {
            return ZonedDateTime.parse(val, this.dateTimeFormatter);
        } catch (final DateTimeParseException ex) {
            List<Throwable> suppressed = new ArrayList<>();
            ZonedDateTime result = deserialize(val, DateTimeFormatter.ISO_ZONED_DATE_TIME, suppressed);
            if (result != null) {
                return result;
            }
            result = deserialize(val, DateTimeFormatter.ISO_OFFSET_DATE_TIME, suppressed);
            if (result != null) {
                return result;
            }
            IllegalArgumentException iae = new IllegalArgumentException(ex);
            suppressed.forEach(iae::addSuppressed);
            throw iae;
        }
    }

    protected ZonedDateTime deserialize(String val, DateTimeFormatter dateTimeFormatter, List<Throwable> suppressed) {
        try {
            if (!dateTimeFormatter.equals(this.dateTimeFormatter)) {
                return ZonedDateTime.parse(val, dateTimeFormatter);
            }
        } catch (DateTimeParseException e) {
            suppressed.add(e);
        }
        return null;
    }

    @Override
    public String serialize(ZonedDateTime val) {
        if (val == null) {
            return null;
        }
        return val.format(this.dateTimeFormatter);
    }
}
