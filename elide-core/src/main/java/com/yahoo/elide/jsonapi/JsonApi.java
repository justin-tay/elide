/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.RefreshableElide;
import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.ErrorResponseException;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.exceptions.InvalidURLException;
import com.yahoo.elide.core.exceptions.JsonApiAtomicOperationsException;
import com.yahoo.elide.core.exceptions.JsonPatchExtensionException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.jsonapi.JsonApi.HandlerResult;
import com.yahoo.elide.jsonapi.extensions.JsonApiAtomicOperations;
import com.yahoo.elide.jsonapi.extensions.JsonApiAtomicOperationsRequestScope;
import com.yahoo.elide.jsonapi.extensions.JsonApiJsonPatch;
import com.yahoo.elide.jsonapi.extensions.JsonApiJsonPatchRequestScope;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.JsonApiError;
import com.yahoo.elide.jsonapi.models.JsonApiError.Links;
import com.yahoo.elide.jsonapi.models.JsonApiError.Source;
import com.yahoo.elide.jsonapi.models.JsonApiErrors;
import com.yahoo.elide.jsonapi.parser.BaseVisitor;
import com.yahoo.elide.jsonapi.parser.DeleteVisitor;
import com.yahoo.elide.jsonapi.parser.GetVisitor;
import com.yahoo.elide.jsonapi.parser.JsonApiParser;
import com.yahoo.elide.jsonapi.parser.PatchVisitor;
import com.yahoo.elide.jsonapi.parser.PostVisitor;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.owasp.encoder.Encode;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * JSON:API.
 */
@Slf4j
public class JsonApi {
    @Getter
    private final Elide elide;

    private final ElideSettings elideSettings;
    private final JsonApiSettings jsonApiSettings;
    private final DataStore dataStore;
    private final JsonApiMapper mapper;
    private final TransactionRegistry transactionRegistry;
    private final AuditLogger auditLogger;
    private boolean strictQueryParameters;

    public JsonApi(RefreshableElide refreshableElide) {
        this(refreshableElide.getElide());
    }

    public JsonApi(Elide elide) {
        this.elide = elide;
        this.jsonApiSettings = elide.getSettings(JsonApiSettings.class);
        this.strictQueryParameters = this.jsonApiSettings.isStrictQueryParameters();
        this.mapper = this.jsonApiSettings.getJsonApiMapper();
        this.dataStore = this.elide.getDataStore();
        this.elideSettings = this.elide.getElideSettings();
        this.transactionRegistry = this.elide.getTransactionRegistry();
        this.auditLogger = this.elide.getAuditLogger();
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
    public ElideResponse get(Route route, User opaqueUser,
                             UUID requestId) {
        UUID requestUuid = requestId != null ? requestId : UUID.randomUUID();

        if (strictQueryParameters) {
            try {
                verifyQueryParams(route.getParameters());
            } catch (BadRequestException e) {
                return buildErrorResponse(e, false);
            }
        }
        return handleRequest(true, opaqueUser, dataStore::beginReadTransaction, requestUuid, (tx, user) -> {
            JsonApiDocument jsonApiDoc = new JsonApiDocument();
            JsonApiRequestScope requestScope = JsonApiRequestScope.builder().route(route).dataStoreTransaction(tx)
                    .user(user).requestId(requestUuid).elideSettings(elideSettings).jsonApiDocument(jsonApiDoc)
                    .build();
            requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getEntityDictionary(),
                    requestScope).parsePath(route.getPath()));
            BaseVisitor visitor = new GetVisitor(requestScope);
            return visit(route.getPath(), requestScope, visitor);
        });
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
    public ElideResponse post(Route route, String jsonApiDocument,
                              User opaqueUser, UUID requestId) {
        UUID requestUuid = requestId != null ? requestId : UUID.randomUUID();

        return handleRequest(false, opaqueUser, dataStore::beginTransaction, requestUuid, (tx, user) -> {
            JsonApiDocument jsonApiDoc = mapper.readJsonApiDocument(jsonApiDocument);
            JsonApiRequestScope requestScope = JsonApiRequestScope.builder().route(route).dataStoreTransaction(tx)
                    .user(user).requestId(requestUuid).elideSettings(elideSettings).jsonApiDocument(jsonApiDoc)
                    .build();
            requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getEntityDictionary(),
                    requestScope).parsePath(route.getPath()));
            BaseVisitor visitor = new PostVisitor(requestScope);
            return visit(route.getPath(), requestScope, visitor);
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
     * @param queryParams the query params
     * @param requestHeaders the request headers
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse patch(Route route, String jsonApiDocument, User opaqueUser, UUID requestId) {
        UUID requestUuid = requestId != null ? requestId : UUID.randomUUID();

        String accept = route.getHeaders().get("accept").stream().findFirst().orElse("");
        String contentType = route.getHeaders().get("content-type").stream().findFirst().orElse("");

        Handler<DataStoreTransaction, User, HandlerResult> handler;
        if (JsonApiJsonPatch.isPatchExtension(contentType) && JsonApiJsonPatch.isPatchExtension(accept)) {
            handler = (tx, user) -> {
                JsonApiJsonPatchRequestScope requestScope = new JsonApiJsonPatchRequestScope(route, tx, user,
                        requestUuid, elideSettings);
                try {
                    Supplier<Pair<Integer, JsonNode>> responder = JsonApiJsonPatch.processJsonPatch(dataStore,
                            route.getPath(), jsonApiDocument, requestScope);
                    return new HandlerResult(requestScope, responder);
                } catch (RuntimeException e) {
                    return new HandlerResult(requestScope, e);
                }
            };
        } else {
            handler = (tx, user) -> {
                JsonApiDocument jsonApiDoc = mapper.readJsonApiDocument(jsonApiDocument);
                JsonApiRequestScope requestScope = JsonApiRequestScope.builder().route(route).dataStoreTransaction(tx)
                        .user(user).requestId(requestUuid).elideSettings(elideSettings).jsonApiDocument(jsonApiDoc)
                        .build();
                requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getEntityDictionary(),
                        requestScope).parsePath(route.getPath()));
                BaseVisitor visitor = new PatchVisitor(requestScope);
                return visit(route.getPath(), requestScope, visitor);
            };
        }

        return handleRequest(false, opaqueUser, dataStore::beginTransaction, requestUuid, handler);
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
    public ElideResponse delete(Route route, String jsonApiDocument,
                                User opaqueUser, UUID requestId) {
        UUID requestUuid = requestId != null ? requestId : UUID.randomUUID();

        return handleRequest(false, opaqueUser, dataStore::beginTransaction, requestUuid, (tx, user) -> {
            JsonApiDocument jsonApiDoc = StringUtils.isEmpty(jsonApiDocument)
                    ? new JsonApiDocument()
                    : mapper.readJsonApiDocument(jsonApiDocument);
            JsonApiRequestScope requestScope = JsonApiRequestScope.builder().route(route).dataStoreTransaction(tx)
                    .user(user).requestId(requestUuid).elideSettings(elideSettings).jsonApiDocument(jsonApiDoc).build();
            requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getEntityDictionary(),
                    requestScope).parsePath(route.getPath()));
            BaseVisitor visitor = new DeleteVisitor(requestScope);
            return visit(route.getPath(), requestScope, visitor);
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
     * @param queryParams the query params
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     * @return
     */
    public ElideResponse operations(Route route,
            String jsonApiDocument, User opaqueUser, UUID requestId) {

        UUID requestUuid = requestId != null ? requestId : UUID.randomUUID();

        String accept = route.getHeaders().get("accept").stream().findFirst().orElse("");
        String contentType = route.getHeaders().get("content-type").stream().findFirst().orElse("");

        Handler<DataStoreTransaction, User, HandlerResult> handler;
        if (JsonApiAtomicOperations.isAtomicOperationsExtension(contentType)
                && JsonApiAtomicOperations.isAtomicOperationsExtension(accept)) {
            handler = (tx, user) -> {
                JsonApiAtomicOperationsRequestScope requestScope = new JsonApiAtomicOperationsRequestScope(
                        route, tx, user, requestUuid, elideSettings);
                try {
                    Supplier<Pair<Integer, JsonNode>> responder = JsonApiAtomicOperations
                            .processAtomicOperations(dataStore, route.getPath(), jsonApiDocument, requestScope);
                    return new HandlerResult(requestScope, responder);
                } catch (RuntimeException e) {
                    return new HandlerResult(requestScope, e);
                }
            };
        } else {
            return new ElideResponse(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type");
        }

        return handleRequest(false, opaqueUser, dataStore::beginTransaction, requestUuid, handler);
    }

    public HandlerResult visit(String path, JsonApiRequestScope requestScope, BaseVisitor visitor) {
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
    protected <T> ElideResponse handleRequest(boolean isReadOnly, User user,
                                          Supplier<DataStoreTransaction> transaction, UUID requestId,
                                          Handler<DataStoreTransaction, User, HandlerResult> handler) {
        boolean isVerbose = false;
        try (DataStoreTransaction tx = transaction.get()) {
            transactionRegistry.addRunningTransaction(requestId, tx);
            HandlerResult result = handler.handle(tx, user);
            JsonApiRequestScope requestScope = result.getRequestScope();
            isVerbose = requestScope.getPermissionExecutor().isVerbose();
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

            ElideResponse response = buildResponse(responder.get());

            auditLogger.commit();
            tx.commit(requestScope);
            requestScope.runQueuedPostCommitTriggers();

            if (log.isTraceEnabled()) {
                requestScope.getPermissionExecutor().logCheckStats();
            }

            return response;
        } catch (IOException e) {
            return handleNonRuntimeException(e, isVerbose);
        } catch (RuntimeException e) {
            return handleRuntimeException(e, isVerbose);
        } finally {
            transactionRegistry.removeRunningTransaction(requestId);
            auditLogger.clear();
        }
    }

    protected ElideResponse buildErrorResponse(HttpStatusException exception, boolean isVerbose) {
        if (exception instanceof InternalServerErrorException) {
            log.error("Internal Server Error", exception);
        }

        ElideErrorResponse errorResponse = (isVerbose ? exception.getVerboseErrorResponse()
                : exception.getErrorResponse());
        if (errorResponse.getBody() != null) {
            return buildErrorResponse(errorResponse.getResponseCode(), errorResponse.getBody());
        } else {
            JsonApiErrors.JsonApiErrorsBuilder builder = JsonApiErrors.builder();
            for (ElideError error : errorResponse.getErrors().getErrors()) {
                builder.error(jsonApiError -> convertToJsonApiError(error, jsonApiError));
            }
            return buildErrorResponse(errorResponse.getResponseCode(), builder.build());
        }
    }

    protected void attribute(String key, Map<String, Object> map, Predicate<Object> processor) {
        if (map.containsKey(key) && processor.test(map.get(key))) {
            map.remove(key);
        }
    }

    protected void convertToJsonApiError(ElideError error, JsonApiError.JsonApiErrorBuilder jsonApiError) {
        if (error.getMessage() != null) {
            jsonApiError.detail(Encode.forHtml(error.getMessage()));
        }
        if (error.getAttributes() != null && !error.getAttributes().isEmpty()) {
            Map<String, Object> meta = new LinkedHashMap<>(error.getAttributes());
            attribute("id", meta, value -> {
                jsonApiError.id(value.toString());
                return true;
            });
            attribute("status", meta, value -> {
                jsonApiError.status(value.toString());
                return true;
            });
            attribute("code", meta, value -> {
                jsonApiError.code(value.toString());
                return true;
            });
            attribute("title", meta, value -> {
                jsonApiError.title(value.toString());
                return true;
            });
            attribute("source", meta, value -> {
                if (value instanceof Source source) {
                    jsonApiError.source(source);
                    return true;
                }
                return false;
            });
            attribute("links", meta, value -> {
                if (value instanceof Links links) {
                    jsonApiError.links(links);
                    return true;
                }
                return false;
            });
            if (!meta.isEmpty()) {
                jsonApiError.meta(meta);
            }
        }
    }

    protected ElideResponse buildErrorResponse(int responseCode, Object errors) {
        try {
            return new ElideResponse(responseCode, this.mapper.writeJsonApiDocument(errors));
        } catch (JsonProcessingException e) {
            return new ElideResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.toString());
        }
    }

    private ElideResponse handleNonRuntimeException(Exception exception, boolean isVerbose) {
        ErrorResponseException mappedException = mapError(exception);
        if (mappedException != null) {
            return buildErrorResponse(mappedException, isVerbose);
        }

        if (exception instanceof JacksonException jacksonException) {
            String message = (jacksonException.getLocation() != null
                    && jacksonException.getLocation().getSourceRef() != null)
                    ? exception.getMessage() //This will leak Java class info if the location isn't known.
                    : jacksonException.getOriginalMessage();

            return buildErrorResponse(new BadRequestException(message), isVerbose);
        }

        if (exception instanceof IOException) {
            log.error("IO Exception uncaught by Elide", exception);
            return buildErrorResponse(new TransactionException(exception), isVerbose);
        }

        log.error("Error or exception uncaught by Elide", exception);
        throw new RuntimeException(exception);
    }

    private ElideResponse handleRuntimeException(RuntimeException exception, boolean isVerbose) {
        ErrorResponseException mappedException = mapError(exception);

        if (mappedException != null) {
            return buildErrorResponse(mappedException, isVerbose);
        }

        if (exception instanceof WebApplicationException) {
            throw exception;
        }

        if (exception instanceof ForbiddenAccessException e) {
            if (log.isDebugEnabled()) {
                log.debug("{}", e.getLoggedMessage());
            }
            return buildErrorResponse(e, isVerbose);
        }

        if (exception instanceof JsonPatchExtensionException e) {
            log.debug("JSON API Json Patch extension exception caught", e);
            return buildErrorResponse(e, isVerbose);
        }

        if (exception instanceof JsonApiAtomicOperationsException e) {
            log.debug("JSON API Atomic Operations extension exception caught", e);
            return buildErrorResponse(e, isVerbose);
        }

        if (exception instanceof HttpStatusException e) {
            log.debug("Caught HTTP status exception", e);
            return buildErrorResponse(e, isVerbose);
        }

        if (exception instanceof ParseCancellationException e) {
            log.debug("Parse cancellation exception uncaught by Elide (i.e. invalid URL)", e);
            return buildErrorResponse(new InvalidURLException(e), isVerbose);
        }

        if (exception instanceof ConstraintViolationException e) {
            log.debug("Constraint violation exception caught", e);
            final JsonApiErrors.JsonApiErrorsBuilder errors = JsonApiErrors.builder();
            for (ConstraintViolation<?> constraintViolation : e.getConstraintViolations()) {
                errors.error(error -> {
                    error.detail(constraintViolation.getMessage());
                    error.code(constraintViolation.getConstraintDescriptor().getAnnotation().annotationType()
                            .getSimpleName());
                    final String propertyPathString = constraintViolation.getPropertyPath().toString();
                    if (!propertyPathString.isEmpty()) {
                        error.source(
                                source -> source.pointer("/data/attributes/" + propertyPathString.replace(".", "/")));
                        error.meta(meta -> {
                            meta.put("type",  "ConstraintViolation");
                            meta.put("property",  propertyPathString);
                        });
                    }
                });
            }
            return buildErrorResponse(HttpStatus.SC_BAD_REQUEST, errors.build());
        }

        log.error("Error or exception uncaught by Elide", exception);
        throw exception;
    }

    public ErrorResponseException mapError(Exception error) {
        if (errorMapper != null) {
            log.trace("Attempting to map unknown exception of type {}", error.getClass());
            ErrorResponseException customizedError = errorMapper.map(error);

            if (customizedError != null) {
                log.debug("Successfully mapped exception from type {} to {}",
                        error.getClass(), customizedError.getClass());
                return customizedError;
            } else {
                log.debug("No error mapping present for {}", error.getClass());
            }
        }

        return null;
    }

    protected <T> ElideResponse buildResponse(Pair<Integer, T> response) {
        try {
            T responseNode = response.getRight();
            Integer responseCode = response.getLeft();
            String body = responseNode == null ? null : mapper.writeJsonApiDocument(responseNode);
            return new ElideResponse(responseCode, body);
        } catch (JsonProcessingException e) {
            return new ElideResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.toString());
        }
    }

    private void verifyQueryParams(Map<String, List<String>> queryParams) {
        String undefinedKeys = queryParams.keySet()
                        .stream()
                        .filter(JsonApi::notAValidKey)
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
        protected JsonApiRequestScope requestScope;
        protected Supplier<Pair<Integer, T>> result;
        protected RuntimeException cause;

        protected HandlerResult(JsonApiRequestScope requestScope, Supplier<Pair<Integer, T>> result) {
            this.requestScope = requestScope;
            this.result = result;
        }

        public HandlerResult(JsonApiRequestScope requestScope, RuntimeException cause) {
            this.requestScope = requestScope;
            this.cause = cause;
        }

        public Supplier<Pair<Integer, T>> getResponder() {
            if (cause != null) {
                throw cause;
            }
            return result;
        }

        public JsonApiRequestScope getRequestScope() {
            return requestScope;
        }
    }

    public static final String MEDIA_TYPE = "application/vnd.api+json";

    public static class JsonPatch {
        private JsonPatch() {
        }

        public static final String MEDIA_TYPE = "application/vnd.api+json; ext=jsonpatch";
    }

    public static class AtomicOperations {
        private AtomicOperations() {
        }

        public static final String MEDIA_TYPE = "application/vnd.api+json; ext=\"https://jsonapi.org/ext/atomic\"";
    }
}
