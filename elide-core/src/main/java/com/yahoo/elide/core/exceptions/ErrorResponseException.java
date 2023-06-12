/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.yahoo.elide.ElideErrorResponse;
import com.yahoo.elide.ElideErrors;

import java.util.Objects;

/**
 * Define your business exception extend this.
 */
public class ErrorResponseException extends HttpStatusException {
    private static final long serialVersionUID = 1L;

    private final ElideErrors errors;

    /**
     * Constructor.
     *
     * @param status http status
     * @param message exception message
     * @param errors custom error objects, not {@code null}
     */
    public ErrorResponseException(int status, String message, ElideErrors errors) {
        this(status, message, null, errors);
    }

    /**
     * Constructor.
     *
     * @param status http status
     * @param message exception message
     * @param cause the cause
     * @param errorObjects custom error objects, not {@code null}
     */
    public ErrorResponseException(int status, String message, Throwable cause, ElideErrors errors) {
        super(status, message, cause, null);
        this.errors = Objects.requireNonNull(errors, "errors must not be null");
    }

    @Override
    public ElideErrorResponse<?> getErrorResponse() {
        return buildCustomResponse();
    }

    @Override
    public ElideErrorResponse<?> getVerboseErrorResponse() {
        return buildCustomResponse();
    }

    private ElideErrorResponse<?> buildCustomResponse() {
        return ElideErrorResponse.status(getStatus()).body(this.errors);
    }
}
