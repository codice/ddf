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
package org.codice.ddf.catalog.download.action;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import org.codice.ddf.catalog.resource.cache.ResourceCacheServiceMBean;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(Parameterized.class)
public class ResourceDownloadActionProviderCanHandleTest {

  private static final String ACTION_PROVIDER_ID = "actionID";

  private static final String DEFAULT_METACARD_ID = "ce4de61db5da46bdbf6dad8fe6394663";

  private static final String LOCAL_SITE_NAME = "local-ddf";

  private static final String REMOTE_SITE_NAME = "remote-ddf";

  private static final String CONTENT_RESOURCE_URI = "content:f74e48380d9347b28a6b4fd88ffe024b";

  private static final String REMOTE_RESOURCE_URI =
      "https://remote-ddf:20002/services/catalog/sources/ddf.distribution/ce4de61db5da46bdbf6dad8fe6394663?transform=resource";

  @Mock private Metacard mockMetacard;

  @Mock private ResourceCacheServiceMBean mockResourceCacheServiceMBeanProxy;

  private ResourceDownloadActionProvider actionProvider;

  private String siteName;

  private String resourceUri;

  private String metacardId;

  private boolean isMetacardResourceCached;

  private boolean expectedCanHandle;

  public ResourceDownloadActionProviderCanHandleTest(
      String siteName,
      String resourceUri,
      String metacardId,
      boolean isMetacardResourceCached,
      boolean expectedCanHandle) {
    this.siteName = siteName;
    this.resourceUri = resourceUri;
    this.metacardId = metacardId;
    this.isMetacardResourceCached = isMetacardResourceCached;
    this.expectedCanHandle = expectedCanHandle;
  }

  @Parameters
  public static Collection<Object[]> getTestData() {
    return Arrays.asList(
        new Object[][] {
          {REMOTE_SITE_NAME, REMOTE_RESOURCE_URI, DEFAULT_METACARD_ID, true, false},
          {REMOTE_SITE_NAME, REMOTE_RESOURCE_URI, DEFAULT_METACARD_ID, false, true},
          {REMOTE_SITE_NAME, CONTENT_RESOURCE_URI, DEFAULT_METACARD_ID, true, false},
          {REMOTE_SITE_NAME, CONTENT_RESOURCE_URI, DEFAULT_METACARD_ID, false, true},
          {LOCAL_SITE_NAME, REMOTE_RESOURCE_URI, DEFAULT_METACARD_ID, true, false},
          {LOCAL_SITE_NAME, REMOTE_RESOURCE_URI, DEFAULT_METACARD_ID, false, true},
          {LOCAL_SITE_NAME, CONTENT_RESOURCE_URI, DEFAULT_METACARD_ID, true, false},
          {LOCAL_SITE_NAME, CONTENT_RESOURCE_URI, DEFAULT_METACARD_ID, false, false},
          {LOCAL_SITE_NAME, null, DEFAULT_METACARD_ID, false, false},
          {REMOTE_SITE_NAME, null, DEFAULT_METACARD_ID, false, false}
        });
  }

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    System.setProperty(SystemBaseUrl.EXTERNAL_HOST, "localhost");
    actionProvider =
        new ResourceDownloadActionProvider(ACTION_PROVIDER_ID) {
          @Override
          ResourceCacheServiceMBean createResourceCacheMBeanProxy() {
            return mockResourceCacheServiceMBeanProxy;
          }

          @Override
          String getLocalSiteName() {
            return LOCAL_SITE_NAME;
          }
        };
  }

  @Test
  public void testCanHandle() throws Exception {
    setupMockBasicMetacard(siteName, resourceUri, metacardId);
    setupMockResourceCacheServiceMBeanProxy(isMetacardResourceCached);
    assertThat(actionProvider.canHandleMetacard(mockMetacard), is(expectedCanHandle));
  }

  private void setupMockResourceCacheServiceMBeanProxy(boolean isMetacardResourceCached) {
    when(mockResourceCacheServiceMBeanProxy.contains(mockMetacard))
        .thenReturn(isMetacardResourceCached);
  }

  private void setupMockBasicMetacard(String sourceId, String resourceUri, String metacardId)
      throws URISyntaxException {
    when(mockMetacard.getId()).thenReturn(metacardId);
    if (sourceId != null) {
      when(mockMetacard.getSourceId()).thenReturn(sourceId);
    }
    if (resourceUri != null) {
      when(mockMetacard.getResourceURI()).thenReturn(new URI(resourceUri));
    }
  }
}
