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
package org.codice.ddf.registry.schemabindings.converter.web;

import java.util.HashMap;
import java.util.Map;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.codice.ddf.registry.schemabindings.helper.WebMapHelper;

public class RegistryPackageWebConverter extends RegistryObjectWebConverter {
  public static final String REGISTRY_OBJECT_LIST_KEY = "RegistryObjectList";

  private WebMapHelper webMapHelper = new WebMapHelper();

  /**
   * This method creates a Map<String, Object> representation of the RegistryPackageType provided.
   * The following keys will be added to the map (Taken from EbrimConstants):
   *
   * <p>REGISTRY_OBJECT_LIST_KEY = "RegistryObjectList";
   *
   * <p>This will also try to parse RegistryObjectType values to the map.
   *
   * <p>Uses: RegistryObjectListWebConverter
   *
   * @param registryPackage the RegistryPackageType to be converted into a map, null returns empty
   *     Map
   * @return Map<String, Object> representation of the RegistryPackageType provided
   */
  public Map<String, Object> convert(RegistryPackageType registryPackage) {
    Map<String, Object> registryPackageMap = new HashMap<>();
    if (registryPackage == null) {
      return registryPackageMap;
    }

    webMapHelper.putAllIfNotEmpty(registryPackageMap, super.convertRegistryObject(registryPackage));

    if (registryPackage.isSetRegistryObjectList()) {
      RegistryObjectListWebConverter registryObjectListConverter =
          new RegistryObjectListWebConverter();
      Map<String, Object> registryObjectListMap =
          registryObjectListConverter.convert(registryPackage.getRegistryObjectList());

      webMapHelper.putIfNotEmpty(
          registryPackageMap, REGISTRY_OBJECT_LIST_KEY, registryObjectListMap);
    }

    return registryPackageMap;
  }
}
