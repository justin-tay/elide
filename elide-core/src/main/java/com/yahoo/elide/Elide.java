/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore;
import com.yahoo.elide.core.dictionary.Injector;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.ExceptionMappers;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.jsonapi.DefaultJsonApiErrorMapper;
import com.yahoo.elide.jsonapi.DefaultJsonApiExceptionHandler;
import com.yahoo.elide.jsonapi.EntityProjectionMaker;
import com.yahoo.elide.jsonapi.JsonApi;
import com.yahoo.elide.jsonapi.JsonApiErrorContext;
import com.yahoo.elide.jsonapi.JsonApiExceptionHandler;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.extensions.JsonApiAtomicOperations;
import com.yahoo.elide.jsonapi.extensions.JsonApiAtomicOperationsRequestScope;
import com.yahoo.elide.jsonapi.extensions.JsonApiJsonPatch;
import com.yahoo.elide.jsonapi.extensions.JsonApiJsonPatchRequestScope;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.parser.BaseVisitor;
import com.yahoo.elide.jsonapi.parser.DeleteVisitor;
import com.yahoo.elide.jsonapi.parser.GetVisitor;
import com.yahoo.elide.jsonapi.parser.JsonApiParser;
import com.yahoo.elide.jsonapi.parser.PatchVisitor;
import com.yahoo.elide.jsonapi.parser.PostVisitor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import jakarta.ws.rs.core.MultivaluedMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * REST Entry point handler.
 */
@Slf4j
public class Elide {
    public static final String JSONAPI_CONTENT_TYPE = JsonApi.MEDIA_TYPE;
    public static final String JSONAPI_CONTENT_TYPE_WITH_JSON_PATCH_EXTENSION =
            JsonApi.JsonPatch.MEDIA_TYPE;

    @Getter private final ElideSettings elideSettings;
    @Getter private final AuditLogger auditLogger;
    @Getter private final DataStore dataStore;
    @Getter private final JsonApiMapper mapper;
    @Getter private final ExceptionMappers exceptionMappers;
    @Getter private final TransactionRegistry transactionRegistry;
    @Getter private final ClassScanner scanner;
    private boolean initialized = false;
    private JsonApiExceptionHandler jsonApiExceptionHandler;

    /**
     * Instantiates a new Elide instance.
     *
     * @param elideSettings Elide settings object.
     */
    public Elide(
            ElideSettings elideSettings
    ) {
        this(elideSettings, new TransactionRegistry(), elideSettings.getDictionary().getScanner(), false);
    }

    /**
     * Instantiates a new Elide instance.
     *
     * @param elideSettings Elide settings object.
     * @param transactionRegistry Global transaction state.
     */
    public Elide(
            ElideSettings elideSettings,
            TransactionRegistry transactionRegistry
    ) {
        this(elideSettings, transactionRegistry, elideSettings.getDictionary().getScanner(), false);
    }

    /**
     * Instantiates a new Elide instance.
     *
     * @param elideSettings Elide settings object.
     * @param transactionRegistry Global transaction state.
     * @param scanner Scans classes for Elide annotations.
     * @param doScans Perform scans now.
     */
    public Elide(
            ElideSettings elideSettings,
            TransactionRegistry transactionRegistry,
            ClassScanner scanner,
            boolean doScans
    ) {
        this.elideSettings = elideSettings;
        this.scanner = scanner;
        this.auditLogger = elideSettings.getAuditLogger();
        this.dataStore = new InMemoryDataStore(elideSettings.getDataStore());
        this.mapper = elideSettings.getMapper();
        this.exceptionMappers = elideSettings.getExceptionMappers();
        this.transactionRegistry = transactionRegistry;

        this.jsonApiExceptionHandler = new DefaultJsonApiExceptionHandler(this.exceptionMappers, this.mapper,
                new DefaultJsonApiErrorMapper());

        if (doScans) {
            doScans();
        }
    }

    /**
     * Scans & binds Elide models, scans for security check definitions, serde definitions, life cycle hooks
     * and more.  Any dependency injection required by objects found from scans must be performed prior to this call.
     */
    public void doScans() {
        if (! initialized) {
            elideSettings.getSerdes().forEach((type, serde) -> registerCustomSerde(type, serde, type.getSimpleName()));
            registerCustomSerde();

            //Scan for security checks prior to populating data stores in case they need them.
            elideSettings.getDictionary().scanForSecurityChecks();

            this.dataStore.populateEntityDictionary(elideSettings.getDictionary());
            initialized = true;
        }
    }

    protected void registerCustomSerde() {
        Injector injector = elideSettings.getDictionary().getInjector();
        Set<Class<?>> classes = registerCustomSerdeScan();

        for (Class<?> clazz : classes) {
            if (!Serde.class.isAssignableFrom(clazz)) {
                log.warn("Skipping Serde registration (not a Serde!): {}", clazz);
                continue;
            }
            Serde serde = (Serde) injector.instantiate(clazz);
            injector.inject(serde);

            ElideTypeConverter converter = clazz.getAnnotation(ElideTypeConverter.class);
            Class baseType = converter.type();
            registerCustomSerde(baseType, serde, converter.name());

            for (Class type : converter.subTypes()) {
                if (!baseType.isAssignableFrom(type)) {
                    throw new IllegalArgumentException("Mentioned type " + type
                            + " not subtype of " + baseType);
                }
                registerCustomSerde(type, serde, converter.name());
            }
        }
    }

    protected void registerCustomSerde(Class<?> type, Serde serde, String name) {
        log.info("Registering serde for type : {}", type);
        CoerceUtil.register(type, serde);
        registerCustomSerdeInObjectMapper(type, serde, name);
    }

    protected void registerCustomSerdeInObjectMapper(Class<?> type, Serde serde, String name) {
        ObjectMapper objectMapper = mapper.getObjectMapper();
        objectMapper.registerModule(new SimpleModule(name)
                .addSerializer(type, new JsonSerializer<Object>() {
                    @Override
                    public void serialize(Object obj, JsonGenerator jsonGenerator,
                                          SerializerProvider serializerProvider)
                            throws IOException, JsonProcessingException {
                        jsonGenerator.writeObject(serde.serialize(obj));
                    }
                }));
    }

    protected Set<Class<?>> registerCustomSerdeScan() {
        return scanner.getAnnotatedClasses(ElideTypeConverter.class);
    }

    /**
     * Handle GET.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param queryParams the query params
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @return Elide response object
     */
    public ElideResponse<String> get(String baseUrlEndPoint, String path, MultivaluedMap<String, String> queryParams,
                             User opaqueUser, String apiVersion) {
        return get(baseUrlEndPoint, path, queryParams, opaqueUser, apiVersion, UUID.randomUUID());
    }

    /**
     * Handle GET.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param queryParams the query params
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse<String> get(String baseUrlEndPoint, String path, MultivaluedMap<String, String> queryParams,
                             User opaqueUser, String apiVersion, UUID requestId) {
        return get(baseUrlEndPoint, path, queryParams, Collections.emptyMap(), opaqueUser, apiVersion, requestId);
    }

    /**
     * Handle GET.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param queryParams the query params
     * @param requestHeaders the request headers
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse<String> get(String baseUrlEndPoint, String path, MultivaluedMap<String, String> queryParams,
                             Map<String, List<String>> requestHeaders, User opaqueUser, String apiVersion,
                             UUID requestId) {
        if (elideSettings.isStrictQueryParams()) {
            try {
                verifyQueryParams(queryParams);
            } catch (BadRequestException e) {
                JsonApiErrorContext errorContext = JsonApiErrorContext.builder().mapper(this.mapper).verbose(false)
                        .build();
                ElideResponse<?> errorResponse = jsonApiExceptionHandler.handleException(e, errorContext);
                return toResponse(errorResponse.getStatus(), errorResponse.getBody());

            }
        }
        return handleRequest(true, opaqueUser, dataStore::beginReadTransaction, requestId, (tx, user) -> {
            JsonApiDocument jsonApiDoc = new JsonApiDocument();
            RequestScope requestScope = new RequestScope(baseUrlEndPoint, path, apiVersion, jsonApiDoc,
                    tx, user, queryParams, requestHeaders, requestId, elideSettings);
            requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getDictionary(),
                    requestScope).parsePath(path));
            BaseVisitor visitor = new GetVisitor(requestScope);
            return visit(path, requestScope, visitor);
        });
    }

    /**
     * Handle POST.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @return Elide response object
     */
    public ElideResponse<String> post(String baseUrlEndPoint, String path, String jsonApiDocument,
                              User opaqueUser, String apiVersion) {
        return post(baseUrlEndPoint, path, jsonApiDocument, null, opaqueUser, apiVersion, UUID.randomUUID());
    }

    /**
     * Handle POST.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param queryParams the query params
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse<String> post(String baseUrlEndPoint, String path, String jsonApiDocument,
                              MultivaluedMap<String, String> queryParams,
                              User opaqueUser, String apiVersion, UUID requestId) {
        return post(baseUrlEndPoint, path, jsonApiDocument, queryParams, Collections.emptyMap(),
                    opaqueUser, apiVersion, requestId);
    }

    /**
     * Handle POST.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param queryParams the query params
     * @param requestHeaders the request headers
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse<String> post(String baseUrlEndPoint, String path, String jsonApiDocument,
                              MultivaluedMap<String, String> queryParams, Map<String, List<String>> requestHeaders,
                              User opaqueUser, String apiVersion, UUID requestId) {
        return handleRequest(false, opaqueUser, dataStore::beginTransaction, requestId, (tx, user) -> {
            JsonApiDocument jsonApiDoc = mapper.readJsonApiDocument(jsonApiDocument);
            RequestScope requestScope = new RequestScope(baseUrlEndPoint, path, apiVersion,
                    jsonApiDoc, tx, user, queryParams, requestHeaders, requestId, elideSettings);
            requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getDictionary(),
                    requestScope).parsePath(path));
            BaseVisitor visitor = new PostVisitor(requestScope);
            return visit(path, requestScope, visitor);
        });
    }

    /**
     * Handle PATCH.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param contentType the content type
     * @param accept the accept
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @return Elide response object
     */
    public ElideResponse<String> patch(String baseUrlEndPoint, String contentType, String accept,
                               String path, String jsonApiDocument,
                               User opaqueUser, String apiVersion) {
        return patch(baseUrlEndPoint, contentType, accept, path, jsonApiDocument,
                     null, opaqueUser, apiVersion, UUID.randomUUID());
    }

    /**
     * Handle PATCH.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param contentType the content type
     * @param accept the accept
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param queryParams the query params
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse<String> patch(String baseUrlEndPoint, String contentType, String accept,
                               String path, String jsonApiDocument, MultivaluedMap<String, String> queryParams,
                               User opaqueUser, String apiVersion, UUID requestId) {

        return patch(baseUrlEndPoint, contentType, accept, path, jsonApiDocument, queryParams,
                null, opaqueUser, apiVersion, requestId);
    }

    /**
     * Handle PATCH.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param contentType the content type
     * @param accept the accept
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param queryParams the query params
     * @param requestHeaders the request headers
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse<String> patch(String baseUrlEndPoint, String contentType, String accept,
                               String path, String jsonApiDocument, MultivaluedMap<String, String> queryParams,
                               Map<String, List<String>> requestHeaders, User opaqueUser,
                               String apiVersion, UUID requestId) {

        Handler<DataStoreTransaction, User, HandlerResult> handler;
        if (JsonApiJsonPatch.isPatchExtension(contentType) && JsonApiJsonPatch.isPatchExtension(accept)) {
            handler = (tx, user) -> {
                JsonApiJsonPatchRequestScope requestScope = new JsonApiJsonPatchRequestScope(baseUrlEndPoint, path,
                        apiVersion, tx, user, requestId, queryParams, requestHeaders, elideSettings);
                try {
                    Supplier<Pair<Integer, JsonNode>> responder =
                            JsonApiJsonPatch.processJsonPatch(dataStore, path, jsonApiDocument, requestScope);
                    return new HandlerResult(requestScope, responder);
                } catch (RuntimeException e) {
                    return new HandlerResult(requestScope, e);
                }
            };
        } else {
            handler = (tx, user) -> {
                JsonApiDocument jsonApiDoc = mapper.readJsonApiDocument(jsonApiDocument);

                RequestScope requestScope = new RequestScope(baseUrlEndPoint, path, apiVersion, jsonApiDoc,
                        tx, user, queryParams, requestHeaders, requestId, elideSettings);
                requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getDictionary(),
                        requestScope).parsePath(path));
                BaseVisitor visitor = new PatchVisitor(requestScope);
                return visit(path, requestScope, visitor);
            };
        }

        return handleRequest(false, opaqueUser, dataStore::beginTransaction, requestId, handler);
    }

    /**
     * Handle DELETE.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @return Elide response object
     */
    public ElideResponse<String> delete(String baseUrlEndPoint, String path, String jsonApiDocument,
                                User opaqueUser, String apiVersion) {
        return delete(baseUrlEndPoint, path, jsonApiDocument, null, opaqueUser, apiVersion, UUID.randomUUID());
    }

    /**
     * Handle DELETE.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param queryParams the query params
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse<String> delete(String baseUrlEndPoint, String path, String jsonApiDocument,
                                MultivaluedMap<String, String> queryParams,
                                User opaqueUser, String apiVersion, UUID requestId) {
        return delete(baseUrlEndPoint, path, jsonApiDocument, queryParams, Collections.emptyMap(),
                      opaqueUser, apiVersion, requestId);
    }

    /**
     * Handle DELETE.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param queryParams the query params
     * @param requestHeaders the request headers
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse<String> delete(String baseUrlEndPoint, String path, String jsonApiDocument,
                                MultivaluedMap<String, String> queryParams,
                                Map<String, List<String>> requestHeaders,
                                User opaqueUser, String apiVersion, UUID requestId) {
        return handleRequest(false, opaqueUser, dataStore::beginTransaction, requestId, (tx, user) -> {
            JsonApiDocument jsonApiDoc = StringUtils.isEmpty(jsonApiDocument)
                    ? new JsonApiDocument()
                    : mapper.readJsonApiDocument(jsonApiDocument);
            RequestScope requestScope = new RequestScope(baseUrlEndPoint, path, apiVersion, jsonApiDoc,
                    tx, user, queryParams, requestHeaders, requestId, elideSettings);
            requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getDictionary(),
                    requestScope).parsePath(path));
            BaseVisitor visitor = new DeleteVisitor(requestScope);
            return visit(path, requestScope, visitor);
        });
    }

    /**
     * Handle operations for the Atomic Operations extension.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param contentType the content type
     * @param accept the accept
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @return Elide response object
     */
    public ElideResponse<String> operations(String baseUrlEndPoint, String contentType, String accept,
                               String path, String jsonApiDocument,
                               User opaqueUser, String apiVersion) {
        return operations(baseUrlEndPoint, contentType, accept, path, jsonApiDocument,
                     null, opaqueUser, apiVersion, UUID.randomUUID());
    }

    /**
     * Handle operations for the Atomic Operations extension.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param contentType the content type
     * @param accept the accept
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param queryParams the query params
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse<String> operations(String baseUrlEndPoint, String contentType, String accept, String path,
            String jsonApiDocument, MultivaluedMap<String, String> queryParams, User opaqueUser, String apiVersion,
            UUID requestId) {
        return operations(baseUrlEndPoint, contentType, accept, path, jsonApiDocument, queryParams, null, opaqueUser,
                apiVersion, requestId);
    }

    /**
     * Handle operations for the Atomic Operations extension.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param contentType the content type
     * @param accept the accept
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param queryParams the query params
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     * @return
     */
    public ElideResponse<String> operations(String baseUrlEndPoint, String contentType, String accept, String path,
            String jsonApiDocument, MultivaluedMap<String, String> queryParams,
            Map<String, List<String>> requestHeaders, User opaqueUser, String apiVersion, UUID requestId) {

        Handler<DataStoreTransaction, User, HandlerResult> handler;
        if (JsonApiAtomicOperations.isAtomicOperationsExtension(contentType)
                && JsonApiAtomicOperations.isAtomicOperationsExtension(accept)) {
            handler = (tx, user) -> {
                JsonApiAtomicOperationsRequestScope requestScope = new JsonApiAtomicOperationsRequestScope(
                        baseUrlEndPoint, path,
                        apiVersion, tx, user, requestId, queryParams, requestHeaders, elideSettings);
                try {
                    Supplier<Pair<Integer, JsonNode>> responder = JsonApiAtomicOperations
                            .processAtomicOperations(dataStore, path, jsonApiDocument, requestScope);
                    return new HandlerResult(requestScope, responder);
                } catch (RuntimeException e) {
                    return new HandlerResult(requestScope, e);
                }
            };
        } else {
            return new ElideResponse<>(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type");
        }

        return handleRequest(false, opaqueUser, dataStore::beginTransaction, requestId, handler);
    }

    public HandlerResult visit(String path, RequestScope requestScope, BaseVisitor visitor) {
        try {
            Supplier<Pair<Integer, JsonApiDocument>> responder = visitor.visit(JsonApiParser.parse(path));
            return new HandlerResult(requestScope, responder);
        } catch (RuntimeException e) {
            return new HandlerResult(requestScope, e);
        }
    }

    /**
     * Handle JSON API requests.
     *
     * @param isReadOnly if the transaction is read only
     * @param user the user object from the container
     * @param transaction a transaction supplier
     * @param requestId the Request ID
     * @param handler a function that creates the request scope and request handler
     * @param <T> The response type (JsonNode or JsonApiDocument)
     * @return the response
     */
    protected <T> ElideResponse<String> handleRequest(boolean isReadOnly, User user,
                                          Supplier<DataStoreTransaction> transaction, UUID requestId,
                                          Handler<DataStoreTransaction, User, HandlerResult> handler) {
        JsonApiErrorContext errorContext = JsonApiErrorContext.builder().mapper(this.mapper).verbose(false).build();
        try (DataStoreTransaction tx = transaction.get()) {
            transactionRegistry.addRunningTransaction(requestId, tx);
            HandlerResult result = handler.handle(tx, user);
            RequestScope requestScope = result.getRequestScope();
            errorContext = JsonApiErrorContext.builder().mapper(this.mapper)
                    .verbose(requestScope.getPermissionExecutor().isVerbose()).build();
            Supplier<Pair<Integer, T>> responder = result.getResponder();
            tx.preCommit(requestScope);
            requestScope.runQueuedPreSecurityTriggers();
            requestScope.getPermissionExecutor().executeCommitChecks();
            requestScope.runQueuedPreFlushTriggers();
            if (!isReadOnly) {
                requestScope.saveOrCreateObjects();
            }
            tx.flush(requestScope);

            requestScope.runQueuedPreCommitTriggers();

            ElideResponse<String> response = buildResponse(responder.get());

            auditLogger.commit();
            tx.commit(requestScope);
            requestScope.runQueuedPostCommitTriggers();

            if (log.isTraceEnabled()) {
                requestScope.getPermissionExecutor().logCheckStats();
            }

            return response;
        } catch (Throwable e) {
            ElideResponse<?> errorResponse = jsonApiExceptionHandler.handleException(e, errorContext);
            return toResponse(errorResponse.getStatus(), errorResponse.getBody());
        } finally {
            transactionRegistry.removeRunningTransaction(requestId);
            auditLogger.clear();
        }
    }

    protected ElideResponse<String> toResponse(int status, Object body) {
        String result = null;
        if (body instanceof String data) {
            result = data;
        } else {
            try {
                result = body != null ? this.mapper.writeJsonApiDocument(body) : null;
            } catch (JsonProcessingException e) {
                return ElideResponse.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).body(e.toString());
            }
        }
        return ElideResponse.status(status).body(result);
    }

    protected <T> ElideResponse<String> buildResponse(Pair<Integer, T> response) {
        T responseNode = response.getRight();
        Integer responseCode = response.getLeft();
        return toResponse(responseCode, responseNode);
    }

    private void verifyQueryParams(MultivaluedMap<String, String> queryParams) {
        String undefinedKeys = queryParams.keySet()
                        .stream()
                        .filter(Elide::notAValidKey)
                        .collect(Collectors.joining(", "));

        if (!undefinedKeys.isEmpty()) {
            throw new BadRequestException("Found undefined keys in request: " + undefinedKeys);
        }
    }

    private static boolean notAValidKey(String key) {
        boolean validKey = key.equals("sort")
                        || key.startsWith("filter")
                        || (key.startsWith("fields[") && key.endsWith("]"))
                        || key.startsWith("page[")
                        || key.equals(EntityProjectionMaker.INCLUDE);
        return !validKey;
    }

    /**
     * A function that sets up the request handling objects.
     *
     * @param <DataStoreTransaction> the request's transaction
     * @param <User> the request's user
     * @param <HandlerResult> the request handling objects
     */
    @FunctionalInterface
    public interface Handler<DataStoreTransaction, User, HandlerResult> {
        HandlerResult handle(DataStoreTransaction a, User b) throws IOException;
    }

    /**
     * A wrapper to return multiple values, less verbose than Pair.
     * @param <T> Response type.
     */
    protected static class HandlerResult<T> {
        protected RequestScope requestScope;
        protected Supplier<Pair<Integer, T>> result;
        protected RuntimeException cause;

        protected HandlerResult(RequestScope requestScope, Supplier<Pair<Integer, T>> result) {
            this.requestScope = requestScope;
            this.result = result;
        }

        public HandlerResult(RequestScope requestScope, RuntimeException cause) {
            this.requestScope = requestScope;
            this.cause = cause;
        }

        public Supplier<Pair<Integer, T>> getResponder() {
            if (cause != null) {
                throw cause;
            }
            return result;
        }

        public RequestScope getRequestScope() {
            return requestScope;
        }
    }
}
