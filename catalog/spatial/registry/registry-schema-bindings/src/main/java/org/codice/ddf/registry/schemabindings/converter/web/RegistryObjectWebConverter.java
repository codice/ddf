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
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import org.apache.commons.collections.MapUtils;
import org.codice.ddf.registry.schemabindings.helper.InternationalStringTypeHelper;
import org.codice.ddf.registry.schemabindings.helper.WebMapHelper;

public class RegistryObjectWebConverter {
  public static final String CLASSIFICATION_KEY = "Classification";

  public static final String DESCRIPTION_KEY = "Description";

  public static final String EXTERNAL_IDENTIFIER_KEY = "ExternalIdentifier";

  public static final String HOME_KEY = "home";

  public static final String ID_KEY = "id";

  public static final String LID_KEY = "Lid";

  public static final String NAME_KEY = "Name";

  public static final String OBJECT_TYPE_KEY = "objectType";

  public static final String SLOT = "Slot";

  public static final String STATUS_KEY = "Status";

  public static final String VERSION_INFO_KEY = "VersionInfo";

  protected static final InternationalStringTypeHelper INTERNATIONAL_STRING_TYPE_HELPER =
      new InternationalStringTypeHelper();

  private WebMapHelper webMapHelper = new WebMapHelper();

  /**
   * This method creates a Map<String, Object> representation of the RegistryObjectType provided.
   * The following keys will be added to the map (Taken from EbrimConstants):
   *
   * <p>CLASSIFICATION_KEY = "Classification"; EXTERNAL_IDENTIFIER_KEY = "ExternalIdentifier";
   * NAME_KEY = "Name"; DESCRIPTION_KEY = "Description"; VERSION_INFO_KEY = "VersionInfo"; SLOT =
   * "Slot"; ID_KEY = "id"; HOME_KEY = "home"; LID_KEY = "Lid"; STATUS_KEY = "Status";
   * OBJECT_TYPE_KEY = "objectType";
   *
   * <p>Uses: ClassificationWebConverter ExternalIdentifierWebConverter SlotWebConverter
   * InternationalStringTypeHelper
   *
   * @param registryObject the RegistryObjectType to be converted into a map, null returns empty Map
   * @return Map<String, Object> representation of the RegistryObjectType provided
   */
  public Map<String, Object> convertRegistryObject(RegistryObjectType registryObject) {
    Map<String, Object> registryObjectMap = new HashMap<>();

    if (registryObject.isSetClassification()) {
      List<Map<String, Object>> classifications = new ArrayList<>();
      ClassificationWebConverter classificationConverter = new ClassificationWebConverter();

      for (ClassificationType classification : registryObject.getClassification()) {
        Map<String, Object> classificationMap = classificationConverter.convert(classification);

        if (MapUtils.isNotEmpty(classificationMap)) {
          classifications.add(classificationMap);
        }
      }

      webMapHelper.putIfNotEmpty(registryObjectMap, CLASSIFICATION_KEY, classifications);
    }

    webMapHelper.putIfNotEmpty(registryObjectMap, DESCRIPTION_KEY, registryObject.getDescription());

    if (registryObject.isSetExternalIdentifier()) {
      List<Map<String, Object>> externalIdentifiers = new ArrayList<>();
      ExternalIdentifierWebConverter externalIdentifierConverter =
          new ExternalIdentifierWebConverter();

      for (ExternalIdentifierType externalIdentifier : registryObject.getExternalIdentifier()) {
        Map<String, Object> externalIdentifierMap =
            externalIdentifierConverter.convert(externalIdentifier);
        if (MapUtils.isNotEmpty(externalIdentifierMap)) {
          externalIdentifiers.add(externalIdentifierMap);
        }
      }

      webMapHelper.putIfNotEmpty(registryObjectMap, EXTERNAL_IDENTIFIER_KEY, externalIdentifiers);
    }

    webMapHelper.putIfNotEmpty(registryObjectMap, HOME_KEY, registryObject.getHome());
    webMapHelper.putIfNotEmpty(registryObjectMap, ID_KEY, registryObject.getId());
    webMapHelper.putIfNotEmpty(registryObjectMap, LID_KEY, registryObject.getLid());
    webMapHelper.putIfNotEmpty(registryObjectMap, NAME_KEY, registryObject.getName());
    webMapHelper.putIfNotEmpty(registryObjectMap, OBJECT_TYPE_KEY, registryObject.getObjectType());

    if (registryObject.isSetSlot()) {
      List<Map<String, Object>> slots = new ArrayList<>();
      SlotWebConverter slotConverter = new SlotWebConverter();

      for (SlotType1 slot : registryObject.getSlot()) {
        Map<String, Object> slotMap = slotConverter.convert(slot);
        if (MapUtils.isNotEmpty(slotMap)) {
          slots.add(slotMap);
        }
      }

      webMapHelper.putIfNotEmpty(registryObjectMap, SLOT, slots);
    }

    webMapHelper.putIfNotEmpty(registryObjectMap, STATUS_KEY, registryObject.getStatus());
    webMapHelper.putIfNotEmpty(
        registryObjectMap, VERSION_INFO_KEY, registryObject.getVersionInfo());

    return registryObjectMap;
  }
}
