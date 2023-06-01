/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.exceptions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builder for creating an ErrorMapper.
 */
public class ErrorMapperBuilder {
    private final List<ErrorMapper> errorMappers = new ArrayList<>();

    public ErrorMapperBuilder errorMapper(ErrorMapper errorMapper) {
        this.errorMappers.add(errorMapper);
        return this;
    }

    public ErrorMapperBuilder errorMappers(Consumer<List<ErrorMapper>> errorMappers) {
        errorMappers.accept(this.errorMappers);
        return this;
    }

    public ErrorMapper build() {
        return exception -> {
          for (ErrorMapper errorMapper : this.errorMappers) {
              ErrorResponseException response = errorMapper.map(exception);
              if (response != null) {
                  return response;
              }
          }
          return null;
        };
    }
}
