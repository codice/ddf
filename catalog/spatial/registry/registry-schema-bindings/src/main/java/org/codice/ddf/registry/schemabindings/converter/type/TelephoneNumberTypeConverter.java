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
package org.codice.ddf.registry.schemabindings.converter.type;

import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;
import static org.codice.ddf.registry.schemabindings.converter.web.TelephoneNumberWebConverter.AREA_CODE;
import static org.codice.ddf.registry.schemabindings.converter.web.TelephoneNumberWebConverter.COUNTRY_CODE;
import static org.codice.ddf.registry.schemabindings.converter.web.TelephoneNumberWebConverter.EXTENSION;
import static org.codice.ddf.registry.schemabindings.converter.web.TelephoneNumberWebConverter.NUMBER;
import static org.codice.ddf.registry.schemabindings.converter.web.TelephoneNumberWebConverter.PHONE_TYPE;

import java.util.Map;
import java.util.Optional;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.TelephoneNumberType;
import org.codice.ddf.registry.schemabindings.helper.MapToSchemaElement;

public class TelephoneNumberTypeConverter {

  private MapToSchemaElement<TelephoneNumberType> mapToSchemaElement =
      new MapToSchemaElement<>(RIM_FACTORY::createTelephoneNumberType);

  /**
   * This method creates an OrganizationType from the values in the provided map. The following keys
   * are expected in the provided map (Taken from EbrimConstants):
   *
   * <p>COUNTRY_CODE = "countryCode"; PHONE_TYPE = "phoneType"; AREA_CODE = "areaCode"; NUMBER =
   * "number"; EXTENSION = "extension";
   *
   * @param map the Map representation of the OrganizationType to generate, null returns empty
   *     Optional
   * @return Optional OrganizationType created from the values in the map
   */
  public Optional<TelephoneNumberType> convert(Map<String, Object> map) {
    Optional<TelephoneNumberType> optionalTelephone = Optional.empty();

    optionalTelephone =
        mapToSchemaElement.populateStringElement(
            map,
            AREA_CODE,
            optionalTelephone,
            (valueToPopulate, phoneNumber) -> phoneNumber.setAreaCode(valueToPopulate));

    optionalTelephone =
        mapToSchemaElement.populateStringElement(
            map,
            COUNTRY_CODE,
            optionalTelephone,
            (valueToPopulate, optional) -> optional.setCountryCode(valueToPopulate));

    optionalTelephone =
        mapToSchemaElement.populateStringElement(
            map,
            EXTENSION,
            optionalTelephone,
            (valueToPopulate, phoneNumber) -> phoneNumber.setExtension(valueToPopulate));

    optionalTelephone =
        mapToSchemaElement.populateStringElement(
            map,
            NUMBER,
            optionalTelephone,
            (valueToPopulate, phoneNumber) -> phoneNumber.setNumber(valueToPopulate));

    optionalTelephone =
        mapToSchemaElement.populateStringElement(
            map,
            PHONE_TYPE,
            optionalTelephone,
            (valueToPopulate, phoneNumber) -> phoneNumber.setPhoneType(valueToPopulate));

    return optionalTelephone;
  }
}
