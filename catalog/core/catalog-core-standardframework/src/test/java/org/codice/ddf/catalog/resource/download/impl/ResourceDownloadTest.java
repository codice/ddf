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
package org.codice.ddf.catalog.resource.download.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.download.ReliableResourceDownloadManager;
import java.io.IOException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import org.codice.ddf.catalog.resource.cache.ResourceCacheServiceMBean;
import org.codice.ddf.catalog.resource.download.ResourceDownloadMBean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResourceDownloadTest {
  private static final String METACARD_ID = "57a4b894e13a455b8cccb87cec778b58";

  private static final String SOURCE_ID = "ddf.distribution";

  @Mock private CatalogFramework mockCatalogFramework;

  @Mock private ReliableResourceDownloadManager mockDownloadManager;

  @Mock private ResourceCacheServiceMBean mockResourceCacheServiceMBean;

  @Mock private ResourceResponse mockResourceResponse;

  @Mock private MBeanServer mockMBeanServer;

  private ObjectName resourceDownloadObjectName;

  @Before
  public void before() throws Exception {
    resourceDownloadObjectName = new ObjectName(ResourceDownloadMBean.OBJECT_NAME);
  }

  @Test(expected = MBeanException.class)
  public void testCopyToLocalSiteWhenNullResourceResponse() throws Exception {
    // Setup
    setupMockResourceCacheServiceMBean(true);
    setupMockCatalogFramework(null, null);
    ResourceDownloadMBean resourceDownloadMBean = createResourceDownloadMBean();

    // Perform Test
    resourceDownloadMBean.copyToLocalSite(SOURCE_ID, METACARD_ID);
  }

  @Test(expected = MBeanException.class)
  public void testCopyToLocalSiteWhenCacheDisabled() throws Exception {
    // Setup
    setupMockResourceCacheServiceMBean(false);
    ResourceDownloadMBean resourceDownloadMBean = createResourceDownloadMBean();

    // Perform Test
    resourceDownloadMBean.copyToLocalSite(SOURCE_ID, METACARD_ID);
  }

  @Test(expected = MBeanException.class)
  public void testCopyToLocalSiteWhenCatalogFrameworkThrowsIOException() throws Exception {
    // Setup
    setupMockResourceCacheServiceMBean(true);
    setupMockCatalogFramework(mockResourceResponse, IOException.class);
    ResourceDownloadMBean resourceDownloadMBean = createResourceDownloadMBean();

    // Perform Test
    resourceDownloadMBean.copyToLocalSite(SOURCE_ID, METACARD_ID);
  }

  @Test(expected = MBeanException.class)
  public void testCopyToLocalSiteWhenCatalogFrameworkThrowsResourceNotSupportedException()
      throws Exception {
    // Setup
    setupMockResourceCacheServiceMBean(true);
    setupMockCatalogFramework(mockResourceResponse, ResourceNotSupportedException.class);
    ResourceDownloadMBean resourceDownloadMBean = createResourceDownloadMBean();

    // Perform Test
    resourceDownloadMBean.copyToLocalSite(SOURCE_ID, METACARD_ID);
  }

  @Test(expected = MBeanException.class)
  public void testCopyToLocalSiteWhenCatalogFrameworkThrowsResourceNotFoundException()
      throws Exception {
    // Setup
    setupMockResourceCacheServiceMBean(true);
    setupMockCatalogFramework(mockResourceResponse, ResourceNotFoundException.class);
    ResourceDownloadMBean resourceDownloadMBean = createResourceDownloadMBean();

    // Perform Test
    resourceDownloadMBean.copyToLocalSite(SOURCE_ID, METACARD_ID);
  }

  @Test
  public void testCopyToLocalSite() throws Exception {
    // Setup
    setupMockResourceCacheServiceMBean(true);
    setupMockCatalogFramework(mockResourceResponse, null);
    ResourceDownloadMBean resourceDownloadMBean = createResourceDownloadMBean();

    // Perform Test
    resourceDownloadMBean.copyToLocalSite(SOURCE_ID, METACARD_ID);
  }

  @Test
  public void testInit() throws Exception {
    // Setup
    setupMockMBeanServer(false, false);
    ResourceDownload resourceDownloadMBean = (ResourceDownload) createResourceDownloadMBean();

    // Perform Test
    resourceDownloadMBean.init();

    verify(mockMBeanServer).registerMBean(any(StandardMBean.class), eq(resourceDownloadObjectName));
  }

  @Test
  public void testInitMBeanAlreadyRegistered() throws Exception {
    // Setup
    setupMockMBeanServer(true, false);
    ResourceDownload resourceDownloadMBean = (ResourceDownload) createResourceDownloadMBean();

    // Perform Test
    resourceDownloadMBean.init();

    verify(mockMBeanServer, never())
        .registerMBean(any(StandardMBean.class), eq(resourceDownloadObjectName));
  }

  @Test(expected = MBeanRegistrationException.class)
  public void testInitMBeanMBeanRegistrationFails() throws Exception {
    // Setup
    setupMockMBeanServer(false, true);
    ResourceDownload resourceDownloadMBean = (ResourceDownload) createResourceDownloadMBean();

    // Perform Test
    resourceDownloadMBean.init();
  }

  @Test
  public void testDestroy() throws Exception {
    // Setup
    setupMockMBeanServer(true, false);
    ResourceDownload resourceDownloadMBean = (ResourceDownload) createResourceDownloadMBean();

    // Perform Test
    resourceDownloadMBean.destroy();

    verify(mockMBeanServer).unregisterMBean(eq(resourceDownloadObjectName));
  }

  @Test
  public void testDestroyMBeanNotRegistered() throws Exception {
    // Setup
    setupMockMBeanServer(false, false);
    ResourceDownload resourceDownloadMBean = (ResourceDownload) createResourceDownloadMBean();

    // Perform Test
    resourceDownloadMBean.destroy();

    verify(mockMBeanServer, never()).unregisterMBean(eq(resourceDownloadObjectName));
  }

  @Test(expected = MBeanRegistrationException.class)
  public void testDestroyMBeanUnregistrationFails() throws Exception {
    // Setup
    setupMockMBeanServer(true, true);
    ResourceDownload resourceDownloadMBean = (ResourceDownload) createResourceDownloadMBean();

    // Perform Test
    resourceDownloadMBean.destroy();
  }

  private ResourceDownloadMBean createResourceDownloadMBean() throws MalformedObjectNameException {
    return new ResourceDownload(
        mockMBeanServer, mockCatalogFramework, mockResourceCacheServiceMBean);
  }

  private void setupMockCatalogFramework(
      ResourceResponse mockResourceResponse, Class<? extends Throwable> exceptionClass)
      throws Exception {
    when(mockCatalogFramework.getResource(any(ResourceRequest.class), eq(SOURCE_ID)))
        .thenReturn(mockResourceResponse);

    if (exceptionClass != null) {
      doThrow(exceptionClass)
          .when(mockCatalogFramework)
          .getResource(any(ResourceRequest.class), eq(SOURCE_ID));
    }
  }

  private void setupMockResourceCacheServiceMBean(boolean isCacheEnabled) {
    when(mockResourceCacheServiceMBean.isCacheEnabled()).thenReturn(isCacheEnabled);
  }

  private void setupMockMBeanServer(boolean isRegistered, boolean throwMBeanRegException)
      throws Exception {
    when(mockMBeanServer.isRegistered(resourceDownloadObjectName)).thenReturn(isRegistered);
    if (throwMBeanRegException) {
      doThrow(new MBeanRegistrationException(new Exception(), ""))
          .when(mockMBeanServer)
          .registerMBean(any(StandardMBean.class), eq(resourceDownloadObjectName));
      doThrow(new MBeanRegistrationException(new Exception(), ""))
          .when(mockMBeanServer)
          .unregisterMBean(resourceDownloadObjectName);
    }
  }
}
