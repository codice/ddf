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

import static org.codice.ddf.catalog.ui.forms.data.AttributeGroupType.ATTRIBUTE_GROUP_TAG;
import static org.codice.ddf.catalog.ui.forms.data.QueryTemplateType.QUERY_TEMPLATE_TAG;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.security.Subject;
import ddf.security.service.SecurityServiceException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.boon.Boon;
import org.codice.ddf.catalog.ui.forms.data.AttributeGroupMetacard;
import org.codice.ddf.catalog.ui.forms.data.QueryTemplateMetacard;
import org.codice.ddf.catalog.ui.forms.model.TemplateTransformer;
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

  public static void bootstrap(
      CatalogFramework framework, EndpointUtil util, List<Metacard> systemTemplates) {

    // Note: Results from querying as admin are never returned to the client or cached
    // This means there is no risk of data leak
    Set<String> queryTitles =
        queryAsAdmin(util, QUERY_TEMPLATE_TAG, SearchFormsLoader::titlesTransform);
    Set<String> resultTitles =
        queryAsAdmin(util, ATTRIBUTE_GROUP_TAG, SearchFormsLoader::titlesTransform);

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
      saveMetacards(framework, dedupedTemplateMetacards);
    }
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
    metacard.setFormsFilter(filterXml);

    //TODO: Remove this once CRUD support gets added
    metacard.setAttribute(Core.METACARD_OWNER, "dummyUser@gmail.com");

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

  /**
   * Results from this method call should not be directly returned to clients or cached in some
   * intermediate block of memory.
   */
  private static Set<String> queryAsAdmin(
      EndpointUtil util, String tag, Function<Map<String, Result>, Set<String>> transform) {
    return SECURITY.runAsAdmin(
        () -> {
          try {
            return SECURITY.runWithSubjectOrElevate(
                () -> transform.apply(util.getMetacardsByFilter(tag)));
          } catch (SecurityServiceException | InvocationTargetException e) {
            LOGGER.warn(
                "Can't query the catalog while trying to initialize system search templates, was "
                    + "unable to elevate privileges",
                e);
          }
          return Collections.emptySet();
        });
  }

  private static void saveMetacards(CatalogFramework framework, List<Metacard> metacards) {
    //TODO: This code will be removed once CRUD support is completed
    /**
     * Purpose: Circumvents the idea of a system template to make it look like the loaded templates were created by a std. user.
     * Additional Info: dummyUser is created (temporarily) in user.properties/user.attributes to demonstrate template creation
     *   - This sets the metacard owner during boot time
     *   - This allows the template to be shared when logged in as dummyUser
     *   - Allows this PR to be hero'ed without the need for CRUD functionality
     */
    Subject userSubject = SECURITY.getSubject("dummyUser", "dummyPassword");
    userSubject.execute(
        () -> {
          try {
            framework.create(new CreateRequestImpl(metacards)).getCreatedMetacards();
          } catch (IngestException e) {
            LOGGER.warn("Unable to create metacard under this user", e);
          } catch (SourceUnavailableException e) {
            LOGGER.warn("Unable to create metacard under this user", e);
          }
        });

    //TODO: Add this code back in once CRUD support is completed
    //    SECURITY.runAsAdmin(
    //        () -> {
    //          try {
    //            return SECURITY.runWithSubjectOrElevate(
    //                () -> framework.create(new
    // CreateRequestImpl(metacards)).getCreatedMetacards());
    //          } catch (SecurityServiceException | InvocationTargetException e) {
    //            LOGGER.warn(
    //                "Can't create metacard for system search template, was unable to elevate
    // privileges",
    //                e);
    //          }
    //          return null;
    //        });
  }
}
