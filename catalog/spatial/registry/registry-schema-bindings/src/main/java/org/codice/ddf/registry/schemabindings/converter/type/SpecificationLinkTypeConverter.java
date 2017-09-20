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
import static org.codice.ddf.registry.schemabindings.converter.web.SpecificationLinkWebConverter.SERVICE_BINDING;
import static org.codice.ddf.registry.schemabindings.converter.web.SpecificationLinkWebConverter.SPECIFICATION_OBJECT;
import static org.codice.ddf.registry.schemabindings.converter.web.SpecificationLinkWebConverter.USAGE_DESCRIPTION;
import static org.codice.ddf.registry.schemabindings.converter.web.SpecificationLinkWebConverter.USAGE_PARAMETERS;

import java.util.Map;
import java.util.Optional;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SpecificationLinkType;
import org.apache.commons.collections.MapUtils;
import org.codice.ddf.registry.schemabindings.helper.MapToSchemaElement;
import org.codice.ddf.registry.schemabindings.helper.WebMapHelper;

public class SpecificationLinkTypeConverter
    extends AbstractRegistryObjectTypeConverter<SpecificationLinkType> {
  private WebMapHelper webMapHelper = new WebMapHelper();

  public SpecificationLinkTypeConverter(
      MapToSchemaElement<SpecificationLinkType> mapToSchemaElement) {
    super(mapToSchemaElement);
  }

  public SpecificationLinkTypeConverter() {
    this(new MapToSchemaElement<>(RIM_FACTORY::createSpecificationLinkType));
  }

  /**
   * This method creates an SpecificationLinkType from the values in the provided map. The following
   * keys are expected in the provided map (Taken from EbrimConstants):
   *
   * <p>SERVICE_BINDING = "serviceBinding"; SPECIFICATION_OBJECT = "specificationObject";
   * USAGE_DESCRIPTION = "UsageDescription"; USAGE_PARAMETERS = "UsageParameters";
   *
   * <p>This will also try to populate the RegistryObjectType values also looked for in the map.
   *
   * <p>Uses: InternationalStringTypeHelper
   *
   * @param map the Map representation of the SpecificationLinkType to generate, null returns empty
   *     Optional
   * @return Optional SpecificationLinkType created from the values in the map
   */
  public Optional<SpecificationLinkType> convert(Map<String, Object> map) {
    Optional<SpecificationLinkType> optionalSpecificationLink = Optional.empty();
    if (MapUtils.isEmpty(map)) {
      return optionalSpecificationLink;
    }

    optionalSpecificationLink = super.convert(map);

    optionalSpecificationLink =
        mapToSchemaElement.populateStringElement(
            map,
            SERVICE_BINDING,
            optionalSpecificationLink,
            (valueToPopulate, specificationLink) ->
                specificationLink.setServiceBinding(valueToPopulate));

    optionalSpecificationLink =
        mapToSchemaElement.populateStringElement(
            map,
            SPECIFICATION_OBJECT,
            optionalSpecificationLink,
            (valueToPopulate, specificationLink) ->
                specificationLink.setSpecificationObject(valueToPopulate));

    optionalSpecificationLink =
        mapToSchemaElement.populateInternationalStringTypeElement(
            map,
            USAGE_DESCRIPTION,
            optionalSpecificationLink,
            (istToPopulate, specificationLink) ->
                specificationLink.setUsageDescription(istToPopulate));

    if (map.containsKey(USAGE_PARAMETERS)) {
      if (!optionalSpecificationLink.isPresent()) {
        optionalSpecificationLink = Optional.of(mapToSchemaElement.getObjectFactory().get());
      }
      optionalSpecificationLink
          .get()
          .getUsageParameter()
          .addAll(webMapHelper.getStringListFromMap(map, USAGE_PARAMETERS));
    }

    return optionalSpecificationLink;
  }
}
