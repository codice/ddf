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
import oasis.names.tc.ebxml_regrep.xsd.rim._3.TelephoneNumberType;
import org.codice.ddf.registry.schemabindings.helper.WebMapHelper;

public class TelephoneNumberWebConverter {
  public static final String AREA_CODE = "areaCode";

  public static final String COUNTRY_CODE = "countryCode";

  public static final String EXTENSION = "extension";

  public static final String NUMBER = "number";

  public static final String PHONE_TYPE = "phoneType";

  private WebMapHelper webMapHelper = new WebMapHelper();

  /**
   * This method creates a Map<String, Object> representation of the TelephoneNumberType provided.
   * The following keys will be added to the map (Taken from EbrimConstants):
   *
   * <p>COUNTRY_CODE = "countryCode"; PHONE_TYPE = "phoneType"; AREA_CODE = "areaCode"; NUMBER =
   * "number"; EXTENSION = "extension";
   *
   * @param phoneNumber the TelephoneNumberType to be converted into a map, null returns empty Map
   * @return Map<String, Object> representation of the TelephoneNumberType provided
   */
  public Map<String, Object> convert(TelephoneNumberType phoneNumber) {
    Map<String, Object> phoneNumberMap = new HashMap<>();

    webMapHelper.putIfNotEmpty(phoneNumberMap, AREA_CODE, phoneNumber.getAreaCode());
    webMapHelper.putIfNotEmpty(phoneNumberMap, COUNTRY_CODE, phoneNumber.getCountryCode());
    webMapHelper.putIfNotEmpty(phoneNumberMap, EXTENSION, phoneNumber.getExtension());
    webMapHelper.putIfNotEmpty(phoneNumberMap, NUMBER, phoneNumber.getNumber());
    webMapHelper.putIfNotEmpty(phoneNumberMap, PHONE_TYPE, phoneNumber.getPhoneType());

    return phoneNumberMap;
  }
}
