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
import oasis.names.tc.ebxml_regrep.xsd.rim._3.EmailAddressType;
import org.codice.ddf.registry.schemabindings.helper.WebMapHelper;

public class EmailAddressWebConverter {
  public static final String ADDRESS = "address";

  public static final String TYPE = "type";

  private WebMapHelper webMapHelper = new WebMapHelper();

  /**
   * This method creates a Map<String, Object> representation of the EmailAddressType provided. The
   * following keys will be added to the map (Taken from EbrimConstants):
   *
   * <p>ADDRESS = "address"; TYPE = "type";
   *
   * @param emailAddress the EmailAddressType to be converted into a map, null returns empty Map
   * @return Map<String, Object> representation of the EmailAddressType provided
   */
  public Map<String, Object> convert(EmailAddressType emailAddress) {
    Map<String, Object> emailAddressMap = new HashMap<>();
    if (emailAddress == null) {
      return emailAddressMap;
    }

    webMapHelper.putIfNotEmpty(emailAddressMap, ADDRESS, emailAddress.getAddress());
    webMapHelper.putIfNotEmpty(emailAddressMap, TYPE, emailAddress.getType());

    return emailAddressMap;
  }
}
