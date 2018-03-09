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
package org.codice.ddf.catalog.ui.forms;

import ddf.catalog.data.Metacard;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.boon.Boon;
import org.codice.ddf.catalog.ui.forms.data.QueryTemplateMetacardImpl;
import org.codice.ddf.catalog.ui.forms.data.ResultTemplateMetacardImpl;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads initial system template configuration from the file system so template defaults in
 * distributions can vary independently.
 */
public class SearchFormsLoader implements Supplier<List<Metacard>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchFormsLoader.class);

  private static final File DEFAULT_FORMS_DIRECTORY =
      new File(new AbsolutePathResolver("etc/forms").getPath());

  private static final String FORMS_FILE_NAME = "forms.json";

  private static final String RESULTS_FILE_NAME = "results.json";

  private final File configDirectory;

  public static Supplier<List<Metacard>> config() {
    return new SearchFormsLoader();
  }

  public SearchFormsLoader() {
    this(DEFAULT_FORMS_DIRECTORY);
  }

  public SearchFormsLoader(File configDirectory) {
    this.configDirectory = configDirectory;
  }

  @Override
  public List<Metacard> get() {
    if (!configDirectory.exists()) {
      LOGGER.debug("Could not locate forms directory");
      return Collections.emptyList();
    }

    // What's our policy here? Do we want the system to blowup / make noise if this isn't possible?
    // Especially due to the security manager?
    if (!configDirectory.canRead()) {
      LOGGER.debug("Forms directory exists but could not be read");
      return Collections.emptyList();
    }

    File formsFile = configDirectory.toPath().resolve(FORMS_FILE_NAME).toFile();
    File resultsFile = configDirectory.toPath().resolve(RESULTS_FILE_NAME).toFile();

    return Stream.concat(
            loadFile(formsFile, this::formMapper), loadFile(resultsFile, this::resultsMapper))
        .collect(Collectors.toList());
  }

  /** Read the provided JSON file and return a stream of metacards. */
  private Stream<Metacard> loadFile(File file, Function<? super Map, Metacard> mapper) {
    if (!file.exists()) {
      LOGGER.debug("Could not locate {}", file.getName());
      return Stream.empty();
    }

    String payload = getFileContent(file);
    if (payload == null) {
      LOGGER.debug("Problem reading {}", file.getName());
      return Stream.empty();
    }

    try {
      return Stream.of(Boon.fromJson(payload))
          .map(List.class::cast)
          .flatMap(List::stream)
          .filter(Map.class::isInstance)
          .map(Map.class::cast)
          .map(mapper)
          .filter(Objects::nonNull)
          // Invoke a terminal operation so the exception can be handled here
          .collect(Collectors.toList())
          .stream();
    } catch (ClassCastException e) {
      LOGGER.warn(
          "Could not load forms configuration in {}, JSON should be a list of maps",
          file.getName());
      return Stream.empty();
    }
  }

  /** Parse the JSON map for initializing system form templates. */
  @Nullable
  private Metacard formMapper(Map map) {
    LOGGER.debug("Starting form processing...");

    String title = safeGet(map, "title", String.class);
    String description = safeGet(map, "description", String.class);
    String filterTemplateFile = safeGet(map, "filterTemplateFile", String.class);

    if (anyNull(title, description, filterTemplateFile)) {
      LOGGER.debug("Invalid entry in forms.json");
      return null;
    }

    File xmlFile = configDirectory.toPath().resolve(filterTemplateFile).toFile();
    if (!xmlFile.exists()) {
      LOGGER.debug("Filter XML file does not exist: {}", filterTemplateFile);
      return null;
    }

    String filterXml = getFileContent(xmlFile);
    if (filterXml == null) {
      LOGGER.debug("Error while reading filter XML file: {}", filterTemplateFile);
      return null;
    }

    QueryTemplateMetacardImpl metacard = new QueryTemplateMetacardImpl(title, description);
    metacard.setFormsFilter(filterXml);
    return metacard;
  }

  /** Parse the JSON map for initializing system result templates. */
  @Nullable
  private Metacard resultsMapper(Map map) {
    LOGGER.debug("Starting result processing...");

    String title = safeGet(map, "title", String.class);
    String description = safeGet(map, "description", String.class);
    List<String> descriptors = safeGetList(map, "descriptors", String.class);

    if (anyNull(title, description, descriptors)) {
      LOGGER.debug("Invalid entry in results.json");
      return null;
    }

    ResultTemplateMetacardImpl metacard = new ResultTemplateMetacardImpl(title, description);
    metacard.setResultDescriptors(new HashSet<>(descriptors));
    return metacard;
  }

  @Nullable
  private static String getFileContent(File file) {
    try (InputStream is = new FileInputStream(file)) {
      return IOUtils.toString(is, "UTF-8");
    } catch (IOException e) {
      LOGGER.error("Problem reading from {}, {}", file.getName(), e.getMessage());
      LOGGER.debug("Problem reading from {}", file.getName(), e);
    }
    return null;
  }

  @SuppressWarnings({"unchecked", "squid:S1168" /* We want to return null */})
  @Nullable
  private static <T> List<T> safeGetList(Map map, String key, Class<T> type) {
    List unchecked = safeGet(map, key, List.class);
    if (unchecked == null) {
      return null;
    }
    try {
      return (List<T>) unchecked.stream().map(type::cast).collect(Collectors.toList());
    } catch (ClassCastException e) {
      LOGGER.warn(
          "Form configuration field {} was malformed, expected a List containing type {}",
          key,
          type.getName());
    }
    return null;
  }

  @Nullable
  private static <T> T safeGet(Map map, String key, Class<T> type) {
    Object value = map.get(key);
    if (value == null) {
      LOGGER.debug("Unexpected null entry: {}", key);
      return null;
    }
    try {
      return type.cast(value);
    } catch (ClassCastException e) {
      LOGGER.warn(
          "Form configuration field {} was malformed, expected a {} but got {}",
          key,
          type.getName(),
          value.getClass().getName());
    }
    return null;
  }

  private static boolean anyNull(Object... args) {
    return args == null || Arrays.stream(args).anyMatch(Objects::isNull);
  }
}
