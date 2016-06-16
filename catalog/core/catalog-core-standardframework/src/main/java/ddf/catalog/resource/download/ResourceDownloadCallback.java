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

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;

import ddf.catalog.cache.ResourceCacheInterface;
import ddf.catalog.resource.data.ReliableResource;

public class ResourceDownloadCallback implements FutureCallback<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceDownloadCallback.class);

    private ReliableResourceDownloader downloader;

    private ResourceCacheInterface resourceCache;

    private ReliableResource reliableResource;

    private File cacheFile;

    public ResourceDownloadCallback(ReliableResourceDownloader downloader, File cacheFile,
            ReliableResource reliableResource, ResourceCacheInterface resourceCache) {

        this.downloader = downloader;
        this.reliableResource = reliableResource;
        this.cacheFile = cacheFile;
        this.resourceCache = resourceCache;
    }

    /**
     * If the download is complete, put the reliable resource into the resource cache.
     *
     * @param b is void.
     */
    public void onSuccess(Void b) {
        DownloadStatus downloadStatus = downloader.getDownloadStatus();

        if (DownloadStatus.RESOURCE_DOWNLOAD_COMPLETE.equals(downloadStatus)) {
            LOGGER.debug("Setting reliableResource size");
            reliableResource.setSize(downloader.getReliableResourceStatus()
                    .getBytesRead());
            LOGGER.debug("Adding caching key = {} to cache map", reliableResource.getKey());
            resourceCache.put(reliableResource);

        } else if (!DownloadStatus.RESOURCE_DOWNLOAD_COMPLETE.equals(downloader.getReliableResourceStatus()
                .getDownloadStatus())) {
            FileUtils.deleteQuietly(cacheFile);
        }

        ReliableResourceStatus reliableResourceStatus = downloader.getReliableResourceStatus();
        cleanupAfterDownload(reliableResourceStatus);

    }

    public void onFailure(Throwable thrown) {
        ReliableResourceStatus reliableResourceStatus = downloader.getReliableResourceStatus();
        cleanupAfterDownload(reliableResourceStatus);
    }

    /**
     * Removes a pending cache entry from the reliable resource cache.
     *
     * @param reliableResourceStatus used to get the key of the item to remove from the pending
     *                               cache.
     */
    private void cleanupAfterDownload(ReliableResourceStatus reliableResourceStatus) {
        if (reliableResourceStatus != null) {
            // If caching was not successful, then remove this product from the pending cache list
            // (Otherwise partially cached files will remain in pending list and returned to
            // subsequent clients)
            resourceCache.removePendingCacheEntry(reliableResource.getKey());
        }
    }
}
