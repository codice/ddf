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

import static org.codice.gsonsupport.GsonTypeAdapters.MAP_STRING_TO_OBJECT_TYPE;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.gsonsupport.GsonTypeAdapters.PersistenceMapTypeAdapter;

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

  int batchSize = 1000;

  private final Gson gson =
      new GsonBuilder().registerTypeAdapterFactory(PersistenceMapTypeAdapter.FACTORY).create();

  @Override
  public void storeCommand() throws PersistenceException {

    final File inputFile = getInputFile();
    if (inputFile == null) {
      return;
    }
    int totalFiles = 0;
    long totalImport = 0;
    try {
      totalFiles = totalFileCount(inputFile);
    } catch (IOException e) {
      console.println("Unable to read directory");
    }

    List<Map<String, Object>> importResults = new ArrayList<>();

    console.println("Found " + totalFiles + " files to import\n");

    try (Stream<Path> ingestStream = Files.walk(inputFile.toPath(), FileVisitOption.FOLLOW_LINKS)) {
      List<Path> regularFiles =
          ingestStream.filter(Files::isRegularFile).collect(Collectors.toList());

      for (Collection<Path> batch : Lists.partition(regularFiles, batchSize)) {
        batch
            .stream()
            .map(Path::toFile)
            .map(this::processFile)
            .filter(Objects::nonNull)
            .forEach(importResults::add);
        persistentStore.add(type, importResults);
        totalImport += importResults.size();
      }

    } catch (IOException e) {
      console.println("Unable to import files.");
      throw new UncheckedIOException(e);
    }

    console.println("Imported " + totalImport + " records \n");
  }

  private Map<String, Object> processFile(File file) {

    Map<String, Object> jsonResult;
    try {
      Reader reader = new FileReader(file);
      jsonResult = gson.fromJson(reader, MAP_STRING_TO_OBJECT_TYPE);

    } catch (FileNotFoundException e) {
      console.println("File not found for import " + file.getName() + "\n");
      return null;
    } catch (JsonSyntaxException | JsonIOException e) {
      console.println("Unable to parse json file. Skipping " + file.getName());
      return null;
    } catch (IOException e) {
      return null;
    }
    console.println("Processing: " + file.getName());
    return jsonResult;
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
      try (Stream<Path> stream = Files.walk(inputFile.toPath(), FileVisitOption.FOLLOW_LINKS)) {
        return (int) stream.filter(Files::isRegularFile).count();
      }
    }
    return inputFile.isHidden() ? 0 : 1;
  }
}
