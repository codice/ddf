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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ServiceBindingType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SpecificationLinkType;
import org.apache.commons.collections.MapUtils;
import org.codice.ddf.registry.schemabindings.helper.WebMapHelper;

public class ServiceBindingWebConverter extends RegistryObjectWebConverter {
  public static final String ACCESS_URI = "accessUri";

  public static final String SERVICE = "service";

  public static final String SPECIFICATION_LINK_KEY = "SpecificationLink";

  public static final String TARGET_BINDING = "targetBinding";

  private WebMapHelper webMapHelper = new WebMapHelper();

  /**
   * This method creates a Map<String, Object> representation of the ServiceBindingType provided.
   * The following keys will be added to the map (Taken from EbrimConstants):
   *
   * <p>ACCESS_URI = "accessUri"; SERVICE = "service"; TARGET_BINDING = "targetBinding";
   * SPECIFICATION_LINK_KEY = "SpecificationLink";
   *
   * <p>This will also try to parse RegistryObjectType values to the map.
   *
   * <p>Uses: SpecificationLinkWebConverter
   *
   * @param binding the ServiceBindingType to be converted into a map, null returns empty Map
   * @return Map<String, Object> representation of the ServiceBindingType provided
   */
  public Map<String, Object> convert(ServiceBindingType binding) {
    Map<String, Object> bindingMap = new HashMap<>();
    if (binding == null) {
      return bindingMap;
    }

    webMapHelper.putAllIfNotEmpty(bindingMap, super.convertRegistryObject(binding));
    webMapHelper.putIfNotEmpty(bindingMap, ACCESS_URI, binding.getAccessURI());
    webMapHelper.putIfNotEmpty(bindingMap, SERVICE, binding.getService());

    if (binding.isSetSpecificationLink()) {
      List<Map<String, Object>> specificationLinks = new ArrayList<>();
      SpecificationLinkWebConverter specificationLinkConverter =
          new SpecificationLinkWebConverter();

      for (SpecificationLinkType specificationLink : binding.getSpecificationLink()) {
        Map<String, Object> specificationLinkMap =
            specificationLinkConverter.convert(specificationLink);

        if (MapUtils.isNotEmpty(specificationLinkMap)) {
          specificationLinks.add(specificationLinkMap);
        }
      }

      webMapHelper.putIfNotEmpty(bindingMap, SPECIFICATION_LINK_KEY, specificationLinks);
    }

    webMapHelper.putIfNotEmpty(bindingMap, TARGET_BINDING, binding.getTargetBinding());

    return bindingMap;
  }
}
