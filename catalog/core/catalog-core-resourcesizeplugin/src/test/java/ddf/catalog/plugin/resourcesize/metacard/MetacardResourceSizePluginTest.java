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
package ddf.catalog.plugin.resourcesize.metacard;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.cache.ResourceCacheInterface;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.resource.data.ReliableResource;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class MetacardResourceSizePluginTest {

  @Test
  public void testMetacardResourceSizePopulatedAndHasProduct() throws Exception {
    ResourceCacheInterface cache = mock(ResourceCacheInterface.class);
    ReliableResource cachedResource = mock(ReliableResource.class);
    when(cachedResource.getSize()).thenReturn(999L);
    when(cachedResource.hasProduct()).thenReturn(true);
    when(cache.getValid(anyString(), (Metacard) anyObject())).thenReturn(cachedResource);

    MetacardImpl metacard = new MetacardImpl();
    metacard.setId("abc123");
    metacard.setSourceId("ddf-1");
    metacard.setResourceSize("N/A");

    Result result = new ResultImpl(metacard);
    List<Result> results = new ArrayList<Result>();
    results.add(result);

    QueryResponse input = mock(QueryResponse.class);
    when(input.getResults()).thenReturn(results);

    MetacardResourceSizePlugin plugin = new MetacardResourceSizePlugin(cache);
    QueryResponse queryResponse = plugin.process(input);
    assertThat(queryResponse.getResults().size(), is(1));
    Metacard resultMetacard = queryResponse.getResults().get(0).getMetacard();
    assertThat(metacard, is(notNullValue()));
    // Since using Metacard vs. MetacardImpl have to get resource-size as an
    // Attribute vs. Long
    Attribute resourceSizeAttr = resultMetacard.getAttribute(Metacard.RESOURCE_SIZE);
    assertThat((String) resourceSizeAttr.getValue(), is("999"));
  }

  /**
   * Verifies case where product has been cached previously but has since been deleted from the
   * product-cache directory, so there is still an entry in the cache map but no cache file on disk.
   *
   * @throws Exception
   */
  @Test
  public void testMetacardResourceSizePopulatedButNoProduct() throws Exception {
    ResourceCacheInterface cache = mock(ResourceCacheInterface.class);
    ReliableResource cachedResource = mock(ReliableResource.class);
    when(cachedResource.getSize()).thenReturn(999L);
    when(cachedResource.hasProduct()).thenReturn(false);
    when(cache.getValid(anyString(), (Metacard) anyObject())).thenReturn(cachedResource);

    MetacardImpl metacard = new MetacardImpl();
    metacard.setId("abc123");
    metacard.setSourceId("ddf-1");
    metacard.setResourceSize("N/A");

    Result result = new ResultImpl(metacard);
    List<Result> results = new ArrayList<Result>();
    results.add(result);

    QueryResponse input = mock(QueryResponse.class);
    when(input.getResults()).thenReturn(results);

    MetacardResourceSizePlugin plugin = new MetacardResourceSizePlugin(cache);
    QueryResponse queryResponse = plugin.process(input);
    assertThat(queryResponse.getResults().size(), is(1));
    Metacard resultMetacard = queryResponse.getResults().get(0).getMetacard();
    assertThat(metacard, is(notNullValue()));
    // Since using Metacard vs. MetacardImpl have to get resource-size as an
    // Attribute vs. String
    Attribute resourceSizeAttr = resultMetacard.getAttribute(Metacard.RESOURCE_SIZE);
    assertThat((String) resourceSizeAttr.getValue(), equalTo("N/A"));
  }

  @Test
  public void testNullMetacard() throws Exception {
    ResourceCacheInterface cache = mock(ResourceCacheInterface.class);

    Result result = mock(Result.class);
    when(result.getMetacard()).thenReturn(null);
    List<Result> results = new ArrayList<Result>();
    results.add(result);

    QueryResponse input = mock(QueryResponse.class);
    when(input.getResults()).thenReturn(results);

    MetacardResourceSizePlugin plugin = new MetacardResourceSizePlugin(cache);
    QueryResponse queryResponse = plugin.process(input);
    assertThat(queryResponse, equalTo(input));
  }

  @Test
  public void testWhenNoCachedResourceFound() throws Exception {
    ResourceCacheInterface cache = mock(ResourceCacheInterface.class);
    when(cache.getValid(anyString(), (Metacard) anyObject())).thenReturn(null);

    MetacardImpl metacard = new MetacardImpl();
    metacard.setId("abc123");
    metacard.setSourceId("ddf-1");
    metacard.setResourceSize("N/A");

    Result result = new ResultImpl(metacard);
    List<Result> results = new ArrayList<Result>();
    results.add(result);

    QueryResponse input = mock(QueryResponse.class);
    when(input.getResults()).thenReturn(results);

    MetacardResourceSizePlugin plugin = new MetacardResourceSizePlugin(cache);
    QueryResponse queryResponse = plugin.process(input);
    assertThat(queryResponse.getResults().size(), is(1));
    Metacard resultMetacard = queryResponse.getResults().get(0).getMetacard();
    assertThat(metacard, is(notNullValue()));
    // Since using Metacard vs. MetacardImpl have to get resource-size as an
    // Attribute vs. Long
    Attribute resourceSizeAttr = resultMetacard.getAttribute(Metacard.RESOURCE_SIZE);
    assertThat((String) resourceSizeAttr.getValue(), equalTo("N/A"));
  }

  @Test
  public void testWhenCachedResourceSizeIsZero() throws Exception {
    ResourceCacheInterface cache = mock(ResourceCacheInterface.class);
    ReliableResource cachedResource = mock(ReliableResource.class);
    when(cachedResource.getSize()).thenReturn(0L);
    when(cache.getValid(anyString(), (Metacard) anyObject())).thenReturn(cachedResource);

    MetacardImpl metacard = new MetacardImpl();
    metacard.setId("abc123");
    metacard.setSourceId("ddf-1");
    metacard.setResourceSize("N/A");

    Result result = new ResultImpl(metacard);
    List<Result> results = new ArrayList<Result>();
    results.add(result);

    QueryResponse input = mock(QueryResponse.class);
    when(input.getResults()).thenReturn(results);

    MetacardResourceSizePlugin plugin = new MetacardResourceSizePlugin(cache);
    QueryResponse queryResponse = plugin.process(input);
    assertThat(queryResponse.getResults().size(), is(1));
    Metacard resultMetacard = queryResponse.getResults().get(0).getMetacard();
    assertThat(metacard, is(notNullValue()));
    // Since using Metacard vs. MetacardImpl have to get resource-size as an
    // Attribute vs. Long
    Attribute resourceSizeAttr = resultMetacard.getAttribute(Metacard.RESOURCE_SIZE);
    assertThat((String) resourceSizeAttr.getValue(), equalTo("N/A"));
  }
}
