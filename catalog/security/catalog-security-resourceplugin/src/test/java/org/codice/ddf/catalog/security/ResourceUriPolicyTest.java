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
package org.codice.ddf.catalog.security;

import static ddf.catalog.Constants.LOCAL_DESTINATION_KEY;
import static ddf.catalog.Constants.OPERATION_TRANSACTION_KEY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.permission.impl.PermissionsImpl;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class ResourceUriPolicyTest {

  public ResourceUriPolicyTest() throws URISyntaxException {}

  @Test
  public void testTwoEmptyUris() throws URISyntaxException, StopProcessingException {

    PolicyResponse response =
        getPolicyPlugin().processPreUpdate(getMockMetacard(""), getMockProperties(""));
    assertEmptyResponse(response);
  }

  @Test
  public void testInputUriNotEmptyAndMatchesCatalogUri()
      throws URISyntaxException, StopProcessingException {

    PolicyResponse response =
        getPolicyPlugin()
            .processPreUpdate(getMockMetacard("sampleURI"), getMockProperties("sampleURI"));
    assertEmptyResponse(response);
  }

  @Test
  public void testInputUriEmptyButCatalogUriNotEmpty()
      throws URISyntaxException, StopProcessingException {

    PolicyResponse response =
        getPolicyPlugin().processPreUpdate(getMockMetacard(""), getMockProperties("sampleURI"));
    assertNotEmpty(
        response,
        "If metacard has resource URI, but update does not, policy needed to ensure no overwriting occurs");
  }

  @Test
  public void testInputUriNotEmptyButCatalogUriEmpty()
      throws URISyntaxException, StopProcessingException {

    PolicyResponse response =
        getPolicyPlugin().processPreUpdate(getMockMetacard("sampleURI"), getMockProperties(""));
    assertNotEmpty(
        response,
        "If metacard has no resource URI, but update does, policy needed to ensure no overwriting occurs");
  }

  @Test
  public void testInputUriNotEmptyAndDifferentThanCatalogUri()
      throws URISyntaxException, StopProcessingException {

    PolicyResponse response =
        getPolicyPlugin()
            .processPreUpdate(getMockMetacard("differentURI"), getMockProperties("foo"));
    assertNotEmpty(
        response,
        "If metacard and update each has resource URI, but differ, policy needed to ensure no overwriting occurs");
  }

  @Test
  public void testCreatePermission() throws URISyntaxException, StopProcessingException {
    PolicyResponse response =
        getPolicyPlugin().processPreCreate(getMockMetacard("sampleURI"), getMockProperties("zoom"));
    Map<String, Set<String>> itemPolicy = response.itemPolicy();

    assertThat(
        "Creating a metacard with a resource URI requires special permissions",
        itemPolicy.containsKey("fizzle"),
        is(true));

    assertThat(itemPolicy.get("fizzle"), containsInAnyOrder("bang"));
  }

  @Test
  public void testNonLocalRequest() throws URISyntaxException, StopProcessingException {
    Map<String, Serializable> properties = getMockProperties("fizzle");
    properties.put(LOCAL_DESTINATION_KEY, false);
    PolicyResponse response =
        getPolicyPlugin().processPreCreate(getMockMetacard("sampleURI"), properties);
    assertEmptyResponse(response);
  }

  @Test
  public void testMissingMetacard() throws URISyntaxException, StopProcessingException {
    Metacard input = getMockMetacard("foo");
    when(input.getId()).thenReturn("this is not an id");
    assertNotEmpty(
        getPolicyPlugin().processPreUpdate(input, getMockProperties("bar")),
        "If the existing metacard is not present, assume the resource URI is being changed and require permission");
  }

  private ResourceUriPolicy getPolicyPlugin() {
    ResourceUriPolicy resourceUriPolicy =
        new ResourceUriPolicy(
            new String[] {"role=admin", "fizzle=bang"}, new String[] {"role=admin", "fizzle=bang"});
    resourceUriPolicy.setPermissions(new PermissionsImpl());
    return resourceUriPolicy;
  }

  private Metacard getMockMetacard(String inputResourceUri) throws URISyntaxException {
    MetacardType metacardType = mock(MetacardType.class);
    when(metacardType.getName()).thenReturn("ddf.metacard");
    Metacard inputMetacard = mock(Metacard.class);
    when(inputMetacard.getId()).thenReturn("id");
    when(inputMetacard.getResourceURI()).thenReturn(new URI(inputResourceUri));
    when(inputMetacard.getMetacardType()).thenReturn(metacardType);
    return inputMetacard;
  }

  private Map<String, Serializable> getMockProperties(String previousResourceUri)
      throws URISyntaxException {

    Metacard previousMetacard = getMockMetacard(previousResourceUri);
    OperationTransaction trx = mock(OperationTransaction.class);
    when(trx.getPreviousStateMetacards()).thenReturn(Collections.singletonList(previousMetacard));
    Map<String, Serializable> properties = new HashMap<>();
    properties.put(OPERATION_TRANSACTION_KEY, trx);
    properties.put(LOCAL_DESTINATION_KEY, true);
    return properties;
  }

  private void assertEmptyResponse(PolicyResponse response) {
    assertThat(
        "If existing metacard has resource URI and it matches updated resource URI, no policy permissions are needed",
        response.itemPolicy().isEmpty(),
        is(equalTo(true)));
  }

  private void assertNotEmpty(PolicyResponse policyResponse, String message) {
    assertThat(message, policyResponse.itemPolicy().isEmpty(), is(false));
  }
}
