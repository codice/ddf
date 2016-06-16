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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.apache.cxf.common.i18n.Exception;
import org.junit.Before;
import org.junit.Test;

import ddf.catalog.cache.ResourceCacheInterface;
import ddf.catalog.resource.data.ReliableResource;

public class ResourceDownloadCallbackTest {
    private ReliableResourceDownloader downloader;

    private File cacheFile;

    private ReliableResource reliableResource;

    private ResourceCacheInterface resourceCache;

    private ReliableResourceStatus reliableResourceStatus;

    @Before
    public void setup() {
        downloader = mock(ReliableResourceDownloader.class);
        cacheFile = mock(File.class);
        reliableResource = mock(ReliableResource.class);
        resourceCache = mock(ResourceCacheInterface.class);
        reliableResourceStatus = mock(ReliableResourceStatus.class);
    }

    /**
     * Checks that if the downloader indicates a success and the download was completed,
     * the size of the reliable resource is set and then the reliable resource is given
     * to the resource cache. Finally, the pending cache entry is removed from the resource cache.
     */
    @Test
    public void onSuccessDownloadComplete() throws Exception {
        when(downloader.getDownloadStatus()).thenReturn(DownloadStatus.RESOURCE_DOWNLOAD_COMPLETE);
        when(downloader.getReliableResourceByteSize()).thenReturn(100L);
        when(downloader.getReliableResourceStatus()).thenReturn(reliableResourceStatus);

        when(reliableResource.getKey()).thenReturn("something");

        ResourceDownloadCallback resourceDownloadCallback = new ResourceDownloadCallback(downloader,
                cacheFile,
                reliableResource,
                resourceCache);

        resourceDownloadCallback.onSuccess(null);

        verify(reliableResource, times(1)).setSize(reliableResourceStatus.getBytesRead());
        verify(resourceCache, times(1)).put(reliableResource);
        verify(resourceCache, times(1)).removePendingCacheEntry(reliableResource.getKey());

    }

    /**
     * Checks that if the downloader indicates a success but the download was not completed
     * it does not add the reliable resource to the resource cache and it
     * enters the if statement that deletes the cache file and then removes the pending cache
     * entry from the resource cache.
     */
    @Test
    public void onSuccessDownloadNotComplete() throws Exception {
        when(downloader.getDownloadStatus()).thenReturn(DownloadStatus.RESOURCE_DOWNLOAD_INTERRUPTED);
        when(downloader.getReliableResourceStatus()).thenReturn(reliableResourceStatus);
        when(reliableResourceStatus.getDownloadStatus()).thenReturn(DownloadStatus.RESOURCE_DOWNLOAD_INTERRUPTED);

        when(reliableResource.getKey()).thenReturn("something");

        ResourceDownloadCallback resourceDownloadCallback = new ResourceDownloadCallback(downloader,
                cacheFile,
                reliableResource,
                resourceCache);

        resourceDownloadCallback.onSuccess(null);

        verify(resourceCache, times(0)).put(reliableResource);

        assertThat(!DownloadStatus.RESOURCE_DOWNLOAD_COMPLETE.equals(downloader.getReliableResourceStatus()
                .getDownloadStatus()), is(true));

        verify(resourceCache, times(1)).removePendingCacheEntry(reliableResource.getKey());
    }

    /**
     * Checks that the pending cache entry is removed, even if the downloader indicates a failure.
     */
    @Test
    public void onFailure() throws Exception {
        when(downloader.getReliableResourceStatus()).thenReturn(reliableResourceStatus);

        when(reliableResource.getKey()).thenReturn("something");

        ResourceDownloadCallback resourceDownloadCallback = new ResourceDownloadCallback(downloader,
                cacheFile,
                reliableResource,
                resourceCache);

        resourceDownloadCallback.onFailure(null);

        verify(downloader, times(1)).getReliableResourceStatus();
        verify(resourceCache, times(1)).removePendingCacheEntry(reliableResource.getKey());
    }
}