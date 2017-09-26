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
package org.codice.ddf.spatial.kml.transformer;

import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This class handles .kmz files by unzipping them and passing the first .kml file to the {@link
 * KmlInputTransformer}. All other files within the .kmz are ignored.
 */
public class KmzInputTransformer implements InputTransformer {

  private KmlInputTransformer kmlInputTransformer;

  public static final String KML_EXTENSION = ".kml";

  public KmzInputTransformer(KmlInputTransformer kmlInputTransformer) {
    this.kmlInputTransformer = kmlInputTransformer;
  }

  @Override
  public Metacard transform(InputStream inputStream)
      throws IOException, CatalogTransformerException {
    return transform(inputStream, null);
  }

  @Override
  public Metacard transform(InputStream inputStream, String id)
      throws IOException, CatalogTransformerException {

    ZipInputStream zipInputStream = new ZipInputStream(inputStream);

    ZipEntry entry;
    while ((entry = zipInputStream.getNextEntry()) != null) {

      // According to Google, a .kmz should only contain a single .kml file
      // so we stop at the first one we find.
      if (entry.getName().endsWith(KML_EXTENSION)) {
        return kmlInputTransformer.transform(zipInputStream, id);
      }
    }

    throw new CatalogTransformerException("Unable to parse any KML from KMZ file");
  }
}
