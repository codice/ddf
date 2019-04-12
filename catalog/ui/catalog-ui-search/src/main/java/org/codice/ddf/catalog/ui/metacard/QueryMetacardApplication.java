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
package org.codice.ddf.catalog.ui.metacard;

import static org.codice.ddf.catalog.ui.metacard.query.util.QueryAttributes.QUERY_TAG;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.util.impl.ResultIterable;
import ddf.security.SubjectIdentity;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.shiro.SecurityUtils;
import org.codice.ddf.catalog.ui.metacard.query.data.model.QueryBasic;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;
import spark.Request;
import spark.servlet.SparkApplication;

public class QueryMetacardApplication implements SparkApplication {

  private static final Gson GSON =
      new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

  private static final int MIN_START = 1;

  private static final int MAX_PAGE_SIZE = 100;

  private static final String START = "start";

  private static final String COUNT = "count";

  private final CatalogFramework catalogFramework;

  private final EndpointUtil endpointUtil;

  private final FilterBuilder filterBuilder;

  private final SubjectIdentity subjectIdentity;

  public QueryMetacardApplication(
      CatalogFramework catalogFramework,
      EndpointUtil endpointUtil,
      FilterBuilder filterBuilder,
      SubjectIdentity subjectIdentity) {
    this.catalogFramework = catalogFramework;
    this.endpointUtil = endpointUtil;
    this.filterBuilder = filterBuilder;
    this.subjectIdentity = subjectIdentity;
  }

  @Override
  public void init() {
    /*
     * The start query parameter is used to specify the starting index for a collection of results.
     * This value must be greater than or equal to 1.
     *
     * The count query parameter is used to specify the size of your page of results. This value
     * must be greater than or equal to 0.
     */
    get(
        "/queries",
        (req, res) -> {
          int start = getOrDefaultParam(req, START, MIN_START);
          int count = getOrDefaultParam(req, COUNT, MAX_PAGE_SIZE);

          Filter filter = filterBuilder.attribute(Core.METACARD_TAGS).is().like().text(QUERY_TAG);

          QueryRequest queryRequest =
              new QueryRequestImpl(
                  new QueryImpl(
                      filter,
                      start,
                      count,
                      new SortByImpl(Core.MODIFIED, SortOrder.DESCENDING),
                      false,
                      TimeUnit.SECONDS.toMillis(10)),
                  false);

          return ResultIterable.resultIterable(catalogFramework, queryRequest, count)
              .stream()
              .filter(Objects::nonNull)
              .map(Result::getMetacard)
              .filter(Objects::nonNull)
              .map(QueryBasic::new)
              .collect(Collectors.toList());
        },
        GSON::toJson);

    get(
        "/queries/:id",
        (req, res) -> {
          String id = req.params("id");

          Metacard metacard = endpointUtil.getMetacardById(id);
          QueryBasic query = new QueryBasic(metacard);

          return GSON.toJson(query);
        });

    post(
        "/queries",
        (req, res) -> {
          String body = endpointUtil.safeGetBody(req);
          QueryBasic query = GSON.fromJson(body, QueryBasic.class);
          query.setOwner(getSubjectIdentifier());

          CreateRequest createRequest = new CreateRequestImpl(query.getMetacard());
          CreateResponse createResponse = catalogFramework.create(createRequest);

          Metacard metacard = createResponse.getCreatedMetacards().get(0);
          QueryBasic created = new QueryBasic(metacard);

          res.status(201);
          return GSON.toJson(created);
        });

    put(
        "/queries/:id",
        (req, res) -> {
          String id = req.params("id");
          String body = endpointUtil.safeGetBody(req);
          QueryBasic query = GSON.fromJson(body, QueryBasic.class);

          UpdateRequest request = new UpdateRequestImpl(id, query.getMetacard());
          UpdateResponse response = catalogFramework.update(request);

          Metacard metacard = response.getUpdatedMetacards().get(0).getNewMetacard();
          QueryBasic updated = new QueryBasic(metacard);

          return GSON.toJson(updated);
        });

    delete(
        "/queries/:id",
        (req, res) -> {
          String id = req.params("id");

          catalogFramework.delete(new DeleteRequestImpl(id));

          res.status(204);
          return null;
        },
        GSON::toJson);
  }

  @VisibleForTesting
  String getSubjectIdentifier() {
    return subjectIdentity.getUniqueIdentifier(SecurityUtils.getSubject());
  }

  private int getOrDefaultParam(Request request, String key, int defaultValue) {
    String value = request.queryParams(key);

    if (value != null) {
      return Integer.parseInt(value);
    }

    return defaultValue;
  }
}
