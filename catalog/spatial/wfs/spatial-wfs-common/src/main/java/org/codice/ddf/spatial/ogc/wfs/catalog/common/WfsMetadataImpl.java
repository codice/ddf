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
package org.codice.ddf.spatial.ogc.wfs.catalog.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.codice.ddf.spatial.ogc.wfs.catalog.message.api.WfsMetadata;

public final class WfsMetadataImpl<T> implements WfsMetadata {
  private final Supplier<String> idSupplier;

  private final Supplier<String> coordinateOrderSupplier;

  private final List<T> descriptors;

  private String featureMemberNodeName;

  private final Class<T> descriptorClass;

  public WfsMetadataImpl(
      Supplier<String> idSupplier,
      Supplier<String> coordinateOrderSupplier,
      String featureMemberNodeName,
      Class<T> descriptorClass) {
    this.idSupplier = idSupplier;
    this.coordinateOrderSupplier = coordinateOrderSupplier;
    this.descriptorClass = descriptorClass;
    this.featureMemberNodeName = featureMemberNodeName;
    this.descriptors = new ArrayList<>();
  }

  @Override
  public String getId() {
    return this.idSupplier.get();
  }

  @Override
  public String getCoordinateOrder() {
    return this.coordinateOrderSupplier.get();
  }

  @Override
  public List<T> getDescriptors() {
    return Collections.unmodifiableList(this.descriptors);
  }

  public void addEntry(T featureDescription) {
    this.descriptors.add(featureDescription);
  }

  @Override
  public String getFeatureMemberNodeName() {
    return this.featureMemberNodeName;
  }
}
