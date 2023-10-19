/*
 * Copyright 2023, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bean with Map of Entity.
 */
@Entity
@Table(name = "map_entity")
@Include
public class MapEntity {
    private long id;
    private String name;

    private Map<String, MapEntry> entries = new LinkedHashMap<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @OneToMany(mappedBy = "mapEntityId")
    @MapKey(name = "name")
    public Map<String, MapEntry> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, MapEntry> entries) {
        this.entries = entries;
    }
}
