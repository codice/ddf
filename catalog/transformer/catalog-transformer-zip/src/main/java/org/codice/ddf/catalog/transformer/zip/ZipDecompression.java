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

import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputCollectionTransformer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipDecompression implements InputCollectionTransformer {

  public static final String FILE_PATH = "filePath";

  public static final String FILE_NAME = "fileName";

  public static final String CONTENT = "content";

  public static final int BUFFER_SIZE = 4096;

  private static final Logger LOGGER = LoggerFactory.getLogger(ZipCompression.class);

  /**
   * Transforms a Zip InputStream into a List of {@link Metacard}s. This method expects there to be
   * a filePath and fileName key-value pair passed in the arguments map.
   *
   * @param inputStream - the InputStream to transform
   * @param arguments - the arguments for the transformation ("filePath" and "fileName").
   * @return the List of {@link Metacard}s produced from the transformation.
   * @throws CatalogTransformerException when the transformation fails.
   */
  @Override
  public List<Metacard> transform(InputStream inputStream, Map<String, Serializable> arguments)
      throws CatalogTransformerException {

    if (inputStream == null) {
      throw new CatalogTransformerException(
          "Unable to transform InputStream : InputStream was null.");
    }

    if (MapUtils.isEmpty(arguments)
        || !arguments.containsKey(FILE_PATH)
        || !arguments.containsKey(FILE_NAME)) {
      throw new CatalogTransformerException(
          "Unable to transform InputStream : Invalid arguments passed.");
    }

    String zipFileName = (String) arguments.get(FILE_PATH);

    Map<String, Metacard> metacards = decompressFile(inputStream, zipFileName);
    return metacards.values().stream().filter(Objects::nonNull).collect(Collectors.toList());
  }

  private Map<String, Metacard> decompressFile(InputStream inputStream, String zipFileName)
      throws CatalogTransformerException {
    try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
      Map<String, Metacard> metacardMap = new HashMap<>();
      ZipEntry zipEntry = zipInputStream.getNextEntry();

      while (zipEntry != null) {
        String filename = zipEntry.getName();

        if (!filename.contains("META-INF")) {

          File zipEntryFile = new File(zipFileName + filename);

          if (!new File(zipEntryFile.getParent()).mkdirs()) {
            LOGGER.debug("File directory already exists in {}", zipFileName);
          }

          if (!zipEntryFile.isDirectory()) {
            FileOutputStream fileOutputStream = new FileOutputStream(zipEntryFile);

            IOUtils.copy(zipInputStream, fileOutputStream);
            IOUtils.closeQuietly(fileOutputStream);

            if (!zipEntryFile.getPath().contains(CONTENT)) {
              metacardMap.put(zipEntryFile.getName(), readMetacard(zipEntryFile));
            }
          }
        }
        zipEntry = zipInputStream.getNextEntry();
      }
      return metacardMap;
    } catch (IOException e) {
      throw new CatalogTransformerException(
          String.format("Unable to transform InputStream for %s.", zipFileName));
    }
  }

  private Metacard readMetacard(File file) {
    Metacard result = null;
    try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file))) {
      result = (Metacard) objectInputStream.readObject();
    } catch (IOException | IllegalArgumentException | ClassNotFoundException e) {
      LOGGER.debug("Unable to create metacard from file {}", file.getName(), e);
    }
    return result;
  }
}
