/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

/**
 * Maps the body in the ElideResponse to another typically for JSON conversion
 * with a specific object mapper.
 *
 * @param <T> body type
 * @param <R> mapped type
 */
public interface ElideResponseBodyMapper<T, R> {
    /**
     * Maps the response body to another object.
     *
     * @param body the input body
     * @return the mapped body
     * @throws Exception the exception
     */
    R map(T body) throws Exception;

    /**
     * Returns a mapper that always returns its input.
     *
     * @param <T>
     * @return the mapper
     */
    public static <T> ElideResponseBodyMapper<T, T> identity() {
        return t -> t;
    }
}
