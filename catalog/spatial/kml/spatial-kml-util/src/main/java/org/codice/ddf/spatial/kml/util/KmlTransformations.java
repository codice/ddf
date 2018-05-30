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
package org.codice.ddf.spatial.kml.util;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Style;

public class KmlTransformations {

  /**
   * Wrap KML document with the opening and closing kml tags
   *
   * @param document
   * @param folderId which should be the subscription id if it exists
   * @return completed KML
   */
  public static Kml encloseKml(Document doc, String docId, String docName) {
    Kml kml = KmlFactory.createKml();
    if (doc != null) {
      kml.setFeature(doc);
      doc.setId(docId); // Id should be subscription id
      doc.setName(docName);
      doc.setOpen(false);
    }
    return kml;
  }

  /**
   * Encapsulate the kml content (placemarks, etc.) with a style in a KML Document element. If
   * either content or style are not null, they will be in the resulting Document
   *
   * @param kml
   * @param style
   * @param documentId which should be the metacard id
   * @return KML DocumentType element with style and content
   */
  public static Document encloseDoc(
      Placemark placemark, Style style, String documentId, String docName) {
    Document document = KmlFactory.createDocument();
    document.setId(documentId);
    document.setOpen(true);
    document.setName(docName);

    if (style != null) {
      document.getStyleSelector().add(style);
    }
    if (placemark != null) {
      document.getFeature().add(placemark);
    }

    return document;
  }
}
