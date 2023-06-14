/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

/**
 * Maps the body in the ElideResponse to another typically for JSON conversion
 * with a specific object mapper.
 */
public interface ElideResponseBodyMapper {
    /**
     * Maps the response body to another object.
     *
     * @param body the input body
     * @return the mapped body
     * @throws Exception the exception
     */
    Object map(Object body) throws Exception;
}
