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
package org.codice.ddf.catalog.resource.cache.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.impl.FrameworkProperties;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.resource.download.ReliableResourceDownloadManager;
import ddf.catalog.source.UnsupportedQueryException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import org.codice.ddf.catalog.resource.cache.ResourceCache;
import org.codice.ddf.catalog.resource.cache.ResourceCacheServiceMBean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengis.filter.Filter;

@RunWith(MockitoJUnitRunner.class)
public class ResourceCacheServiceTest {

  private static final String METACARD_ID = "metacardID";

  private ObjectName resourceCacheServiceObjectName;

  @Mock private ReliableResourceDownloadManager mockDownloadManager;

  @Mock private ResourceCache mockResourceCache;

  @Mock private ResourceCacheServiceMBean mockResourceCacheServiceMBean;

  @Mock private MBeanServer mockMBeanServer;

  @Mock private Metacard mockMetacard;

  @Mock private FrameworkProperties mockFrameworkProperties;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private FilterBuilder mockFilterBuilder;

  @Mock private Filter mockFilter;

  @Mock private CatalogFramework mockCatalogFramework;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private QueryResponse mockQueryResponse;

  @Before
  public void before() throws Exception {
    resourceCacheServiceObjectName = new ObjectName(ResourceCacheServiceMBean.OBJECT_NAME);
  }

  @Test
  public void testIsCacheEnabled() throws Exception {
    // Setup
    setupMockDownloadManager(true);

    // Perform Test
    ResourceCacheServiceMBean resourceCacheService = createResourceCacheServiceMBean();
    boolean isCacheEnabled = resourceCacheService.isCacheEnabled();

    assertThat(isCacheEnabled, is(true));
  }

  @Test
  public void testContainsResourceNotInCache() throws Exception {
    // Setup
    setupMockDownloadManager(true);
    setupMockResourceCache(false);

    // Perform Test
    ResourceCacheServiceMBean resourceCacheService = createResourceCacheServiceMBean();
    boolean isMetacardCached = resourceCacheService.contains(mockMetacard);

    assertThat(isMetacardCached, is(false));
  }

  @Test
  public void testContainsCacheDisabled() throws Exception {
    // Setup
    setupMockDownloadManager(false);
    setupMockResourceCache(false);

    // Perform Test
    ResourceCacheServiceMBean resourceCacheService = createResourceCacheServiceMBean();
    boolean isMetacardCached = resourceCacheService.contains(mockMetacard);

    assertThat(isMetacardCached, is(false));
  }

  @Test
  public void testContainsResourceInCache() throws Exception {
    // Setup
    setupMockDownloadManager(true);
    setupMockResourceCache(true);

    // Perform Test
    ResourceCacheServiceMBean resourceCacheService = createResourceCacheServiceMBean();
    boolean isMetacardCached = resourceCacheService.contains(mockMetacard);

    assertThat(isMetacardCached, is(true));
  }

  @Test
  public void testInit() throws Exception {
    // Setup
    setupMockMBeanServer(false, false);
    ResourceCacheService resourceCacheService =
        (ResourceCacheService) createResourceCacheServiceMBean();

    // Perform Test
    resourceCacheService.init();

    verify(mockMBeanServer)
        .registerMBean(any(StandardMBean.class), eq(resourceCacheServiceObjectName));
  }

  @Test
  public void testInitMBeanAlreadyRegistered() throws Exception {
    // Setup
    setupMockMBeanServer(true, false);
    ResourceCacheService resourceCacheService =
        (ResourceCacheService) createResourceCacheServiceMBean();

    // Perform Test
    resourceCacheService.init();

    verify(mockMBeanServer, never())
        .registerMBean(any(StandardMBean.class), eq(resourceCacheServiceObjectName));
  }

  @Test(expected = MBeanRegistrationException.class)
  public void testInitMBeanMBeanRegistrationFails() throws Exception {
    // Setup
    setupMockMBeanServer(false, true);
    ResourceCacheService resourceCacheService =
        (ResourceCacheService) createResourceCacheServiceMBean();

    // Perform Test
    resourceCacheService.init();
  }

  @Test
  public void testDestroy() throws Exception {
    // Setup
    setupMockMBeanServer(true, false);
    ResourceCacheService resourceCacheService =
        (ResourceCacheService) createResourceCacheServiceMBean();

    // Perform Test
    resourceCacheService.destroy();

    verify(mockMBeanServer).unregisterMBean(eq(resourceCacheServiceObjectName));
  }

  @Test
  public void testDestroyMBeanNotRegistered() throws Exception {
    // Setup
    setupMockMBeanServer(false, false);
    ResourceCacheService resourceCacheService =
        (ResourceCacheService) createResourceCacheServiceMBean();

    // Perform Test
    resourceCacheService.destroy();

    verify(mockMBeanServer, never()).unregisterMBean(eq(resourceCacheServiceObjectName));
  }

  @Test(expected = MBeanRegistrationException.class)
  public void testDestroyMBeanUnregistrationFails() throws Exception {
    // Setup
    setupMockMBeanServer(true, true);
    ResourceCacheService resourceCacheService =
        (ResourceCacheService) createResourceCacheServiceMBean();

    // Perform Test
    resourceCacheService.destroy();
  }

  @Test
  public void testContainsByIdResourceInCache() throws Exception {
    // Setup
    setupMockDownloadManager(true);
    setupMockResourceCache(true);
    setupMockFilterBuilder(METACARD_ID);
    setupMockFrameworkProperties();
    setupMockQueryResponse(1);
    setupMockCatalogFramework(null);

    // Perform Test
    ResourceCacheServiceMBean resourceCacheService = createResourceCacheServiceMBean();
    boolean isMetacardCached = resourceCacheService.containsById(METACARD_ID);

    assertThat(isMetacardCached, is(true));
  }

  @Test
  public void testContainsByIdNoMetacardFound() throws Exception {
    // Setup
    setupMockDownloadManager(true);
    setupMockResourceCache(true);
    setupMockFilterBuilder(METACARD_ID);
    setupMockFrameworkProperties();
    setupMockQueryResponse(0);
    setupMockCatalogFramework(null);

    // Perform Test
    ResourceCacheServiceMBean resourceCacheService = createResourceCacheServiceMBean();
    boolean isMetacardCached = resourceCacheService.containsById(METACARD_ID);

    assertThat(isMetacardCached, is(false));
  }

  @Test
  public void testContainsByIdCatalogFrameworkThrowsException() throws Exception {
    // Setup
    setupMockDownloadManager(true);
    setupMockResourceCache(true);
    setupMockFilterBuilder(METACARD_ID);
    setupMockFrameworkProperties();
    setupMockQueryResponse(0);
    setupMockCatalogFramework(UnsupportedQueryException.class);

    // Perform Test
    ResourceCacheServiceMBean resourceCacheService = createResourceCacheServiceMBean();
    boolean isMetacardCached = resourceCacheService.containsById(METACARD_ID);

    assertThat(isMetacardCached, is(false));
  }

  private void setupMockMBeanServer(boolean isRegistered, boolean throwMBeanRegException)
      throws Exception {
    when(mockMBeanServer.isRegistered(resourceCacheServiceObjectName)).thenReturn(isRegistered);
    if (throwMBeanRegException) {
      doThrow(new MBeanRegistrationException(new Exception(), ""))
          .when(mockMBeanServer)
          .registerMBean(any(StandardMBean.class), eq(resourceCacheServiceObjectName));
      doThrow(new MBeanRegistrationException(new Exception(), ""))
          .when(mockMBeanServer)
          .unregisterMBean(resourceCacheServiceObjectName);
    }
  }

  private ResourceCacheServiceMBean createResourceCacheServiceMBean()
      throws MalformedObjectNameException {
    return new ResourceCacheService(
        mockMBeanServer,
        mockResourceCache,
        mockDownloadManager,
        mockFrameworkProperties,
        mockCatalogFramework);
  }

  private void setupMockDownloadManager(boolean isCacheEnabled) {
    when(mockDownloadManager.isCacheEnabled()).thenReturn(isCacheEnabled);
  }

  private void setupMockResourceCache(boolean containsValid) {
    when(mockResourceCache.contains(mockMetacard)).thenReturn(containsValid);
  }

  private void setupMockFilterBuilder(String metacardId) {
    when(mockFilterBuilder.attribute(Metacard.ID).is().equalTo().text(metacardId))
        .thenReturn(mockFilter);
  }

  private void setupMockFrameworkProperties() {
    when(mockFrameworkProperties.getFilterBuilder()).thenReturn(mockFilterBuilder);
  }

  private void setupMockCatalogFramework(Class<? extends Exception> exceptionClass)
      throws Exception {
    if (exceptionClass == null) {
      when(mockCatalogFramework.query(any(QueryRequest.class))).thenReturn(mockQueryResponse);
    } else {
      doThrow(exceptionClass).when(mockCatalogFramework).query(any(QueryRequest.class));
    }
  }

  private void setupMockQueryResponse(int numberOfResults) {
    when(mockQueryResponse.getResults().size()).thenReturn(numberOfResults);
    when(mockQueryResponse.getResults().get(0).getMetacard()).thenReturn(mockMetacard);
  }
}
