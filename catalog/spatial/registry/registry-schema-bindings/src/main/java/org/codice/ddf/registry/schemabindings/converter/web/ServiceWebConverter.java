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
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ServiceType;
import org.apache.commons.collections.MapUtils;
import org.codice.ddf.registry.schemabindings.helper.WebMapHelper;

public class ServiceWebConverter extends RegistryObjectWebConverter {
  public static final String SERVICE_BINDING_KEY = "ServiceBinding";

  private WebMapHelper webMapHelper = new WebMapHelper();

  /**
   * This method creates a Map<String, Object> representation of the ServiceType provided. The
   * following keys will be added to the map (Taken from EbrimConstants):
   *
   * <p>SERVICE_BINDING_KEY = "ServiceBinding";
   *
   * <p>This will also try to parse RegistryObjectType values to the map.
   *
   * <p>Uses: ServiceBindingWebConverter
   *
   * @param service the ServiceType to be converted into a map, null returns empty Map
   * @return Map<String, Object> representation of the ServiceType provided
   */
  public Map<String, Object> convert(ServiceType service) {
    Map<String, Object> serviceMap = new HashMap<>();
    if (service == null) {
      return serviceMap;
    }

    webMapHelper.putAllIfNotEmpty(serviceMap, super.convertRegistryObject(service));

    if (service.isSetServiceBinding()) {
      List<Map<String, Object>> bindings = new ArrayList<>();
      ServiceBindingWebConverter bindingConverter = new ServiceBindingWebConverter();

      for (ServiceBindingType binding : service.getServiceBinding()) {
        Map<String, Object> bindingMap = bindingConverter.convert(binding);

        if (MapUtils.isNotEmpty(bindingMap)) {
          bindings.add(bindingMap);
        }
      }

      webMapHelper.putIfNotEmpty(serviceMap, SERVICE_BINDING_KEY, bindings);
    }

    return serviceMap;
  }
}
