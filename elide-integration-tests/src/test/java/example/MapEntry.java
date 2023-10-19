/*
 * Copyright 2023, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Map entry.
 */
@Entity
@Table(name = "map_entry")
@Include
public class MapEntry extends BaseId {
    private String name;
    private String value;
    private long mapEntityId;

    public long getMapEntityId() {
        return mapEntityId;
    }

    public void setMapEntityId(long mapEntityId) {
        this.mapEntityId = mapEntityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
