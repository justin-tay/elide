/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideError;
import com.yahoo.elide.ElideErrorResponse;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.ErrorResponseException;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.graphql.models.GraphQLErrors;
import com.yahoo.elide.graphql.parser.GraphQLEntityProjectionMaker;
import com.yahoo.elide.graphql.parser.GraphQLProjectionInfo;
import com.yahoo.elide.graphql.parser.GraphQLQuery;
import com.yahoo.elide.graphql.parser.QueryParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.GraphQLException;
import graphql.execution.AsyncSerialExecutionStrategy;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.SimpleDataFetcherExceptionHandler;
import graphql.validation.ValidationError;
import graphql.validation.ValidationErrorType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entry point for REST endpoints to execute GraphQL queries.
 */
@Slf4j
public class QueryRunner {

    @Getter
    private final Elide elide;
    private GraphQL api;

    @Getter
    private String apiVersion;

    private static final String QUERY = "query";
    private static final String OPERATION_NAME = "operationName";
    private static final String VARIABLES = "variables";
    private static final String MUTATION = "mutation";

    /**
     * Builds a new query runner.
     * @param apiVersion The API version.
     * @param elide The singular elide instance for this service.
     */
    public QueryRunner(Elide elide, String apiVersion) {
        this(elide, apiVersion, new SimpleDataFetcherExceptionHandler());
    }
    /**
     * Builds a new query runner.
     * @param elide The singular elide instance for this service.
     * @param apiVersion The API version.
     * @param exceptionHandler Overrides the default exception handler.
     */
    public QueryRunner(Elide elide, String apiVersion, DataFetcherExceptionHandler exceptionHandler) {
        this.elide = elide;
        this.apiVersion = apiVersion;

        EntityDictionary dictionary = elide.getElideSettings().getEntityDictionary();

        NonEntityDictionary nonEntityDictionary = new NonEntityDictionary(
                dictionary.getScanner(),
                dictionary.getSerdeLookup());

        PersistentResourceFetcher fetcher = new PersistentResourceFetcher(nonEntityDictionary);
        ModelBuilder builder = new ModelBuilder(elide.getElideSettings().getEntityDictionary(),
                nonEntityDictionary, elide.getElideSettings(), fetcher, apiVersion);

        this.api = GraphQL.newGraphQL(builder.build())
                .defaultDataFetcherExceptionHandler(exceptionHandler)
                .queryExecutionStrategy(new AsyncSerialExecutionStrategy(exceptionHandler))
                .build();

        // TODO - add serializers to allow for custom handling of ExecutionResult and GraphQLError objects
        GraphQLErrorSerializer errorSerializer = new GraphQLErrorSerializer();
        SimpleModule module = new SimpleModule("ExecutionResultSerializer", Version.unknownVersion());
        module.addSerializer(ExecutionResult.class, new ExecutionResultSerializer(errorSerializer));
        module.addSerializer(GraphQLError.class, errorSerializer);
        elide.getElideSettings().getObjectMapper().registerModule(module);
    }

    /**
     * Execute a GraphQL query and return the response.
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param graphQLDocument The graphQL document (wrapped in JSON payload).
     * @param user The user who issued the query.
     * @return The response.
     */
    public ElideResponse run(String baseUrlEndPoint, String graphQLDocument, User user) {
        return run(baseUrlEndPoint, graphQLDocument, user, UUID.randomUUID());
    }

    /**
     * Check if a query string is mutation.
     * @param query The graphQL Query to verify.
     * @return is a mutation.
     */
    public static boolean isMutation(String query) {
        if (query == null) {
            return false;
        }

        String[] lines = query.split("\n");

        StringBuilder withoutComments = new StringBuilder();

        for (String line : lines) {
            //Remove GraphiQL comment lines....
            if (line.matches("^(\\s*)#.*")) {
                continue;
            }
            withoutComments.append(line);
            withoutComments.append("\n");
        }

        query = withoutComments.toString().trim();

        return query.startsWith(MUTATION);
    }

    /**
     * Extracts the top level JsonNode from GraphQL document.
     * @param mapper ObjectMapper instance.
     * @param graphQLDocument The graphQL document (wrapped in JSON payload).
     * @return The JsonNode after parsing graphQLDocument.
     * @throws IOException IOException
     */
    public static JsonNode getTopLevelNode(ObjectMapper mapper, String graphQLDocument) throws IOException {
        return mapper.readTree(graphQLDocument);
    }

    /**
     * Execute a GraphQL query and return the response.
     * @param graphQLDocument The graphQL document (wrapped in JSON payload).
     * @param user The user who issued the query.
     * @param requestId the Request ID.
     * @return The response.
     */
    public ElideResponse run(String baseUrlEndPoint, String graphQLDocument, User user, UUID requestId) {
        return run(baseUrlEndPoint, graphQLDocument, user, requestId, null);
    }

    /**
     * Execute a GraphQL query and return the response.
     * @param graphQLDocument The graphQL document (wrapped in JSON payload).
     * @param user The user who issued the query.
     * @param requestId the Request ID.
     * @return The response.
     */
    public ElideResponse run(String baseUrlEndPoint, String graphQLDocument, User user, UUID requestId,
                             Map<String, List<String>> requestHeaders) {
        ObjectMapper mapper = elide.getObjectMapper();

        List<GraphQLQuery> queries;
        try {
            queries = new QueryParser() {
            }.parseDocument(graphQLDocument, mapper);
        } catch (IOException e) {
            log.debug("Invalid json body provided to GraphQL", e);
            // NOTE: Can't get at isVerbose setting here for hardcoding to false. If necessary, we can refactor
            // so this can be set appropriately.
            return buildErrorResponse(mapper, new InvalidEntityBodyException(graphQLDocument), false);
        }

        List<ElideResponse> responses = new ArrayList<>();
        for (GraphQLQuery query : queries) {
            responses.add(executeGraphQLRequest(baseUrlEndPoint, mapper, user,
                    graphQLDocument, query, requestId, requestHeaders));
        }

        if (responses.size() == 1) {
            return responses.get(0);
        }

        //Convert the list of responses into a single JSON Array.
        ArrayNode result = responses.stream()
                .map(response -> {
                    try {
                        return mapper.readTree(response.getBody());
                    } catch (IOException e) {
                        log.debug("Caught an IO exception while trying to read response body");
                        return JsonNodeFactory.instance.objectNode();
                    }
                })
                .reduce(JsonNodeFactory.instance.arrayNode(),
                        (arrayNode, node) -> arrayNode.add(node),
                        (left, right) -> left.addAll(right));

        try {

            //Build and elide response from the array of responses.
            return ElideResponse.builder()
                    .responseCode(HttpStatus.SC_OK)
                    .body(mapper.writeValueAsString(result))
                    .build();
        } catch (IOException e) {
            log.error("An unexpected error occurred trying to serialize array response.", e);
            return ElideResponse.builder()
                    .responseCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /**
     * Extracts the executable query from Json Node.
     * @param jsonDocument The JsonNode object.
     * @return query to execute.
     */
    public static String extractQuery(JsonNode jsonDocument) {
        return jsonDocument.has(QUERY) ? jsonDocument.get(QUERY).asText() : null;
    }

    /**
     * Extracts the variables for the query from Json Node.
     * @param mapper ObjectMapper instance.
     * @param jsonDocument The JsonNode object.
     * @return variables to pass.
     */
    public static Map<String, Object> extractVariables(ObjectMapper mapper, JsonNode jsonDocument) {
        // get variables from request for constructing entityProjections
        Map<String, Object> variables = new HashMap<>();
        if (jsonDocument.has(VARIABLES) && !jsonDocument.get(VARIABLES).isNull()) {
            variables = mapper.convertValue(jsonDocument.get(VARIABLES), Map.class);
        }

        return variables;
    }

    /**
     * Extracts the operation name from Json Node.
     * @param jsonDocument The JsonNode object.
     * @return variables to pass.
     */
    public static String extractOperation(JsonNode jsonDocument) {
        if (jsonDocument.has(OPERATION_NAME) && !jsonDocument.get(OPERATION_NAME).isNull()) {
            return jsonDocument.get(OPERATION_NAME).asText();
        }

        return null;
    }

    private ElideResponse executeGraphQLRequest(String baseUrlEndPoint, ObjectMapper mapper, User principal,
                                                String graphQLDocument, GraphQLQuery query, UUID requestId,
                                                Map<String, List<String>> requestHeaders) {
        boolean isVerbose = false;
        String queryText = query.getQuery();
        boolean isMutation = isMutation(queryText);

        try (DataStoreTransaction tx = isMutation
                ? elide.getDataStore().beginTransaction()
                : elide.getDataStore().beginReadTransaction()) {

            elide.getTransactionRegistry().addRunningTransaction(requestId, tx);
            if (query.getQuery() == null || query.getQuery().isEmpty()) {
                return ElideResponse.builder().responseCode(HttpStatus.SC_BAD_REQUEST)
                        .body("A `query` key is required.").build();
            }

            // get variables from request for constructing entityProjections
            Map<String, Object> variables = query.getVariables();

            //TODO - get API version.
            GraphQLProjectionInfo projectionInfo = new GraphQLEntityProjectionMaker(elide.getElideSettings(), variables,
                    apiVersion).make(queryText);
            Route route = Route.builder()
                    .baseUrl(baseUrlEndPoint)
                    .apiVersion(apiVersion)
                    .headers(requestHeaders)
                    .build();
            GraphQLRequestScope requestScope = GraphQLRequestScope.builder()
                    .route(route)
                    .dataStoreTransaction(tx)
                    .user(principal)
                    .requestId(requestId)
                    .elideSettings(elide.getElideSettings())
                    .projectionInfo(projectionInfo)
                    .build();

            isVerbose = requestScope.getPermissionExecutor().isVerbose();

            // Logging all queries. It is recommended to put any private information that shouldn't be logged into
            // the "variables" section of your query. Variable values are not logged.
            log.info("Processing GraphQL query:\n{}", queryText);

            ExecutionInput.Builder executionInput = new ExecutionInput.Builder()
                    .localContext(requestScope)
                    .query(queryText);

            if (query.getOperationName() != null) {
                executionInput.operationName(query.getOperationName());
            }
            executionInput.variables(variables);

            ExecutionResult result = api.execute(executionInput);

            tx.preCommit(requestScope);
            requestScope.getPermissionExecutor().executeCommitChecks();
            if (isMutation) {
                if (!result.getErrors().isEmpty()) {
                    HashMap<String, Object> abortedResponseObject = new HashMap<>();
                    abortedResponseObject.put("errors", mapErrors(result.getErrors()));
                    abortedResponseObject.put("data", null);
                    // Do not commit. Throw OK response to process tx.close correctly.
                    throw new GraphQLException(mapper.writeValueAsString(abortedResponseObject));
                }
                requestScope.saveOrCreateObjects();
            }

            tx.flush(requestScope);

            requestScope.runQueuedPreCommitTriggers();
            elide.getAuditLogger().commit();
            tx.commit(requestScope);
            requestScope.runQueuedPostCommitTriggers();

            if (log.isTraceEnabled()) {
                requestScope.getPermissionExecutor().logCheckStats();
            }

            return ElideResponse.builder().responseCode(HttpStatus.SC_OK).body(mapper.writeValueAsString(result))
                    .build();
        } catch (IOException e) {
            return handleNonRuntimeException(elide, e, graphQLDocument, isVerbose);
        } catch (RuntimeException e) {
            return handleRuntimeException(elide, e, isVerbose);
        } finally {
            elide.getTransactionRegistry().removeRunningTransaction(requestId);
            elide.getAuditLogger().clear();
        }
    }

    /**
     * Generate more user friendly error messages.
     *
     * @param errors the errors to map
     * @return the mapped errors
     */
    private List<GraphQLError> mapErrors(List<GraphQLError> errors) {
        List<GraphQLError> result = new ArrayList<>(errors.size());
        for (GraphQLError error : errors) {
            if (error instanceof ValidationError validationError
                    && ValidationErrorType.WrongType.equals(validationError.getValidationErrorType())) {
                if (validationError.getDescription().contains("ElideRelationshipOp")) {
                    String queryPath = String.join(" ", validationError.getQueryPath());
                    RelationshipOp relationshipOp = Arrays.stream(RelationshipOp.values())
                            .filter(op -> validationError.getDescription().contains(op.name()))
                            .findFirst()
                            .orElse(null);
                    if (relationshipOp != null) {
                        result.add(ValidationError.newValidationError()
                                .description("Invalid operation: " + relationshipOp.name() + " is not permitted on "
                                        + queryPath + ".")
                                .extensions(validationError.getExtensions())
                                .validationErrorType(validationError.getValidationErrorType())
                                .sourceLocations(validationError.getLocations())
                                .queryPath(validationError.getQueryPath())
                                .build());
                        continue;
                    }
                }
            }
            result.add(error);
        }

        return result;
    }

    public static ElideResponse handleNonRuntimeException(
            Elide elide,
            Exception exception,
            String graphQLDocument,
            boolean verbose
    ) {
        ErrorResponseException mappedException = elide.mapError(exception);
        ObjectMapper mapper = elide.getObjectMapper();

        if (mappedException != null) {
            return buildErrorResponse(mapper, mappedException, verbose);
        }

        if (exception instanceof JsonProcessingException) {
            log.debug("Invalid json body provided to GraphQL", exception);
            return buildErrorResponse(mapper, new InvalidEntityBodyException(graphQLDocument), verbose);
        }

        if (exception instanceof IOException) {
            log.error("Uncaught IO Exception by Elide in GraphQL", exception);
            return buildErrorResponse(mapper, new TransactionException(exception), verbose);
        }

        log.error("Error or exception uncaught by Elide", exception);
        throw new RuntimeException(exception);
    }

    public static ElideResponse handleRuntimeException(Elide elide, RuntimeException exception, boolean verbose) {
        ErrorResponseException mappedException = elide.mapError(exception);
        ObjectMapper mapper = elide.getObjectMapper();

        if (mappedException != null) {
            return buildErrorResponse(mapper, mappedException, verbose);
        }

        if (exception instanceof GraphQLException e) {
            log.debug("GraphQLException", e);
            String body = e.getMessage();
            return ElideResponse.builder().responseCode(HttpStatus.SC_OK).body(body).build();
        }

        if (exception instanceof HttpStatusException e) {
            if (e instanceof ForbiddenAccessException forbiddenAccessException) {
                if (log.isDebugEnabled()) {
                    log.debug("{}", forbiddenAccessException.getLoggedMessage());
                }
            } else {
                log.debug("Caught HTTP status exception {}", e.getStatus(), e);
            }

            return buildErrorResponse(mapper, new HttpStatusException(200, e.getMessage()) {
                @Override
                public int getStatus() {
                    return 200;
                }

                @Override
                public ElideErrorResponse getErrorResponse() {
                    ElideErrorResponse r = e.getErrorResponse();
                    return ElideErrorResponse.builder().body(r.getBody()).errors(r.getErrors())
                            .responseCode(getStatus()).build();
                }

                @Override
                public ElideErrorResponse getVerboseErrorResponse() {
                    ElideErrorResponse r = e.getVerboseErrorResponse();
                    return ElideErrorResponse.builder().body(r.getBody()).errors(r.getErrors())
                            .responseCode(getStatus()).build();
                }

                @Override
                public String getVerboseMessage() {
                    return e.getVerboseMessage();
                }

                @Override
                public String toString() {
                    return e.toString();
                }
            }, verbose);
        }

        if (exception instanceof ConstraintViolationException e) {
            log.debug("Constraint violation exception caught", e);
            final GraphQLErrors.GraphQLErrorsBuilder errors = GraphQLErrors.builder();
            for (ConstraintViolation<?> constraintViolation : e.getConstraintViolations()) {
                errors.error(error -> {
                    error.message(constraintViolation.getMessage());
                    error.extension("code", constraintViolation.getConstraintDescriptor().getAnnotation()
                            .annotationType().getSimpleName());
                    error.extension("type",  "ConstraintViolation");
                    final String propertyPathString = constraintViolation.getPropertyPath().toString();
                    if (!propertyPathString.isEmpty()) {
                        error.extension("property",  propertyPathString);
                    }
                });
            }
            return buildErrorResponse(mapper, HttpStatus.SC_OK, errors.build());
        }

        log.error("Error or exception uncaught by Elide", exception);
        throw exception;
    }

    public static ElideResponse buildErrorResponse(ObjectMapper mapper, HttpStatusException exception,
            boolean verbose) {
        if (exception instanceof InternalServerErrorException) {
            log.error("Internal Server Error", exception);
        }

        ElideErrorResponse errorResponse = (verbose ? exception.getVerboseErrorResponse()
                : exception.getErrorResponse());
        if (errorResponse.getBody() != null) {
            return buildErrorResponse(mapper, errorResponse.getResponseCode(), errorResponse.getBody());
        } else {
            GraphQLErrors.GraphQLErrorsBuilder builder = GraphQLErrors.builder();
            for (ElideError error : errorResponse.getErrors().getErrors()) {
                builder.error(graphqlError -> convertToGraphQLError(error, graphqlError));
            }
            return buildErrorResponse(mapper, errorResponse.getResponseCode(), builder.build());
        }
    }

    public static ElideResponse buildErrorResponse(ObjectMapper mapper, int responseCode, Object errors) {
        try {
            return new ElideResponse(responseCode, mapper.writeValueAsString(errors));
        } catch (JsonProcessingException e) {
            return new ElideResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.toString());
        }
    }

    public static void convertToGraphQLError(ElideError error,
            com.yahoo.elide.graphql.models.GraphQLError.GraphQLErrorBuilder graphqlError) {
        if (error.getMessage() != null) {
            graphqlError.message(error.getMessage()); // The serializer will encode the message
        }
        if (error.getAttributes() != null && !error.getAttributes().isEmpty()) {
            Map<String, Object> extensions = new LinkedHashMap<>(error.getAttributes());
            if (!extensions.isEmpty()) {
                graphqlError.extensions(extensions);
            }
        }
    }
}
