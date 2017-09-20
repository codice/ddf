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
import static org.codice.ddf.registry.schemabindings.converter.web.PostalAddressWebConverter.CITY;
import static org.codice.ddf.registry.schemabindings.converter.web.PostalAddressWebConverter.COUNTRY;
import static org.codice.ddf.registry.schemabindings.converter.web.PostalAddressWebConverter.POSTAL_CODE;
import static org.codice.ddf.registry.schemabindings.converter.web.PostalAddressWebConverter.STATE_OR_PROVINCE;
import static org.codice.ddf.registry.schemabindings.converter.web.PostalAddressWebConverter.STREET;
import static org.codice.ddf.registry.schemabindings.converter.web.PostalAddressWebConverter.STREET_NUMBER;

import java.util.Map;
import java.util.Optional;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PostalAddressType;
import org.apache.commons.collections.MapUtils;
import org.codice.ddf.registry.schemabindings.helper.MapToSchemaElement;

public class PostalAddressTypeConverter {

  private MapToSchemaElement<PostalAddressType> mapToSchemaElement =
      new MapToSchemaElement<>(RIM_FACTORY::createPostalAddressType);

  /**
   * This method creates an PostalAddressType from the values in the provided map. The following
   * keys are expected in the provided map (Taken from EbrimConstants):
   *
   * <p>CITY = "city"; COUNTRY = "country"; POSTAL_CODE = "postalCode"; STATE_OR_PROVINCE =
   * "stateOrProvince"; STREET = "street"; STREET_NUMBER = "streetNumber";
   *
   * @param map the Map representation of the PostalAddressType to generate, null returns empty
   *     Optional
   * @return Optional PostalAddressType created from the values in the map
   */
  public Optional<PostalAddressType> convert(Map<String, Object> map) {
    Optional<PostalAddressType> optionalAddress = Optional.empty();
    if (MapUtils.isEmpty(map)) {
      return optionalAddress;
    }

    optionalAddress =
        mapToSchemaElement.populateStringElement(
            map,
            CITY,
            optionalAddress,
            (valueToPopulate, postalAddress) -> postalAddress.setCity(valueToPopulate));

    optionalAddress =
        mapToSchemaElement.populateStringElement(
            map,
            COUNTRY,
            optionalAddress,
            (valueToPopulate, postalAddress) -> postalAddress.setCountry(valueToPopulate));

    optionalAddress =
        mapToSchemaElement.populateStringElement(
            map,
            POSTAL_CODE,
            optionalAddress,
            (valueToPopulate, postalAddress) -> postalAddress.setPostalCode(valueToPopulate));

    optionalAddress =
        mapToSchemaElement.populateStringElement(
            map,
            STATE_OR_PROVINCE,
            optionalAddress,
            (valueToPopulate, postalAddress) -> postalAddress.setStateOrProvince(valueToPopulate));

    optionalAddress =
        mapToSchemaElement.populateStringElement(
            map,
            STREET,
            optionalAddress,
            (valueToPopulate, postalAddress) -> postalAddress.setStreet(valueToPopulate));

    optionalAddress =
        mapToSchemaElement.populateStringElement(
            map,
            STREET_NUMBER,
            optionalAddress,
            (valueToPopulate, postalAddress) -> postalAddress.setStreetNumber(valueToPopulate));

    return optionalAddress;
  }
}
