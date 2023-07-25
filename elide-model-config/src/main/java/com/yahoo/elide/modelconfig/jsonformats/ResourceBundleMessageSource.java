/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.jsonformats;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * {@link MessageSource} that retrieves messages from a {@link ResourceBundle}.
 */
public class ResourceBundleMessageSource implements MessageSource {

    /**
     * Resource Bundle Cache. baseName -> locale -> resourceBundle.
     */
    private Map<String, Map<Locale, ResourceBundle>> resourceBundleMap = new ConcurrentHashMap<>();

    /**
     * Message Cache. locale -> key -> message.
     */
    private Map<Locale, Map<String, String>> messageMap = new ConcurrentHashMap<>();

    /**
     * Message Format Cache. locale -> message -> messageFormat.
     * <p>
     * Note that Message Format is not threadsafe.
     */
    private Map<Locale, Map<String, MessageFormat>> messageFormatMap = new ConcurrentHashMap<>();

    private final List<String> baseNames;

    public ResourceBundleMessageSource(String... baseName) {
        this.baseNames = Arrays.asList(baseName);
    }

    @Override
    public String getMessage(String key, Supplier<String> defaultMessage, Locale locale, Object... arguments) {
        String message = getMessageFromCache(locale, key);
        if (message.isEmpty() && defaultMessage != null) {
            message = defaultMessage.get();
        }
        if (message.isEmpty()) {
            // Fallback on message key
            message = key;
        }
        if (arguments == null || arguments.length == 0) {
            // When no arguments just return the message without formatting
            return message;
        }
        MessageFormat messageFormat = getMessageFormat(locale, message);
        synchronized (messageFormat) {
            // Synchronized block on messageFormat as it is not threadsafe
            return messageFormat.format(arguments, new StringBuffer(), null).toString();
        }
    }

    protected MessageFormat getMessageFormat(Locale locale, String message) {
        Map<String, MessageFormat> map = messageFormatMap.computeIfAbsent(locale, l -> {
            return new ConcurrentHashMap<>();
        });
        return map.computeIfAbsent(message, m -> {
            return new MessageFormat(m, locale);
        });
    }


    /**
     * Gets the message from cache or the resource bundles. Returns an empty string
     * if not found.
     *
     * @param locale the locale
     * @param key the message key
     * @return the message
     */
    protected String getMessageFromCache(Locale locale, String key) {
        Map<String, String> map = messageMap.computeIfAbsent(locale, l -> new ConcurrentHashMap<>());
        return map.computeIfAbsent(key, k -> {
            return resolveMessage(locale, k);
        });
    }

    /**
     * Gets the message from the resource bundles. Returns an empty string if not
     * found.
     *
     * @param locale the locale
     * @param key the message key
     * @return the message
     */
    protected String resolveMessage(Locale locale, String key) {
        Optional<String> optionalPattern = this.baseNames.stream()
                .map(baseName -> getResourceBundle(baseName, locale))
                .filter(Objects::nonNull)
                .map(resourceBundle -> {
                    try {
                        return resourceBundle.getString(key);
                    } catch (MissingResourceException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst();
        return optionalPattern.orElse("");
    }

    protected Map<Locale, ResourceBundle> getResourceBundle(String baseName) {
        return resourceBundleMap.computeIfAbsent(baseName, key -> {
           return new ConcurrentHashMap<>();
        });
    }

    protected ResourceBundle getResourceBundle(String baseName, Locale locale) {
        return getResourceBundle(baseName).computeIfAbsent(locale, key -> {
            try {
                return ResourceBundle.getBundle(baseName, key);
            } catch (MissingResourceException e) {
                return null;
            }
        });
    }
}
