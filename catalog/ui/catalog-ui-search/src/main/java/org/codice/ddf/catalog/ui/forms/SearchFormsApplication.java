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

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.codice.ddf.catalog.ui.forms.data.AttributeGroupType.ATTRIBUTE_GROUP_TAG;
import static org.codice.ddf.catalog.ui.forms.data.QueryTemplateType.QUERY_TEMPLATE_TAG;
import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.post;

import com.google.common.collect.ImmutableMap;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.security.SubjectUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.boon.json.JsonFactory;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.boon.json.ObjectMapper;
import org.codice.ddf.catalog.ui.forms.model.FilterNodeValueSerializer;
import org.codice.ddf.catalog.ui.forms.model.TemplateTransformer;
import org.codice.ddf.catalog.ui.forms.model.pojo.CommonTemplate;
import org.codice.ddf.catalog.ui.forms.model.pojo.FieldFilter;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Response;
import spark.servlet.SparkApplication;

/** Provides an internal REST interface for working with custom form data for Intrigue. */
public class SearchFormsApplication implements SparkApplication {
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

  private static final String RESP_MSG = "Message";

  private static final Logger LOGGER = LoggerFactory.getLogger(SearchFormsApplication.class);

  public SearchFormsApplication(
      CatalogFramework catalogFramework, TemplateTransformer transformer, EndpointUtil util) {
    this.catalogFramework = catalogFramework;
    this.transformer = transformer;
    this.util = util;
  }

  /**
   * Called via blueprint on initialization. Reads configuration in {@code etc/forms} and
   * initializes Solr with query templates and attribute groups using the {@link
   * ddf.catalog.data.types.Core#TITLE} field as a unique key.
   */
  public void setup() {
    List<Metacard> systemTemplates = SearchFormsLoader.config().get();
    if (systemTemplates.isEmpty()) {
      return;
    }
    SearchFormsLoader.bootstrap(catalogFramework, util, systemTemplates);
  }

  /** Spark's API-mandated init (not OSGi related) for registering REST functions. */
  @Override
  public void init() {
    get(
        "/forms/query",
        (req, res) ->
            util.getMetacardsByFilter(QUERY_TEMPLATE_TAG)
                .values()
                .stream()
                .map(Result::getMetacard)
                .filter(Objects::nonNull)
                .map(transformer::toFormTemplate)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CommonTemplate::getTitle))
                .collect(Collectors.toList()),
        MAPPER::toJson);

    get(
        "/forms/result",
        (req, res) ->
            util.getMetacardsByFilter(ATTRIBUTE_GROUP_TAG)
                .values()
                .stream()
                .map(Result::getMetacard)
                .filter(Objects::nonNull)
                .map(transformer::toFieldFilter)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CommonTemplate::getTitle))
                .collect(Collectors.toList()),
        MAPPER::toJson);

    post(
        "/forms/query",
        APPLICATION_JSON,
        (req, res) ->
            doCreate(
                res,
                Stream.of(util.safeGetBody(req))
                    .map(MAPPER::fromJson)
                    .map(Map.class::cast)
                    .map(transformer::toQueryTemplateMetacard)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())),
        MAPPER::toJson);

    post(
        "/forms/result",
        APPLICATION_JSON,
        (req, res) ->
            doCreate(
                res,
                Stream.of(util.safeGetBody(req))
                    .map(b -> MAPPER.fromJson(b, FieldFilter.class))
                    .map(transformer::toAttributeGroupMetacard)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())),
        MAPPER::toJson);

    delete(
        "/forms/:id",
        APPLICATION_JSON,
        (req, res) -> {
          String id = req.params(":id");

          Subject subject = SecurityUtils.getSubject();
          String currentUser = SubjectUtils.getName(subject);

          Map<String, Object> originalMetacardOwner =
              JsonFactory.create().parser().parseMap(util.safeGetBody(req));

          if (!originalMetacardOwner.get(Core.METACARD_OWNER).equals(currentUser)) {
            res.status(500);
            LOGGER.debug("Failed to Delete Form {}", id);
            return ImmutableMap.of(RESP_MSG, "Failed to delete.");
          }

          DeleteResponse deleteResponse = catalogFramework.delete(new DeleteRequestImpl(id));
          if (!deleteResponse.getProcessingErrors().isEmpty()) {
            res.status(500);
            LOGGER.debug("Failed to Delete Form {}", id);
            return ImmutableMap.of(RESP_MSG, "Failed to delete.");
          }
          return ImmutableMap.of(RESP_MSG, "Successfully deleted.");
        },
        util::getJson);

    exception(
        Exception.class,
        (exception, request, response) -> {
          LOGGER.error("Something went wrong", exception);
          response.status(500);
          response.header(CONTENT_TYPE, APPLICATION_JSON);
          response.body(util.getJson(ImmutableMap.of("message", "Something went wrong")));
        });
  }

  private Map<String, Object> doCreate(Response response, List<Metacard> metacards) {
    if (metacards.isEmpty()) {
      response.status(400);
      return ImmutableMap.of("message", "Could not create, no valid template specified");
    }
    try {
      catalogFramework.create(new CreateRequestImpl(metacards));
    } catch (IngestException | SourceUnavailableException e) {
      LOGGER.error("Error creating metacard", e);
      response.status(500);
      return ImmutableMap.of("message", "Could not create");
    }
    return ImmutableMap.of("message", "Successfully created");
  }
}
