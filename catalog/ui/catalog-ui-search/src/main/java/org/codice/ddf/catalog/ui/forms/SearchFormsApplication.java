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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.codice.ddf.catalog.ui.forms.data.AttributeGroupType.ATTRIBUTE_GROUP_TAG;
import static org.codice.ddf.catalog.ui.forms.data.QueryTemplateType.QUERY_TEMPLATE_TAG;
import static spark.Spark.delete;
import static spark.Spark.get;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.security.SubjectUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.boon.json.JsonFactory;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.boon.json.ObjectMapper;
import org.codice.ddf.catalog.ui.forms.model.FilterNodeValueSerializer;
import org.codice.ddf.catalog.ui.forms.model.TemplateTransformer;
import org.codice.ddf.catalog.ui.security.ShareableMetacardImpl;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
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
                .map(ShareableMetacardImpl::clone)
                .filter(Objects::nonNull)
                .filter(
                    metacard ->
                        !(sharedByGroup().test(metacard) && sharedByIndividual().test(metacard)))
                .map(transformer::toFormTemplate)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()),
        MAPPER::toJson);

    get(
        "/forms/result",
        (req, res) ->
            util.getMetacardsByFilter(ATTRIBUTE_GROUP_TAG)
                .values()
                .stream()
                .map(Result::getMetacard)
                .map(ShareableMetacardImpl::clone)
                .filter(Objects::nonNull)
                .filter(
                    metacard ->
                        !(sharedByGroup().test(metacard) && sharedByIndividual().test(metacard)))
                .map(transformer::toFieldFilter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()),
        MAPPER::toJson);

    delete(
        "/forms/:id",
        APPLICATION_JSON,
        (req, res) -> {
          String id = req.params(":id");
          String currentUser = getRequesterEmail();
          Map<String, Object> originalMetacardOwner = getOriginalMetacardOwner(req);

          if (!originalMetacardOwner.get(Core.METACARD_OWNER).equals(currentUser)) {
            res.status(500);
            res.body("error");
            LOGGER.debug("Failed to Delete Form {}", id);
            return ImmutableMap.of(RESP_MSG, "Failed to delete.");
          }

          DeleteResponse deleteResponse = catalogFramework.delete(new DeleteRequestImpl(id));
          if (!deleteResponse.getProcessingErrors().isEmpty()) {
            res.status(500);
            LOGGER.debug("Failed to Delete Form {}", id);
            return ImmutableMap.of(RESP_MSG, "Failed to delete.");
          }
          res.body("Deleted");
          return ImmutableMap.of(RESP_MSG, "Successfully deleted.");
        });

    /**
     * Filters metacards based on:
     *
     * <ul>
     *   <li>{@link org.codice.ddf.catalog.ui.forms.data.AttributeGroupType#ATTRIBUTE_GROUP_TAG}
     *   <li>{@link org.codice.ddf.catalog.ui.forms.data.QueryTemplateType#QUERY_TEMPLATE_TAG}
     * </ul>
     *
     * that are explicitly shared with the requesting subject via {@link
     * ddf.catalog.data.impl.types.SecurityAttributes.ACCESS_INDIVIDUALS} or that are implicitly
     * available via association by roles in {@link
     * ddf.catalog.data.impl.types.SecurityAttributes.ACCESS_GROUPS}
     */
    get(
        "/forms/:formType/shared",
        (req, res) -> {
          final String REQUESTED_FORM = req.params(":formType");

          if (!(REQUESTED_FORM.equals(QUERY_TEMPLATE_TAG)
              || REQUESTED_FORM.equals(ATTRIBUTE_GROUP_TAG))) {
            res.status(400);
            LOGGER.debug("Invalid form type requested {}", REQUESTED_FORM);
            return ImmutableMap.of("Error", "The requested form type is invalid");
          }

          return util.getMetacardsByFilter(REQUESTED_FORM)
              .values()
              .stream()
              .map(Result::getMetacard)
              .map(ShareableMetacardImpl::clone)
              .filter(Objects::nonNull)
              .filter(
                  metacard -> sharedByGroup().test(metacard) || sharedByIndividual().test(metacard))
              .map(transformer::toFormTemplate)
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
        },
        MAPPER::toJson);
  }

  /**
   * List intersection between current {@link Subject} and {@link
   * ddf.catalog.data.impl.types.SecurityAttributes#ACCESS_GROUPS} to see if related
   */
  @VisibleForTesting
  Predicate<ShareableMetacardImpl> sharedByGroup() {
    return metacard ->
        !metacard
            .getAccessGroups()
            .stream()
            .filter(getRequesterGroups()::contains)
            .collect(Collectors.toList())
            .isEmpty();
  }

  /**
   * Comparison between {@link Subject} email-address and list of {@link
   * ddf.catalog.data.impl.types.SecurityAttributes#ACCESS_INDIVIDUALS} present on the {@link
   * ShareableMetacardImpl}
   */
  @VisibleForTesting
  Predicate<ShareableMetacardImpl> sharedByIndividual() {
    return metacard ->
        metacard
            .getAccessIndividuals()
            .stream()
            .anyMatch(accessIndividual -> accessIndividual.trim().equals(getRequesterEmail()));
  }

  /** Simple subject utility method to grab email associated with current {@link Subject} */
  @VisibleForTesting
  String getRequesterEmail() {
    Subject subject = SecurityUtils.getSubject();
    return SubjectUtils.getEmailAddress(subject);
  }

  @VisibleForTesting
  Map<String, Object> getOriginalMetacardOwner(Request req) throws IOException {
    return JsonFactory.create().parser().parseMap(util.safeGetBody(req));
  }

  /** Simple subject utility method to grab roles associated with current {@link Subject} */
  @VisibleForTesting
  List<String> getRequesterGroups() {
    Subject subject = SecurityUtils.getSubject();
    return SubjectUtils.getAttribute(subject, SubjectUtils.ROLE_CLAIM_URI);
  }
}
