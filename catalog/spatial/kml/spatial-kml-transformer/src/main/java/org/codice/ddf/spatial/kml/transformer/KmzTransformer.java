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

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.tika.io.IOUtils;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transformer for handling requests to take a {@link Metacard} or {@link SourceResponse} and
 * converting to a zipped KMZ file.
 */
public class KmzTransformer implements QueryResponseTransformer, MetacardTransformer {

  private static final MimeType KMZ_MIMETYPE;

  private static final String DOC_KML = "doc.kml";

  private static final Logger LOGGER = LoggerFactory.getLogger(KmzTransformer.class);

  static {
    try {
      KMZ_MIMETYPE = new MimeType("application/vnd.google-earth.kmz");
    } catch (MimeTypeParseException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private KMLTransformer kmlTransformer;

  public KmzTransformer(KMLTransformer kmlTransformer) {
    this.kmlTransformer = kmlTransformer;
  }

  /**
   * Transforms a {@link SourceResponse} to a zipped KMZ file.
   *
   * @param upstreamResponse {@link SourceResponse} containing the results of a query.
   * @param arguments the arguments that may be used to execute the transform
   * @return KMZ BinaryContent
   * @throws CatalogTransformerException
   */
  @Override
  public BinaryContent transform(
      SourceResponse upstreamResponse, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    BinaryContent kml = kmlTransformer.transform(upstreamResponse, arguments);
    BinaryContent kmz = transformKmlToKmz(kml);

    if (kmz == null) {
      throw new CatalogTransformerException("Unable to transform KML to KMZ.");
    }
    return kmz;
  }

  /**
   * Transforms a single metacard to a zipped KMZ file.
   *
   * @param metacard the {@link Metacard} to be transformed
   * @param arguments any arguments to be used in the transformation. Keys are specific to each
   *     {@link MetacardTransformer} implementation
   * @return KMZ BinaryContent
   * @throws CatalogTransformerException
   */
  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    BinaryContent kml = kmlTransformer.transform(metacard, arguments);
    BinaryContent kmz = transformKmlToKmz(kml);

    if (kmz == null) {
      throw new CatalogTransformerException(
          "Unable to transform to KMZ for metacard ID: " + metacard.getId());
    }
    return kmz;
  }

  /**
   * Converts an unzipped KML file packaged as a {@link BinaryContent} to a zipped KMZ file.
   *
   * @param kml - unzipped kml {@link BinaryContent}
   * @return BinaryContent - zipped KML file containing KML data.
   */
  public BinaryContent transformKmlToKmz(BinaryContent kml) {
    try (TemporaryFileBackedOutputStream temporaryFileBackedOutputStream =
            new TemporaryFileBackedOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(temporaryFileBackedOutputStream);
        InputStream inputStream = kml.getInputStream(); ) {
      final ZipEntry e = new ZipEntry(DOC_KML);
      zipOutputStream.putNextEntry(e);
      IOUtils.copy(inputStream, zipOutputStream);
      zipOutputStream.closeEntry();
      zipOutputStream.finish();
      final InputStream zipFile = temporaryFileBackedOutputStream.asByteSource().openStream();
      return new BinaryContentImpl(zipFile, KMZ_MIMETYPE);
    } catch (IOException e) {
      LOGGER.debug("Failed to create KMZ file from KML BinaryContent.", e);
    }
    return null;
  }
}
