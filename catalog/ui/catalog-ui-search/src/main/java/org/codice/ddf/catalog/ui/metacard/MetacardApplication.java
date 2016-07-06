/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.ui.metacard;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.patch;
import static spark.Spark.post;
import static spark.Spark.put;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;

import org.apache.shiro.SecurityUtils;
import org.boon.json.JsonFactory;
import org.codice.ddf.catalog.ui.metacard.associations.Associated;
import org.codice.ddf.catalog.ui.metacard.associations.AssociatedItem;
import org.codice.ddf.catalog.ui.metacard.edit.AttributeChange;
import org.codice.ddf.catalog.ui.metacard.edit.MetacardChanges;
import org.codice.ddf.catalog.ui.metacard.enumerations.ExperimentalEnumerationExtractor;
import org.codice.ddf.catalog.ui.metacard.history.HistoryResponse;
import org.codice.ddf.catalog.ui.metacard.validation.Validator;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardTypeImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceTransformer;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import ddf.catalog.CatalogFramework;
import ddf.catalog.core.versioning.MetacardVersion;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.SubjectUtils;
import spark.servlet.SparkApplication;

public class MetacardApplication implements SparkApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardApplication.class);

    private static final String UPDATE_ERROR_MESSAGE =
            "Workspace is either restricted or not found.";

    private final CatalogFramework catalogFramework;

    private final FilterBuilder filterBuilder;

    private final EndpointUtil util;

    private final Validator validator;

    private final WorkspaceTransformer transformer;

    private final ExperimentalEnumerationExtractor enumExtractor;

    public MetacardApplication(CatalogFramework catalogFramework, FilterBuilder filterBuilder,
            EndpointUtil endpointUtil, Validator validator, WorkspaceTransformer transformer,
            ExperimentalEnumerationExtractor enumExtractor) {
        this.catalogFramework = catalogFramework;
        this.filterBuilder = filterBuilder;
        this.util = endpointUtil;
        this.validator = validator;
        this.transformer = transformer;
        this.enumExtractor = enumExtractor;
    }

    @Override
    public void init() {
        get("/metacardtype", (req, res) -> {
            res.type(APPLICATION_JSON);
            return util.getJson(util.getMetacardTypeMap());
        });

        get("/metacard/:id", (req, res) -> {
            String id = req.params(":id");
            res.type(APPLICATION_JSON);
            return util.metacardToJson(id);
        });

        get("/metacard/:id/attribute/validation", (req, res) -> {
            String id = req.params(":id");
            res.type(APPLICATION_JSON);
            return util.getJson(validator.getValidation(util.getMetacard(id)));
        });

        get("/metacard/:id/validation", (req, res) -> {
            String id = req.params(":id");
            res.type(APPLICATION_JSON);
            return util.getJson(validator.getFullValidation(util.getMetacard(id)));
        });

        post("/metacards", APPLICATION_JSON, (req, res) -> {
            List<String> ids = JsonFactory.create()
                    .parser()
                    .parseList(String.class, req.body());
            List<Metacard> metacards = util.getMetacards(ids, "*")
                    .entrySet()
                    .stream()
                    .map(Map.Entry::getValue)
                    .map(Result::getMetacard)
                    .collect(Collectors.toList());

            res.type(APPLICATION_JSON);
            return util.metacardsToJson(metacards);
        });

        delete("/metacards", APPLICATION_JSON, (req, res) -> {
            List<String> ids = JsonFactory.create()
                    .parser()
                    .parseList(String.class, req.body());
            DeleteResponse deleteResponse =
                    catalogFramework.delete(new DeleteRequestImpl(new ArrayList<>(ids),
                            Metacard.ID,
                            null));
            if (deleteResponse.getProcessingErrors() != null
                    && !deleteResponse.getProcessingErrors()
                    .isEmpty()) {
                res.status(500);
                return "";
            }
            // TODO (RCZ) - wat do.
            return "";
        });

        patch("/metacards", APPLICATION_JSON, (req, res) -> {
            List<MetacardChanges> metacardChanges = JsonFactory.create()
                    .parser()
                    .parseList(MetacardChanges.class, req.body());

            UpdateResponse updateResponse = patchMetacards(metacardChanges);

            if (updateResponse.getProcessingErrors() != null
                    && !updateResponse.getProcessingErrors()
                    .isEmpty()) {
                res.status(500);
                return updateResponse.getProcessingErrors();
            }

            return req.body();
        });

        /**
         * query parameter: pageNumber  - Which page of results to get      - default 1
         * query parameter: pageSize    - How many results to get per page  - default 250
         * query parameter: daysBack    - How many days back to search      - default 7
         */
        get("/metacards/recent", (req, res) -> {
            int pageSize = 250;
            int pageNumber = 1;
            int daysBack = 7;
            if (req.queryParams("pageSize") != null) {
                pageSize = Integer.parseInt(req.queryParams("pageSize"));
            }
            if (req.queryParams("pageNumber") != null) {
                pageNumber = Integer.parseInt(req.queryParams("pageNumber"));
            }
            if (req.queryParams("pageNumber") != null) {
                daysBack = Integer.parseInt(req.queryParams("daysBack"));
            }

            List<Metacard> results = util.getRecentMetacards(pageSize,
                    pageNumber,
                    SubjectUtils.getEmailAddress(SecurityUtils.getSubject()),
                    daysBack);

            return util.getJson(results.stream()
                    .map(mc -> mc.getAttribute(MetacardVersion.VERSION_OF_ID))
                    .filter(Objects::nonNull)
                    .map(Attribute::getValue)
                    .collect(Collectors.toList()));
        });

        put("/validate/attribute/:attribute", TEXT_PLAIN, (req, res) -> {
            String attribute = req.params(":attribute");
            String value = req.body();
            return util.getJson(validator.validateAttribute(attribute, value));
        });

        // TODO (RCZ) - this could use some help
        get("/history/:id", (req, res) -> {
            String id = req.params(":id");
            List<Result> queryResponse = getMetacardHistory(id);
            if (queryResponse == null || queryResponse.isEmpty()) {
                throw new NotFoundException("Could not find metacard with id: " + id);
            }
            List<HistoryResponse> response = queryResponse.stream()
                    .map(Result::getMetacard)
                    .map(mc -> new HistoryResponse(mc.getId(),
                            (String) mc.getAttribute(MetacardVersion.EDITED_BY)
                                    .getValue(),
                            (Date) mc.getAttribute(MetacardVersion.VERSIONED_ON)
                                    .getValue()))
                    .sorted(Comparator.comparing(HistoryResponse::getVersioned))
                    .collect(Collectors.toList());
            res.type(APPLICATION_JSON);
            return util.getJson(response);
        });

        get("/history/:id/revert/:revertid", (req, res) -> {
            String id = req.params(":id");
            String revertId = req.params(":revertid");

            Metacard versionMetacard = util.getMetacard(revertId);

            Metacard revertMetacard = MetacardVersion.toBasicMetacard(versionMetacard);
            if (versionMetacard.getAttribute(MetacardVersion.ACTION)
                    .getValue()
                    .equals(MetacardVersion.Action.DELETED.getKey())) {
                catalogFramework.create(new CreateRequestImpl(revertMetacard));
            } else {
                catalogFramework.update(new UpdateRequestImpl(id, revertMetacard));
            }

            return util.metacardToJson(revertMetacard);
        });

        get("/associations/:id", (req, res) -> {
            String id = req.params(":id");
            Associated associated = new Associated(catalogFramework, util, id);
            List<AssociatedItem> associations = associated.getAssociations();
            Map<String, AssociationResult> resultMap = new HashMap<>();

            for (AssociatedItem association : associations) {
                AssociationResult result = resultMap.getOrDefault(association.getType(),
                        new AssociationResult(association.getType()));
                result.metacards.add(new AssociationResultItem(association.getId(),
                        association.getTitle()));
                resultMap.put(association.getType(), result);
            }

            return util.getJson(resultMap.values());
        });

        put("/associations/:id", (req, res) -> {
            String id = req.params(":id");
            Associated associated = new Associated(catalogFramework, util, id);
            List<AssociationResult> associationsIncoming = JsonFactory.create()
                    .parser()
                    .parseList(AssociationResult.class, req.body());

            List<AssociatedItem> associations = new ArrayList<>();
            for (AssociationResult incoming : associationsIncoming) {
                for (AssociationResultItem item : incoming.metacards) {
                    associations.add(new AssociatedItem(incoming.type, item.title, item.id));
                }
            }
            List<String> emptyAssociations = associationsIncoming.stream()
                    .filter(ar -> ar.metacards.isEmpty())
                    .map(ar -> ar.type)
                    .collect(Collectors.toList());
            associated.putAssociations(associations, emptyAssociations);
            return req.body();
        });

        get("/workspaces/:id", (req, res) -> {
            String id = req.params(":id");
            Metacard metacard = util.getMetacard(id);
            return util.getJson(transformer.transform(metacard));
        });

        get("/workspaces", (req, res) -> {
            Map<String, Result> workspaceMetacards = util.getMetacardsByFilter(
                    WorkspaceMetacardTypeImpl.WORKSPACE_TAG);
            List<Metacard> workspaceList = workspaceMetacards.entrySet()
                    .stream()
                    .map(Map.Entry::getValue)
                    .map(Result::getMetacard)
                    .collect(Collectors.toList());

            return util.getJson(transformer.transform(workspaceList));
        });

        post("/workspaces", APPLICATION_JSON, (req, res) -> {
            Map<String, Object> incoming = JsonFactory.create()
                    .parser()
                    .parseMap(req.body());
            Metacard saved = saveMetacard(transformer.transform(incoming));
            Map<String, Object> response = transformer.transform(saved);

            res.type(APPLICATION_JSON);
            res.status(201);
            return util.getJson(response);
        });

        put("/workspaces/:id", APPLICATION_JSON, (req, res) -> {
            String id = req.params(":id");

            Map<String, Object> workspace = JsonFactory.create()
                    .parser()
                    .parseMap(req.body());

            Metacard metacard = transformer.transform(workspace);
            metacard.setAttribute(new AttributeImpl(Metacard.ID, id));

            Metacard updated = updateMetacard(id, metacard);
            return util.getJson(transformer.transform(updated));
        });

        delete("/workspaces/:id", APPLICATION_JSON, (req, res) -> {
            String id = req.params(":id");
            catalogFramework.delete(new DeleteRequestImpl(id));
            return ImmutableMap.of("message", "Successfully deleted.");
        }, util::getJson);

        get("/enumerations/:type", APPLICATION_JSON, (req, res) -> {
            return util.getJson(enumExtractor.getEnumerations(req.params(":type")));
        });

        exception(IngestException.class, (ex, req, res) -> {
            res.status(404);
            LOGGER.warn("Failed to ingest metacard", ex);
            res.body(util.getJson(ImmutableMap.of("message", UPDATE_ERROR_MESSAGE)));
        });

        exception(NumberFormatException.class, (ex, req, res) -> {
            res.status(400);
            res.body(util.getJson(ImmutableMap.of("message", "Invalid values for numbers")));
        });
    }

    private AttributeDescriptor getDescriptor(Metacard target, String attribute) {
        return Optional.ofNullable(target)
                .map(Metacard::getMetacardType)
                .map(mt -> mt.getAttributeDescriptor(attribute))
                .orElseThrow(() -> new RuntimeException(
                        "Could not find attribute descriptor for: " + attribute));
    }

    protected UpdateResponse patchMetacards(List<MetacardChanges> metacardChanges)
            throws SourceUnavailableException, IngestException, FederationException,
            UnsupportedQueryException {
        Set<String> changedIds = metacardChanges.stream()
                .flatMap(mc -> mc.getIds()
                        .stream())
                .collect(Collectors.toSet());

        Map<String, Result> results = util.getMetacards(changedIds, "*");

        for (MetacardChanges changeset : metacardChanges) {
            for (AttributeChange attributeChange : changeset.getAttributes()) {
                METACARD_LOOP:
                for (String id : changeset.getIds()) {
                    Metacard result = results.get(id)
                            .getMetacard();
                    boolean multivalued = getDescriptor(result,
                            attributeChange.getAttribute()).isMultiValued();

                    if (getDescriptor(result, attributeChange.getAttribute()).getType()
                            .getAttributeFormat()
                            .equals(AttributeType.AttributeFormat.DATE)) {
                        Attribute attribute = result.getAttribute(attributeChange.getAttribute());
                        if (multivalued) {

                            result.setAttribute(new AttributeImpl(attributeChange.getAttribute(),
                                    attribute.getValues()
                                            .stream()
                                            .map(util::parseDate)
                                            .collect(Collectors.toList())));
                        } else { // not multivalued
                            result.setAttribute(new AttributeImpl(attributeChange.getAttribute(),
                                    util.parseDate(attribute.getValue())));

                        }
                        continue METACARD_LOOP;
                    }

                    if (multivalued) {
                        result.setAttribute(new AttributeImpl(attributeChange.getAttribute(),
                                (List<Serializable>) new ArrayList<Serializable>(attributeChange.getValues())));
                    } else {
                        result.setAttribute(new AttributeImpl(attributeChange.getAttribute(),
                                Collections.singletonList(attributeChange.getValues()
                                        .get(0))));
                    }
                }
            }
        }

        List<Metacard> changedMetacards = results.values()
                .stream()
                .map(Result::getMetacard)
                .collect(Collectors.toList());
        return catalogFramework.update(new UpdateRequestImpl(changedIds.toArray(new String[0]),
                changedMetacards));
    }

    private List<Result> getMetacardHistory(String id) throws Exception {
        Filter historyFilter = filterBuilder.attribute(Metacard.TAGS)
                .is()
                .equalTo()
                .text(MetacardVersion.VERSION_TAG);
        Filter idFilter = filterBuilder.attribute(MetacardVersion.VERSION_OF_ID)
                .is()
                .equalTo()
                .text(id);

        Filter filter = filterBuilder.allOf(historyFilter, idFilter);
        QueryResponse response = catalogFramework.query(new QueryRequestImpl(new QueryImpl(filter,
                1,
                -1,
                SortBy.NATURAL_ORDER,
                false,
                TimeUnit.SECONDS.toMillis(10)), false));
        return response.getResults();
    }

    private Metacard updateMetacard(String id, Metacard metacard)
            throws SourceUnavailableException, IngestException {
        return catalogFramework.update(new UpdateRequestImpl(id, metacard))
                .getUpdatedMetacards()
                .get(0)
                .getNewMetacard();
    }

    private Metacard saveMetacard(Metacard metacard)
            throws IngestException, SourceUnavailableException {
        return catalogFramework.create(new CreateRequestImpl(metacard))
                .getCreatedMetacards()
                .get(0);

    }

    private static class AssociationResultItem {
        String id;

        String title;

        private AssociationResultItem(String id, String title) {
            this.id = id;
            this.title = title;
        }
    }

    private static class AssociationResult {
        String type;

        List<AssociationResultItem> metacards = new ArrayList<>();

        AssociationResult(String type) {
            this.type = type;
        }
    }
}
