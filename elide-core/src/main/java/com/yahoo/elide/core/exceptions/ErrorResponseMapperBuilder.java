/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.exceptions;

import com.yahoo.elide.ElideErrorResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builder for creating an ErrorResponseMapper.
 *
 * @see ErrorResponseMapper
 */
public class ErrorResponseMapperBuilder {
    private final List<ErrorResponseMapper> errorResponseMappers = new ArrayList<>();

    public ErrorResponseMapperBuilder errorResponseMapper(ErrorResponseMapper errorResponseMapper) {
        this.errorResponseMappers.add(errorResponseMapper);
        return this;
    }

    public ErrorResponseMapperBuilder errorResponseMappers(Consumer<List<ErrorResponseMapper>> errorResponseMappers) {
        errorResponseMappers.accept(this.errorResponseMappers);
        return this;
    }

    public ErrorResponseMapper build() {
        return (exception, verbose) -> {
          for (ErrorResponseMapper errorResponseMapper : this.errorResponseMappers) {
              ElideErrorResponse response = errorResponseMapper.map(exception, verbose);
              if (response != null) {
                  return response;
              }
          }
          return null;
        };
    }
}
