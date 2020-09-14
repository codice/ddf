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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.collections.CollectionUtils;
import org.codice.ddf.spatial.ogc.wfs.featuretransformer.WfsMetadata;

public final class WfsMetadataImpl<T> implements WfsMetadata<T> {
  private final Supplier<String> idSupplier;

  private final Supplier<String> coordinateOrderSupplier;

  private final Set<T> descriptors;

  private List<String> featureMemberNodeNames;

  private String activeFeatureMemberNodeName;

  private final Class<T> descriptorClass;

  public WfsMetadataImpl(
      Supplier<String> idSupplier,
      Supplier<String> coordinateOrderSupplier,
      List<String> featureMemberNodeNames,
      Class<T> descriptorClass) {
    this.idSupplier = idSupplier;
    this.coordinateOrderSupplier = coordinateOrderSupplier;
    this.descriptorClass = descriptorClass;
    this.featureMemberNodeNames = featureMemberNodeNames;
    this.descriptors = new HashSet<>();

    if (CollectionUtils.isNotEmpty(featureMemberNodeNames)) {
      this.activeFeatureMemberNodeName = featureMemberNodeNames.get(0);
    }
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
  public Set<T> getDescriptors() {
    return Collections.unmodifiableSet(this.descriptors);
  }

  public void addEntry(T featureDescription) {
    this.descriptors.add(featureDescription);
  }

  @Override
  public List<String> getFeatureMemberNodeNames() {
    return this.featureMemberNodeNames;
  }

  public Class<T> getDescriptorClass() {
    return descriptorClass;
  }

  @Override
  public String getActiveFeatureMemberNodeName() {
    return activeFeatureMemberNodeName;
  }

  @Override
  public void setActiveFeatureMemberNodeName(String featureMemberNodeName) {
    activeFeatureMemberNodeName = featureMemberNodeName;
  }
}
