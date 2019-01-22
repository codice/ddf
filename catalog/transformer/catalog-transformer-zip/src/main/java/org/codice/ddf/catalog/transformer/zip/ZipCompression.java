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

import ddf.catalog.CatalogFramework;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipCompression implements QueryResponseTransformer {

  public static final String METACARD_PATH = "metacards" + File.separator;

  private static final Logger LOGGER = LoggerFactory.getLogger(ZipCompression.class);

  private static final String TRANSFORMER_ID = "transformerId";

  private CatalogFramework catalogFramework;

  private JarSigner jarSigner;

  private List<ServiceReference> metacardTransformers;

  private BundleContext bundleContext;

  private static MimeType mimeType;

  private static final File DEFAULT_PATH =
      Paths.get(System.getProperty("ddf.home"), "data", "tmp", "export.zip").toFile();

  static {
    try {
      mimeType = new MimeType("application/zip");
    } catch (MimeTypeParseException e) {
      LOGGER.warn("Failed to apply mimetype application/zip");
    }
  }

  public ZipCompression(
      JarSigner jarSigner,
      List<ServiceReference> metacardTransformers,
      BundleContext bundleContext) {
    this.jarSigner = jarSigner;
    this.metacardTransformers = metacardTransformers;
    this.bundleContext = bundleContext;
  }

  /**
   * Transforms a SourceResponse with a list of {@link Metacard}s into a {@link BinaryContent} item
   * with an {@link InputStream}. This transformation expects a key-value pair
   * "fileName"-zipFileName to be present.
   *
   * @param upstreamResponse - a SourceResponse with a list of {@link Metacard}s to compress
   * @param arguments - a map of arguments to use for processing. This method expects "fileName" to
   *     be set
   * @return - a {@link BinaryContent} item with the {@link InputStream} for the Zip file
   * @throws CatalogTransformerException when the transformation fails
   */
  @Override
  public BinaryContent transform(
      SourceResponse upstreamResponse, Map<String, Serializable> arguments)
      throws CatalogTransformerException {

    if (upstreamResponse == null || CollectionUtils.isEmpty(upstreamResponse.getResults())) {
      throw new CatalogTransformerException(
          "The source response does not contain any metacards to transform.");
    }

    String filePath =
        (String) arguments.getOrDefault(ZipDecompression.FILE_PATH, DEFAULT_PATH.toString());
    String transformerId = (String) arguments.getOrDefault(TRANSFORMER_ID, "");

    if (StringUtils.isNotBlank(transformerId)) {
      createZip(upstreamResponse, filePath, transformerId);
    } else {
      createZip(upstreamResponse, filePath);
    }

    return getBinaryContentFromZip(filePath);
  }

  private void createZip(SourceResponse sourceResponse, String filePath, String transformerId)
      throws CatalogTransformerException {
    ServiceReference<MetacardTransformer> serviceRef =
        getTransformerServiceReference(transformerId);
    MetacardTransformer transformer = bundleContext.getService(serviceRef);

    String extension = getFileExtensionFromService(serviceRef);

    if (StringUtils.isNotBlank(extension)) {
      extension = "." + extension;
    }

    try (FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {

      for (Result result : sourceResponse.getResults()) {
        Metacard metacard = result.getMetacard();

        BinaryContent binaryContent = transformer.transform(metacard, Collections.emptyMap());
        writeBinaryContentToZip(zipOutputStream, binaryContent, metacard.getId(), extension);
      }

    } catch (IOException e) {
      throw new CatalogTransformerException(
          String.format(
              "Error occurred when initializing or closing ZipOutputStream with path %s.",
              filePath),
          e);
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
        .filter(serviceRef -> serviceRef.getProperty("id") != null)
        .filter(serviceRef -> serviceRef.getProperty("id").toString().equals(transformerId))
        .findFirst()
        .orElseThrow(
            () ->
                new CatalogTransformerException(
                    "The metacard transformer with ID " + transformerId + " could not be found."));
  }

  private void createZip(SourceResponse upstreamResponse, String filePath)
      throws CatalogTransformerException {
    try (FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {

      List<Result> resultList = upstreamResponse.getResults();
      Map<String, Resource> resourceMap = new HashMap<>();

      // write the metacards to the zip
      resultList
          .stream()
          .map(Result::getMetacard)
          .forEach(
              metacard -> {
                writeMetacardToZip(zipOutputStream, metacard);

                if (hasLocalResources(metacard)) {
                  resourceMap.putAll(getAllMetacardContent(metacard));
                }
              });

      // write the resources to the zip
      resourceMap.forEach(
          (filename, resource) -> writeResourceToZip(zipOutputStream, filename, resource));

    } catch (IOException e) {
      throw new CatalogTransformerException(
          String.format(
              "Error occurred when initializing/closing ZipOutputStream with path %s.", filePath),
          e);
    }
  }

  private void writeMetacardToZip(ZipOutputStream zipOutputStream, Metacard metacard) {

    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {

      ZipEntry zipEntry = new ZipEntry(METACARD_PATH + metacard.getId());
      zipOutputStream.putNextEntry(zipEntry);

      objectOutputStream.writeObject(new MetacardImpl(metacard));
      InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

      IOUtils.copy(inputStream, zipOutputStream);
      inputStream.close();
    } catch (IOException e) {
      LOGGER.debug("Failed to add metacard with id {}.", metacard.getId(), e);
    }
  }

  private void writeBinaryContentToZip(
      ZipOutputStream zipOutputStream,
      BinaryContent binaryContent,
      String metacardId,
      String extension) {
    try {
      ZipEntry zipEntry = new ZipEntry(METACARD_PATH + metacardId + extension);
      zipOutputStream.putNextEntry(zipEntry);

      InputStream inputStream = binaryContent.getInputStream();

      IOUtils.copy(inputStream, zipOutputStream);
      inputStream.close();
    } catch (IOException e) {
      LOGGER.debug("Failed to add metacard with id {}.", metacardId, e);
    }
  }

  private boolean hasLocalResources(Metacard metacard) {
    URI uri = metacard.getResourceURI();
    return (uri != null && ContentItem.CONTENT_SCHEME.equals(uri.getScheme()));
  }

  private Map<String, Resource> getAllMetacardContent(Metacard metacard) {
    Map<String, Resource> resourceMap = new HashMap<>();
    Attribute attribute = metacard.getAttribute(Metacard.DERIVED_RESOURCE_URI);

    if (attribute != null) {
      List<Serializable> serializables = attribute.getValues();
      serializables.forEach(
          serializable -> {
            String fragment = ZipDecompression.CONTENT + File.separator;
            URI uri = null;
            try {
              uri = new URI((String) serializable);
              String derivedResourceFragment = uri.getFragment();
              if (ContentItem.CONTENT_SCHEME.equals(uri.getScheme())
                  && StringUtils.isNotBlank(derivedResourceFragment)) {
                fragment += derivedResourceFragment + File.separator;
                Resource resource = getResource(metacard);
                if (resource != null) {
                  resourceMap.put(
                      fragment + uri.getSchemeSpecificPart() + "-" + resource.getName(), resource);
                }
              }
            } catch (URISyntaxException e) {
              LOGGER.debug(
                  "Invalid Derived Resource URI Syntax for metacard : {}", metacard.getId(), e);
            }
          });
    }

    URI resourceUri = metacard.getResourceURI();

    Resource resource = getResource(metacard);

    if (resource != null) {
      resourceMap.put(
          ZipDecompression.CONTENT
              + File.separator
              + resourceUri.getSchemeSpecificPart()
              + "-"
              + resource.getName(),
          resource);
    }

    return resourceMap;
  }

  private Resource getResource(Metacard metacard) {
    Resource resource = null;

    try {
      ResourceRequest resourceRequest = new ResourceRequestById(metacard.getId());
      ResourceResponse resourceResponse = catalogFramework.getLocalResource(resourceRequest);
      resource = resourceResponse.getResource();
    } catch (IOException | ResourceNotFoundException | ResourceNotSupportedException e) {
      LOGGER.debug("Unable to retrieve content from metacard : {}", metacard.getId(), e);
    }
    return resource;
  }

  private void writeResourceToZip(
      ZipOutputStream zipOutputStream, String filename, Resource resource) {

    try {
      ZipEntry zipEntry = new ZipEntry(filename);
      zipOutputStream.putNextEntry(zipEntry);

      InputStream inputStream = resource.getInputStream();

      IOUtils.copy(inputStream, zipOutputStream);
      inputStream.close();
    } catch (IOException e) {
      LOGGER.debug("Failed to add resource with id {} to zip.", resource.getName(), e);
    }
  }

  private BinaryContent getBinaryContentFromZip(String filePath)
      throws CatalogTransformerException {
    byte[] bytes;
    File zipFile = new File(filePath);
    try {
      bytes = Files.readAllBytes(zipFile.toPath().toAbsolutePath());
    } catch (IOException e) {
      throw new CatalogTransformerException(
          "An error occurred while streaming the temporary zip file", e);
    }

    AccessController.doPrivileged(
        (PrivilegedAction<Void>)
            () -> {
              jarSigner.signJar(
                  zipFile,
                  AccessController.doPrivileged(
                      (PrivilegedAction<String>)
                          () -> System.getProperty(SystemBaseUrl.EXTERNAL_HOST)),
                  AccessController.doPrivileged(
                      (PrivilegedAction<String>)
                          () -> System.getProperty("javax.net.ssl.keyStorePassword")),
                  AccessController.doPrivileged(
                      (PrivilegedAction<String>)
                          () -> System.getProperty("javax.net.ssl.keyStore")),
                  AccessController.doPrivileged(
                      (PrivilegedAction<String>)
                          () -> System.getProperty("javax.net.ssl.keyStorePassword")));
              return null;
            });

    return new BinaryContentImpl(new ByteArrayInputStream(bytes), mimeType);
  }

  public void setCatalogFramework(CatalogFramework catalogFramework) {
    this.catalogFramework = catalogFramework;
  }

  public CatalogFramework getCatalogFramework() {
    return catalogFramework;
  }
}
