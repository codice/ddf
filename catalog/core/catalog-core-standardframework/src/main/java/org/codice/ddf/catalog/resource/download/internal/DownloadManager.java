package org.codice.ddf.catalog.resource.download.internal;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resourceretriever.ResourceRetriever;
import org.codice.ddf.catalog.resource.download.DownloadException;

public interface DownloadManager {

  ResourceResponse download(
      ResourceRequest resourceRequest, Metacard metacard, ResourceRetriever retriever)
      throws DownloadException;
}
