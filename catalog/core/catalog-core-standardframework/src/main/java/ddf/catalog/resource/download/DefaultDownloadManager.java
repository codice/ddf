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
package ddf.catalog.resource.download;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resourceretriever.ResourceRetriever;
import java.io.IOException;
import org.codice.ddf.catalog.resource.download.DownloadException;
import org.codice.ddf.catalog.resource.download.internal.DownloadManager;

public class DefaultDownloadManager implements DownloadManager {

  @Override
  public ResourceResponse download(
      ResourceRequest resourceRequest, Metacard metacard, ResourceRetriever retriever)
      throws DownloadException {
    try {
      ResourceResponse resourceResponse = retriever.retrieveResource();
      return addResourceRequestToResponse(resourceRequest, metacard, resourceResponse);
    } catch (ResourceNotFoundException | ResourceNotSupportedException | IOException e) {
      throw new DownloadException(
          String.format(
              "The [%s] resource of metacard [%s] could not be retrieved.",
              resourceRequest.getAttributeName(), metacard.getId()),
          e);
    }
  }

  private ResourceResponse addResourceRequestToResponse(
      ResourceRequest resourceRequest, Metacard metacard, ResourceResponse resourceResponse) {
    resourceResponse.getProperties().put(Metacard.ID, metacard.getId());
    // Sources do not create ResourceResponses with the original ResourceRequest, so it is added
    // here
    resourceResponse =
        new ResourceResponseImpl(
            resourceRequest, resourceResponse.getProperties(), resourceResponse.getResource());
    return resourceResponse;
  }
}
