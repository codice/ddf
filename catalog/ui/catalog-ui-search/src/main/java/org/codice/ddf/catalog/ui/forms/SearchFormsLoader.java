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

import static java.util.AbstractMap.SimpleEntry;
import static org.codice.ddf.catalog.ui.forms.data.AttributeGroupType.ATTRIBUTE_GROUP_TAG;
import static org.codice.ddf.catalog.ui.forms.data.QueryTemplateType.QUERY_TEMPLATE_TAG;
import static org.codice.ddf.catalog.ui.security.Constants.SYSTEM_TEMPLATE;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.security.Subject;
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
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.io.IOUtils;
import org.boon.Boon;
import org.codice.ddf.catalog.ui.forms.data.AttributeGroupMetacard;
import org.codice.ddf.catalog.ui.forms.data.QueryTemplateMetacard;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.codice.ddf.security.common.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads initial system template configuration from the file system so template defaults in
 * distributions can vary independently.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class SearchFormsLoader implements Supplier<List<Metacard>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchFormsLoader.class);

  private static final Security SECURITY = Security.getInstance();

  private static final File DEFAULT_FORMS_DIRECTORY =
      new File(new AbsolutePathResolver("etc/forms").getPath());

  private static final String EXCEPTION_OCCURRED_PARSING = "Exception occurred parsing config file";

  private static final String FORMS_FILE_NAME = "forms.json";

  private static final String RESULTS_FILE_NAME = "results.json";

  private final File configDirectory;

  public SearchFormsLoader() {
    this(DEFAULT_FORMS_DIRECTORY);
  }

  public SearchFormsLoader(File configDirectory) {
    this.configDirectory = configDirectory;
  }

  public static Supplier<List<Metacard>> config() {
    return new SearchFormsLoader();
  }

  public static boolean enabled() {
    return new SearchFormsLoader().configDirectory.exists();
  }

  /**
   * Setup the catalog with system templates.
   *
   * <p>Caution should be exercised when executing code as system. Results from querying as system
   * are never returned to the client or cached. This means there is no risk of data leak.
   *
   * @param catalogFramework the catalog framework, for creating system templates.
   * @param endpointUtil for querying the catalog.
   * @param systemTemplates system templates loaded from config.
   */
  public static void bootstrap(
      CatalogFramework catalogFramework,
      EndpointUtil endpointUtil,
      List<Metacard> systemTemplates) {
    Subject systemSubject = SECURITY.runAsAdmin(SECURITY::getSystemSubject);
    if (systemSubject == null) {
      throw new SecurityException("Could not get systemSubject to initialize system templates");
    }

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    Failsafe.with(
            new RetryPolicy()
                .retryOn(Collections.singletonList(RuntimeException.class))
                .withMaxRetries(5)
                .withBackoff(2, 60, TimeUnit.SECONDS))
        .with(executor)
        .onComplete(
            (result, error) -> {
              if (result != null) {
                LOGGER.info("Successfully loaded System Search Forms {}", result);
              } else if (error != null) {

                LOGGER.error("Failed to load System Search Forms", error);
              }
              executor.shutdown();
            })
        .run(
            () ->
                systemSubject.execute(
                    () ->
                        SearchFormsLoader.createSystemMetacards(
                            endpointUtil, systemTemplates, catalogFramework)));
  }

  @Override
  public List<Metacard> get() {
    if (!configDirectory.exists()) {
      LOGGER.warn("Could not locate forms directory [{}]", configDirectory.getAbsolutePath());
      return Collections.emptyList();
    }

    File formsFile = configDirectory.toPath().resolve(FORMS_FILE_NAME).toFile();
    File resultsFile = configDirectory.toPath().resolve(RESULTS_FILE_NAME).toFile();

    return Stream.concat(
            loadFile(formsFile, this::formMapper), loadFile(resultsFile, this::resultsMapper))
        .collect(Collectors.toList());
  }

  /**
   * Read the provided JSON file and return a stream of metacards.
   *
   * @param file the JSON file to read.
   * @param mapper a transform function for converting raw JSON config into either a query or result
   *     template metacard, as appropriate
   * @return a stream of the converted metacards.
   */
  @SuppressWarnings("unchecked" /* Actually is checked, see early return if not a List */)
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

    Object configObject = Boon.fromJson(payload);
    if (!List.class.isInstance(configObject)) {
      LOGGER.warn(
          "Could not load forms configuration in {}, JSON should be a list of maps",
          file.getName());
      return Stream.empty();
    }

    List<Object> configs = (List) configObject;
    return configs
        .stream()
        .peek(obj -> loggingConsumerFactory(file).accept(obj))
        .filter(Map.class::isInstance)
        .map(Map.class::cast)
        .map(mapper)
        .filter(Objects::nonNull);
  }

  /** Parse the JSON map for initializing system form templates. */
  @Nullable
  private Metacard formMapper(Map map) {
    String title = safeGet(map, Core.TITLE, String.class);
    String description = safeGet(map, Core.DESCRIPTION, String.class);
    String filterTemplateFile = safeGet(map, "filterTemplateFile", String.class);
    Map<String, Object> querySettings = safeGetMap(map, "querySettings", Object.class);

    if (anyNull(title, description, filterTemplateFile)) {
      return null;
    }

    File xmlFile = configDirectory.toPath().resolve(filterTemplateFile).toFile();
    if (!xmlFile.exists()) {
      LOGGER.warn("Filter XML file does not exist: {}", filterTemplateFile);
      return null;
    }

    String filterXml = getFileContent(xmlFile);
    if (filterXml == null) {
      return null;
    }

    QueryTemplateMetacard metacard = new QueryTemplateMetacard(title, description);
    Set<String> newTags = new HashSet<>(metacard.getTags());
    newTags.add(SYSTEM_TEMPLATE);
    metacard.setTags(newTags);
    metacard.setFormsFilter(filterXml);
    if (querySettings != null) {
      metacard.setQuerySettings(querySettings);
    }

    // Validation so the catalog is not contaminated on startup, which would impact every request
    if (TemplateTransformer.invalidFormTemplate(metacard)) {
      LOGGER.warn("System forms configuration for template '{}' had one or more problems", title);
      return null;
    }

    return metacard;
  }

  /** Parse the JSON map for initializing system result templates. */
  @Nullable
  private Metacard resultsMapper(Map map) {
    String title = safeGet(map, Core.TITLE, String.class);
    String description = safeGet(map, Core.DESCRIPTION, String.class);
    List<String> descriptors = safeGetList(map, "descriptors", String.class);

    if (anyNull(title, description, descriptors)) {
      return null;
    }

    AttributeGroupMetacard metacard = new AttributeGroupMetacard(title, description);
    Set<String> newTags = new HashSet<>(metacard.getTags());
    newTags.add(SYSTEM_TEMPLATE);
    metacard.setTags(newTags);
    metacard.setGroupDescriptors(new HashSet<>(descriptors));
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

  private static <T> Map<String, T> safeGetMap(Map map, String key, Class<T> valueType) {
    Map<?, ?> unchecked = safeGet(map, key, Map.class);
    if (unchecked == null) {
      return null;
    }
    try {
      return unchecked
          .entrySet()
          .stream()
          .map(e -> new SimpleEntry<>(String.class.cast(e.getKey()), e.getValue()))
          .map(e -> new SimpleEntry<>(e.getKey(), valueType.cast(e.getValue())))
          .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    } catch (ClassCastException e) {
      LOGGER.debug(EXCEPTION_OCCURRED_PARSING, e);
      LOGGER.warn(
          "Form configuration field {} was malformed, expected a querySettings Map containing type {}",
          key,
          valueType.getName());
    }
    return null;
  }

  @Nullable
  private static <T> List<T> safeGetList(Map map, String key, Class<T> type) {
    List<?> unchecked = safeGet(map, key, List.class);
    if (unchecked == null) {
      return null;
    }
    try {
      return (List<T>) unchecked.stream().map(type::cast).collect(Collectors.toList());
    } catch (ClassCastException e) {
      LOGGER.debug(EXCEPTION_OCCURRED_PARSING, e);
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
      LOGGER.debug(EXCEPTION_OCCURRED_PARSING, e);
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

  private static Set<String> titlesTransform(Map<String, Result> resultMap) {
    return resultMap
        .values()
        .stream()
        .map(Result::getMetacard)
        .map(Metacard::getTitle)
        .collect(Collectors.toSet());
  }

  private static Consumer<? super Object> loggingConsumerFactory(File file) {
    return obj -> {
      if (!Map.class.isInstance(obj)) {
        LOGGER.warn(
            "Unexpected configuration in {}, values should be maps not {}",
            file.getName(),
            obj.getClass().getName());
      }
    };
  }

  private static void createSystemMetacards(
      EndpointUtil util, List<Metacard> systemTemplates, CatalogFramework catalogFramework) {
    Set<String> queryTitles = titlesTransform(util.getMetacardsByTag(QUERY_TEMPLATE_TAG));
    Set<String> resultTitles = titlesTransform(util.getMetacardsByTag(ATTRIBUTE_GROUP_TAG));
    List<Metacard> dedupedTemplateMetacards =
        Stream.concat(
                systemTemplates
                    .stream()
                    .filter(QueryTemplateMetacard::isQueryTemplateMetacard)
                    .filter(metacard -> !queryTitles.contains(metacard.getTitle())),
                systemTemplates
                    .stream()
                    .filter(AttributeGroupMetacard::isAttributeGroupMetacard)
                    .filter(metacard -> !resultTitles.contains(metacard.getTitle())))
            .collect(Collectors.toList());

    if (!dedupedTemplateMetacards.isEmpty()) {
      try {
        catalogFramework.create(new CreateRequestImpl(dedupedTemplateMetacards));
      } catch (SourceUnavailableException | IngestException e) {
        throw new RuntimeException("Could not load System Templates", e);
      }
    }
  }
}
