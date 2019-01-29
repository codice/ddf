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
package org.codice.ddf.catalog.transformer.zip;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipCompression implements QueryResponseTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ZipCompression.class);

  private static final String METACARD_PATH = "metacards" + File.separator;

  private static final String TRANSFORMER_ID = "transformerId";

  private List<ServiceReference> metacardTransformers;

  private BundleContext bundleContext;

  private static MimeType mimeType;

  static {
    try {
      mimeType = new MimeType("application/zip");
    } catch (MimeTypeParseException e) {
      LOGGER.warn("Failed to apply mimetype application/zip");
    }
  }

  public ZipCompression(BundleContext bundleContext, List<ServiceReference> metacardTransformers) {
    this.bundleContext = bundleContext;
    this.metacardTransformers = metacardTransformers;
  }

  /**
   * Transforms a SourceResponse with a list of {@link Metacard}s into a {@link BinaryContent} item
   * with an {@link InputStream}. This transformation expects a key-value pair
   * "fileName"-zipFileName to be present.
   *
   * @param sourceResponse - a SourceResponse with a list of {@link Metacard}s to compress
   * @param arguments - a map of arguments to use for processing. This method expects "fileName" to
   *     be set
   * @return - a {@link BinaryContent} item with the {@link InputStream} for the Zip file
   * @throws CatalogTransformerException when the transformation fails
   */
  @Override
  public BinaryContent transform(SourceResponse sourceResponse, Map<String, Serializable> arguments)
      throws CatalogTransformerException {

    if (sourceResponse == null || CollectionUtils.isEmpty(sourceResponse.getResults())) {
      throw new CatalogTransformerException(
          "The source response does not contain any metacards to transform.");
    }

    if (arguments.get(TRANSFORMER_ID) == null) {
      throw new CatalogTransformerException("Transformer ID cannot be null");
    }

    String transformerId = arguments.getOrDefault(TRANSFORMER_ID, "").toString();

    if (StringUtils.isBlank(transformerId)) {
      throw new CatalogTransformerException("A valid transformer ID must be provided.");
    }

    InputStream inputStream = createZip(sourceResponse, transformerId);

    return new BinaryContentImpl(inputStream, mimeType);
  }

  private InputStream createZip(SourceResponse sourceResponse, String transformerId)
      throws CatalogTransformerException {
    ServiceReference<MetacardTransformer> serviceRef =
        getTransformerServiceReference(transformerId);
    MetacardTransformer transformer = bundleContext.getService(serviceRef);

    String extension = getFileExtensionFromService(serviceRef);

    if (StringUtils.isNotBlank(extension)) {
      extension = "." + extension;
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(baos)) {

      for (Result result : sourceResponse.getResults()) {
        Metacard metacard = result.getMetacard();

        BinaryContent binaryContent = transformer.transform(metacard, Collections.emptyMap());

        ZipEntry entry = new ZipEntry(METACARD_PATH + metacard.getId() + extension);

        zipOutputStream.putNextEntry(entry);
        zipOutputStream.write(binaryContent.getByteArray());
        zipOutputStream.closeEntry();
      }

      return new ByteArrayInputStream(baos.toByteArray());

    } catch (IOException e) {
      throw new CatalogTransformerException(
          "An error occurred while initializing or closing output stream", e);
    }
  }

  private String getFileExtensionFromService(ServiceReference<MetacardTransformer> serviceRef) {
    Object mimeTypeProperty = serviceRef.getProperty("mime-type");

    if (mimeTypeProperty == null) {
      return "";
    }

    String mimeTypeStr = mimeTypeProperty.toString();

    try {
      MimeType exportMimeType = new MimeType(mimeTypeStr);
      return exportMimeType.getSubType();
    } catch (MimeTypeParseException e) {
      LOGGER.debug("Failed to parse mime type {}", mimeTypeStr, e);
      return "";
    }
  }

  private ServiceReference getTransformerServiceReference(String transformerId)
      throws CatalogTransformerException {
    return metacardTransformers
        .stream()
        .filter(serviceRef -> transformerId.equals(serviceRef.getProperty("id")))
        .findFirst()
        .orElseThrow(
            () ->
                new CatalogTransformerException(
                    "The metacard transformer with ID " + transformerId + " could not be found."));
  }
}
