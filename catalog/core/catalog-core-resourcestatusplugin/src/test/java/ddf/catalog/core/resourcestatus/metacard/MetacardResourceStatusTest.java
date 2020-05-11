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
package ddf.catalog.core.resourcestatus.metacard;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import ddf.catalog.cache.ResourceCacheInterface;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.resource.data.ReliableResource;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MetacardResourceStatusTest {

  private static final String INTERNAL_LOCAL_RESOURCE = "internal.local-resource";

  private static final String LOCAL_SITE_NAME = "local-ddf";

  private static final String REMOTE_SITE_NAME = "remote-ddf";

  private static final String CONTENT_RESOURCE_URI = "content:f74e48380d9347b28a6b4fd88ffe024b";

  private static final String REMOTE_RESOURCE_URI =
      "https://remote-ddf:20002/services/catalog/sources/ddf.distribution/ce4de61db5da46bdbf6dad8fe6394663?transform=resource";

  private static final String METACARD_ID = "f74e48380d9347b28a6b4fd88ffe024b";

  private static final String RESOURCE_SIZE = "354";

  @Mock private ResourceCacheInterface cache;

  @Mock private ReliableResource cachedResource;

  @Mock private QueryResponse queryResponse;

  @Mock private MetacardImpl basicMetacard;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  /**
   * Metacard source id is local, Metacard contains no resource uri, Metacard resource is not cached
   */
  @Test
  public void testMetacardResourceIsNotLocal1() throws Exception {
    setupCache(false);
    setupSingleResultResponseMock(getBasicMetacard(LOCAL_SITE_NAME, null));
    MetacardResourceStatus plugin = getMetacardResourceStatusPlugin();
    Attribute resourceStatusAttribute =
        getInternalLocalResurceAttribute(plugin.process(queryResponse));
    assertThat(resourceStatusAttribute.getValue(), is(false));
  }

  /**
   * Metacard source id is local, Metacard contains remote resource uri, Metacard resource is not
   * cached
   */
  @Test
  public void testMetacardResourceIsNotLocal2() throws Exception {
    setupCache(false);
    setupSingleResultResponseMock(getBasicMetacard(LOCAL_SITE_NAME, REMOTE_RESOURCE_URI));
    MetacardResourceStatus plugin = getMetacardResourceStatusPlugin();
    Attribute resourceStatusAttribute =
        getInternalLocalResurceAttribute(plugin.process(queryResponse));
    assertThat(resourceStatusAttribute.getValue(), is(false));
  }

  /**
   * Metacard source id is remote, Metacard contains content resource uri, Metacard resource is not
   * cached
   */
  @Test
  public void testMetacardResourceIsNotLocal3() throws Exception {
    setupCache(false);
    setupSingleResultResponseMock(getBasicMetacard(REMOTE_SITE_NAME, CONTENT_RESOURCE_URI));
    MetacardResourceStatus plugin = getMetacardResourceStatusPlugin();
    Attribute resourceStatusAttribute =
        getInternalLocalResurceAttribute(plugin.process(queryResponse));
    assertThat(resourceStatusAttribute.getValue(), is(false));
  }

  /**
   * Metacard source id is remote, Metacard contains remote resource uri, Metacard resource is not
   * cached
   */
  @Test
  public void testMetacardResourceIsNotLocal4() throws Exception {
    setupCache(false);
    setupSingleResultResponseMock(getBasicMetacard(REMOTE_SITE_NAME, REMOTE_RESOURCE_URI));
    MetacardResourceStatus plugin = getMetacardResourceStatusPlugin();
    Attribute resourceStatusAttribute =
        getInternalLocalResurceAttribute(plugin.process(queryResponse));
    assertThat(resourceStatusAttribute.getValue(), is(false));
  }

  /**
   * Metacard contains no source id, Metacard contains remote resource uri, Metacard resource is not
   * cached
   */
  @Test
  public void testMetacardResourceIsNotLocal5() throws Exception {
    setupCache(false);
    setupSingleResultResponseMock(getBasicMetacard(null, REMOTE_RESOURCE_URI));
    MetacardResourceStatus plugin = getMetacardResourceStatusPlugin();
    Attribute resourceStatusAttribute =
        getInternalLocalResurceAttribute(plugin.process(queryResponse));
    assertThat(resourceStatusAttribute.getValue(), is(false));
  }

  /**
   * Metacard source id is local, Metacard contains content resource uri, Metacard resource is not
   * cached
   */
  @Test
  public void testMetacardResourceIsLocal1() throws Exception {
    setupCache(false);
    setupSingleResultResponseMock(getBasicMetacard(LOCAL_SITE_NAME, CONTENT_RESOURCE_URI));
    MetacardResourceStatus plugin = getMetacardResourceStatusPlugin();
    Attribute resourceStatusAttribute =
        getInternalLocalResurceAttribute(plugin.process(queryResponse));
    assertThat(resourceStatusAttribute.getValue(), is(true));
  }

  /**
   * Metacard source id is local, Metacard contains remote resource uri, Metacard resource is cached
   */
  @Test
  public void testMetacardResourceIsLocal2() throws Exception {
    setupCache(true);
    setupSingleResultResponseMock(getBasicMetacard(LOCAL_SITE_NAME, REMOTE_RESOURCE_URI));
    MetacardResourceStatus plugin = getMetacardResourceStatusPlugin();
    Attribute resourceStatusAttribute =
        getInternalLocalResurceAttribute(plugin.process(queryResponse));
    assertThat(resourceStatusAttribute.getValue(), is(true));
  }

  /**
   * Metacard source id is remote, Metacard contains content resource uri, Metacard resource is
   * cached
   */
  @Test
  public void testMetacardResourceIsLocal3() throws Exception {
    setupCache(true);
    setupSingleResultResponseMock(getBasicMetacard(REMOTE_SITE_NAME, CONTENT_RESOURCE_URI));
    MetacardResourceStatus plugin = getMetacardResourceStatusPlugin();
    Attribute resourceStatusAttribute =
        getInternalLocalResurceAttribute(plugin.process(queryResponse));
    assertThat(resourceStatusAttribute.getValue(), is(true));
  }

  /**
   * Metacard source id is remote, Metacard contains remote resource uri, Metacard resource is
   * cached
   */
  @Test
  public void testMetacardResourceIsLocal4() throws Exception {
    setupCache(true);
    setupSingleResultResponseMock(getBasicMetacard(REMOTE_SITE_NAME, REMOTE_RESOURCE_URI));
    MetacardResourceStatus plugin = getMetacardResourceStatusPlugin();
    Attribute resourceStatusAttribute =
        getInternalLocalResurceAttribute(plugin.process(queryResponse));
    assertThat(resourceStatusAttribute.getValue(), is(true));
  }

  private MetacardImpl getBasicMetacard(String sourceId, String resourceUri)
      throws URISyntaxException {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId(METACARD_ID);
    if (sourceId != null) {
      metacard.setSourceId(sourceId);
    }
    metacard.setResourceSize(RESOURCE_SIZE);
    if (resourceUri != null) {
      metacard.setResourceURI(new URI(resourceUri));
    }
    return metacard;
  }

  private MetacardResourceStatus getMetacardResourceStatusPlugin() {
    MetacardResourceStatus plugin =
        new MetacardResourceStatus(cache) {
          @Override
          String getLocalSiteName() {
            return LOCAL_SITE_NAME;
          }
        };

    return plugin;
  }

  private void setupCache(boolean isResourceCached) {
    when(cachedResource.getSize()).thenReturn(999L);
    when(cachedResource.hasProduct()).thenReturn(true);
    if (isResourceCached) {
      when(cache.getValid(anyString(), anyObject())).thenReturn(cachedResource);
    } else {
      when(cache.getValid(anyString(), anyObject())).thenReturn(null);
    }
  }

  private void setupSingleResultResponseMock(MetacardImpl metacard) {
    Result result = new ResultImpl(metacard);
    List<Result> results = new ArrayList<>();
    results.add(result);

    when(queryResponse.getResults()).thenReturn(results);
  }

  private Attribute getInternalLocalResurceAttribute(QueryResponse queryResponse) {
    Metacard resultMetacard = queryResponse.getResults().get(0).getMetacard();
    return resultMetacard.getAttribute(INTERNAL_LOCAL_RESOURCE);
  }
}
