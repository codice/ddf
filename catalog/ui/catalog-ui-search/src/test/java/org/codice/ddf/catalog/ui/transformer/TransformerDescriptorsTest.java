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
package org.codice.ddf.catalog.ui.transformer;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

public class TransformerDescriptorsTest {

  private TransformerDescriptors descriptors =
      new TransformerDescriptors(
          ImmutableList.of(
              createMockServiceReference("foo", "bar"),
              createMockServiceReference("hello", null),
              createMockServiceReference("world", null)),
          ImmutableList.of(
              createMockServiceReference("bar", "foo"), createMockServiceReference(null, null)));

  @Before
  public void setup() {
    descriptors.setBlackListedMetacardTransformerIds(Collections.singleton("world"));
  }

  @Test
  public void testGetMetacardTransformerDescriptors() {
    List<Map<String, String>> transformerDescriptors = descriptors.getMetacardTransformers();

    assertThat(transformerDescriptors, hasSize(2));
    assertTransformerDescriptor(transformerDescriptors.get(0), "foo", "bar");
  }

  @Test
  public void testGetQueryResponseTransformerDescriptors() {
    List<Map<String, String>> transformerDescriptors = descriptors.getQueryResponseTransformers();

    assertThat(transformerDescriptors, hasSize(1));
    assertTransformerDescriptor(transformerDescriptors.get(0), "bar", "foo");
  }

  @Test
  public void testGetMetacardTransformerDescriptorNotFound() {
    Map<String, String> descriptor = descriptors.getMetacardTransformer("bar");

    assertThat(descriptor, is(nullValue()));
  }

  @Test
  public void testGetQueryResponseTransformerDescriptorNotFound() {
    Map<String, String> descriptor = descriptors.getQueryResponseTransformer("foo");

    assertThat(descriptor, is(nullValue()));
  }

  @Test
  public void testGetTransformerDescriptorNullDisplayName() {
    Map<String, String> descriptor = descriptors.getMetacardTransformer("hello");

    assertTransformerDescriptor(descriptor, "hello", "hello");
  }

  @Test
  public void testGetBlacklistedTransformerDescriptor() {
    Map<String, String> descriptor = descriptors.getMetacardTransformer("world");

    assertThat(descriptor, is(nullValue()));
  }

  @Test
  public void testGetBlacklistedQueryResponseTransformerDescriptor() {
    Map<String, String> descriptor = descriptors.getQueryResponseTransformer("zipCompression");

    assertThat(descriptor, is(nullValue()));
  }

  @Test
  public void testGetBlacklistedQueryResponseTransformer() {
    descriptors.setBlackListedQueryResponseTransformerIds(ImmutableSet.of("bar"));

    Map<String, String> descriptor = descriptors.getQueryResponseTransformer("bar");

    assertThat(descriptor, is(nullValue()));
  }

  @Test
  public void testGetBlacklistedMetacardTransformers() {
    Set<String> blacklist = ImmutableSet.of("hello", "world");

    descriptors.setBlackListedMetacardTransformerIds(blacklist);

    assertThat(descriptors.getBlackListedMetacardTransformerIds(), is(blacklist));
  }

  private void assertTransformerDescriptor(
      Map<String, String> descriptor, String id, String displayName) {
    assertThat(descriptor, hasEntry("id", id));
    assertThat(descriptor, hasEntry("displayName", displayName));
  }

  private ServiceReference createMockServiceReference(String id, String displayName) {
    ServiceReference serviceReference = mock(ServiceReference.class);

    when(serviceReference.getProperty("id")).thenReturn(id);
    when(serviceReference.getProperty("displayName")).thenReturn(displayName);

    return serviceReference;
  }
}
