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
package org.codice.ddf.registry.schemabindings.helper;

import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.InternationalStringType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.VersionInfoType;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

public class MapToSchemaElement<T> {
  private final Supplier<T> objectFactory;

  public static final InternationalStringTypeHelper INTERNATIONAL_STRING_TYPE_HELPER =
      new InternationalStringTypeHelper();

  public MapToSchemaElement(Supplier<T> objectFactory) {
    this.objectFactory = objectFactory;
  }

  public Optional<T> populateBooleanElement(
      Map<String, Object> map,
      String mapKey,
      final Optional<T> referenceElement,
      BiConsumer<Boolean, T> updater) {
    Optional<T> elementToPopulate = Optional.empty();
    if (referenceElement.isPresent()) {
      elementToPopulate = Optional.of(referenceElement.get());
    }

    Boolean booleanToPopulate = MapUtils.getBoolean(map, mapKey);
    if (booleanToPopulate != null) {
      if (!elementToPopulate.isPresent()) {
        elementToPopulate = Optional.of(objectFactory.get());
      }

      updater.accept(booleanToPopulate, elementToPopulate.get());
    }

    return elementToPopulate;
  }

  public Optional<T> populateStringElement(
      Map<String, Object> map,
      String mapKey,
      final Optional<T> referenceElement,
      BiConsumer<String, T> updater) {
    Optional<T> elementToPopulate = Optional.empty();
    if (referenceElement.isPresent()) {
      elementToPopulate = Optional.of(referenceElement.get());
    }

    String valueToPopulate = MapUtils.getString(map, mapKey);
    if (StringUtils.isNotBlank(valueToPopulate)) {
      if (!elementToPopulate.isPresent()) {
        elementToPopulate = Optional.of(objectFactory.get());
      }

      updater.accept(valueToPopulate, elementToPopulate.get());
    }
    return elementToPopulate;
  }

  public Optional<T> populateInternationalStringTypeElement(
      Map<String, Object> map,
      String mapKey,
      final Optional<T> referenceElement,
      BiConsumer<InternationalStringType, T> updater) {
    Optional<T> elementToPopulate = Optional.empty();
    if (referenceElement.isPresent()) {
      elementToPopulate = Optional.of(referenceElement.get());
    }

    String valueToPopulate = MapUtils.getString(map, mapKey);
    if (StringUtils.isNotBlank(valueToPopulate)) {
      if (!elementToPopulate.isPresent()) {
        elementToPopulate = Optional.of(objectFactory.get());
      }

      InternationalStringType istToPopulate =
          INTERNATIONAL_STRING_TYPE_HELPER.create(valueToPopulate);
      if (istToPopulate != null) {
        updater.accept(istToPopulate, elementToPopulate.get());
      }
    }
    return elementToPopulate;
  }

  public Optional<T> populateVersionInfoTypeElement(
      Map<String, Object> map,
      String mapKey,
      final Optional<T> referenceElement,
      BiConsumer<VersionInfoType, T> updater) {
    Optional<T> elementToPopulate = Optional.empty();
    if (referenceElement.isPresent()) {
      elementToPopulate = Optional.of(referenceElement.get());
    }

    String valueToPopulate = MapUtils.getString(map, mapKey);
    if (StringUtils.isNotBlank(valueToPopulate)) {
      if (!elementToPopulate.isPresent()) {
        elementToPopulate = Optional.of(objectFactory.get());
      }

      VersionInfoType versionInfo = RIM_FACTORY.createVersionInfoType();
      versionInfo.setVersionName(valueToPopulate);

      updater.accept(versionInfo, elementToPopulate.get());
    }
    return elementToPopulate;
  }

  public Supplier<T> getObjectFactory() {
    return objectFactory;
  }
}
