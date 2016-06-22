/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.resource.actions;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ddf.catalog.resource.download.ResourceDownloadEndpoint.CONTEXT_PATH;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.lang.CharEncoding;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ddf.action.Action;
import ddf.catalog.cache.ResourceCacheInterface;
import ddf.catalog.cache.impl.CacheKey;
import ddf.catalog.data.Metacard;
import ddf.catalog.resource.download.ReliableResourceDownloadManager;

@RunWith(MockitoJUnitRunner.class)
public class DownloadResourceActionProviderTest {

    private static final String ACTION_PROVIDER_ID = "actionID";

    private static final String METACARD_ID = "metacardID";

    private static final String REMOTE_SOURCE_ID = "remote";

    @Mock
    private ResourceCacheInterface resourceCache;

    @Mock
    private ReliableResourceDownloadManager downloadManager;

    @Mock
    private CacheKey cacheKey;

    @Mock
    private Metacard metacard;

    private DownloadResourceActionProvider actionProvider;

    @Before
    public void setup() {
        System.setProperty(SystemBaseUrl.HOST, "localhost");

        when(resourceCache.containsValid(anyString(), same(metacard))).thenReturn(false);
        when(resourceCache.isPending(anyString())).thenReturn(false);
        when(downloadManager.isCacheEnabled()).thenReturn(true);
        when(metacard.getId()).thenReturn(METACARD_ID);

        actionProvider = new DownloadResourceActionProvider(ACTION_PROVIDER_ID,
                resourceCache,
                downloadManager);
    }

    @Test
    public void canHandle() {
        assertThat(actionProvider.canHandleMetacard(metacard), is(true));
        verify(resourceCache).containsValid(anyString(), same(metacard));
        verify(resourceCache).isPending(anyString());
        verify(downloadManager).isCacheEnabled();
    }

    @Test
    public void canHandleWhenCachingDisabled() {
        when(downloadManager.isCacheEnabled()).thenReturn(false);
        assertThat(actionProvider.canHandleMetacard(metacard), is(false));
    }

    @Test
    public void canHandleWhenResourceCached() {
        when(resourceCache.containsValid(anyString(), same(metacard))).thenReturn(true);
        assertThat(actionProvider.canHandleMetacard(metacard), is(false));
    }

    @Test
    public void canHandleWhenResourceBeingDownloaded() {
        when(resourceCache.isPending(anyString())).thenReturn(true);
        assertThat(actionProvider.canHandleMetacard(metacard), is(false));
    }

    @Test
    public void createMetacardAction() throws MalformedURLException {
        String title = "title";
        String description = "description";
        URL url = new URL("https://localhost/url");

        Action action = actionProvider.createMetacardAction(ACTION_PROVIDER_ID,
                title,
                description,
                url);

        assertThat(action.getId(), is(ACTION_PROVIDER_ID));
        assertThat(action.getTitle(), is(title));
        assertThat(action.getDescription(), is(description));
        assertThat(action.getUrl(), is(url));
    }

    @Test
    public void getMetacardActionUrl() throws Exception {
        URL url = actionProvider.getMetacardActionUrl(REMOTE_SOURCE_ID, metacard);

        assertThat(url, is(getUrl(METACARD_ID)));
    }

    @Test
    public void getMetacardActionUrlEncodedAmpersand() throws Exception {
        String metacardId = "abc&def";
        when(metacard.getId()).thenReturn(metacardId);

        URL url = actionProvider.getMetacardActionUrl(REMOTE_SOURCE_ID, metacard);

        assertThat(url, is(getUrl(metacardId)));
    }

    @Test(expected = URISyntaxException.class)
    public void getMetacardActionUrlWhenUrlIsMalformed() throws Exception {
        String invalidHost = "23^&*#";
        System.setProperty(SystemBaseUrl.HOST, invalidHost);

        actionProvider.getMetacardActionUrl(REMOTE_SOURCE_ID, metacard);
    }

    private URL getUrl(String metacardId)
            throws MalformedURLException, UnsupportedEncodingException {
        String encodedMetacardId = URLEncoder.encode(metacardId, CharEncoding.UTF_8);
        String urlString = String.format("%s/%s/%s",
                CONTEXT_PATH,
                REMOTE_SOURCE_ID,
                encodedMetacardId);
        return new URL(SystemBaseUrl.constructUrl(urlString, true));
    }
}
