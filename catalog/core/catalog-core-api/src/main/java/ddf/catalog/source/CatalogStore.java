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
package ddf.catalog.source;

import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b> A CatalogStore represents an editable store that
 * is either local or remote
 */
@Deprecated
public interface CatalogStore extends FederatedSource {

  /**
   * Publishes a list of {@link ddf.catalog.data.Metacard} objects into the catalog.
   *
   * @param createRequest - the {@link CreateRequest} that includes a {@link java.util.List} of
   *     {@link ddf.catalog.data.Metacard} objects to be stored in a {@link Source}. The ID of the
   *     {@link ddf.catalog.data.Metacard} object will be ignored and populated / generated by the
   *     {@link CatalogStore} when the record has been stored.
   * @return the {@link CreateResponse} containing a {@link java.util.List} of fully populated
   *     metacards. This should be similar to the parameter list of {@link
   *     ddf.catalog.data.Metacard} objects but it must have the ddf.catalog.data.Metacard ID
   *     populated.
   * @throws IngestException if any problem occurs when storing the metacards
   */
  CreateResponse create(CreateRequest createRequest) throws IngestException;

  /**
   * Updates a list of {@link ddf.catalog.data.Metacard} records. {@link ddf.catalog.data.Metacard}
   * records that are not in the Catalog will not be created.
   *
   * @param updateRequest - the {@link UpdateRequest} that includes updates to {@link
   *     ddf.catalog.data.Metacard} records that have been previously stored in a {@link Source}. A
   *     given {@link ddf.catalog.data.Attribute} name-value pair in this request must uniquely
   *     identify zero metacards or one metacard in the {@link Source}, otherwise an {@link
   *     IngestException} will be thrown.
   * @return the {@link UpdateResponse} containing a {@link java.util.List} of {@link
   *     ddf.catalog.operation.Update} objects that represent the new (updated) and old (previous)
   *     {@link ddf.catalog.data.Metacard} records.
   * @throws IngestException if an issue occurs during the update such as multiple records were
   *     matched for a single update entry
   */
  UpdateResponse update(UpdateRequest updateRequest) throws IngestException;

  /**
   * Deletes records specified by a list of attribute values such as an id attribute.
   *
   * @param deleteRequest - the {@link DeleteRequest} containing the attribute values associated
   *     with {@link ddf.catalog.data.Metacard}s to delete
   * @return a {@link DeleteResponse} with {@link ddf.catalog.data.Metacard}s that were deleted.
   *     These {@link ddf.catalog.data.Metacard}s are fully populated in preparation for any
   *     processing services.
   * @throws IngestException if an issue occurs during the delete
   */
  DeleteResponse delete(DeleteRequest deleteRequest) throws IngestException;
}
