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
package ddf.catalog.impl.operations;

import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.impl.FrameworkProperties;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.source.CatalogStore;
import ddf.catalog.source.IngestException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteDeleteOperations {

  static final Logger INGEST_LOGGER = LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

  private FrameworkProperties frameworkProperties;

  private OperationsMetacardSupport opsMetacardSupport;

  private OperationsCatalogStoreSupport opsCatStoreSupport;

  public RemoteDeleteOperations(
      FrameworkProperties frameworkProperties,
      OperationsMetacardSupport opsMetacardSupport,
      OperationsCatalogStoreSupport opsCatStoreSupport) {
    this.frameworkProperties = frameworkProperties;
    this.opsMetacardSupport = opsMetacardSupport;
    this.opsCatStoreSupport = opsCatStoreSupport;
  }

  @Nullable
  public DeleteResponse performRemoteDelete(
      DeleteRequest deleteRequest, @Nullable DeleteResponse deleteResponse) {

    if (!opsCatStoreSupport.isCatalogStoreRequest(deleteRequest)) {
      return deleteResponse;
    }

    DeleteResponse remoteDeleteResponse = doRemoteDelete(deleteRequest);
    if (deleteResponse == null) {
      deleteResponse = remoteDeleteResponse;
      deleteResponse = injectAttributes(deleteResponse);
    } else {
      deleteResponse.getProperties().putAll(remoteDeleteResponse.getProperties());
      deleteResponse.getProcessingErrors().addAll(remoteDeleteResponse.getProcessingErrors());
    }
    return deleteResponse;
  }

  private DeleteResponse doRemoteDelete(DeleteRequest deleteRequest) {
    HashSet<ProcessingDetails> exceptions = new HashSet<>();
    Map<String, Serializable> properties = new HashMap<>();

    List<CatalogStore> stores =
        opsCatStoreSupport.getCatalogStoresForRequest(deleteRequest, exceptions);

    List<Metacard> metacards = new ArrayList<>();
    for (CatalogStore store : stores) {
      try {
        if (!store.isAvailable()) {
          exceptions.add(
              new ProcessingDetailsImpl(store.getId(), null, "CatalogStore is not available"));
        } else {
          // TODO: 4/27/17 Address bug in DDF-2970 for overwriting deleted metacards
          DeleteResponse response = store.delete(deleteRequest);
          properties.put(store.getId(), new ArrayList<>(response.getDeletedMetacards()));
          metacards = response.getDeletedMetacards();
        }
      } catch (IngestException e) {
        INGEST_LOGGER.error("Error deleting metacards for CatalogStore {}", store.getId(), e);
        exceptions.add(new ProcessingDetailsImpl(store.getId(), e));
      }
    }

    return new DeleteResponseImpl(deleteRequest, properties, metacards, exceptions);
  }

  public void setOpsCatStoreSupport(OperationsCatalogStoreSupport opsCatStoreSupport) {
    this.opsCatStoreSupport = opsCatStoreSupport;
  }

  // TODO: 4/24/17 Move to future utility class (called in DeleteOperations as well)
  // https://codice.atlassian.net/browse/DDF-2962
  private DeleteResponse injectAttributes(DeleteResponse response) {
    List<Metacard> deletedMetacards =
        response.getDeletedMetacards().stream()
            .map(
                (original) ->
                    opsMetacardSupport.applyInjectors(
                        original, frameworkProperties.getAttributeInjectors()))
            .collect(Collectors.toList());

    return new DeleteResponseImpl(
        response.getRequest(),
        response.getProperties(),
        deletedMetacards,
        response.getProcessingErrors());
  }
}
