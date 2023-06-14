/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Streaming response body.
 *
 * @see jakarta.ws.rs.core.StreamingOutput
 * @see org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
 */
@FunctionalInterface
public interface ElideStreamingBody {
    /**
     * Callback to write to the output stream.
     *
     * @param outputStream the output stream
     * @throws IOException the exception
     */
    void writeTo(OutputStream outputStream) throws IOException;
}
