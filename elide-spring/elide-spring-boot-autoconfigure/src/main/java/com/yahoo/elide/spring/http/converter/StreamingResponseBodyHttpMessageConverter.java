/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.http.converter;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;

/**
 * A {@link HttpMessageConverter} implementation for processing
 * {@link StreamingResponseBody}.
 * <p>
 * The
 * {@link org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBodyReturnValueHandler}
 * only processes methods with a fixed method signature.
 */
public class StreamingResponseBodyHttpMessageConverter extends AbstractHttpMessageConverter<StreamingResponseBody> {
    public StreamingResponseBodyHttpMessageConverter() {
        super(MediaType.ALL);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return StreamingResponseBody.class.isAssignableFrom(clazz);
    }

    @Override
    protected StreamingResponseBody readInternal(Class<? extends StreamingResponseBody> clazz,
            HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return outputStream -> inputMessage.getBody().transferTo(outputStream);
    }

    @Override
    protected void writeInternal(StreamingResponseBody t, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        t.writeTo(outputMessage.getBody());
    }
}
