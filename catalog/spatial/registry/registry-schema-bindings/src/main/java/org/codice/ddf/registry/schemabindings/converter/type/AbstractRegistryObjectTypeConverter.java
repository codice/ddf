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

import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectWebConverter.CLASSIFICATION_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectWebConverter.DESCRIPTION_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectWebConverter.EXTERNAL_IDENTIFIER_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectWebConverter.HOME_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectWebConverter.ID_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectWebConverter.LID_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectWebConverter.NAME_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectWebConverter.OBJECT_TYPE_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectWebConverter.SLOT;
import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectWebConverter.STATUS_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectWebConverter.VERSION_INFO_KEY;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import org.codice.ddf.registry.schemabindings.helper.MapToSchemaElement;

public abstract class AbstractRegistryObjectTypeConverter<T extends RegistryObjectType> {

  protected MapToSchemaElement<T> mapToSchemaElement;

  public AbstractRegistryObjectTypeConverter(MapToSchemaElement<T> mapToSchemaElement) {
    this.mapToSchemaElement = mapToSchemaElement;
  }

  /**
   * This method creates a RegistryObjectType from the values in the provided map. The following
   * keys are expected in the provided map (Taken from EbrimConstants):
   *
   * <p>CLASSIFICATION_KEY = "Classification"; EXTERNAL_IDENTIFIER_KEY = "ExternalIdentifier";
   * NAME_KEY = "Name"; DESCRIPTION_KEY = "Description"; VERSION_INFO_KEY = "VersionInfo"; SLOT =
   * "Slot"; ID_KEY = "id"; HOME_KEY = "home"; LID_KEY = "Lid"; STATUS_KEY = "Status";
   * OBJECT_TYPE_KEY = "objectType";
   *
   * <p>Uses: ClassificationTypeConverter ExternalIdentifierTypeConverter SlotTypeConverter
   * InternationalStringTypeHelper
   *
   * @param map the Map representation of the RegistryObjectType to generate, null returns empty
   *     Optional
   * @return Optional RegistryObjectType created from the values in the map
   */
  public Optional<T> convert(Map<String, Object> map) {
    Optional<T> optionalRegistryObject = Optional.empty();

    if (map.containsKey(CLASSIFICATION_KEY)) {
      List<Map<String, Object>> classificationMaps =
          (List<Map<String, Object>>) map.get(CLASSIFICATION_KEY);

      ClassificationTypeConverter classificationConverter = new ClassificationTypeConverter();

      Optional<ClassificationType> optionalClassification;
      for (Map<String, Object> classificationMap : classificationMaps) {
        optionalClassification = classificationConverter.convert(classificationMap);
        if (optionalClassification.isPresent()) {
          if (!optionalRegistryObject.isPresent()) {
            optionalRegistryObject = Optional.of(mapToSchemaElement.getObjectFactory().get());
          }

          optionalRegistryObject.get().getClassification().add(optionalClassification.get());
        }
      }
    }

    optionalRegistryObject =
        mapToSchemaElement.populateInternationalStringTypeElement(
            map,
            DESCRIPTION_KEY,
            optionalRegistryObject,
            (istValue, registryObject) -> registryObject.setDescription(istValue));

    if (map.containsKey(EXTERNAL_IDENTIFIER_KEY)) {
      Optional<ExternalIdentifierType> optionalExternalIdentifier;
      ExternalIdentifierTypeConverter eitConverter = new ExternalIdentifierTypeConverter();
      for (Map<String, Object> externalIdentifierMap :
          (List<Map<String, Object>>) map.get(EXTERNAL_IDENTIFIER_KEY)) {
        optionalExternalIdentifier = eitConverter.convert(externalIdentifierMap);

        if (optionalExternalIdentifier.isPresent()) {
          if (!optionalRegistryObject.isPresent()) {
            optionalRegistryObject = Optional.of(mapToSchemaElement.getObjectFactory().get());
          }

          optionalRegistryObject
              .get()
              .getExternalIdentifier()
              .add(optionalExternalIdentifier.get());
        }
      }
    }

    optionalRegistryObject =
        mapToSchemaElement.populateStringElement(
            map,
            HOME_KEY,
            optionalRegistryObject,
            (value, registryObject) -> registryObject.setHome(value));
    optionalRegistryObject =
        mapToSchemaElement.populateStringElement(
            map,
            ID_KEY,
            optionalRegistryObject,
            (value, registryObject) -> registryObject.setId(value));
    optionalRegistryObject =
        mapToSchemaElement.populateStringElement(
            map,
            LID_KEY,
            optionalRegistryObject,
            (value, registryObject) -> registryObject.setLid(value));

    optionalRegistryObject =
        mapToSchemaElement.populateInternationalStringTypeElement(
            map,
            NAME_KEY,
            optionalRegistryObject,
            (istValue, registryObject) -> registryObject.setName(istValue));
    optionalRegistryObject =
        mapToSchemaElement.populateStringElement(
            map,
            OBJECT_TYPE_KEY,
            optionalRegistryObject,
            (value, registryObject) -> registryObject.setObjectType(value));

    if (map.containsKey(SLOT)) {
      Optional<SlotType1> optionalSlot;
      SlotTypeConverter stConverter = new SlotTypeConverter();
      for (Map<String, Object> slotMap : (List<Map<String, Object>>) map.get(SLOT)) {
        optionalSlot = stConverter.convert(slotMap);
        if (optionalSlot.isPresent()) {
          if (!optionalRegistryObject.isPresent()) {
            optionalRegistryObject = Optional.of(mapToSchemaElement.getObjectFactory().get());
          }

          optionalRegistryObject.get().getSlot().add(optionalSlot.get());
        }
      }
    }

    optionalRegistryObject =
        mapToSchemaElement.populateStringElement(
            map,
            STATUS_KEY,
            optionalRegistryObject,
            (value, registryObject) -> registryObject.setStatus(value));
    optionalRegistryObject =
        mapToSchemaElement.populateVersionInfoTypeElement(
            map,
            VERSION_INFO_KEY,
            optionalRegistryObject,
            (versionInfoValue, registryObject) -> registryObject.setVersionInfo(versionInfoValue));

    return optionalRegistryObject;
  }
}
