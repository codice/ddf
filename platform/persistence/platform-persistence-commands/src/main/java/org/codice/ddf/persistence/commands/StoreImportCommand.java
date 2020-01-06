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
package org.codice.ddf.persistence.commands;

import static org.codice.ddf.persistence.PersistentItem.BINARY_SUFFIX;
import static org.codice.ddf.persistence.PersistentItem.DATE_SUFFIX;
import static org.codice.ddf.persistence.PersistentItem.INT_SUFFIX;
import static org.codice.ddf.persistence.PersistentItem.LONG_SUFFIX;
import static org.codice.ddf.persistence.PersistentItem.SUFFIXES;
import static org.codice.ddf.persistence.PersistentItem.TEXT_SUFFIX;
import static org.codice.ddf.persistence.PersistentItem.XML_SUFFIX;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.codice.ddf.persistence.PersistenceException;

@Service
@Command(
  scope = "store",
  name = "import",
  description = "Import entries into the persistent store."
)
public class StoreImportCommand extends AbstractStoreCommand {

  @Argument(
    name = "File path or Directory path",
    description =
        "Path to a file or a directory of file(s) to be ingested. Paths can be absolute or relative to installation directory.",
    index = 0,
    multiValued = false,
    required = true
  )
  @Completion(FileCompleter.class)
  String filePath;

  private final Gson gson = new Gson();
  private final SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

  @Override
  public void storeCommand() throws PersistenceException {

    final File inputFile = getInputFile();
    if (inputFile == null) {
      return;
    }
    int totalFiles = 0;

    try {
      totalFiles = totalFileCount(inputFile);
    } catch (IOException e) {
      console.println("Unable to read directory");
    }

    List<Map<String, Object>> importResults = new ArrayList<>();

    console.println("Found " + totalFiles + " files to import\n");

    try (Stream<Path> ingestStream = Files.walk(inputFile.toPath(), FileVisitOption.FOLLOW_LINKS)) {
      ingestStream
          .map(Path::toFile)
          .filter(file -> !file.isDirectory())
          .map(this::processFile)
          .filter(Objects::nonNull)
          .forEach(importResults::add);
    } catch (IOException e) {
      console.println("Unable to import files.");
      throw new UncheckedIOException(e);
    }
    persistentStore.add(type, importResults);

    console.println("Imported " + importResults.size() + " records \n");
  }

  private Map<String, Object> processFile(File file) {

    Map<String, String> jsonResult;
    try {
      jsonResult = gson.fromJson(new FileReader(file), Map.class);
    } catch (FileNotFoundException e) {
      console.println("File not found for import " + file.getName() + "\n");
      return null;
    } catch (JsonSyntaxException | JsonIOException e) {
      console.println("Unable to parse json file. Skipping " + file.getName());
      return null;
    }

    Map<String, Object> result = new HashMap<>();

    for (String key : jsonResult.keySet()) {
      String attributeType = extractTypeSuffix(key);
      Object value = getValue(attributeType, jsonResult.get(key));
      if (value != null) {
        result.put(key, value);
      }
    }

    console.println("Processing: " + file.getName());
    return result;
  }

  private File getInputFile() {
    final File inputFile = new File(filePath);

    if (!inputFile.exists()) {
      console.println("If the file does indeed exist, try putting the path in quotes.");
      return null;
    }
    return inputFile;
  }

  private int totalFileCount(File inputFile) throws IOException {
    if (inputFile.isDirectory()) {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputFile.toPath())) {
        return (int)
            StreamSupport.stream(stream.spliterator(), false)
                .map(Path::toFile)
                .filter(file -> !file.isHidden())
                .count();
      }
    }
    return inputFile.isHidden() ? 0 : 1;
  }

  private String extractTypeSuffix(String key) {
    int index = StringUtils.lastIndexOfAny(key, SUFFIXES);
    if (index > 0) {
      return key.substring(index);
    } else {
      console.println("Warning Key found without type suffix, skipping attribute: " + key);
      return null;
    }
  }

  /**
   * Convert the string value to its given type
   *
   * @param attributeType attribute type
   * @param stringValue value of the attribute
   * @return the attribute value as its coverted object type
   */
  private Object getValue(String attributeType, String stringValue) {
    if (attributeType == null) {
      return null;
    }
    switch (attributeType.toLowerCase()) {
      case BINARY_SUFFIX:
        return Base64.getDecoder().decode(stringValue);
      case DATE_SUFFIX:
        try {
          return formatter.parse(stringValue);
        } catch (ParseException e) {
          console.println("Failed to parse date: " + stringValue);
          return null;
        }
      case LONG_SUFFIX:
        return Long.valueOf(stringValue);
      case INT_SUFFIX:
        return Integer.valueOf(stringValue);
      case TEXT_SUFFIX:
      case XML_SUFFIX:
        return stringValue;
      default:
        return null;
    }
  }
}
