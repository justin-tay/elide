/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils.coerce.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Test for ZonedDateTimeSerde.
 */
class ZonedDateTimeSerdeTest {

    @Test
    void deserializeNull() {
        ZonedDateTimeSerde serde = new ZonedDateTimeSerde(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        ZonedDateTime zonedDateTime = serde.deserialize(null);
        assertNull(zonedDateTime);
    }

    @Test
    void deserializeEmptyString() {
        ZonedDateTimeSerde serde = new ZonedDateTimeSerde(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        ZonedDateTime zonedDateTime = serde.deserialize("");
        assertNull(zonedDateTime);
    }

    @Test
    void deserializeWithZone() {
        ZonedDateTimeSerde serde = new ZonedDateTimeSerde(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        ZonedDateTime zonedDateTime = serde.deserialize("2007-12-03T10:15:30+01:00[Europe/Paris]");
        assertEquals(ZoneId.of("Europe/Paris"), zonedDateTime.getZone());
        assertEquals(2007, zonedDateTime.getYear());
        assertEquals(12, zonedDateTime.getMonthValue());
        assertEquals(3, zonedDateTime.getDayOfMonth());
        assertEquals(10, zonedDateTime.getHour());
        assertEquals(15, zonedDateTime.getMinute());
        assertEquals(30, zonedDateTime.getSecond());
    }

    @Test
    void deserializeWithoutZone() {
        ZonedDateTimeSerde serde = new ZonedDateTimeSerde(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        ZonedDateTime zonedDateTime = serde.deserialize("2007-12-03T10:15:30+01:00");
        assertEquals(ZoneId.of("+01:00"), zonedDateTime.getZone());
        assertEquals(2007, zonedDateTime.getYear());
        assertEquals(12, zonedDateTime.getMonthValue());
        assertEquals(3, zonedDateTime.getDayOfMonth());
        assertEquals(10, zonedDateTime.getHour());
        assertEquals(15, zonedDateTime.getMinute());
        assertEquals(30, zonedDateTime.getSecond());
    }

    @Test
    void deserializeOffsetDateTimeWithZone() {
        ZonedDateTimeSerde serde = new ZonedDateTimeSerde(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        ZonedDateTime zonedDateTime = serde.deserialize("2007-12-03T10:15:30+01:00[Europe/Paris]");
        assertEquals(ZoneId.of("Europe/Paris"), zonedDateTime.getZone());
        assertEquals(2007, zonedDateTime.getYear());
        assertEquals(12, zonedDateTime.getMonthValue());
        assertEquals(3, zonedDateTime.getDayOfMonth());
        assertEquals(10, zonedDateTime.getHour());
        assertEquals(15, zonedDateTime.getMinute());
        assertEquals(30, zonedDateTime.getSecond());
    }

    @Test
    void deserializeOffsetDateTimeWithoutZone() {
        ZonedDateTimeSerde serde = new ZonedDateTimeSerde(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        ZonedDateTime zonedDateTime = serde.deserialize("2007-12-03T10:15:30+01:00");
        assertEquals(ZoneId.of("+01:00"), zonedDateTime.getZone());
        assertEquals(2007, zonedDateTime.getYear());
        assertEquals(12, zonedDateTime.getMonthValue());
        assertEquals(3, zonedDateTime.getDayOfMonth());
        assertEquals(10, zonedDateTime.getHour());
        assertEquals(15, zonedDateTime.getMinute());
        assertEquals(30, zonedDateTime.getSecond());
    }

    @Test
    void serializeNull() {
        ZonedDateTimeSerde serde = new ZonedDateTimeSerde();
        String result = serde.serialize(null);
        assertNull(result);
    }

    @Test
    void serializeWithZone() {
        ZonedDateTime time = ZonedDateTime.of(LocalDate.of(2007, 12, 3), LocalTime.of(10, 15, 30),
                ZoneId.of("Europe/Paris"));
        ZonedDateTimeSerde serde = new ZonedDateTimeSerde(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        String result = serde.serialize(time);
        assertEquals("2007-12-03T10:15:30+01:00[Europe/Paris]", result);
    }

    @Test
    void serializeWithoutZone() {
        ZonedDateTime time = ZonedDateTime.of(LocalDate.of(2007, 12, 3), LocalTime.of(10, 15, 30),
                ZoneId.of("Europe/Paris"));
        ZonedDateTimeSerde serde = new ZonedDateTimeSerde(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String result = serde.serialize(time);
        assertEquals("2007-12-03T10:15:30+01:00", result);
    }

    @Test
    void serializeWithDefault() {
        ZonedDateTime time = ZonedDateTime.of(LocalDate.of(2007, 12, 3), LocalTime.of(10, 15, 30),
                ZoneId.of("Europe/Paris"));
        ZonedDateTimeSerde serde = new ZonedDateTimeSerde();
        String result = serde.serialize(time);
        assertEquals("2007-12-03T10:15:30+01:00", result);
    }

    @Test
    void failsParsingWithIllegalArgumentException() {
        ZonedDateTimeSerde serde = new ZonedDateTimeSerde(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        assertThrows(IllegalArgumentException.class, () -> serde.deserialize("2019-06-01T09:42:55.12X3Z"));
    }

    @Test
    void failsParsingWithIllegalArgumentExceptionWithoutZone() {
        ZonedDateTimeSerde serde = new ZonedDateTimeSerde(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertThrows(IllegalArgumentException.class, () -> serde.deserialize("2019-06-01T09:42:55.12X3Z"));
    }
}
