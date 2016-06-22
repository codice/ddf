/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.resource.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;

import ddf.catalog.CatalogFramework;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;

@RunWith(MockitoJUnitRunner.class)
public class ResourceDownloadEndpointTest {

    private static final String METACARD_ID = "57a4b894e13a455b8cccb87cec778b58";

    private static final String SOURCE_ID = "ddf.distribution";

    private static final String DOWNLOAD_ID = "download ID";

    private static final String DOWNLOAD_ID_KEY = "downloadId";

    private static final String FILE_NAME_KEY = "fileName";

    private static final String BYTES_DOWNLOADED_KEY = "bytesDownloaded";

    private static final String PERCENT_KEY = "percent";

    private static final String PERCENT_DOWNLOADED_KEY = "percentDownloaded";

    private static final String USER_KEY = "user";

    private static final String USERS_KEY = "users";

    private static final String STATUS_KEY = "status";

    @Mock
    private CatalogFramework mockCatalogFramework;

    @Mock
    private ReliableResourceDownloadManager mockDownloadManager;

    @Mock
    private ResourceResponse mockResourceResponse;

    private ObjectMapper objectMapper = JsonFactory.create();

    @Before
    public void setup() {
        when(mockResourceResponse.getPropertyValue("downloadIdentifier")).thenReturn(DOWNLOAD_ID);
    }

    @Test(expected = DownloadToCacheOnlyException.class)
    public void downloadToCacheOnlyUsingGetWhenCacheDisabled() throws Exception {
        // Setup
        setupMockDownloadManager(false);
        ResourceDownloadEndpoint resourceDownloadEndpoint = createResourceDownloadEndpoint();

        // Perform Test
        resourceDownloadEndpoint.getDownloadListOrDownloadToCache(SOURCE_ID, METACARD_ID);
    }

    @Test(expected = DownloadToCacheOnlyException.class)
    public void downloadToCacheOnlyUsingGetWhenNullResourceResponse() throws Exception {
        // Setup
        setupMockDownloadManager(true);
        setupMockCatalogFramework(null, null);
        ResourceDownloadEndpoint resourceDownloadEndpoint = createResourceDownloadEndpoint();

        // Perform Test
        resourceDownloadEndpoint.getDownloadListOrDownloadToCache(SOURCE_ID, METACARD_ID);
    }

    @Test(expected = DownloadToCacheOnlyException.class)
    public void downloadToCacheOnlyUsingGetWhenCatalogFrameworkThrowsIOException()
            throws Exception {
        // Setup
        setupMockDownloadManager(true);
        setupMockCatalogFramework(mockResourceResponse, IOException.class);
        ResourceDownloadEndpoint resourceDownloadEndpoint = createResourceDownloadEndpoint();

        // Perform Test
        resourceDownloadEndpoint.getDownloadListOrDownloadToCache(SOURCE_ID, METACARD_ID);
    }

    @Test(expected = DownloadToCacheOnlyException.class)
    public void downloadToCacheOnlyUsingGetWhenCatalogFrameworkThrowsResourceNotSupportedException()
            throws Exception {
        // Setup
        setupMockDownloadManager(true);
        setupMockCatalogFramework(mockResourceResponse, ResourceNotSupportedException.class);
        ResourceDownloadEndpoint resourceDownloadEndpoint = createResourceDownloadEndpoint();

        // Perform Test
        resourceDownloadEndpoint.getDownloadListOrDownloadToCache(SOURCE_ID, METACARD_ID);
    }

    @Test(expected = DownloadToCacheOnlyException.class)
    public void downloadToCacheOnlyUsingGetWhenCatalogFrameworkThrowsResourceNotFoundException()
            throws Exception {
        // Setup
        setupMockDownloadManager(true);
        setupMockCatalogFramework(mockResourceResponse, ResourceNotFoundException.class);
        ResourceDownloadEndpoint resourceDownloadEndpoint = createResourceDownloadEndpoint();

        // Perform Test
        resourceDownloadEndpoint.getDownloadListOrDownloadToCache(SOURCE_ID, METACARD_ID);
    }

    @Test
    public void downloadToCacheOnlyUsingGet() throws Exception {
        // Setup
        setupMockDownloadManager(true);
        setupMockCatalogFramework(mockResourceResponse, null);
        ResourceDownloadEndpoint resourceDownloadEndpoint = createResourceDownloadEndpoint();

        // Perform Test
        Response response = resourceDownloadEndpoint.getDownloadListOrDownloadToCache(SOURCE_ID,
                METACARD_ID);

        ResourceDownloadEndpoint.ResourceDownloadResponse resourceDownloadResponse =
                objectMapper.fromJson((String) response.getEntity(),
                        ResourceDownloadEndpoint.ResourceDownloadResponse.class);

        assertThat(resourceDownloadResponse.getDownloadIdentifier(), is(DOWNLOAD_ID));
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
    }

    @Test
    public void downloadToCacheUsingPost() throws Exception {
        // Setup
        setupMockDownloadManager(true);
        setupMockCatalogFramework(mockResourceResponse, null);
        ResourceDownloadEndpoint resourceDownloadEndpoint = createResourceDownloadEndpoint();

        // Perform Test
        Response response = resourceDownloadEndpoint.downloadToCache(SOURCE_ID, METACARD_ID);

        ResourceDownloadEndpoint.ResourceDownloadResponse resourceDownloadResponse =
                objectMapper.fromJson((String) response.getEntity(),
                        ResourceDownloadEndpoint.ResourceDownloadResponse.class);

        assertThat(resourceDownloadResponse.getDownloadIdentifier(), is(DOWNLOAD_ID));
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
    }

    @Test(expected = DownloadToCacheOnlyException.class)
    public void downloadToCacheUsingPostWithNullSourceId() throws Exception {
        ResourceDownloadEndpoint resourceDownloadEndpoint = createResourceDownloadEndpoint();
        resourceDownloadEndpoint.downloadToCache(null, METACARD_ID);
    }

    @Test(expected = DownloadToCacheOnlyException.class)
    public void downloadToCacheUsingPostWithNullMetacardId() throws Exception {
        ResourceDownloadEndpoint resourceDownloadEndpoint = createResourceDownloadEndpoint();
        resourceDownloadEndpoint.downloadToCache(SOURCE_ID, null);
    }

    @Test
    public void getDownloadList() throws Exception {
        DownloadInfo downloadInfo1 = createDownloadInfoMap("123",
                "file1.txt",
                DownloadManagerState.DownloadState.IN_PROGRESS.name(),
                "100",
                "UNKNOWN",
                "user1");
        DownloadInfo downloadInfo2 = createDownloadInfoMap("456",
                "file2.txt",
                DownloadManagerState.DownloadState.NOT_STARTED.name(),
                "200",
                "50",
                "user2");
        List<DownloadInfo> downloads = ImmutableList.of(downloadInfo1, downloadInfo2);
        when(mockDownloadManager.getDownloadsInProgress()).thenReturn(downloads);

        ResourceDownloadEndpoint resourceDownloadEndpoint = createResourceDownloadEndpoint();

        Response downloadListResponse = resourceDownloadEndpoint.getDownloadList();
        List<Map<String, Object>> downloadList =
                objectMapper.fromJson((String) downloadListResponse.getEntity(), List.class);
        assertThat(downloadList, hasSize(2));
        assertDownloadInfo(downloadList.get(0), downloadInfo1);
        assertDownloadInfo(downloadList.get(1), downloadInfo2);
    }

    @Test(expected = DownloadToCacheOnlyException.class)
    public void getDownloadListFails() throws Exception {
        when(mockDownloadManager.getDownloadsInProgress()).thenThrow(new RuntimeException());
        ResourceDownloadEndpoint resourceDownloadEndpoint = createResourceDownloadEndpoint();
        resourceDownloadEndpoint.getDownloadList();
    }

    private ResourceDownloadEndpoint createResourceDownloadEndpoint() {
        return new ResourceDownloadEndpoint(mockCatalogFramework,
                mockDownloadManager,
                objectMapper);
    }

    private DownloadInfo createDownloadInfoMap(String id, String file, String status,
            String bytesDownloaded, String percent, String userName) {
        Map<String, String> downloadStatus = new HashMap<>();
        downloadStatus.put(DOWNLOAD_ID_KEY, id);
        downloadStatus.put(FILE_NAME_KEY, file);
        downloadStatus.put(STATUS_KEY, status);
        downloadStatus.put(BYTES_DOWNLOADED_KEY, bytesDownloaded);
        downloadStatus.put(PERCENT_KEY, percent);
        downloadStatus.put(USER_KEY, userName);
        return new DownloadInfo(downloadStatus);
    }

    private void assertDownloadInfo(Map<String, Object> actualDownloadInfo,
            DownloadInfo expectedDownloadInfo) {
        assertThat(actualDownloadInfo.get(DOWNLOAD_ID_KEY),
                is(expectedDownloadInfo.getDownloadId()));
        assertThat(actualDownloadInfo.get(FILE_NAME_KEY), is(expectedDownloadInfo.getFileName()));
        assertThat(actualDownloadInfo.get(STATUS_KEY), is(expectedDownloadInfo.getStatus()));
        assertThat(actualDownloadInfo.get(BYTES_DOWNLOADED_KEY),
                is((int) expectedDownloadInfo.getBytesDownloaded()));
        assertThat(actualDownloadInfo.get(PERCENT_DOWNLOADED_KEY),
                is(expectedDownloadInfo.getPercentDownloaded()));
        List<String> users = (List<String>) actualDownloadInfo.get(USERS_KEY);
        assertThat(users, hasSize(1));
        assertThat(users.get(0),
                is(expectedDownloadInfo.getUsers()
                        .get(0)));
    }

    private void setupMockDownloadManager(boolean isCacheEnabled) {
        when(mockDownloadManager.isCacheEnabled()).thenReturn(isCacheEnabled);
    }

    private void setupMockCatalogFramework(ResourceResponse mockResourceResponse,
            Class<? extends Throwable> exceptionClass) throws Exception {
        when(mockCatalogFramework.getResource(any(ResourceRequest.class),
                eq(SOURCE_ID))).thenReturn(mockResourceResponse);

        if (exceptionClass != null) {
            doThrow(exceptionClass).when(mockCatalogFramework)
                    .getResource(any(ResourceRequest.class), eq(SOURCE_ID));
        }
    }
}
