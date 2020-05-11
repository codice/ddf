/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.security.policy.metacard;

import static ddf.catalog.Constants.OPERATION_TRANSACTION_KEY;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.plugin.PolicyResponse;
import ddf.security.permission.impl.PermissionsImpl;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PointOfContactPolicyPluginTest {

  public static final String TEST_POINT_OF_CONTACT = "test-point-of-contact";

  public static final String TEST_ID = "test-id";

  private PointOfContactPolicyPlugin pointOfContactPolicyPlugin =
      new PointOfContactPolicyPlugin(new PermissionsImpl());

  private List<Metacard> listWithMetacard = new ArrayList<Metacard>();

  @Mock private OperationTransaction mockOperationTransaction;

  @Test
  public void processPreCreateDoesNothing() throws java.lang.Exception {
    PolicyResponse response =
        pointOfContactPolicyPlugin.processPreCreate(new MetacardImpl(), Collections.emptyMap());

    responseIsEmpty(response);
  }

  @Test
  public void processPreUpdateDoesNothingWithNoPreviousMetacard() throws java.lang.Exception {
    when(mockOperationTransaction.getPreviousStateMetacards()).thenReturn(Collections.emptyList());
    Map<String, Serializable> inputProperties = new HashMap<String, Serializable>();
    inputProperties.put(OPERATION_TRANSACTION_KEY, mockOperationTransaction);

    PolicyResponse response =
        pointOfContactPolicyPlugin.processPreUpdate(new MetacardImpl(), inputProperties);

    responseIsEmpty(response);
  }

  @Test
  public void processPreUpdateDoesNothingWhenPointOfContactsAreSame() throws java.lang.Exception {
    MetacardImpl metacard = getMetacardWithPointOfContact(TEST_POINT_OF_CONTACT);

    PolicyResponse response =
        pointOfContactPolicyPlugin.processPreUpdate(metacard, setupAndGetInputProperties(metacard));

    responseIsEmpty(response);
  }

  @Test
  public void processPreUpdateDoesNothingWithWorkspaceMetacard() throws java.lang.Exception {
    Set<String> setOfTags = getSetWithGivenTag("workspace");

    MetacardImpl oldMetacard = getMetacardWithPointOfContact("edited-" + TEST_POINT_OF_CONTACT);
    oldMetacard.setTags(setOfTags);

    MetacardImpl newMetacard = getMetacardWithPointOfContact(TEST_POINT_OF_CONTACT);
    newMetacard.setTags(setOfTags);

    PolicyResponse response =
        pointOfContactPolicyPlugin.processPreUpdate(
            newMetacard, setupAndGetInputProperties(oldMetacard));

    responseIsEmpty(response);
  }

  @Test
  public void processPreUpdateReturnsPolicyWhenPointOfContactsAreDifferent()
      throws java.lang.Exception {
    Set<String> setOfTags = getSetWithGivenTag("resource");

    MetacardImpl oldMetacard = getMetacardWithPointOfContact("edited-" + TEST_POINT_OF_CONTACT);
    oldMetacard.setTags(setOfTags);

    MetacardImpl newMetacard = getMetacardWithPointOfContact(TEST_POINT_OF_CONTACT);
    newMetacard.setTags(setOfTags);

    PolicyResponse response =
        pointOfContactPolicyPlugin.processPreUpdate(
            newMetacard, setupAndGetInputProperties(oldMetacard));

    responseHasPolicy(response);
  }

  @Test
  public void processPreUpdateReturnsPolicyWhenOldPointOfContactIsNull()
      throws java.lang.Exception {
    MetacardImpl oldMetacard = getMetacardWithPointOfContact(null);

    PolicyResponse response =
        pointOfContactPolicyPlugin.processPreUpdate(
            getMetacardWithPointOfContact(TEST_POINT_OF_CONTACT),
            setupAndGetInputProperties(oldMetacard));

    responseHasPolicy(response);
  }

  @Test
  public void processPreUpdateReturnsPolicyWhenNewPointOfContactIsNull()
      throws java.lang.Exception {
    MetacardImpl oldMetacard = getMetacardWithPointOfContact(TEST_POINT_OF_CONTACT);

    PolicyResponse response =
        pointOfContactPolicyPlugin.processPreUpdate(
            getMetacardWithPointOfContact(null), setupAndGetInputProperties(oldMetacard));

    responseHasPolicy(response);
  }

  @Test
  public void processPreDeleteDoesNothing() throws java.lang.Exception {
    PolicyResponse response =
        pointOfContactPolicyPlugin.processPreDelete(
            Collections.emptyList(), Collections.emptyMap());

    responseIsEmpty(response);
  }

  @Test
  public void processPostDeleteDoesNothing() throws java.lang.Exception {
    PolicyResponse response =
        pointOfContactPolicyPlugin.processPostDelete(new MetacardImpl(), Collections.emptyMap());

    responseIsEmpty(response);
  }

  @Test
  public void processPreQueryDoesNothing() throws java.lang.Exception {
    PolicyResponse response =
        pointOfContactPolicyPlugin.processPreQuery(mock(Query.class), Collections.emptyMap());

    responseIsEmpty(response);
  }

  @Test
  public void processPostQueryDoesNothing() throws java.lang.Exception {
    PolicyResponse response =
        pointOfContactPolicyPlugin.processPostQuery(new ResultImpl(), Collections.emptyMap());

    responseIsEmpty(response);
  }

  @Test
  public void processPreResourceDoesNothing() throws java.lang.Exception {
    PolicyResponse response =
        pointOfContactPolicyPlugin.processPreResource(new ResourceRequestById(TEST_ID));

    responseIsEmpty(response);
  }

  @Test
  public void processPostResourceDoesNothing() throws java.lang.Exception {
    PolicyResponse response =
        pointOfContactPolicyPlugin.processPostResource(
            mock(ResourceResponse.class), new MetacardImpl());

    responseIsEmpty(response);
  }

  private Set<String> getSetWithGivenTag(String tag) {
    Set<String> setOfTags = new HashSet<String>();
    setOfTags.add(tag);
    return setOfTags;
  }

  private MetacardImpl getMetacardWithPointOfContact(String pointOfContact) {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId(TEST_ID);
    metacard.setPointOfContact(pointOfContact);
    return metacard;
  }

  private void responseIsEmpty(PolicyResponse response) {
    assertThat(response.itemPolicy().entrySet(), hasSize(0));
    assertThat(response.operationPolicy().entrySet(), hasSize(0));
  }

  private Map<String, Serializable> setupAndGetInputProperties(Metacard metacard) {
    listWithMetacard.add(metacard);
    listWithMetacard.add(new MetacardImpl());
    when(mockOperationTransaction.getPreviousStateMetacards()).thenReturn(listWithMetacard);
    Map<String, Serializable> inputProperties = new HashMap<String, Serializable>();
    inputProperties.put(OPERATION_TRANSACTION_KEY, mockOperationTransaction);
    return inputProperties;
  }

  private void responseHasPolicy(PolicyResponse response) {
    assertThat(response.itemPolicy().entrySet(), hasSize(1));
    assertTrue(
        response
            .itemPolicy()
            .get("read-only")
            .contains("Cannot update the point-of-contact field"));
    assertThat(response.operationPolicy().entrySet(), hasSize(0));
  }
}
