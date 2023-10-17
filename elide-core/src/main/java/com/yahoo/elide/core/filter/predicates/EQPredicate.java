/*
 * Copyright 2023, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.predicates;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.filter.Operator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * EQ Predicate class.
 */
public class EQPredicate extends FilterPredicate {

    public EQPredicate(Path path, List<Object> values) {
        super(path, Operator.EQ, values);
    }

    public <T> EQPredicate(Path path, T a) {
        this(path, Arrays.asList(a));
    }

    public EQPredicate(PathElement pathElement, List<Object> values) {
        this(new Path(Collections.singletonList(pathElement)), values);
    }

    public <T> EQPredicate(PathElement pathElement, T a) {
        this(pathElement, Arrays.asList(a));
    }
}
