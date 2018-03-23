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

import static org.codice.ddf.catalog.ui.forms.SearchFormsLoader.config;
import static org.codice.ddf.catalog.ui.forms.data.AttributeGroupType.ATTRIBUTE_GROUP_TAG;
import static org.codice.ddf.catalog.ui.forms.data.QueryTemplateType.QUERY_TEMPLATE_TAG;
import static spark.Spark.get;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.security.service.SecurityServiceException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.boon.json.JsonFactory;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.boon.json.ObjectMapper;
import org.codice.ddf.catalog.ui.forms.data.AttributeGroupMetacard;
import org.codice.ddf.catalog.ui.forms.data.QueryTemplateMetacard;
import org.codice.ddf.catalog.ui.forms.model.FilterNodeValueSerializer;
import org.codice.ddf.catalog.ui.forms.model.TemplateTransformer;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.ddf.security.common.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.servlet.SparkApplication;

/**
 * Provides an internal REST interface for working with custom form data for Intrigue.
 *
 * <p><i>This code is experimental. While it is functional and tested, it may change or be removed
 * in a future version of the library.</i>
 */
public class SearchFormsApplication implements SparkApplication {
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchFormsApplication.class);

  private static final Security SECURITY = Security.getInstance();

  private static final ObjectMapper MAPPER =
      JsonFactory.create(
          new JsonParserFactory().usePropertyOnly(),
          new JsonSerializerFactory()
              .addPropertySerializer(new FilterNodeValueSerializer())
              .useAnnotations()
              .includeEmpty()
              .includeDefaultValues()
              .setJsonFormatForDates(false));

  private final CatalogFramework catalogFramework;

  private final TemplateTransformer transformer;

  private final EndpointUtil util;

  public SearchFormsApplication(
      CatalogFramework catalogFramework, TemplateTransformer transformer, EndpointUtil util) {
    this.catalogFramework = catalogFramework;
    this.transformer = transformer;
    this.util = util;
  }

  /**
   * Called via blueprint on initialization. Reads configuration in {@code etc/forms} and
   * initializes Solr with query and result templates using the {@link Metacard#TITLE} field as a
   * unique key.
   */
  public void setup() {
    Function<Map<String, Result>, Set<String>> titles =
        map ->
            map.entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .map(Result::getMetacard)
                .map(Metacard::getTitle)
                .collect(Collectors.toSet());

    Set<String> queryTitles = queryAsAdmin(QUERY_TEMPLATE_TAG, titles);
    Set<String> resultTitles = queryAsAdmin(ATTRIBUTE_GROUP_TAG, titles);

    List<Metacard> systemTemplates = config().get();
    if (systemTemplates.isEmpty()) {
      return;
    }

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
      saveMetacards(dedupedTemplateMetacards);
    }
  }

  /** Spark's API-mandated init (not OSGi related) for registering REST functions. */
  @Override
  public void init() {
    get(
        "/forms/query",
        (req, res) ->
            util.getMetacardsByFilter(QUERY_TEMPLATE_TAG)
                .entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .map(Result::getMetacard)
                .map(transformer::toFormTemplate)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()),
        MAPPER::toJson);

    get(
        "/forms/result",
        (req, res) ->
            util.getMetacardsByFilter(ATTRIBUTE_GROUP_TAG)
                .entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .map(Result::getMetacard)
                .map(transformer::toFieldFilter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()),
        MAPPER::toJson);
  }

  private Set<String> queryAsAdmin(
      String tag, Function<Map<String, Result>, Set<String>> transform) {
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

  private void saveMetacards(List<Metacard> metacards) {
    SECURITY.runAsAdmin(
        () -> {
          try {
            return SECURITY.runWithSubjectOrElevate(
                () ->
                    catalogFramework
                        .create(new CreateRequestImpl(metacards))
                        .getCreatedMetacards());
          } catch (SecurityServiceException | InvocationTargetException e) {
            LOGGER.warn(
                "Can't create metacard for system search template, was unable to elevate privileges",
                e);
          }
          return null;
        });
  }
}
