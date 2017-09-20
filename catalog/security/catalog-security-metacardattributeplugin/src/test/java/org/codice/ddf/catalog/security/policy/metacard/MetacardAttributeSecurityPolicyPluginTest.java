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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class MetacardAttributeSecurityPolicyPluginTest {

  MetacardAttributeSecurityPolicyPlugin plugin;

  Metacard metacard;

  Metacard metacard1;

  Metacard metacard2;

  @Before
  public void setup() {
    metacard = new MetacardImpl();
    metacard.setAttribute(new AttributeImpl("parsed.security", Arrays.asList("A", "B", "C")));
    metacard.setAttribute(new AttributeImpl("parsed.countries", Arrays.asList("USA", "CAN")));
    metacard.setAttribute(new AttributeImpl("parsed.other", Arrays.asList("X", "Y")));
    metacard1 = new MetacardImpl();
    metacard1.setAttribute(new AttributeImpl("parsed.security", Arrays.asList("X", "Y", "Z")));
    metacard1.setAttribute(new AttributeImpl("parsed.countries", Arrays.asList("GBR", "CAN")));
    metacard1.setAttribute(new AttributeImpl("parsed.other", Arrays.asList("E", "F")));
    metacard2 = new MetacardImpl();
    metacard2.setAttribute(new AttributeImpl("source1", Arrays.asList("A", "B", "C")));
    metacard2.setAttribute(new AttributeImpl("source2", Arrays.asList("B", "C")));
    metacard2.setAttribute(new AttributeImpl("source3", Arrays.asList("D", "E", "F")));
    metacard2.setAttribute(new AttributeImpl("source4", Arrays.asList("F", "G", "H")));

    plugin = new MetacardAttributeSecurityPolicyPlugin();
    plugin.setUnionMetacardAttributes(
        Arrays.asList("parsed.security=mapped.security", "parsed.countries=mapped.countries"));
  }

  @Test
  public void testUnionAttributes() throws Exception {
    plugin.setUnionMetacardAttributes(
        Arrays.asList("source1=dest", "source2=dest", "source3=dest", "source4=dest2"));
    PolicyResponse policyResponse = plugin.processPreCreate(metacard2, new HashMap<>());
    Map<String, Set<String>> itemPolicy = policyResponse.itemPolicy();
    assertThat(itemPolicy.size(), is(2));

    assertThat(itemPolicy.get("dest").size(), is(6));
    assertTrue(itemPolicy.get("dest").containsAll(ImmutableSet.of("A", "B", "C", "D", "E", "F")));
    assertThat(itemPolicy.get("dest2").size(), is(3));
    assertTrue(itemPolicy.get("dest2").containsAll(ImmutableSet.of("F", "G", "H")));
  }

  @Test
  public void testIntersectAttributes() throws Exception {
    plugin.setIntersectMetacardAttributes(
        Arrays.asList("source1=dest", "source2=dest", "source3=dest2", "source4=dest2"));
    PolicyResponse policyResponse = plugin.processPreCreate(metacard2, new HashMap<>());
    Map<String, Set<String>> itemPolicy = policyResponse.itemPolicy();
    assertThat(itemPolicy.size(), is(2));

    assertThat(itemPolicy.get("dest").size(), is(2));
    assertTrue(itemPolicy.get("dest").containsAll(ImmutableSet.of("B", "C")));
    assertThat(itemPolicy.get("dest2").size(), is(1));
    assertTrue(itemPolicy.get("dest2").containsAll(ImmutableSet.of("F")));
  }

  @Test
  public void testUnionAndIntersectAttributes() throws Exception {
    plugin.setIntersectMetacardAttributes(Arrays.asList("source1=dest", "source2=dest"));
    plugin.setUnionMetacardAttributes(Arrays.asList("source3=dest2", "source4=dest2"));
    PolicyResponse policyResponse = plugin.processPreCreate(metacard2, new HashMap<>());
    Map<String, Set<String>> itemPolicy = policyResponse.itemPolicy();
    assertThat(itemPolicy.size(), is(2));

    assertThat(itemPolicy.get("dest").size(), is(2));
    assertTrue(itemPolicy.get("dest").containsAll(ImmutableSet.of("B", "C")));
    assertThat(itemPolicy.get("dest2").size(), is(5));
    assertTrue(itemPolicy.get("dest2").containsAll(ImmutableSet.of("D", "E", "F", "G", "H")));
  }

  @Test
  public void testBadAttributeDef() throws StopProcessingException {
    plugin.setUnionMetacardAttributes(Arrays.asList("parsed.security", "parsed.countries"));
    PolicyResponse policyResponse = plugin.processPreCreate(metacard, new HashMap<>());
    assertThat(policyResponse.itemPolicy().size(), is(2));

    assertTrue(policyResponse.itemPolicy().get("parsed.security").contains("A"));
    assertTrue(policyResponse.itemPolicy().get("parsed.security").contains("B"));
    assertTrue(policyResponse.itemPolicy().get("parsed.security").contains("C"));

    assertTrue(policyResponse.itemPolicy().get("parsed.countries").contains("USA"));
    assertTrue(policyResponse.itemPolicy().get("parsed.countries").contains("CAN"));

    assertNull(policyResponse.itemPolicy().get("mapped.security"));
    assertNull(policyResponse.itemPolicy().get("mapped.countries"));
  }

  @Test
  public void testProcessPreCreate() throws StopProcessingException {
    PolicyResponse policyResponse = plugin.processPreCreate(metacard, new HashMap<>());
    assertThat(policyResponse.itemPolicy().size(), is(2));

    assertTrue(policyResponse.itemPolicy().get("mapped.security").contains("A"));
    assertTrue(policyResponse.itemPolicy().get("mapped.security").contains("B"));
    assertTrue(policyResponse.itemPolicy().get("mapped.security").contains("C"));

    assertTrue(policyResponse.itemPolicy().get("mapped.countries").contains("USA"));
    assertTrue(policyResponse.itemPolicy().get("mapped.countries").contains("CAN"));

    assertNull(policyResponse.itemPolicy().get("parsed.security"));
    assertNull(policyResponse.itemPolicy().get("parsed.countries"));
    assertNull(policyResponse.itemPolicy().get("parsed.other"));
  }

  @Test
  public void testProcessPreUpdate() throws StopProcessingException {
    PolicyResponse policyResponse = plugin.processPreUpdate(metacard, new HashMap<>());
    assertThat(policyResponse.itemPolicy().size(), is(2));

    assertTrue(policyResponse.itemPolicy().get("mapped.security").contains("A"));
    assertTrue(policyResponse.itemPolicy().get("mapped.security").contains("B"));
    assertTrue(policyResponse.itemPolicy().get("mapped.security").contains("C"));

    assertTrue(policyResponse.itemPolicy().get("mapped.countries").contains("USA"));
    assertTrue(policyResponse.itemPolicy().get("mapped.countries").contains("CAN"));

    assertNull(policyResponse.itemPolicy().get("parsed.security"));
    assertNull(policyResponse.itemPolicy().get("parsed.countries"));
    assertNull(policyResponse.itemPolicy().get("parsed.other"));
  }

  @Test
  public void testProcessPreDelete() throws StopProcessingException {
    PolicyResponse policyResponse =
        plugin.processPreDelete(Arrays.asList(metacard, metacard1), new HashMap<>());
    assertThat(policyResponse.operationPolicy().size(), is(2));

    assertTrue(policyResponse.operationPolicy().get("mapped.security").contains("A"));
    assertTrue(policyResponse.operationPolicy().get("mapped.security").contains("B"));
    assertTrue(policyResponse.operationPolicy().get("mapped.security").contains("C"));
    assertTrue(policyResponse.operationPolicy().get("mapped.security").contains("X"));
    assertTrue(policyResponse.operationPolicy().get("mapped.security").contains("Y"));
    assertTrue(policyResponse.operationPolicy().get("mapped.security").contains("Z"));

    assertTrue(policyResponse.operationPolicy().get("mapped.countries").contains("USA"));
    assertTrue(policyResponse.operationPolicy().get("mapped.countries").contains("CAN"));
    assertTrue(policyResponse.operationPolicy().get("mapped.countries").contains("GBR"));

    assertNull(policyResponse.itemPolicy().get("parsed.security"));
    assertNull(policyResponse.itemPolicy().get("parsed.countries"));
  }

  @Test
  public void testProcessPostDelete() throws StopProcessingException {
    PolicyResponse policyResponse = plugin.processPostDelete(metacard, new HashMap<>());
    assertThat(policyResponse.itemPolicy().size(), is(2));

    assertTrue(policyResponse.itemPolicy().get("mapped.security").contains("A"));
    assertTrue(policyResponse.itemPolicy().get("mapped.security").contains("B"));
    assertTrue(policyResponse.itemPolicy().get("mapped.security").contains("C"));

    assertTrue(policyResponse.itemPolicy().get("mapped.countries").contains("USA"));
    assertTrue(policyResponse.itemPolicy().get("mapped.countries").contains("CAN"));

    assertNull(policyResponse.itemPolicy().get("parsed.security"));
    assertNull(policyResponse.itemPolicy().get("parsed.countries"));
    assertNull(policyResponse.itemPolicy().get("parsed.other"));
  }

  @Test
  public void testProcessPostQuery() throws StopProcessingException {
    Result result = mock(Result.class);
    when(result.getMetacard()).thenReturn(metacard);
    PolicyResponse policyResponse = plugin.processPostQuery(result, new HashMap<>());
    assertThat(policyResponse.itemPolicy().size(), is(2));

    assertTrue(policyResponse.itemPolicy().get("mapped.security").contains("A"));
    assertTrue(policyResponse.itemPolicy().get("mapped.security").contains("B"));
    assertTrue(policyResponse.itemPolicy().get("mapped.security").contains("C"));

    assertTrue(policyResponse.itemPolicy().get("mapped.countries").contains("USA"));
    assertTrue(policyResponse.itemPolicy().get("mapped.countries").contains("CAN"));

    assertNull(policyResponse.itemPolicy().get("parsed.security"));
    assertNull(policyResponse.itemPolicy().get("parsed.countries"));
    assertNull(policyResponse.itemPolicy().get("parsed.other"));
  }

  @Test
  public void testProcessPostResource() throws StopProcessingException {
    PolicyResponse policyResponse =
        plugin.processPostResource(mock(ResourceResponse.class), metacard);
    assertThat(policyResponse.itemPolicy().size(), is(2));

    assertTrue(policyResponse.itemPolicy().get("mapped.security").contains("A"));
    assertTrue(policyResponse.itemPolicy().get("mapped.security").contains("B"));
    assertTrue(policyResponse.itemPolicy().get("mapped.security").contains("C"));

    assertTrue(policyResponse.itemPolicy().get("mapped.countries").contains("USA"));
    assertTrue(policyResponse.itemPolicy().get("mapped.countries").contains("CAN"));

    assertNull(policyResponse.itemPolicy().get("parsed.security"));
    assertNull(policyResponse.itemPolicy().get("parsed.countries"));
    assertNull(policyResponse.itemPolicy().get("parsed.other"));
  }

  @Test
  public void testUnusedMethods() throws StopProcessingException {
    PolicyResponse policyResponse = plugin.processPreQuery(mock(Query.class), new HashMap<>());
    assertThat(policyResponse.itemPolicy().size(), is(0));

    policyResponse = plugin.processPreResource(mock(ResourceRequest.class));
    assertThat(policyResponse.itemPolicy().size(), is(0));
  }
}
