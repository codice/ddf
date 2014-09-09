/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.resource.download;

import ddf.catalog.cache.ResourceCache;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resourceretriever.ResourceRetriever;

public interface ReliableResourceDownloadManager extends Runnable {

    /**
     * @param resourceRequest the original @ResourceRequest to retrieve the resource
     * @param metacard        the @Metacard associated with the resource being downloaded
     * @param retriever       the @ResourceRetriever to be used to get the resource
     * @return the modified @ResourceResponse with the @ReliableResourceInputStream that the client
     * should read from
     * @throws ddf.catalog.resource.download.DownloadException
     */
    public ResourceResponse download(ResourceRequest resourceRequest, Metacard metacard,
            ResourceRetriever retriever) throws DownloadException;

    public Long getReliableResourceInputStreamBytesCached();

    public String getReliableResourceInputStreamState();

    public String getResourceSize();

    public ResourceResponse getResourceResponse();

    public ResourceCache getResourceCache();
}
