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
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import org.codice.ddf.registry.schemabindings.helper.WebMapHelper;

public class ExtrinsicObjectWebConverter extends RegistryObjectWebConverter {
  public static final String CONTENT_VERSION_INFO = "ContentVersionInfo";

  public static final String IS_OPAQUE = "isOpaque";

  public static final String MIME_TYPE = "mimeType";

  private WebMapHelper webMapHelper = new WebMapHelper();

  /**
   * This method creates a Map<String, Object> representation of the ExtrinsicObjectType provided.
   * The following keys will be added to the map (Taken from EbrimConstants):
   *
   * <p>CONTENT_VERSION_INFO = "ContentVersionInfo"; IS_OPAQUE = "isOpaque"; MIME_TYPE = "mimeType";
   *
   * <p>This will also try to parse RegistryObjectType values to the map.
   *
   * @param extrinsicObject the ExtrinsicObjectType to be converted into a map, null returns empty
   *     Map
   * @return Map<String, Object> representation of the ExtrinsicObjectType provided
   */
  public Map<String, Object> convert(ExtrinsicObjectType extrinsicObject) {
    Map<String, Object> extrinsicObjectMap = new HashMap<>();
    if (extrinsicObject == null) {
      return extrinsicObjectMap;
    }

    webMapHelper.putAllIfNotEmpty(extrinsicObjectMap, super.convertRegistryObject(extrinsicObject));
    webMapHelper.putIfNotEmpty(
        extrinsicObjectMap, CONTENT_VERSION_INFO, extrinsicObject.getContentVersionInfo());
    webMapHelper.putIfNotEmpty(extrinsicObjectMap, IS_OPAQUE, extrinsicObject.isIsOpaque());
    webMapHelper.putIfNotEmpty(extrinsicObjectMap, MIME_TYPE, extrinsicObject.getMimeType());

    return extrinsicObjectMap;
  }
}
