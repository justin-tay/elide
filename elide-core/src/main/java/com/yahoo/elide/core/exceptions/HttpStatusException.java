/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.yahoo.elide.ElideErrorResponse;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Superclass for exceptions that return a Http error status.
 * Creates fast exceptions without stack traces since filter checks can throw many of these.
 */
@Slf4j
public abstract class HttpStatusException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    protected final int status;
    private final Optional<Supplier<String>> verboseMessageSupplier;

    protected HttpStatusException(int status, String message) {
        this(status, message, (Throwable) null, null);
    }

    protected HttpStatusException(int status, String message, Throwable cause,
            Supplier<String> verboseMessageSupplier) {
        super(message, cause, true, log.isTraceEnabled());
        this.status = status;
        this.verboseMessageSupplier = Optional.ofNullable(verboseMessageSupplier);
    }

    protected static String formatExceptionCause(Throwable e) {
        // if the throwable has a cause use that, otherwise the throwable is the cause
        Throwable error = e.getCause() == null ? e : e.getCause();
        return error != null
                ? StringUtils.defaultIfBlank(error.getMessage(), error.toString())
                : null;
    }

    /**
     * Get a response detailing the error that occurred.
     * Encode the error message to be safe for HTML.
     * @return Pair containing status code and a JsonNode containing error details
     */
    public ElideErrorResponse getErrorResponse() {
        return buildResponse(getMessage());
    }

    /**
     * Get a verbose response detailing the error that occurred.
     * Encode the error message to be safe for HTML.
     * @return Pair containing status code and a JsonNode containing error details
     */
    public ElideErrorResponse getVerboseErrorResponse() {
        return buildResponse(getVerboseMessage());
    }

    private ElideErrorResponse buildResponse(String message) {
        return ElideErrorResponse.builder().responseCode(getStatus())
                .errors(errors -> errors.error(error -> error.message(message))).build();
    }

    public String getVerboseMessage() {
        String result = verboseMessageSupplier.map(Supplier::get)
                .orElseGet(this::getMessage);

        if (result.equals(this.getMessage())) {
            return result;
        } else {

            //return both the message and the verbose message.
            return this.getMessage() + "\n" + result;
        }
    }

    public int getStatus() {
        return status;
    }

    @Override
    public String toString() {
        String message = getMessage();
        String className = EntityDictionary.getSimpleName(ClassType.of(getClass()));

        if (message == null) {
            message = className;
        } else {
            message = className + ": " + message;
        }

        return message;
    }
}
