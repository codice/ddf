/*
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.kml.transformer;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.codice.ddf.spatial.kml.converter.KmlToMetacard;

public class KmlInputTransformer implements InputTransformer {
  private MetacardType metacardType;

  public KmlInputTransformer(MetacardType metacardType) {
    this.metacardType = metacardType;
  }

  @Override
  public Metacard transform(InputStream inputStream)
      throws IOException, CatalogTransformerException {
    return transform(inputStream, null);
  }

  @Override
  public Metacard transform(InputStream inputStream, String id)
      throws IOException, CatalogTransformerException {

    MetacardImpl metacard;

    try (TemporaryFileBackedOutputStream fileBackedOutputStream =
        new TemporaryFileBackedOutputStream()) {

      try {
        IOUtils.copy(inputStream, fileBackedOutputStream);

      } catch (IOException e) {
        throw new CatalogTransformerException(
            "Unable to transform KML to Metacard. Error reading input stream.", e);
      } finally {
        IOUtils.closeQuietly(inputStream);
      }

      try (InputStream inputStreamCopy = fileBackedOutputStream.asByteSource().openStream()) {
        metacard = (MetacardImpl) unmarshal(inputStreamCopy);
      }

      if (metacard == null) {
        throw new CatalogTransformerException("Unable to transform Kml to Metacard.");
      } else if (StringUtils.isNotEmpty(id)) {
        metacard.setAttribute(Core.ID, id);
      }

      try (Reader reader =
          fileBackedOutputStream.asByteSource().asCharSource(Charsets.UTF_8).openStream()) {
        String kmlString = CharStreams.toString(reader);
        metacard.setAttribute(Core.METADATA, kmlString);
      }

    } catch (IOException e) {
      throw new CatalogTransformerException(
          "Unable to transform KML to Metacard. Error using file-backed stream.", e);
    }

    return metacard;
  }

  private Metacard unmarshal(InputStream inputStream) {
    Kml kml = Kml.unmarshal(inputStream);

    return KmlToMetacard.from(new MetacardImpl(metacardType), kml);
  }
}
