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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

public class TransformerDescriptorsTest {

  private TransformerDescriptors descriptors =
      new TransformerDescriptors(
          ImmutableList.of(createMockServiceReference("foo", "bar")),
          ImmutableList.of(createMockServiceReference("foo", "bar")));

  @Test
  public void testGetTransformerDescriptor() {
    List<Map<String, String>> transformers =
        ImmutableList.of(ImmutableMap.of("id", "foo", "displayName", "bar"));

    Map<String, String> descriptor = descriptors.getTransformerDescriptor(transformers, "foo");

    assertThat(descriptor, hasEntry("id", "foo"));
    assertThat(transformers.get(0), hasEntry("displayName", "bar"));
  }

  @Test
  public void testGetTransformerDescriptorNotFound() {
    List<Map<String, String>> transformers = ImmutableList.of(ImmutableMap.of("id", "bar"));

    Map<String, String> descriptor = descriptors.getTransformerDescriptor(transformers, "foo");

    assertThat(descriptor, is(nullValue()));
  }

  private ServiceReference createMockServiceReference(String id, String displayName) {
    ServiceReference serviceReference = mock(ServiceReference.class);

    when(serviceReference.getProperty("id")).thenReturn(id);
    when(serviceReference.getProperty("displayName")).thenReturn(displayName);

    return serviceReference;
  }
}
