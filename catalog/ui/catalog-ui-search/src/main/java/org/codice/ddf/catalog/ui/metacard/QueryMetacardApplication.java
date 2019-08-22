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
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.util.impl.ResultIterable;
import ddf.security.SubjectIdentity;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.shiro.SecurityUtils;
import org.codice.ddf.catalog.ui.metacard.query.data.model.QueryBasic;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import spark.servlet.SparkApplication;

public class QueryMetacardApplication implements SparkApplication {

  private static final Gson GSON =
      new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

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
          RetreiveAllQueryRequests retreiveAllQueryRequests =
              new RetreiveAllQueryRequests(req, filterBuilder);

          return ResultIterable.resultIterable(
                  catalogFramework,
                  retreiveAllQueryRequests.getQueryRequest(),
                  retreiveAllQueryRequests.getCount())
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
}
