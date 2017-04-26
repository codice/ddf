/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.security.policy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.security.SubjectUtils;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;

public class UploaderPolicyExtensionTest {

    public static final String UPDATE_ACTION = "update";

    public static final String READ_ACTION = "read";

    public static final String ALL_SUFFIX = "-all";

    public static final String TEST_KEY = "test-key";

    public static final String CREATE_ACTION = "create";

    private UploaderPolicyExtension uploaderPolicyExtension = new UploaderPolicyExtension();

    private Set<String> pocPermissionValues = new HashSet<String>();

    private List<KeyValuePermission> listOfOperationPermissions =
            new ArrayList<KeyValuePermission>();

    @Before
    public void setup() {
        pocPermissionValues.add("admin@localhost");
        listOfOperationPermissions.add(new KeyValuePermission(TEST_KEY));
    }

    @Test
    public void isPermittedMatchAllWithNoPOCPermission() throws Exception {
        KeyValueCollectionPermission matchAllCollection = new KeyValueCollectionPermission(
                READ_ACTION,
                listOfOperationPermissions);

        KeyValueCollectionPermission returnedPermissions =
                uploaderPolicyExtension.isPermittedMatchAll(new CollectionPermission(),
                        matchAllCollection);

        assertThat(returnedPermissions.getAction(), equalTo(READ_ACTION));
        assertThat(returnedPermissions.getKeyValuePermissionList(), is(listOfOperationPermissions));
    }

    @Test
    public void isPermittedMatchAllImpliesAllPermissionsWithMatchingPOC() throws Exception {
        KeyValueCollectionPermission matchAllCollection =
                getMatchCollectionWithGivenSuffixAndAction(ALL_SUFFIX, READ_ACTION);

        CollectionPermission subjectAllCollection = getSubjectCollectionWithEmailAndGivenAction(
                matchAllCollection,
                READ_ACTION);

        KeyValueCollectionPermission returnedPermissions =
                uploaderPolicyExtension.isPermittedMatchAll(subjectAllCollection,
                        matchAllCollection);

        assertThat(returnedPermissions.getAction(), equalTo(READ_ACTION));
        assertThat(returnedPermissions.getKeyValuePermissionList(), hasSize(0));
    }

    @Test
    public void isPermittedMatchAllImpliesOnlyPOCOnCreate() throws Exception {
        KeyValueCollectionPermission matchAllCollection =
                getMatchCollectionWithGivenSuffixAndAction(ALL_SUFFIX, CREATE_ACTION);

        CollectionPermission subjectAllCollection = getSubjectCollectionWithEmailAndGivenAction(
                matchAllCollection, CREATE_ACTION);

        KeyValueCollectionPermission returnedPermissions =
                uploaderPolicyExtension.isPermittedMatchAll(subjectAllCollection,
                        matchAllCollection);

        hasAllPermissionsExceptPOCWithGivenAction(returnedPermissions, CREATE_ACTION);
    }

    @Test
    public void isPermittedMatchAllImpliesOnlyPOCWithNoEmail() throws Exception {
        KeyValueCollectionPermission matchAllCollection =
                getMatchCollectionWithGivenSuffixAndAction(ALL_SUFFIX, READ_ACTION);

        CollectionPermission subjectAllCollection = new CollectionPermission(READ_ACTION,
                matchAllCollection.getKeyValuePermissionList());

        KeyValueCollectionPermission returnedPermissions =
                uploaderPolicyExtension.isPermittedMatchAll(subjectAllCollection,
                        matchAllCollection);

        hasAllPermissionsExceptPOCWithGivenAction(returnedPermissions, READ_ACTION);
    }

    @Test
    public void isPermittedMatchAllImpliesOnlyPOCWithDifferentEmail() throws Exception {
        KeyValueCollectionPermission matchAllCollection =
                getMatchCollectionWithGivenSuffixAndAction(ALL_SUFFIX, READ_ACTION);

        Set<String> emailPermissionValues = new HashSet<String>();
        emailPermissionValues.add("test@localhost");
        List<KeyValuePermission> listOfSubjectPermissions = new ArrayList<KeyValuePermission>(
                matchAllCollection.getKeyValuePermissionList());
        listOfSubjectPermissions.add(new KeyValuePermission(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI,
                emailPermissionValues));
        CollectionPermission subjectAllCollection = new CollectionPermission(READ_ACTION,
                listOfSubjectPermissions);

        KeyValueCollectionPermission returnedPermissions =
                uploaderPolicyExtension.isPermittedMatchAll(subjectAllCollection,
                        matchAllCollection);

        hasAllPermissionsExceptPOCWithGivenAction(returnedPermissions, READ_ACTION);
    }

    @Test
    public void isPermittedMatchOneDoesNothingWithNoPOCPermission() throws Exception {
        KeyValueCollectionPermission matchAllCollection = new KeyValueCollectionPermission(
                READ_ACTION,
                listOfOperationPermissions);

        KeyValueCollectionPermission returnedPermissions =
                uploaderPolicyExtension.isPermittedMatchOne(new CollectionPermission(),
                        matchAllCollection);

        hasAllPermissionsExceptPOCWithGivenAction(returnedPermissions, READ_ACTION);
    }

    @Test
    public void isPermittedMatchOneImpliesAllPermissionsWithMatchingPOC() throws Exception {
        KeyValueCollectionPermission matchAllCollection =
                getMatchCollectionWithGivenSuffixAndAction("-one", UPDATE_ACTION);

        CollectionPermission subjectAllCollection = getSubjectCollectionWithEmailAndGivenAction(
                matchAllCollection,
                UPDATE_ACTION);

        KeyValueCollectionPermission returnedPermissions =
                uploaderPolicyExtension.isPermittedMatchOne(subjectAllCollection,
                        matchAllCollection);

        assertThat(returnedPermissions.getAction(), equalTo(UPDATE_ACTION));
        assertThat(returnedPermissions.getKeyValuePermissionList(), hasSize(0));
    }

    private CollectionPermission getSubjectCollectionWithEmailAndGivenAction(
            KeyValueCollectionPermission matchAllCollection, String action) {
        List<KeyValuePermission> listOfSubjectPermissions = new ArrayList<KeyValuePermission>(
                matchAllCollection.getKeyValuePermissionList());
        listOfSubjectPermissions.add(new KeyValuePermission(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI,
                pocPermissionValues));
        return new CollectionPermission(action, listOfSubjectPermissions);
    }

    private KeyValueCollectionPermission getMatchCollectionWithGivenSuffixAndAction(
            String pocSuffix, String action) {
        listOfOperationPermissions.add(new KeyValuePermission(Metacard.POINT_OF_CONTACT + pocSuffix,
                pocPermissionValues));

        return new KeyValueCollectionPermission(action, listOfOperationPermissions);
    }

    private void hasAllPermissionsExceptPOCWithGivenAction(KeyValueCollectionPermission returnedPermissions, String action) {
        assertThat(returnedPermissions.getAction(), equalTo(action));
        assertThat(returnedPermissions.getKeyValuePermissionList(), hasSize(1));
        KeyValuePermission returnedKeyValuePermission =
                (KeyValuePermission) returnedPermissions.getKeyValuePermissionList()
                        .get(0);
        assertThat(returnedKeyValuePermission.getKey(), equalTo(TEST_KEY));
    }
}
