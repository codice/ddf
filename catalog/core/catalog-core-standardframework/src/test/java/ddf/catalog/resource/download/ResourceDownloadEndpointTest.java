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
import static org.hamcrest.Matchers.is;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ddf.catalog.CatalogFramework;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;

@RunWith(MockitoJUnitRunner.class)
public class ResourceDownloadEndpointTest {

    private static final String METACARD_ID = "57a4b894e13a455b8cccb87cec778b58";

    private static final String SOURCE_ID = "ddf.distribution";

    private static final String SUCCESSFUL_DOWNLOAD_TO_CACHE_MSG_TEMPLATE = "The product associated with metacard [%s] from source [%s] is being downloaded to the product cache.";

    @Mock
    private CatalogFramework mockCatalogFramework;

    @Mock
    private ReliableResourceDownloadManager mockDownloadManager;

    @Mock
    private ResourceResponse mockResourceResponse;

    @Test(expected = DownloadToCacheOnlyException.class)
    public void testStartDownloadToCacheOnlyCacheDisabled() throws Exception {
        // Setup
        setupMockDownloadManager(false);
        ResourceDownloadEndpoint resourceDownloadEndpoint = new ResourceDownloadEndpoint(
                mockCatalogFramework, mockDownloadManager);

        // Perform Test
        resourceDownloadEndpoint.startDownloadToCacheOnly(SOURCE_ID, METACARD_ID);
    }

    @Test(expected = DownloadToCacheOnlyException.class)
    public void testStartDownloadToCacheOnlyNullResourceResponse() throws Exception {
        // Setup
        setupMockDownloadManager(true);
        setupMockCatalogFramework(null, null);
        ResourceDownloadEndpoint resourceDownloadEndpoint = new ResourceDownloadEndpoint(
                mockCatalogFramework, mockDownloadManager);

        // Perform Test
        resourceDownloadEndpoint.startDownloadToCacheOnly(SOURCE_ID, METACARD_ID);
    }

    @Test(expected = DownloadToCacheOnlyException.class)
    public void testStartDownloadToCacheOnlyCatalogFrameworkThrowsIOException() throws Exception {
        // Setup
        setupMockDownloadManager(true);
        setupMockCatalogFramework(mockResourceResponse, IOException.class);
        ResourceDownloadEndpoint resourceDownloadEndpoint = new ResourceDownloadEndpoint(
                mockCatalogFramework, mockDownloadManager);

        // Perform Test
        resourceDownloadEndpoint.startDownloadToCacheOnly(SOURCE_ID, METACARD_ID);
    }

    @Test(expected = DownloadToCacheOnlyException.class)
    public void testStartDownloadToCacheOnlyCatalogFrameworkThrowsResourceNotSupportedException()
        throws Exception {
        // Setup
        setupMockDownloadManager(true);
        setupMockCatalogFramework(mockResourceResponse, ResourceNotSupportedException.class);
        ResourceDownloadEndpoint resourceDownloadEndpoint = new ResourceDownloadEndpoint(
                mockCatalogFramework, mockDownloadManager);

        // Perform Test
        resourceDownloadEndpoint.startDownloadToCacheOnly(SOURCE_ID, METACARD_ID);
    }

    @Test(expected = DownloadToCacheOnlyException.class)
    public void testStartDownloadToCacheOnlyCatalogFrameworkThrowsResourceNotFoundException()
        throws Exception {
        // Setup
        setupMockDownloadManager(true);
        setupMockCatalogFramework(mockResourceResponse, ResourceNotFoundException.class);
        ResourceDownloadEndpoint resourceDownloadEndpoint = new ResourceDownloadEndpoint(
                mockCatalogFramework, mockDownloadManager);

        // Perform Test
        resourceDownloadEndpoint.startDownloadToCacheOnly(SOURCE_ID, METACARD_ID);
    }

    @Test
    public void testStartDownloadToCacheOnly() throws Exception {
        // Setup
        setupMockDownloadManager(true);
        setupMockCatalogFramework(mockResourceResponse, null);
        ResourceDownloadEndpoint resourceDownloadEndpoint = new ResourceDownloadEndpoint(
                mockCatalogFramework, mockDownloadManager);

        // Perform Test
        Response response = resourceDownloadEndpoint.startDownloadToCacheOnly(SOURCE_ID,
                METACARD_ID);

        assertThat(response.getEntity(), is(
                String.format(SUCCESSFUL_DOWNLOAD_TO_CACHE_MSG_TEMPLATE, METACARD_ID, SOURCE_ID)));
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getMediaType().toString(), is(MediaType.TEXT_PLAIN));
    }

    private void setupMockDownloadManager(boolean isCacheEnabled) {
        when(mockDownloadManager.isCacheEnabled()).thenReturn(isCacheEnabled);
    }

    private void setupMockCatalogFramework(ResourceResponse mockResourceResponse,
            Class<? extends Throwable> exceptionClass)
        throws Exception {
        when(mockCatalogFramework.getResource(any(ResourceRequest.class), eq(SOURCE_ID)))
                .thenReturn(mockResourceResponse);

        if (exceptionClass != null) {
            doThrow(exceptionClass).when(mockCatalogFramework)
                    .getResource(any(ResourceRequest.class), eq(SOURCE_ID));
        }
    }
}
