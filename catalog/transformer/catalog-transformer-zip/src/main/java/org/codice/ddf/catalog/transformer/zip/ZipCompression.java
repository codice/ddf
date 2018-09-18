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
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipCompression implements QueryResponseTransformer {

  public static final String METACARD_PATH = "metacards" + File.separator;

  private static final Logger LOGGER = LoggerFactory.getLogger(ZipCompression.class);

  private CatalogFramework catalogFramework;

  private JarSigner jarSigner;

  public ZipCompression(JarSigner jarSigner) {
    this.jarSigner = jarSigner;
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

    checkArguments(upstreamResponse, arguments);

    String filePath = (String) arguments.get(ZipDecompression.FILE_PATH);
    createZip(upstreamResponse, filePath);

    return getBinaryContentFromZip(filePath);
  }

  private void checkArguments(SourceResponse upstreamResponse, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    if (upstreamResponse == null || CollectionUtils.isEmpty(upstreamResponse.getResults())) {
      throw new CatalogTransformerException("No Metacards were found to transform.");
    }

    if (MapUtils.isEmpty(arguments) || !arguments.containsKey(ZipDecompression.FILE_PATH)) {
      throw new CatalogTransformerException("No 'filePath' argument found in arguments map.");
    }
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

      writeDataToZip(zipOutputStream, inputStream);
      inputStream.close();
    } catch (IOException e) {
      LOGGER.debug("Failed to add metacard with id {}.", metacard.getId(), e);
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

      writeDataToZip(zipOutputStream, inputStream);
      inputStream.close();
    } catch (IOException e) {
      LOGGER.debug("Failed to add resource with id {} to zip.", resource.getName(), e);
    }
  }

  private void writeDataToZip(ZipOutputStream zipOutputStream, InputStream inputStream)
      throws IOException {
    final byte[] bytes = new byte[1024];
    int length;
    while ((length = inputStream.read(bytes)) >= 0) {
      zipOutputStream.write(bytes, 0, length);
    }
  }

  private BinaryContent getBinaryContentFromZip(String filePath)
      throws CatalogTransformerException {
    BinaryContent binaryContent;
    InputStream fileInputStream;
    File zipFile = new File(filePath);
    try {
      fileInputStream = new ZipInputStream(new FileInputStream(zipFile));
      binaryContent = new BinaryContentImpl(fileInputStream);
    } catch (FileNotFoundException e) {
      throw new CatalogTransformerException("Unable to get ZIP file from ZipInputStream.", e);
    }

    jarSigner.signJar(
        zipFile,
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty(SystemBaseUrl.EXTERNAL_HOST)),
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("javax.net.ssl.keyStorePassword")),
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("javax.net.ssl.keyStore")),
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("javax.net.ssl.keyStorePassword")));
    return binaryContent;
  }

  public void setCatalogFramework(CatalogFramework catalogFramework) {
    this.catalogFramework = catalogFramework;
  }

  public CatalogFramework getCatalogFramework() {
    return catalogFramework;
  }
}
