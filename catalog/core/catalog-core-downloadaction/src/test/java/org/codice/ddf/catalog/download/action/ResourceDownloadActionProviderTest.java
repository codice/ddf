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

import static org.codice.ddf.catalog.download.action.ResourceDownloadActionEndpoint.CONTEXT_PATH;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import ddf.action.Action;
import ddf.catalog.data.Metacard;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import org.apache.commons.lang.CharEncoding;
import org.codice.ddf.catalog.resource.cache.ResourceCacheServiceMBean;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResourceDownloadActionProviderTest {

  private static final String ACTION_PROVIDER_ID = "actionID";

  private static final String DEFAULT_METACARD_ID = "ce4de61db5da46bdbf6dad8fe6394663";

  private static final String LOCAL_SITE_NAME = "local-ddf";

  private static final String REMOTE_SITE_NAME = "remote-ddf";

  private static final String REMOTE_RESOURCE_URI =
      "https://remote-ddf:20002/services/catalog/sources/ddf.distribution/ce4de61db5da46bdbf6dad8fe6394663?transform=resource";

  @Mock private Metacard mockMetacard;

  @Mock private ResourceCacheServiceMBean mockResourceCacheServiceMBeanProxy;

  private ResourceDownloadActionProvider actionProvider;

  @Before
  public void setup() {
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
  public void createMetacardAction() throws MalformedURLException {
    String title = "title";
    String description = "description";
    URL url = new URL("https://localhost/url");

    Action action =
        actionProvider.createMetacardAction(ACTION_PROVIDER_ID, title, description, url);

    assertThat(action.getId(), is(ACTION_PROVIDER_ID));
    assertThat(action.getTitle(), is(title));
    assertThat(action.getDescription(), is(description));
    assertThat(action.getUrl(), is(url));
  }

  @Test
  public void getMetacardActionUrl() throws Exception {
    when(mockMetacard.getId()).thenReturn(DEFAULT_METACARD_ID);
    URL url = actionProvider.getMetacardActionUrl(REMOTE_SITE_NAME, mockMetacard);

    assertThat(url, is(getUrl(DEFAULT_METACARD_ID)));
  }

  @Test
  public void getMetacardActionUrlEncodedAmpersand() throws Exception {
    String metacardId = "abc&def";
    when(mockMetacard.getId()).thenReturn(metacardId);
    URL url = actionProvider.getMetacardActionUrl(REMOTE_SITE_NAME, mockMetacard);
    assertThat(url, is(getUrl(metacardId)));
  }

  @Test(expected = URISyntaxException.class)
  public void getMetacardActionUrlWhenUrlIsMalformed() throws Exception {
    String invalidHost = "23^&*#";
    System.setProperty(SystemBaseUrl.EXTERNAL_HOST, invalidHost);

    when(mockMetacard.getId()).thenReturn(DEFAULT_METACARD_ID);
    actionProvider.getMetacardActionUrl(REMOTE_SITE_NAME, mockMetacard);
  }

  private URL getUrl(String metacardId) throws MalformedURLException, UnsupportedEncodingException {
    String encodedMetacardId = URLEncoder.encode(metacardId, CharEncoding.UTF_8);
    String urlString =
        String.format(
            "%s?source=%s&metacard=%s", CONTEXT_PATH, REMOTE_SITE_NAME, encodedMetacardId);
    return new URL(SystemBaseUrl.EXTERNAL.constructUrl(urlString, true));
  }
}
