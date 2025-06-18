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
package org.codice.ddf.catalog.security.policy.xml;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.Assert;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.geotools.api.filter.Filter;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class XmlAttributeSecurityPolicyPluginTest {
  private static final String TEST_METADATA =
      "<metadata>\n"
          + "  <title controls=\"high med\" marker=\"A\" countries=\"USA AUS\">\n"
          + "      Title1!\n"
          + "  </title>\n"
          + "  <creator controls=\"low high\" marker=\"B\" countries=\"USA CAN\">\n"
          + "   <Organization>\n"
          + "    <name>Somebody</name>\n"
          + "    <phone>911</phone>\n"
          + "    <email>something@somehwere.com</email>\n"
          + "   </Organization>\n"
          + "  </creator> \n"
          + "  <security controls=\"low up\" marker=\"C\" countries=\"USA GBR\"/>\n"
          + " </metadata>";

  private static final String TEST_METADATA_2 =
      "<metadata>\n"
          + "  <title controls=\"high low\" marker=\"A\" countries=\"USA CAN AUS\">\n"
          + "      Title!\n"
          + "  </title>\n"
          + "  <creator controls=\"low med\" marker=\"B\" countries=\"USA GBR CAN\">\n"
          + "   <Organization>\n"
          + "    <name>Somebody</name>\n"
          + "    <phone>911</phone>\n"
          + "    <email>something@somehwere.com</email>\n"
          + "   </Organization>\n"
          + "  </creator> \n"
          + "  <security controls=\"low up\" marker=\"C\" countries=\"CAN USA DEN\"/>\n"
          + " </metadata>";

  private static final String TEST_METADATA_3 =
      "<metadata>\n"
          + "  <title controls=\"high up\" marker=\"A\" countries=\"CAN AUS\">\n"
          + "      Title2!\n"
          + "  </title>\n"
          + "  <creator controls=\"low down\" marker=\"B\" countries=\"GBR CAN\">\n"
          + "   <Organization>\n"
          + "    <name>Somebody</name>\n"
          + "    <phone>911</phone>\n"
          + "    <email>something@somehwere.com</email>\n"
          + "   </Organization>\n"
          + "  </creator> \n"
          + "  <security controls=\"high up\" marker=\"C\" countries=\"CAN DEN\"/>\n"
          + " </metadata>";

  private MetacardImpl metacard = new MetacardImpl();

  private XmlAttributeSecurityPolicyPlugin plugin;

  @Before
  public void setUp() {
    org.apache.log4j.Logger.getRootLogger()
        .addAppender(new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)));
    metacard.setMetadata(TEST_METADATA);
    plugin = new XmlAttributeSecurityPolicyPlugin();
    List<String> attributeUnions = Arrays.asList("controls", "marker");
    List<String> attributeIntersections = Arrays.asList("countries");
    List<String> xmlElements = Arrays.asList("title", "creator", "security");
    plugin.setXmlElements(xmlElements);
    plugin.setSecurityAttributeUnions(attributeUnions);
    plugin.setSecurityAttributeIntersections(attributeIntersections);
  }

  @Test
  public void testMetadataParse() {
    Assert.assertNull(metacard.getSecurity());

    Map<String, Set<String>> stringSetMap = plugin.parseSecurityMetadata(metacard);
    Collection<Set<String>> vals = stringSetMap.values();
    Iterator<Set<String>> iterator = vals.iterator();
    Set<String> list = iterator.next();
    Assert.assertTrue(list.contains("high"));
    Assert.assertTrue(list.contains("med"));
    Assert.assertTrue(list.contains("low"));
    Assert.assertTrue(list.contains("up"));

    list = iterator.next();
    Assert.assertTrue(list.size() == 3);
    Assert.assertTrue(list.contains("A"));
    Assert.assertTrue(list.contains("B"));
    Assert.assertTrue(list.contains("C"));

    list = iterator.next();
    Assert.assertTrue(list.size() == 1);
    Assert.assertTrue(list.contains("USA"));

    // Test same element, result should be default and the same element
    metacard = new MetacardImpl();
    metacard.setMetadata(TEST_METADATA_2);
    plugin.setSecurityAttributeUnions(Arrays.asList("countries"));
    plugin.setSecurityAttributeIntersections(new ArrayList<>());
    stringSetMap = plugin.parseSecurityMetadata(metacard);
    vals = stringSetMap.values();
    iterator = vals.iterator();
    list = iterator.next();
    Assert.assertTrue(list.size() == 5);
    Assert.assertTrue(list.contains("USA"));
    Assert.assertTrue(list.contains("CAN"));
    Assert.assertTrue(list.contains("GBR"));
    Assert.assertTrue(list.contains("DEN"));
    Assert.assertTrue(list.contains("AUS"));
  }

  @Test
  public void testEmptyGetters() {
    MetacardImpl m = new MetacardImpl();
    plugin.parseSecurityMetadata(m);

    Assert.assertNotNull(plugin.getXmlElements());
    Assert.assertNotNull(plugin.getSecurityAttributeIntersections());
    Assert.assertNotNull(plugin.getSecurityAttributeUnions());
  }

  @Test
  public void testCaseSensitivity() {
    plugin.setXmlElements(Arrays.asList("Title", "Creator", "Security"));
    Map<String, Set<String>> stringSetMap = plugin.parseSecurityMetadata(metacard);
    Assert.assertTrue(stringSetMap.isEmpty());
    plugin.setXmlElements(Arrays.asList("title", "creator", "security"));
    stringSetMap = plugin.parseSecurityMetadata(metacard);
    Assert.assertFalse(stringSetMap.isEmpty());
  }

  @Test
  public void testProcessQuery() throws StopProcessingException, PluginExecutionException {
    Result mockResult = mock(Result.class);
    when(mockResult.getMetacard()).thenReturn(metacard);
    PolicyResponse policyResponse = plugin.processPostQuery(mockResult, new HashMap<>());
    org.junit.Assert.assertThat(policyResponse.itemPolicy().entrySet().size(), Matchers.is(3));
  }

  @Test
  public void testProcessResource() throws StopProcessingException, PluginExecutionException {
    PolicyResponse policyResponse =
        plugin.processPostResource(mock(ResourceResponse.class), metacard);
    org.junit.Assert.assertThat(policyResponse.itemPolicy().entrySet().size(), Matchers.is(3));
  }

  @Test
  public void testProcessPreCreate() throws StopProcessingException {
    PolicyResponse policyResponse = plugin.processPreCreate(metacard, new HashMap<>());
    org.junit.Assert.assertThat(policyResponse.itemPolicy().entrySet().size(), Matchers.is(3));
  }

  @Test
  public void testProcessPreUpdate() throws StopProcessingException {
    PolicyResponse policyResponse = plugin.processPreUpdate(metacard, new HashMap<>());
    org.junit.Assert.assertThat(policyResponse.itemPolicy().entrySet().size(), Matchers.is(3));
  }

  @Test
  public void testProcessPreDelete() throws StopProcessingException {
    MetacardImpl metacard1 = new MetacardImpl();
    metacard1.setMetadata(TEST_METADATA_3);
    PolicyResponse policyResponse =
        plugin.processPreDelete(Arrays.asList(metacard, metacard1), new HashMap<>());
    org.junit.Assert.assertThat(policyResponse.operationPolicy().entrySet().size(), Matchers.is(3));
  }

  @Test
  public void testProcessUnusedMethods() throws StopProcessingException {
    PolicyResponse policyResponse =
        plugin.processPreQuery(new QueryImpl(Filter.INCLUDE), new HashMap<>());
    org.junit.Assert.assertThat(policyResponse.itemPolicy().entrySet().size(), Matchers.is(0));
    policyResponse = plugin.processPreResource(new ResourceRequestById(""));
    org.junit.Assert.assertThat(policyResponse.itemPolicy().entrySet().size(), Matchers.is(0));
  }
}
