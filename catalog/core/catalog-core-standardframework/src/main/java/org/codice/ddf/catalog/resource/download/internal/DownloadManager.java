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
package org.codice.ddf.catalog.resource.download.internal;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import ddf.catalog.resourceretriever.ResourceRetriever;
import org.codice.ddf.catalog.resource.download.DownloadException;

/**
 * A download manager orchestrates the retrieval of resources. This can also included some kind of
 * caching mechanism, but it is not required.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface DownloadManager {

  /**
   * Download the requested resource.
   *
   * @param resourceRequest the original {@link ResourceRequest}
   * @param metacard the {@link Metacard} associated with this resource
   * @param retriever the {@link ResourceRetriever} used to actually fetch the resource
   * @return a {@link ResourceResponse} that wraps around the requested resource. It can be read via
   *     {@link Resource#getInputStream()} after calling {@link ResourceResponse#getResource()}
   * @throws DownloadException
   */
  ResourceResponse download(
      ResourceRequest resourceRequest, Metacard metacard, ResourceRetriever retriever)
      throws DownloadException;
}
