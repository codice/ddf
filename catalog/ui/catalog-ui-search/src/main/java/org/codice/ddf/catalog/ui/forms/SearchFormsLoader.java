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

import static org.codice.ddf.catalog.ui.security.Constants.SYSTEM_TEMPLATE;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.ui.forms.data.AttributeGroupMetacard;
import org.codice.ddf.catalog.ui.forms.data.QueryTemplateMetacard;
import org.codice.ddf.catalog.ui.query.utility.EndpointUtility;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.codice.ddf.security.common.Security;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads initial system template configuration from the file system so template defaults in
 * distributions can vary independently.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class SearchFormsLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchFormsLoader.class);

  private static final Security SECURITY = Security.getInstance();

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .create();

  private static final File DEFAULT_FORMS_DIRECTORY =
      new File(new AbsolutePathResolver("etc/forms").getPath());

  private static final String DEFAULT_FORMS_FILE = "forms.json";

  private static final String DEFAULT_RESULTS_FILE = "results.json";

  private final File formsDirectory;

  private final String formsFileName;

  private final String resultsFileName;

  private final CatalogFramework catalogFramework;

  private final TemplateTransformer transformer;

  private final EndpointUtility endpointUtil;

  public SearchFormsLoader(
      CatalogFramework catalogFramework,
      TemplateTransformer transformer,
      EndpointUtil endpointUtil) {
    this(catalogFramework, transformer, endpointUtil, null, null, null);
  }

  public SearchFormsLoader(
      CatalogFramework catalogFramework,
      TemplateTransformer transformer,
      EndpointUtility endpointUtil,
      @Nullable String formsDirectory,
      @Nullable String formsFileName,
      @Nullable String resultsFileName) {
    this.catalogFramework = catalogFramework;
    this.transformer = transformer;
    this.endpointUtil = endpointUtil;
    this.formsFileName = (formsFileName == null) ? DEFAULT_FORMS_FILE : formsFileName;
    this.resultsFileName = (resultsFileName == null) ? DEFAULT_RESULTS_FILE : resultsFileName;
    this.formsDirectory =
        (formsDirectory == null)
            ? DEFAULT_FORMS_DIRECTORY
            : new File(new AbsolutePathResolver(formsDirectory).getPath());
    LOGGER.debug(
        "Initializing forms loader with directory [{}], forms file name [{}], and results file name [{}]",
        formsDirectory,
        formsFileName,
        resultsFileName);
  }

  /**
   * Setup the catalog with system templates.
   *
   * @param systemTemplates system templates loaded from config.
   */
  public void bootstrap(List<Metacard> systemTemplates) {
    Subject systemSubject = SECURITY.runAsAdmin(SECURITY::getSystemSubject);
    systemSubject.execute(() -> this.createSystemMetacards(systemTemplates));
  }

  public List<Metacard> retrieveSystemTemplateMetacards() {
    if (!formsDirectory.exists()) {
      LOGGER.warn("Could not locate forms directory [{}]", formsDirectory.getAbsolutePath());
      return Collections.emptyList();
    }

    File formsFile = formsDirectory.toPath().resolve(formsFileName).toFile();
    File resultsFile = formsDirectory.toPath().resolve(resultsFileName).toFile();

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

    Object configObject = GSON.fromJson(payload, Object.class);
    if (!(configObject instanceof List)) {
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
    String title = SearchFormsUtil.safeGet(map, Core.TITLE, String.class);
    String description = SearchFormsUtil.safeGet(map, Core.DESCRIPTION, String.class);
    String filterTemplateFile = SearchFormsUtil.safeGet(map, "filterTemplateFile", String.class);
    Map<String, Object> querySettings =
        SearchFormsUtil.safeGetMap(map, "querySettings", Object.class);

    if (SearchFormsUtil.anyNull(title, description, filterTemplateFile)) {
      return null;
    }

    File xmlFile = formsDirectory.toPath().resolve(filterTemplateFile).toFile();
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
    if (transformer.invalidFormTemplate(metacard)) {
      LOGGER.warn("System forms configuration for template '{}' had one or more problems", title);
      return null;
    }

    return metacard;
  }

  /** Parse the JSON map for initializing system result templates. */
  @Nullable
  private Metacard resultsMapper(Map map) {
    String title = SearchFormsUtil.safeGet(map, Core.TITLE, String.class);
    String description = SearchFormsUtil.safeGet(map, Core.DESCRIPTION, String.class);
    List<String> descriptors = SearchFormsUtil.safeGetList(map, "descriptors", String.class);

    if (SearchFormsUtil.anyNull(title, description, descriptors)) {
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
  private String getFileContent(File file) {
    try (InputStream is = new FileInputStream(file)) {
      return IOUtils.toString(is, "UTF-8");
    } catch (IOException e) {
      LOGGER.error("Problem reading from {}, {}", file.getName(), e.getMessage());
      LOGGER.debug("Problem reading from {}", file.getName(), e);
    }
    return null;
  }

  private Set<String> titlesTransform(Map<String, Result> resultMap) {
    return resultMap
        .values()
        .stream()
        .map(Result::getMetacard)
        .map(Metacard::getTitle)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private Consumer<? super Object> loggingConsumerFactory(File file) {
    return obj -> {
      if (!(obj instanceof Map)) {
        LOGGER.warn(
            "Unexpected configuration in {}, values should be maps not {}",
            file.getName(),
            obj.getClass().getName());
      }
    };
  }

  /**
   * Creates system-template metacards from the transformed forms directory data. Ensures that no
   * duplicate system forms can be stored.
   *
   * @param systemTemplates List of metacards loaded from the specified forms directory to store
   *     into the catalog
   */
  private void createSystemMetacards(List<Metacard> systemTemplates) {
    Set<String> existentSystemTemplates =
        titlesTransform(endpointUtil.getMetacardsByTag(SYSTEM_TEMPLATE));
    List<Metacard> dedupedTemplateMetacards =
        systemTemplates
            .stream()
            .filter(metacard -> !existentSystemTemplates.contains(metacard.getTitle()))
            .collect(Collectors.toList());

    if (!dedupedTemplateMetacards.isEmpty()) {
      try {
        catalogFramework.create(new CreateRequestImpl(dedupedTemplateMetacards));
      } catch (SourceUnavailableException | IngestException e) {
        // The wrapped exception stacktrace isn't shown when the forms are loaded from the
        // console so we also log it here
        LOGGER.debug("Could not create System Template metacards", e);
        throw new RuntimeException("Could not load System Templates", e);
      }
    }
  }
}
