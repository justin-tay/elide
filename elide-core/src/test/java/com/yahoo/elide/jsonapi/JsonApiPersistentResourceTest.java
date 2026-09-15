/*
 * Copyright 2025, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.PersistenceResourceTestSetup;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreIterableBuilder;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.InvalidObjectIdentifierException;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.core.security.TestUser;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.jsonapi.extensions.JsonApiJsonPatchRequestScope;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.models.Relationship;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.jsonapi.models.ResourceIdentifier;
import com.google.common.collect.Sets;
import example.Author;
import example.Book;
import example.Child;
import example.FunWithPermissions;
import example.Left;
import example.NoReadEntity;
import example.NoShareEntity;
import example.Parent;
import example.Right;
import example.nontransferable.ContainerWithPackageShare;
import example.nontransferable.ShareableWithPackageShare;
import example.nontransferable.Untransferable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Tests how {@link PersistentResource} handles JSON-API relationship payloads (i.e. data deserialized via
 * {@link Relationship#toPersistentResources(RequestScope)}), plus {@link JsonApiPersistentResource} itself.
 */
public class JsonApiPersistentResourceTest extends PersistenceResourceTestSetup {

    private final User goodUser = new TestUser("1");
    private final User badUser = new TestUser("-1");

    private final DataStoreTransaction tx = mock(DataStoreTransaction.class);

    /* Unlike the generic elideSettings inherited from PersistenceResourceTestSetup, this registers JsonApiSettings,
     * which JsonApiRequestScope (and its subclasses) require to be present. */
    private final ElideSettings jsonApiElideSettings = ElideSettings.builder().dataStore(null)
            .entityDictionary(dictionary)
            .settings(JsonApiSettings.builder())
            .build();

    @BeforeEach
    public void beforeTest() {
        reset(tx);
    }

    private JsonApiRequestScope buildJsonApiRequestScope(String path, DataStoreTransaction tx, User user,
            Map<String, List<String>> queryParams) {
        Route route = Route.builder().path(path).apiVersion(NO_VERSION).parameters(queryParams).build();
        return JsonApiRequestScope.builder().route(route).dataStoreTransaction(tx).user(user)
                .requestId(UUID.randomUUID()).jsonApiDocument(new JsonApiDocument())
                .elideSettings(jsonApiElideSettings).build();
    }

    private static void add(Map<String, List<String>> params, String key, String value) {
        params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    @Test
    /**
     * Verifies that persistentResource.toPersistentResource() throws a ForbiddenAccessException when the user
     * cannot read one of resources.
     */
    public void testToPersistentResourceForbidden() {
        when(tx.loadObject(any(), eq(1L), any())).thenReturn(new NoReadEntity());
        Relationship ids = new Relationship(null,
                new Data<>(new ResourceIdentifier("noread", "1").castToResource()));
        RequestScope goodScope = buildRequestScope(tx, goodUser);
        assertThrows(ForbiddenAccessException.class, () -> ids.toPersistentResources(goodScope));
    }

    @Test
    public void testGetRelationships() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setRelation1(Sets.newHashSet());
        fun.setRelation2(Sets.newHashSet());
        fun.setRelation3(null);

        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);

        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", scope);

        Map<String, Relationship> relationships = JsonApiPersistentResource.getRelationships(funResource);

        assertEquals(5, relationships.size(), "All relationships should be returned.");
        assertTrue(relationships.containsKey("relation1"), "relation1 should be present");
        assertTrue(relationships.containsKey("relation2"), "relation2 should be present");
        assertTrue(relationships.containsKey("relation3"), "relation3 should be present");
        assertTrue(relationships.containsKey("relation4"), "relation4 should be present");
        assertTrue(relationships.containsKey("relation5"), "relation5 should be present");

        scope = new TestRequestScope(tx, badUser, dictionary);

        PersistentResource<FunWithPermissions> funResourceWithBadScope = new PersistentResource<>(fun, "3", scope);
        relationships = JsonApiPersistentResource.getRelationships(funResourceWithBadScope);

        assertEquals(0, relationships.size(), "All relationships should be filtered out");
    }

    @Test
    public void testGetAttributes() {
        FunWithPermissions fun = new FunWithPermissions();
        fun.setField3("Foobar");
        fun.setField1("blah");
        fun.setField2(null);
        fun.setField4("bar");

        when(tx.getAttribute(any(), any(), any())).thenCallRealMethod();

        RequestScope scope = new TestRequestScope(tx, goodUser, dictionary);
        PersistentResource<FunWithPermissions> funResource = new PersistentResource<>(fun, "3", scope);

        Map<String, Object> attributes = JsonApiPersistentResource.getAttributes(funResource);

        assertEquals(6, attributes.size(),
                "A valid user should have access to all attributes that are readable."
        );

        assertTrue(attributes.containsKey("field2"), "Readable attributes should include field2");
        assertTrue(attributes.containsKey("field3"), "Readable attributes should include field3");
        assertTrue(attributes.containsKey("field4"), "Readable attributes should include field4");
        assertTrue(attributes.containsKey("field5"), "Readable attributes should include field5");
        assertTrue(attributes.containsKey("field6"), "Readable attributes should include field6");
        assertTrue(attributes.containsKey("field8"), "Readable attributes should include field8");
        assertNull(attributes.get("field2"), "field2 should be set to original value.");
        assertEquals(attributes.get("field3"), "Foobar", "field3 should be set to original value.");
        assertEquals(attributes.get("field4"), "bar", "field4 should be set to original value.");

        RequestScope badUserScope = new TestRequestScope(tx, badUser, dictionary);
        PersistentResource<FunWithPermissions> funResourceBad = new PersistentResource<>(fun, "3", badUserScope);

        attributes = JsonApiPersistentResource.getAttributes(funResourceBad);

        assertEquals(3, attributes.size(), "An invalid user should have access to a subset of attributes.");
        assertTrue(attributes.containsKey("field2"), "Readable attributes should include field2");
        assertTrue(attributes.containsKey("field4"), "Readable attributes should include field4");
        assertTrue(attributes.containsKey("field5"), "Readable attributes should include field5");
        assertNull(attributes.get("field2"), "field2 should be set to original value.");
        assertEquals(attributes.get("field4"), "bar", "field4 should be set to original value.");
    }

    @Test
    public void testSuccessfulOneToOneRelationshipAdd() throws Exception {
        Left left = new Left();
        Right right = new Right();
        left.setId(2);
        right.setId(3);

        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<Left> leftResource = new PersistentResource<>(left, "2", goodScope);

        Relationship ids = new Relationship(null, new Data<>(new ResourceIdentifier("right", "3").castToResource()));

        when(tx.loadObject(any(), eq(3L), any())).thenReturn(right);
        boolean updated = leftResource.updateRelation("one2one", ids.toPersistentResources(goodScope));
        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(left, goodScope);
        verify(tx, times(1)).save(right, goodScope);
        verify(tx, times(1)).getToOneRelation(tx, left, getRelationship(ClassType.of(Right.class), "one2one"), goodScope);

        assertTrue(updated, "The one-2-one relationship should be added.");
        assertEquals(3, left.getOne2one().getId(), "The correct object was set in the one-2-one relationship");
    }

    /**
     * Avoid NPE when PATCH or POST defines relationship with null id.
     * <pre>
     * <code>
     * "relationships": {
     *   "left": {
     *     "data": {
     *       "type": "right",
     *       "id": null
     *     }
     *   }
     * }
     * </code>
     * </pre>
     */
    @Test
    public void testSuccessfulOneToOneRelationshipAddNull() throws Exception {
        Left left = new Left();
        left.setId(2);

        RequestScope goodScope = buildRequestScope(tx, goodUser);

        PersistentResource<Left> leftResource = new PersistentResource<>(left, "2", goodScope);

        Relationship ids = new Relationship(null, new Data<>(new Resource("right", null, null, null, null, null, null)));

        InvalidObjectIdentifierException thrown = assertThrows(
                InvalidObjectIdentifierException.class,
                () -> leftResource.updateRelation("one2one", ids.toPersistentResources(goodScope)));

        assertEquals("Unknown identifier null for right", thrown.getMessage());
    }

    @Test
    /*
     * The following are ids for a hypothetical relationship.
     * GIVEN:
     * all (all the ids in the DB) = 1,2,3,4,5
     * mine (everything the current user has access to) = 1,2,3
     * requested (what the user wants to change to) = 3,6
     * THEN:
     * deleted (what gets removed from the DB) = 1,2
     * final (what get stored in the relationship) = 3,4,5,6
     * BECAUSE:
     * notMine = all - mine
     * updated = (requested UNION mine) - (requested INTERSECT mine)
     * deleted = (mine - requested)
     * final = (notMine) UNION requested
     */
    public void testSuccessfulManyToManyRelationshipUpdate() throws Exception {
        Parent parent = new Parent();
        RequestScope goodScope = buildRequestScope(tx, goodUser);

        Child child1 = newChild(1);
        Child child2 = newChild(2);
        Child child3 = newChild(3);
        Child child4 = newChild(-4); //Not accessible to goodUser
        Child child5 = newChild(-5); //Not accessible to goodUser
        Child child6 = newChild(6);

        //All = (1,2,3,4,5)
        //Mine = (1,2,3)
        Set<Child> allChildren = new HashSet<>();
        allChildren.add(child1);
        allChildren.add(child2);
        allChildren.add(child3);
        allChildren.add(child4);
        allChildren.add(child5);
        parent.setChildren(allChildren);
        parent.setSpouses(Sets.newHashSet());

        when(tx.getToManyRelation(any(), eq(parent), any(), any())).thenReturn(new DataStoreIterableBuilder(allChildren).build());

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "1", goodScope);

        //Requested = (3,6)
        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("child", "3").castToResource());
        idList.add(new ResourceIdentifier("child", "6").castToResource());
        Relationship ids = new Relationship(null, new Data<>(idList));

        when(tx.loadObject(any(), eq(2L), any())).thenReturn(child2);
        when(tx.loadObject(any(), eq(3L), any())).thenReturn(child3);
        when(tx.loadObject(any(), eq(-4L), any())).thenReturn(child4);
        when(tx.loadObject(any(), eq(-5L), any())).thenReturn(child5);
        when(tx.loadObject(any(), eq(6L), any())).thenReturn(child6);

        //Final set after operation = (3,4,5,6)
        Set<Child> expected = new HashSet<>();
        expected.add(child3);
        expected.add(child4);
        expected.add(child5);
        expected.add(child6);

        boolean updated = parentResource.updateRelation("children", ids.toPersistentResources(goodScope));

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(parent, goodScope);
        verify(tx, times(1)).save(child1, goodScope);
        verify(tx, times(1)).save(child2, goodScope);
        verify(tx, times(1)).save(child6, goodScope);
        verify(tx, never()).save(child4, goodScope);
        verify(tx, never()).save(child5, goodScope);
        verify(tx, never()).save(child3, goodScope);

        assertTrue(updated, "Many-2-many relationship should be updated.");
        assertTrue(parent.getChildren().containsAll(expected), "All expected members were updated");
        assertTrue(expected.containsAll(parent.getChildren()), "All expected members were updated");

        /*
         * No tests for reference integrity since the parent is the owner and
         * this is a many to many relationship.
         */
    }

    @Test
    /*
     * The following are ids for a hypothetical relationship.
     * GIVEN:
     * all (all the ids in the DB) = 1,2,3,4,5
     * mine (everything the current user has access to) = 1,2,3
     * requested (what the user wants to change to) = 1,2,3
     * THEN:
     * deleted (what gets removed from the DB) = nothing
     * final (what get stored in the relationship) = 1,2,3,4,5
     * BECAUSE:
     * notMine = all - mine
     * updated = (requested UNION mine) - (requested INTERSECT mine)
     * deleted = (mine - requested)
     * final = (notMine) UNION requested
     */
    public void testSuccessfulManyToManyRelationshipNoopUpdate() throws Exception {
        Parent parent = new Parent();
        RequestScope goodScope = buildRequestScope(tx, goodUser);

        Child child1 = newChild(1);
        Child child2 = newChild(2);
        Child child3 = newChild(3);
        Child child4 = newChild(-4); //Not accessible to goodUser
        Child child5 = newChild(-5); //Not accessible to goodUser

        //All = (1,2,3,4,5)
        //Mine = (1,2,3)
        Set<Child> allChildren = new HashSet<>();
        allChildren.add(child1);
        allChildren.add(child2);
        allChildren.add(child3);
        allChildren.add(child4);
        allChildren.add(child5);
        parent.setChildren(allChildren);
        parent.setSpouses(Sets.newHashSet());

        when(tx.getToManyRelation(any(), eq(parent), any(), any())).thenReturn(new DataStoreIterableBuilder(allChildren).build());

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "1", goodScope);

        //Requested = (1,2,3)
        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("child", "3").castToResource());
        idList.add(new ResourceIdentifier("child", "2").castToResource());
        idList.add(new ResourceIdentifier("child", "1").castToResource());
        Relationship ids = new Relationship(null, new Data<>(idList));

        when(tx.loadObject(any(), eq(1L), any())).thenReturn(child1);
        when(tx.loadObject(any(), eq(2L), any())).thenReturn(child2);
        when(tx.loadObject(any(), eq(3L), any())).thenReturn(child3);
        when(tx.loadObject(any(), eq(-4L), any())).thenReturn(child4);
        when(tx.loadObject(any(), eq(-5L), any())).thenReturn(child5);

        //Final set after operation = (1,2,3,4,5)
        Set<Child> expected = new HashSet<>();
        expected.add(child1);
        expected.add(child2);
        expected.add(child3);
        expected.add(child4);
        expected.add(child5);

        boolean updated = parentResource.updateRelation("children", ids.toPersistentResources(goodScope));

        goodScope.saveOrCreateObjects();
        verify(tx, never()).save(parent, goodScope);
        verify(tx, never()).save(child1, goodScope);
        verify(tx, never()).save(child2, goodScope);
        verify(tx, never()).save(child4, goodScope);
        verify(tx, never()).save(child5, goodScope);
        verify(tx, never()).save(child3, goodScope);

        assertFalse(updated, "Many-2-many relationship should not be updated.");
        assertTrue(parent.getChildren().containsAll(expected), "All expected members were updated");
        assertTrue(expected.containsAll(parent.getChildren()), "All expected members were updated");

        /*
         * No tests for reference integrity since the parent is the owner and
         * this is a many to many relationship.
         */
    }

    @Test
    /*
     * The following are ids for a hypothetical relationship.
     * GIVEN:
     * all (all the ids in the DB) = null
     * mine (everything the current user has access to) = null
     * requested (what the user wants to change to) = 1,2,3
     * THEN:
     * deleted (what gets removed from the DB) = nothing
     * final (what get stored in the relationship) = 1,2,3
     * BECAUSE:
     * notMine = all - mine
     * updated = (requested UNION mine) - (requested INTERSECT mine)
     * deleted = (mine - requested)
     * final = (notMine) UNION requested
     */
    public void testSuccessfulManyToManyRelationshipNullUpdate() throws Exception {
        Parent parent = new Parent();
        RequestScope goodScope = buildRequestScope(tx, goodUser);

        Child child1 = newChild(1);
        Child child2 = newChild(2);
        Child child3 = newChild(3);

        //All = null
        //Mine = null
        Set<Child> allChildren = new HashSet<>();
        allChildren.add(child1);
        allChildren.add(child2);
        allChildren.add(child3);
        parent.setChildren(null);
        parent.setSpouses(Sets.newHashSet());

        when(tx.getToManyRelation(any(), eq(parent), any(), any())).thenReturn(null);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "1", goodScope);

        //Requested = (1,2,3)
        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("child", "3").castToResource());
        idList.add(new ResourceIdentifier("child", "2").castToResource());
        idList.add(new ResourceIdentifier("child", "1").castToResource());
        Relationship ids = new Relationship(null, new Data<>(idList));

        when(tx.loadObject(any(), eq(1L), any())).thenReturn(child1);
        when(tx.loadObject(any(), eq(2L), any())).thenReturn(child2);
        when(tx.loadObject(any(), eq(3L), any())).thenReturn(child3);

        //Final set after operation = (1,2,3)
        Set<Child> expected = new HashSet<>();
        expected.add(child1);
        expected.add(child2);
        expected.add(child3);

        boolean updated = parentResource.updateRelation("children", ids.toPersistentResources(goodScope));

        goodScope.saveOrCreateObjects();
        verify(tx, times(1)).save(parent, goodScope);
        verify(tx, times(1)).save(child1, goodScope);
        verify(tx, times(1)).save(child2, goodScope);
        verify(tx, times(1)).save(child3, goodScope);

        assertTrue(updated, "Many-2-many relationship should be updated.");
        assertTrue(parent.getChildren().containsAll(expected), "All expected members were updated");
        assertTrue(expected.containsAll(parent.getChildren()), "All expected members were updated");

        /*
         * No tests for reference integrity since the parent is the owner and
         * this is a many to many relationship.
         */
    }

    /**
     * Verify that Relationship toMany cannot contain null resources, but toOne can.
     *
     * @throws Exception
     */
    @Test
    public void testRelationshipMissingData() throws Exception {
        User goodUser = new TestUser("1");

        @SuppressWarnings("resource")
        DataStoreTransaction tx = mock(DataStoreTransaction.class);

        RequestScope goodScope = RequestScope.builder().route(Route.builder().apiVersion(NO_VERSION).build())
                .dataStoreTransaction(tx).user(goodUser).requestId(UUID.randomUUID()).elideSettings(elideSettings)
                .build();

        // null resource in toMany relationship is not valid
        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("child", "3").castToResource());
        idList.add(new ResourceIdentifier("child", "6").castToResource());
        idList.add(null);
        assertThrows(
                NullPointerException.class,
                () -> new Relationship(Collections.emptyMap(), new Data<>(idList)));

        // However null toOne relationship is valid
        Relationship toOneRelationship = new Relationship(Collections.emptyMap(), new Data<>((Resource) null));
        assertTrue(toOneRelationship.getData().get().isEmpty());
        assertNull(toOneRelationship.toPersistentResources(goodScope));

        // no Data
        Relationship nullRelationship = new Relationship(Collections.emptyMap(), null);
        assertNull(nullRelationship.getData());
        assertNull(nullRelationship.toPersistentResources(goodScope));
    }

    @Test
    public void testUpdatePermissionCheckedOnInverseRelationship() {
        Left left = new Left();
        left.setId(1);
        Right right = new Right();

        Set<Right> rights = Sets.newHashSet(right);
        left.setNoInverseUpdate(rights);
        right.setNoUpdate(Sets.newHashSet(left));

        List<Resource> empty = new ArrayList<>();
        Relationship ids = new Relationship(null, new Data<>(empty));

        when(tx.getToManyRelation(any(), eq(left), any(), any()))
                .thenReturn(new DataStoreIterableBuilder(rights).build());

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<Left> leftResource = new PersistentResource<>(left, goodScope.getUUIDFor(left), goodScope);

        assertThrows(
                ForbiddenAccessException.class,
                () -> leftResource.updateRelation("noInverseUpdate", ids.toPersistentResources(goodScope)));
        // Modifications have a deferred check component:
        leftResource.getRequestScope().getPermissionExecutor().executeCommitChecks();
    }

    @Test
    public void testTransferPermissionErrorOnUpdateSingularRelationship() {
        example.User userModel = new example.User();
        userModel.setId(1);

        NoShareEntity noShare = new NoShareEntity();
        noShare.setId(1);

        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("noshare", "1").castToResource());
        Relationship ids = new Relationship(null, new Data<>(idList));

        EntityProjection collection = EntityProjection.builder()
                .type(NoShareEntity.class)

                .build();

        when(tx.loadObject(eq(collection), eq(1L), any())).thenReturn(noShare);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<example.User> userResource =
                new PersistentResource<>(userModel, goodScope.getUUIDFor(userModel), goodScope);

        assertThrows(
                ForbiddenAccessException.class,
                () -> userResource.updateRelation("noShare", ids.toPersistentResources(goodScope)));
    }

    @Test
    public void testTransferPermissionErrorOnUpdateRelationshipPackageLevel() {
        ContainerWithPackageShare containerWithPackageShare = new ContainerWithPackageShare();

        Untransferable untransferable = new Untransferable();
        untransferable.setContainerWithPackageShare(containerWithPackageShare);

        List<Resource> unShareableList = new ArrayList<>();
        unShareableList.add(new ResourceIdentifier("untransferable", "1").castToResource());
        Relationship unShareales = new Relationship(null, new Data<>(unShareableList));

        when(tx.loadObject(any(), eq(1L), any())).thenReturn(untransferable);


        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<ContainerWithPackageShare> containerResource = new PersistentResource<>(
                containerWithPackageShare, goodScope.getUUIDFor(containerWithPackageShare), goodScope);

        assertThrows(
                ForbiddenAccessException.class,
                () -> containerResource.updateRelation(
                        "untransferables", unShareales.toPersistentResources(goodScope)));
    }

    @Test
    public void testTransferPermissionSuccessOnUpdateManyRelationshipPackageLevel() {
        ContainerWithPackageShare containerWithPackageShare = new ContainerWithPackageShare();

        ShareableWithPackageShare shareableWithPackageShare = new ShareableWithPackageShare();
        shareableWithPackageShare.setContainerWithPackageShare(containerWithPackageShare);

        List<Resource> shareableList = new ArrayList<>();
        shareableList.add(new ResourceIdentifier("shareableWithPackageShare", "1").castToResource());
        Relationship shareables = new Relationship(null, new Data<>(shareableList));

        when(tx.loadObject(any(), eq(1L), any())).thenReturn(shareableWithPackageShare);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<ContainerWithPackageShare> containerResource = new PersistentResource<>(
                containerWithPackageShare, goodScope.getUUIDFor(containerWithPackageShare), goodScope);

        containerResource.updateRelation(
                "shareableWithPackageShares", shareables.toPersistentResources(goodScope));

        assertEquals(1, containerWithPackageShare.getShareableWithPackageShares().size());
        assertTrue(containerWithPackageShare.getShareableWithPackageShares().contains(shareableWithPackageShare));
    }

    @Test
    public void testTransferPermissionErrorOnUpdateManyRelationship() {
        example.User userModel = new example.User();
        userModel.setId(1);

        NoShareEntity noShare1 = new NoShareEntity();
        noShare1.setId(1);
        NoShareEntity noShare2 = new NoShareEntity();
        noShare2.setId(2);

        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("noshare", "1").castToResource());
        idList.add(new ResourceIdentifier("noshare", "2").castToResource());
        Relationship ids = new Relationship(null, new Data<>(idList));

        when(tx.loadObject(any(), eq(1L), any())).thenReturn(noShare1);
        when(tx.loadObject(any(), eq(2L), any())).thenReturn(noShare2);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<example.User> userResource =
                new PersistentResource<>(userModel, goodScope.getUUIDFor(userModel), goodScope);

        assertThrows(
                ForbiddenAccessException.class,
                () -> userResource.updateRelation("noShares", ids.toPersistentResources(goodScope)));
    }

    @Test
    public void testTransferPermissionSuccessOnUpdateManyRelationship() {
        example.User userModel = new example.User();
        userModel.setId(1);

        NoShareEntity noShare1 = new NoShareEntity();
        noShare1.setId(1);
        NoShareEntity noShare2 = new NoShareEntity();
        noShare2.setId(2);
        HashSet<NoShareEntity> noshares = Sets.newHashSet(noShare1, noShare2);

        /* The no shares already exist so no exception should be thrown */
        userModel.setNoShares(noshares);

        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("noshare", "1").castToResource());
        Relationship ids = new Relationship(null, new Data<>(idList));

        when(tx.loadObject(any(), eq(1L), any())).thenReturn(noShare1);
        when(tx.getToManyRelation(any(), eq(userModel), any(), any()))
                .thenReturn(new DataStoreIterableBuilder(noshares).build());

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<example.User> userResource =
                new PersistentResource<>(userModel, goodScope.getUUIDFor(userModel), goodScope);

        boolean returnVal = userResource.updateRelation("noShares", ids.toPersistentResources(goodScope));

        assertTrue(returnVal);
        assertEquals(1, userModel.getNoShares().size());
        assertTrue(userModel.getNoShares().contains(noShare1));
    }

    @Test
    public void testTransferPermissionSuccessOnUpdateSingularRelationship() {
        example.User userModel = new example.User();
        userModel.setId(1);

        NoShareEntity noShare = new NoShareEntity();

        /* The noshare already exists so no exception should be thrown */
        userModel.setNoShare(noShare);

        List<Resource> idList = new ArrayList<>();
        idList.add(new ResourceIdentifier("noshare", "1").castToResource());
        Relationship ids = new Relationship(null, new Data<>(idList));

        when(tx.getToOneRelation(any(), eq(userModel), any(), any())).thenReturn(noShare);
        when(tx.loadObject(any(), eq(1L), any())).thenReturn(noShare);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<example.User> userResource =
                new PersistentResource<>(userModel, goodScope.getUUIDFor(userModel), goodScope);

        boolean returnVal = userResource.updateRelation("noShare", ids.toPersistentResources(goodScope));

        assertFalse(returnVal);
        assertEquals(noShare, userModel.getNoShare());
    }

    @Test
    public void testTransferPermissionSuccessOnClearSingularRelationship() {
        example.User userModel = new example.User();
        userModel.setId(1);

        NoShareEntity noShare = new NoShareEntity();

        /* The noshare already exists so no exception should be thrown */
        userModel.setNoShare(noShare);

        List<Resource> empty = new ArrayList<>();
        Relationship ids = new Relationship(null, new Data<>(empty));

        when(tx.getToOneRelation(any(), eq(userModel), any(), any())).thenReturn(noShare);

        RequestScope goodScope = buildRequestScope(tx, goodUser);
        PersistentResource<example.User> userResource =
                new PersistentResource<>(userModel, goodScope.getUUIDFor(userModel), goodScope);

        boolean returnVal = userResource.updateRelation("noShare", ids.toPersistentResources(goodScope));

        assertTrue(returnVal);
        assertNull(userModel.getNoShare());
    }

    @Test
    public void testPatchRequestScope() {
        DataStoreTransaction tx = mock(DataStoreTransaction.class);
        Route route = Route.builder().path("/book").apiVersion(NO_VERSION).build();
        JsonApiJsonPatchRequestScope parentScope = new JsonApiJsonPatchRequestScope(
                route,
                tx,
                new TestUser("1"),
                UUID.randomUUID(),
                jsonApiElideSettings);
        JsonApiJsonPatchRequestScope scope = new JsonApiJsonPatchRequestScope(
                parentScope.getRoute().getPath(), parentScope.getJsonApiDocument(), parentScope);
        // verify wrap works
        assertEquals(parentScope.getUpdateStatusCode(), scope.getUpdateStatusCode());
        assertEquals(parentScope.getObjectEntityCache(), scope.getObjectEntityCache());

        Parent parent = newParent(7);

        PersistentResource<Parent> parentResource = new PersistentResource<>(parent, "1", scope);
        parentResource.updateAttribute("firstName", "foobar");

        ArgumentCaptor<Attribute> attributeArgument = ArgumentCaptor.forClass(Attribute.class);
        verify(tx, times(1)).setAttribute(eq(parent), attributeArgument.capture(), eq(scope));
        assertEquals(attributeArgument.getValue().getName(), "firstName");
        assertEquals(attributeArgument.getValue().getArguments().iterator().next().getValue(), "foobar");
    }

    @Test
    public void testFilterExpressionByType() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter[author.name][infix]",
                "Hemingway"
        );

        JsonApiRequestScope scope = buildJsonApiRequestScope("/", mock(DataStoreTransaction.class),
                new TestUser("1"), queryParams);

        Optional<FilterExpression> filter = scope.getLoadFilterExpression(ClassType.of(Author.class));
        FilterPredicate predicate = (FilterPredicate) filter.get();
        assertEquals("name", predicate.getField());
        assertEquals("name", predicate.getFieldPath());
        assertEquals(Operator.INFIX, predicate.getOperator());
        assertEquals(Arrays.asList("Hemingway"), predicate.getValues());
        assertEquals("[Author].name", predicate.getPath().toString());
    }

    @Test
    public void testFilterExpressionCollection() {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();

        add(queryParams,
                "filter[book.authors.name][infix]",
                "Hemingway"
        );

        JsonApiRequestScope scope = buildJsonApiRequestScope("/", mock(DataStoreTransaction.class), new TestUser("1"),
                queryParams);

        Optional<FilterExpression> filter = scope.getLoadFilterExpression(ClassType.of(Book.class));
        FilterPredicate predicate = (FilterPredicate) filter.get();
        assertEquals("name", predicate.getField());
        assertEquals("authors.name", predicate.getFieldPath());
        assertEquals(Operator.INFIX, predicate.getOperator());
        assertEquals(Arrays.asList("Hemingway"), predicate.getValues());
        assertEquals("[Book].authors/[Author].name", predicate.getPath().toString());
    }
}
