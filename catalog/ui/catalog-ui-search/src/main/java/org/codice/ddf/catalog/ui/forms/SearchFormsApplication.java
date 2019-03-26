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
import static org.codice.gsonsupport.GsonTypeAdapters.MAP_STRING_TO_OBJECT_TYPE;
import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.Subject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.shiro.SecurityUtils;
import org.codice.ddf.catalog.ui.forms.model.pojo.CommonTemplate;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.gsonsupport.GsonTypeAdapters.DateLongFormatTypeAdapter;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.servlet.SparkApplication;

/** Provides an internal REST interface for working with custom form data for Intrigue. */
public class SearchFormsApplication implements SparkApplication {
  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .registerTypeAdapter(Date.class, new DateLongFormatTypeAdapter())
          .create();

  private final CatalogFramework catalogFramework;

  private final FilterBuilder filterBuilder;

  private final TemplateTransformer transformer;

  private final EndpointUtil util;

  private final Supplier<Subject> subjectSupplier;

  private static final String RESP_MSG = "message";

  private static final String SOMETHING_WENT_WRONG = "Something went wrong.";

  private static final String NO_VALID_TEMPLATE = "Could not update, no valid template specified";

  private static final String CREATED_ON = "createdOn";

  private static final Logger LOGGER = LoggerFactory.getLogger(SearchFormsApplication.class);

  public SearchFormsApplication(
      CatalogFramework catalogFramework,
      FilterBuilder filterBuilder,
      TemplateTransformer transformer,
      EndpointUtil util) {
    this(catalogFramework, filterBuilder, transformer, util, SearchFormsApplication::getSubject);
  }

  public SearchFormsApplication(
      CatalogFramework catalogFramework,
      FilterBuilder filterBuilder,
      TemplateTransformer transformer,
      EndpointUtil util,
      Supplier<Subject> subjectSupplier) {
    this.catalogFramework = catalogFramework;
    this.filterBuilder = filterBuilder;
    this.transformer = transformer;
    this.util = util;
    this.subjectSupplier = subjectSupplier;
  }

  /**
   * Spark's API-mandated init (not OSGi related) for registering REST functions. If no forms
   * directory exists, no PUT/DELETE routes will be registered. The feature is effectively "off".
   */
  @Override
  public void init() {
    get(
        "/forms/query",
        (req, res) ->
            util.getMetacardsByTag(QUERY_TEMPLATE_TAG)
                .values()
                .stream()
                .map(Result::getMetacard)
                .filter(Objects::nonNull)
                .map(transformer::toFormTemplate)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CommonTemplate::getTitle))
                .collect(Collectors.toList()),
        GSON::toJson);

    get(
        "/forms/result",
        (req, res) ->
            util.getMetacardsByTag(ATTRIBUTE_GROUP_TAG)
                .values()
                .stream()
                .map(Result::getMetacard)
                .filter(Objects::nonNull)
                .map(transformer::toFieldFilter)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CommonTemplate::getTitle))
                .collect(Collectors.toList()),
        GSON::toJson);

    post(
        "/forms/query",
        APPLICATION_JSON,
        (req, res) ->
            runWhenNotGuest(
                res,
                () ->
                    doCreate(
                        res,
                        Stream.of(safeGetBody(req))
                            .map(this::parseMap)
                            .map(transformer::toQueryTemplateMetacard)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null))),
        GSON::toJson);

    put(
        "/forms/query/:id",
        APPLICATION_JSON,
        (req, res) ->
            runWhenNotGuest(
                res,
                () ->
                    doUpdate(
                        res,
                        Stream.of(safeGetBody(req))
                            .map(this::parseMap)
                            .map(transformer::toQueryTemplateMetacard)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null),
                        req.params("id"))),
        GSON::toJson);

    post(
        "/forms/result",
        APPLICATION_JSON,
        (req, res) ->
            runWhenNotGuest(
                res,
                () ->
                    doCreate(
                        res,
                        Stream.of(safeGetBody(req))
                            .map(this::parseMap)
                            .map(transformer::toAttributeGroupMetacard)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null))),
        GSON::toJson);

    put(
        "/forms/result/:id",
        APPLICATION_JSON,
        (req, res) ->
            runWhenNotGuest(
                res,
                () ->
                    doUpdate(
                        res,
                        Stream.of(safeGetBody(req))
                            .map(this::parseMap)
                            .map(transformer::toAttributeGroupMetacard)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null),
                        req.params("id"))),
        GSON::toJson);

    delete(
        "/forms/:id",
        APPLICATION_JSON,
        (req, res) -> {
          String id = req.params(":id");
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
        IllegalArgumentException.class,
        (e, req, res) -> {
          LOGGER.debug("Template input was not valid", e);
          res.status(400);
          res.header(CONTENT_TYPE, APPLICATION_JSON);
          res.body(util.getJson(ImmutableMap.of(RESP_MSG, "Input was not valid.")));
        });

    exception(
        IngestException.class,
        (ex, req, res) -> {
          LOGGER.debug("Failed to persist form", ex);
          res.status(404);
          res.header(CONTENT_TYPE, APPLICATION_JSON);
          res.body(
              util.getJson(ImmutableMap.of(RESP_MSG, "Form is either restricted or not found.")));
        });

    exception(
        UnsupportedOperationException.class,
        (e, req, res) -> {
          LOGGER.debug("Could not use filter JSON because it contains unsupported operations", e);
          res.status(501);
          res.header(CONTENT_TYPE, APPLICATION_JSON);
          res.body(util.getJson(ImmutableMap.of(RESP_MSG, "This operation is not supported.")));
        });

    exception(
        SourceUnavailableException.class,
        (ex, req, res) -> {
          LOGGER.debug("Failed to persist form", ex);
          res.status(503);
          res.header(CONTENT_TYPE, APPLICATION_JSON);
          res.body(
              util.getJson(
                  ImmutableMap.of(RESP_MSG, "Source not available, please try again later.")));
        });

    exception(UncheckedIOException.class, util::handleIOException);

    exception(
        RuntimeException.class,
        (e, req, res) -> {
          LOGGER.error(SOMETHING_WENT_WRONG, e);
          res.status(500);
          res.header(CONTENT_TYPE, APPLICATION_JSON);
          res.body(util.getJson(ImmutableMap.of(RESP_MSG, SOMETHING_WENT_WRONG)));
        });

    exception(
        Exception.class,
        (e, req, res) -> {
          LOGGER.error(SOMETHING_WENT_WRONG, e);
          res.status(500);
          res.header(CONTENT_TYPE, APPLICATION_JSON);
          res.body(util.getJson(ImmutableMap.of(RESP_MSG, SOMETHING_WENT_WRONG)));
        });
  }

  private Map<String, Object> parseMap(String json) {
    return GSON.fromJson(json, MAP_STRING_TO_OBJECT_TYPE);
  }

  private Map<String, Object> runWhenNotGuest(
      Response res, CheckedSupplier<Map<String, Object>> templateOperation) throws Exception {
    Subject subject = subjectSupplier.get();
    if (subject.isGuest()) {
      res.status(403);
      return ImmutableMap.of(RESP_MSG, "Guests cannot perform this action.");
    }
    return templateOperation.get();
  }

  private String safeGetBody(Request req) {
    try {
      return util.safeGetBody(req);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Map<String, Object> doCreate(Response response, @Nullable Metacard metacard)
      throws IngestException, SourceUnavailableException {
    if (metacard == null) {
      response.status(400);
      return ImmutableMap.of(RESP_MSG, "Could not create metacard, no valid template specified");
    }
    if (metacard.getId() != null) {
      LOGGER.debug(
          "Cannot specify an ID [{}] when creating form/result metacard", metacard.getId());
      response.status(400);
      return ImmutableMap.of(RESP_MSG, "Could not create metacard, ID attribute not allowed");
    }
    Metacard createdMetacard =
        catalogFramework.create(new CreateRequestImpl(metacard)).getCreatedMetacards().get(0);
    return ImmutableMap.<String, Object>builder()
        .put(Core.ID, createdMetacard.getId())
        .put(CREATED_ON, createdMetacard.getAttribute(Core.CREATED).getValue())
        .build();
  }

  private Map<String, Object> doUpdate(
      Response response, @Nullable Metacard metacard, @Nullable String expectedId)
      throws IngestException, SourceUnavailableException, FederationException,
          UnsupportedQueryException {
    if (metacard == null) {
      LOGGER.debug("Form/Result metacard was null");
      response.status(400);
      return ImmutableMap.of(RESP_MSG, NO_VALID_TEMPLATE);
    }
    String id = metacard.getId();
    if (id == null) {
      LOGGER.debug("Form/Result metacard [{}] had no ID on HTTP body", metacard);
      response.status(400);
      return ImmutableMap.of(RESP_MSG, NO_VALID_TEMPLATE);
    }
    if (expectedId == null || !expectedId.equals(id)) {
      LOGGER.debug(
          "Query param ID [{}] did not match body ID [{}] or was not provided", expectedId, id);
      response.status(400);
      return ImmutableMap.of(RESP_MSG, NO_VALID_TEMPLATE);
    }
    Metacard oldMetacard = getMetacardIfExistsOrNull(id);
    if (oldMetacard == null) {
      LOGGER.debug(
          "Form/Result metacard [{}] with ID [{}] does not exist and cannot be updated",
          metacard,
          id);
      response.status(400);
      return ImmutableMap.of(RESP_MSG, NO_VALID_TEMPLATE);
    }
    for (AttributeDescriptor descriptor : oldMetacard.getMetacardType().getAttributeDescriptors()) {
      Attribute metacardAttribute = metacard.getAttribute(descriptor.getName());
      if (metacardAttribute == null || metacardAttribute.getValue() == null) {
        continue;
      }
      LOGGER.trace("Setting attribute [{}]", metacardAttribute);
      oldMetacard.setAttribute(metacardAttribute);
    }
    catalogFramework.update(new UpdateRequestImpl(id, oldMetacard));
    return ImmutableMap.of(RESP_MSG, "Successfully updated");
  }

  @Nullable
  private Metacard getMetacardIfExistsOrNull(String id)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    Filter idFilter = filterBuilder.attribute(Core.ID).is().equalTo().text(id);
    Filter tagsFilter = filterBuilder.attribute(Core.METACARD_TAGS).is().like().text("*");
    Filter filter = filterBuilder.allOf(idFilter, tagsFilter);

    QueryResponse queryResponse =
        catalogFramework.query(new QueryRequestImpl(new QueryImpl(filter), false));

    if (!queryResponse.getResults().isEmpty()) {
      return queryResponse.getResults().get(0).getMetacard();
    }

    return null;
  }

  private static Subject getSubject() {
    return (Subject) SecurityUtils.getSubject();
  }

  @FunctionalInterface
  @SuppressWarnings("squid:S00112" /* Supplier to mimic Spark's routing API */)
  private interface CheckedSupplier<T> {
    T get() throws Exception;
  }
}
