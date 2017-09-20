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
import static org.codice.ddf.registry.schemabindings.converter.web.PersonNameWebConverter.FIRST_NAME;
import static org.codice.ddf.registry.schemabindings.converter.web.PersonNameWebConverter.LAST_NAME;
import static org.codice.ddf.registry.schemabindings.converter.web.PersonNameWebConverter.MIDDLE_NAME;

import java.util.Map;
import java.util.Optional;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PersonNameType;
import org.apache.commons.collections.MapUtils;
import org.codice.ddf.registry.schemabindings.helper.MapToSchemaElement;

public class PersonNameTypeConverter {

  private final MapToSchemaElement<PersonNameType> mapToSchemaElement =
      new MapToSchemaElement<>(RIM_FACTORY::createPersonNameType);

  /**
   * This method creates an PersonNameType from the values in the provided map. The following keys
   * are expected in the provided map (Taken from EbrimConstants):
   *
   * <p>FIRST_NAME = "firstName"; MIDDLE_NAME = "middleName"; LAST_NAME = "lastName";
   *
   * @param map the Map representation of the PersonNameType to generate, null returns empty
   *     Optional
   * @return Optional PersonNameType created from the values in the map
   */
  public Optional<PersonNameType> convert(Map<String, Object> map) {
    Optional<PersonNameType> optionalPersonName = Optional.empty();
    if (MapUtils.isEmpty(map)) {
      return optionalPersonName;
    }

    optionalPersonName =
        mapToSchemaElement.populateStringElement(
            map,
            FIRST_NAME,
            optionalPersonName,
            (value, personName) -> personName.setFirstName(value));
    optionalPersonName =
        mapToSchemaElement.populateStringElement(
            map,
            MIDDLE_NAME,
            optionalPersonName,
            (value, personName) -> personName.setMiddleName(value));
    optionalPersonName =
        mapToSchemaElement.populateStringElement(
            map,
            LAST_NAME,
            optionalPersonName,
            (value, personName) -> personName.setLastName(value));

    return optionalPersonName;
  }
}
