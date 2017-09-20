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
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import org.codice.ddf.registry.schemabindings.helper.WebMapHelper;

public class ClassificationWebConverter extends RegistryObjectWebConverter {
  public static final String CLASSIFICATION_NODE = "classificationNode";

  public static final String CLASSIFIED_OBJECT = "classifiedObject";

  public static final String CLASSIFICATION_SCHEME = "classificationScheme";

  public static final String NODE_REPRESENTATION = "nodeRepresentation";

  private WebMapHelper webMapHelper = new WebMapHelper();

  /**
   * This method creates a Map<String, Object> representation of the ClassificationType provided.
   * The following keys will be added to the map (Taken from EbrimConstants):
   *
   * <p>CLASSIFICATION_NODE = "classificationNode"; CLASSIFIED_OBJECT = "classifiedObject";
   * CLASSIFICATION_SCHEME = "classificationScheme"; NODE_REPRESENTATION = "nodeRepresentation";
   *
   * <p>This will also try to parse RegistryObjectType values to the map.
   *
   * @param classification the ClassificationType to be converted into a map, null returns empty Map
   * @return Map<String, Object> representation of the ClassificationType provided
   */
  public Map<String, Object> convert(ClassificationType classification) {
    Map<String, Object> classificationMap = new HashMap<>();
    if (classification == null) {
      return classificationMap;
    }

    webMapHelper.putAllIfNotEmpty(classificationMap, super.convertRegistryObject(classification));
    webMapHelper.putIfNotEmpty(
        classificationMap, CLASSIFICATION_NODE, classification.getClassificationNode());
    webMapHelper.putIfNotEmpty(
        classificationMap, CLASSIFICATION_SCHEME, classification.getClassificationScheme());
    webMapHelper.putIfNotEmpty(
        classificationMap, CLASSIFIED_OBJECT, classification.getClassifiedObject());
    webMapHelper.putIfNotEmpty(
        classificationMap, NODE_REPRESENTATION, classification.getNodeRepresentation());

    return classificationMap;
  }
}
