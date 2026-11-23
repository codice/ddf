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
package ddf.catalog.source.solr;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.plugin.StopProcessingException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

public class PartialMetacardIngestPluginTest {

  private AttributeRegistry attributeRegistry;

  private PartialMetacardIngestPlugin plugin;

  @Before
  public void setup() {
    attributeRegistry = mock(AttributeRegistry.class);
    plugin = new PartialMetacardIngestPlugin(attributeRegistry);
  }

  @Test
  public void testInitAndDestroyRegisterPartialAttribute() {
    plugin.init();
    verify(attributeRegistry).register(SolrMetacardClientImpl.PARTIAL_RESULT_DESCRIPTOR);

    plugin.destroy();
    verify(attributeRegistry).deregister(SolrMetacardClientImpl.PARTIAL_RESULT_DESCRIPTOR);
  }

  @Test
  public void testCreateOfFullMetacardsAllowed() throws Exception {
    CreateRequest request = new CreateRequestImpl(Arrays.asList(metacard("1", null)));

    assertThat(plugin.process(request), is(request));
  }

  @Test
  public void testCreateOfPartialMetacardRejected() {
    CreateRequest request =
        new CreateRequestImpl(Arrays.asList(metacard("1", null), metacard("2", true)));

    try {
      plugin.process(request);
      fail("Expected StopProcessingException");
    } catch (StopProcessingException e) {
      assertThat(e.getMessage(), containsString("[2]"));
      assertThat(e.getMessage(), containsString("created"));
    }
  }

  @Test
  public void testUpdateOfFullMetacardAllowed() throws Exception {
    UpdateRequest request = new UpdateRequestImpl("1", metacard("1", false));

    assertThat(plugin.process(request), is(request));
  }

  @Test
  public void testUpdateOfPartialMetacardRejected() {
    UpdateRequest request = new UpdateRequestImpl("1", metacard("1", true));

    try {
      plugin.process(request);
      fail("Expected StopProcessingException");
    } catch (StopProcessingException e) {
      assertThat(e.getMessage(), containsString("[1]"));
      assertThat(e.getMessage(), containsString("updated"));
    }
  }

  @Test(expected = StopProcessingException.class)
  public void testUpdateWithStringPartialValueRejected() throws Exception {
    plugin.process(new UpdateRequestImpl("1", metacard("1", "true")));
  }

  @Test
  public void testDeleteAlwaysAllowed() {
    DeleteRequest request = new DeleteRequestImpl("1");

    assertThat(plugin.process(request), is(request));
  }

  @Test
  public void testEmptyRequestsAllowed() throws Exception {
    CreateRequest create = new CreateRequestImpl(Collections.emptyList());
    assertThat(plugin.process(create), is(create));
  }

  private static Metacard metacard(String id, Serializable partial) {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId(id);
    if (partial != null) {
      metacard.setAttribute(
          new AttributeImpl(SolrMetacardClientImpl.PARTIAL_RESULT_ATTRIBUTE, partial));
    }
    return metacard;
  }
}
