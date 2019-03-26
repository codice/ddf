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
package ddf.catalog.operation.impl;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.UpdateRequest;
import java.io.Serializable;
import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/** The UpdateRequestImpl represents the default implementation of {@link UpdateRequest}. */
public class UpdateRequestImpl extends OperationImpl implements UpdateRequest {

  protected String name;

  protected List<Entry<Serializable, Metacard>> updates;

  private Set<String> destinations = new HashSet<>();

  /**
   * Instantiates a new UpdateRequestImpl.
   *
   * @param updateList the list of updated {@link Metacard}
   * @param attributeName the attribute name (e.g. Core.ID, Metacard.PRODUCT_URI)
   * @param properties the properties associated with the operation
   * @param destinations the destination ids this request should be sent to
   */
  public UpdateRequestImpl(
      List<Entry<Serializable, Metacard>> updateList,
      String attributeName,
      Map<String, Serializable> properties,
      Set<String> destinations) {
    super(properties);
    this.name = attributeName;
    this.updates = updateList;
    if (destinations != null) {
      this.destinations = destinations;
    }
  }

  /**
   * Instantiates a new UpdateRequestImpl.
   *
   * @param updateList - the list of updated {@link Metacard}
   * @param attributeName the attribute name (e.g. Core.ID, Metacard.PRODUCT_URI)
   * @param properties the properties associated with the operation
   */
  public UpdateRequestImpl(
      List<Entry<Serializable, Metacard>> updateList,
      String attributeName,
      Map<String, Serializable> properties) {
    this(updateList, attributeName, properties, new HashSet<>());
  }

  /**
   * Instantiates a new UpdateRequestImpl from an id and metacard
   *
   * @param id the id of the {@link Metacard} to update
   * @param metacard the updated {@link Metacard} value.
   */
  public UpdateRequestImpl(String id, Metacard metacard) {
    this(new String[] {id}, Arrays.asList(metacard));
  }

  /**
   * Instantiates a new UpdateRequestImpl from an array of ids and a list of {@link Metacard}
   *
   * @param ids - the ids associated with the {@link Metacard} list
   * @param metacards the updated {@link Metacard} values
   * @throws IllegalArgumentException if the ids array size and list of {@link Metacard} size does
   *     not match
   */
  public UpdateRequestImpl(String[] ids, List<Metacard> metacards) throws IllegalArgumentException {
    this(formatEntryList((Serializable[]) ids, metacards), UpdateRequest.UPDATE_BY_ID, null);
  }

  /**
   * Instantiates a new UpdateRequestImpl from an array of {@link URI} and a list of {@link
   * Metacard}
   *
   * @param uris - the uris associated with the {@link Metacard} list
   * @param metacards the updated {@link Metacard} values
   * @throws IllegalArgumentException if the uris array size and list of {@link Metacard} size does
   *     not match
   */
  public UpdateRequestImpl(URI[] uris, List<Metacard> metacards) throws IllegalArgumentException {
    this(
        formatEntryList((Serializable[]) uris, metacards),
        UpdateRequest.UPDATE_BY_PRODUCT_URI,
        null);
  }

  /**
   * Formats the {@link List} of {@link Metacard} into a {@link List} of {@link Entry}.
   *
   * @param values the values of the identifiers
   * @param metacards the metacards to format
   * @return the list of {@link Entry}
   */
  private static List<Entry<Serializable, Metacard>> formatEntryList(
      Serializable[] values, List<Metacard> metacards) {
    List<Entry<Serializable, Metacard>> updateList = new ArrayList<Entry<Serializable, Metacard>>();
    if (values.length != metacards.size()) {
      throw new IllegalArgumentException("Id List and Metacard List must be the same size.");
    } else {
      for (int i = 0; i < metacards.size(); i++) {
        updateList.add(new SimpleEntry<Serializable, Metacard>(values[i], metacards.get(i)));
      }
    }
    return updateList;
  }

  @Override
  public Set<String> getStoreIds() {
    return destinations;
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.operation.UpdateRequest#getAttributeName()
   */
  @Override
  public String getAttributeName() {
    return name;
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.operation.UpdateRequest#getUpdates()
   */
  @Override
  public List<Entry<Serializable, Metacard>> getUpdates() {
    return updates;
  }
}
