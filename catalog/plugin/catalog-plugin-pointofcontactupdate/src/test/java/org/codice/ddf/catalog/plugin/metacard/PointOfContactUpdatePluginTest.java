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
package org.codice.ddf.catalog.plugin.metacard;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

public class PointOfContactUpdatePluginTest {

  private static final String RESOURCE_ID1 = "resource-id1";

  private static final String RESOURCE_ID2 = "resource-id2";

  private static final String REGISTRY_ID = "registry-id";

  private static final String ADMIN_EMAIL = "admin@localhost.local";

  private static final String NEW_EMAIL = "new@email.com";

  private PointOfContactUpdatePlugin pointOfContactUpdatePlugin = new PointOfContactUpdatePlugin();

  private List<Metacard> listOfUpdatedMetacards;

  private UpdateRequestImpl updateRequestInput;

  private Map<String, Metacard> existingMetacards;

  @Before
  public void setup() throws Exception {
    listOfUpdatedMetacards = getListOfUpdatedMetacards();
    updateRequestInput =
        new UpdateRequestImpl(
            new String[] {RESOURCE_ID1, RESOURCE_ID2, REGISTRY_ID}, listOfUpdatedMetacards);
    existingMetacards = getPreviousMetacardsWithPointOfContact();
  }

  @Test
  public void processPreUpdateOnlyModifiesResourceMetacards() throws Exception {
    UpdateRequest updateRequestOutput =
        pointOfContactUpdatePlugin.processPreUpdate(updateRequestInput, existingMetacards);

    assertThat(
        updateRequestOutput
            .getUpdates()
            .get(0)
            .getValue()
            .getAttribute(Metacard.POINT_OF_CONTACT)
            .getValue(),
        equalTo(ADMIN_EMAIL));
    assertThat(
        updateRequestOutput
            .getUpdates()
            .get(1)
            .getValue()
            .getAttribute(Metacard.POINT_OF_CONTACT)
            .getValue(),
        equalTo(ADMIN_EMAIL));
    assertThat(
        updateRequestOutput.getUpdates().get(2).getValue().getAttribute(Metacard.POINT_OF_CONTACT),
        is(nullValue()));
  }

  @Test
  public void processPreUpdateDoesNothingIfNewPOCHasAValue() throws Exception {
    listOfUpdatedMetacards.forEach(
        m -> m.setAttribute(new AttributeImpl(Metacard.POINT_OF_CONTACT, NEW_EMAIL)));

    UpdateRequest updateRequestOutput =
        pointOfContactUpdatePlugin.processPreUpdate(updateRequestInput, existingMetacards);

    assertThat(
        updateRequestOutput
            .getUpdates()
            .get(0)
            .getValue()
            .getAttribute(Metacard.POINT_OF_CONTACT)
            .getValue(),
        equalTo(NEW_EMAIL));
    assertThat(
        updateRequestOutput
            .getUpdates()
            .get(1)
            .getValue()
            .getAttribute(Metacard.POINT_OF_CONTACT)
            .getValue(),
        equalTo(NEW_EMAIL));
    assertThat(
        updateRequestOutput
            .getUpdates()
            .get(2)
            .getValue()
            .getAttribute(Metacard.POINT_OF_CONTACT)
            .getValue(),
        equalTo(NEW_EMAIL));
  }

  @Test
  public void testPassthroughMethods() throws Exception {

    CreateRequest createRequest = mock(CreateRequest.class);
    DeleteRequest deleteRequest = mock(DeleteRequest.class);
    QueryRequest queryRequest = mock(QueryRequest.class);
    ResourceRequest resourceRequest = mock(ResourceRequest.class);

    DeleteResponse deleteResponse = mock(DeleteResponse.class);
    QueryResponse queryResponse = mock(QueryResponse.class);
    ResourceResponse resourceResponse = mock(ResourceResponse.class);

    assertThat(pointOfContactUpdatePlugin.processPreCreate(createRequest), is(createRequest));
    assertThat(pointOfContactUpdatePlugin.processPreDelete(deleteRequest), is(deleteRequest));
    assertThat(pointOfContactUpdatePlugin.processPostDelete(deleteResponse), is(deleteResponse));
    assertThat(pointOfContactUpdatePlugin.processPreQuery(queryRequest), is(queryRequest));
    assertThat(pointOfContactUpdatePlugin.processPostQuery(queryResponse), is(queryResponse));
    assertThat(pointOfContactUpdatePlugin.processPreResource(resourceRequest), is(resourceRequest));
    assertThat(
        pointOfContactUpdatePlugin.processPostResource(resourceResponse, mock(Metacard.class)),
        is(resourceResponse));

    verifyZeroInteractions(
        createRequest,
        deleteRequest,
        queryRequest,
        resourceRequest,
        deleteResponse,
        queryResponse,
        resourceResponse);
  }

  @Test
  public void getPreviousMetacardWithIdNullTest() throws Exception {
    UpdateRequestImpl updateRequestInput =
        new UpdateRequestImpl(
            new String[] {REGISTRY_ID},
            ImmutableList.of(getMetacardWithIdAndTag(REGISTRY_ID, "registry")));
    UpdateRequest updateRequestOutput =
        pointOfContactUpdatePlugin.processPreUpdate(updateRequestInput, ImmutableMap.of());
    assertEquals(updateRequestInput.getUpdates().get(0), updateRequestOutput.getUpdates().get(0));
  }

  private List<Metacard> getListOfUpdatedMetacards() {
    Metacard resourceMetacard1 = getMetacardWithIdAndTag(RESOURCE_ID1, "resource");
    Metacard resourceMetacard2 = getMetacardWithIdAndTag(RESOURCE_ID2, null);
    Metacard registryMetacard = getMetacardWithIdAndTag(REGISTRY_ID, "registry");

    return Arrays.asList(resourceMetacard1, resourceMetacard2, registryMetacard);
  }

  private Map<String, Metacard> getPreviousMetacardsWithPointOfContact() {
    Attribute pocAttribute = new AttributeImpl(Metacard.POINT_OF_CONTACT, ADMIN_EMAIL);
    Metacard resourceMetacard1 = getMetacardWithIdAndTag(RESOURCE_ID1, "resource");
    resourceMetacard1.setAttribute(pocAttribute);
    Metacard resourceMetacard2 = getMetacardWithIdAndTag(RESOURCE_ID2, null);
    resourceMetacard2.setAttribute(pocAttribute);
    Metacard registryMetacard = getMetacardWithIdAndTag(REGISTRY_ID, "registry");
    registryMetacard.setAttribute(pocAttribute);

    Map<String, Metacard> existingMetacards = new HashMap<String, Metacard>();
    existingMetacards.put(RESOURCE_ID1, resourceMetacard1);
    existingMetacards.put(RESOURCE_ID2, resourceMetacard2);
    existingMetacards.put(REGISTRY_ID, registryMetacard);

    return existingMetacards;
  }

  private Metacard getMetacardWithIdAndTag(String id, String tag) {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId(id);

    if (!StringUtils.isEmpty(tag)) {
      Set<String> tags = new HashSet<String>();
      tags.add(tag);
      metacard.setTags(tags);
    }

    return metacard;
  }
}
