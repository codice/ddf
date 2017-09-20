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
import ddf.catalog.operation.CreateRequest;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CreateRequestImpl represents a {@link CreateRequest} and supports passing a {@link Map} of
 * properties for create operations.
 */
public class CreateRequestImpl extends OperationImpl implements CreateRequest {

  /** The metacards to be created */
  protected List<Metacard> metacards;

  /** The set of destination ids to send this request to */
  protected Set<String> destinations = new HashSet<>();

  /**
   * Instantiates a new CreateRequestImpl with a single {@link Metacard}.
   *
   * @param metacard the metacard
   */
  public CreateRequestImpl(Metacard metacard) {
    this(Arrays.asList(metacard), null);
  }

  /**
   * Instantiates a new CreateRequestImpl with a {@link List} of {@link Metacard}.
   *
   * @param metacards the metacards
   */
  public CreateRequestImpl(List<Metacard> metacards) {
    this(metacards, null);
  }

  /**
   * Instantiates a new CreateRequestImpl with a {@link List} of {@link Metacard}. and a {@link Map}
   * of properties.
   *
   * @param metacards the metacards
   * @param properties the properties
   */
  public CreateRequestImpl(List<Metacard> metacards, Map<String, Serializable> properties) {
    this(metacards, properties, new HashSet<>());
  }

  public CreateRequestImpl(
      List<Metacard> metacards, Map<String, Serializable> properties, Set<String> destinations) {
    super(properties);
    this.metacards = metacards;
    if (destinations != null) {
      this.destinations = destinations;
    }
  }

  @Override
  public Set<String> getStoreIds() {
    return destinations;
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.operation.CreateRequest#getMetacards()
   */
  @Override
  public List<Metacard> getMetacards() {
    return metacards;
  }
}
