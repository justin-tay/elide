/*
 * Copyright 2025, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Include
@Entity
@Data
public class LazyGroup {

    @Id
    @GeneratedValue
    private Long id;

    private String description;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private List<LazyProduct> products = new ArrayList<>();
}
