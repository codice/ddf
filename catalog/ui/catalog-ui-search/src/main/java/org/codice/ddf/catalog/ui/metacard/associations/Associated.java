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
package org.codice.ddf.catalog.ui.metacard.associations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.codice.ddf.catalog.ui.config.ConfigurationApplication;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import ddf.catalog.CatalogFramework;
import ddf.catalog.core.versioning.DeletedMetacard;
import ddf.catalog.core.versioning.MetacardVersion;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.types.AssociationsAttributes;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

public class Associated {

    private static final Set<String> ASSOCIATION_TYPES = ImmutableSet.of(AssociationsAttributes.DERIVED,
            AssociationsAttributes.RELATED);

    private final EndpointUtil endpointUtil;

    private final CatalogFramework catalogFramework;

    private int querySize;

    public Associated(EndpointUtil endpointUtil, CatalogFramework catalogFramework, ConfigurationApplication configurationApplication) {
        this.endpointUtil = endpointUtil;
        this.catalogFramework = catalogFramework;
        this.querySize = configurationApplication.getResultCount();
    }

    public Collection<Edge> getAssociations(List<String> writableSourceIds, String metacardId)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        Map<String, Metacard> metacardMap = query(writableSourceIds, withNonrestrictedTags(
                forRootAndParents(metacardId)));
        if (metacardMap.isEmpty()) {
            return Collections.emptyList();
        }

        Metacard root = metacardMap.get(metacardId);
        if (root == null) {
            return Collections.emptyList();
        }

        Collection<Metacard> parents = metacardMap.values()
                .stream()
                .filter(m -> !m.getId()
                        .equals(metacardId))
                .collect(Collectors.toList());

        Map<String, Metacard> childMetacardMap = query(writableSourceIds, withNonrestrictedTags(
                forChildAssociations(root)));

        Collection<Edge> parentEdges = createParentEdges(parents, root);
        Collection<Edge> childrenEdges = createChildEdges(childMetacardMap.values(), root);

        Collection<Edge> edges = Stream.of(parentEdges, childrenEdges)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        return edges;
    }

    public void putAssociations(List<String> writableSourceIds, String id, Collection<Edge> edges)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            IngestException {
        Collection<Edge> oldEdges = getAssociations(writableSourceIds, id);

        List<String> ids = Stream.concat(oldEdges.stream(), edges.stream())
                .flatMap(e -> Stream.of(e.child, e.parent))
                .filter(Objects::nonNull)
                .map(m -> m.get(Metacard.ID))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Metacard> metacards = endpointUtil.getMetacards(writableSourceIds,
                ids,
                getNonrestrictedTagsFilter())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue()
                                .getMetacard()));

        Map<String, Metacard> changedMetacards = new HashMap<>();
        Set<Edge> oldEdgeSet = new HashSet<>(oldEdges);
        Set<Edge> newEdgeSet = new HashSet<>(edges);

        Set<Edge> oldDiff = Sets.difference(oldEdgeSet, newEdgeSet);
        Set<Edge> newDiff = Sets.difference(newEdgeSet, oldEdgeSet);

        for (Edge edge : oldDiff) {
            removeEdge(edge, metacards, changedMetacards);
        }
        for (Edge edge : newDiff) {
            addEdge(edge, metacards, changedMetacards);
        }

        if (changedMetacards.isEmpty()) {
            return;
        }

        catalogFramework.update(new UpdateRequestImpl(changedMetacards.keySet()
                .toArray(new String[0]), new ArrayList<>(changedMetacards.values())));
    }

    private void removeEdge(Edge edge, Map<String, Metacard> metacards,
            /*Mutable*/ Map<String, Metacard> changedMetacards) throws IngestException {
        String id = edge.parent.get(Metacard.ID)
                .toString();
        Metacard target = changedMetacards.getOrDefault(id, metacards.get(id));

        if (target == null) {
            throw new IngestException(String.format("Could not find metacard with id %s", id));
        }

        ArrayList<String> values = Optional.of(target)
                .map(m -> m.getAttribute(edge.relation))
                .map(Attribute::getValues)
                .map(endpointUtil::getStringList)
                .orElseGet(ArrayList::new);
        values.remove(edge.child.get(Metacard.ID)
                .toString());
        target.setAttribute(new AttributeImpl(edge.relation, values));
        changedMetacards.put(id, target);
    }

    private void addEdge(Edge edge, Map<String, Metacard> metacards,
            Map<String, Metacard> changedMetacards) throws IngestException {
        String id = edge.parent.get(Metacard.ID)
                .toString();
        Metacard target = changedMetacards.getOrDefault(id, metacards.get(id));

        if (target == null) {
            throw new IngestException(String.format("Could not find metacard with id %s", id));
        }

        ArrayList<String> values = Optional.of(target)
                .map(m -> m.getAttribute(edge.relation))
                .map(Attribute::getValues)
                .map(endpointUtil::getStringList)
                .orElseGet(ArrayList::new);
        values.add(edge.child.get(Metacard.ID)
                .toString());
        target.setAttribute(new AttributeImpl(edge.relation, values));
        changedMetacards.put(id, target);
    }

    private Collection<Edge> createChildEdges(Collection<Metacard> children, Metacard root) {
        List<Edge> edges = new ArrayList<>();
        for (Metacard child : children) {
            List<String> relations = getRelationsToChild(root, child);
            edges.addAll(relations.stream()
                    .map(relation -> new Edge(root, child, relation))
                    .collect(Collectors.toList()));
        }
        return edges;
    }

    private Collection<Edge> createParentEdges(Collection<Metacard> parents, Metacard root) {
        List<Edge> edges = new ArrayList<>();
        for (Metacard parent : parents) {
            List<String> relations = getRelationsToChild(parent, root);
            edges.addAll(relations.stream()
                    .map(relation -> new Edge(parent, root, relation))
                    .collect(Collectors.toList()));
        }
        return edges;
    }

    private Filter forRootAndParents(String rootId) {
        Filter root = endpointUtil.getFilterBuilder()
                .attribute(Metacard.ID)
                .is()
                .equalTo()
                .text(rootId);
        Filter related = endpointUtil.getFilterBuilder()
                .attribute(Metacard.RELATED)
                .is()
                .like()
                .text(rootId);
        Filter derived = endpointUtil.getFilterBuilder()
                .attribute(Metacard.DERIVED)
                .is()
                .like()
                .text(rootId);
        Filter parents = endpointUtil.getFilterBuilder()
                .anyOf(related, derived);

        return endpointUtil.getFilterBuilder()
                .anyOf(root, parents);
    }

    private Filter withNonrestrictedTags(Filter filter) {
        if (filter == null) {
            return null;
        }
        return endpointUtil.getFilterBuilder()
                .allOf(filter, getNonrestrictedTagsFilter());
    }

    private Filter getNonrestrictedTagsFilter() {
        return endpointUtil.getFilterBuilder()
                .not(endpointUtil.getFilterBuilder()
                        .anyOf(endpointUtil.getFilterBuilder()
                                        .attribute(Metacard.TAGS)
                                        .is()
                                        .like()
                                        .text(DeletedMetacard.DELETED_TAG),
                                endpointUtil.getFilterBuilder()
                                        .attribute(Metacard.TAGS)
                                        .is()
                                        .like()
                                        .text(MetacardVersion.VERSION_TAG)));
    }

    private Filter forChildAssociations(Metacard metacard) {
        Set<String> childIds = ASSOCIATION_TYPES.stream()
                .map(metacard::getAttribute)
                .filter(Objects::nonNull)
                .map(Attribute::getValues)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toSet());

        if (childIds.isEmpty()) {
            return null;
        }
        return endpointUtil.getFilterBuilder()
                .anyOf(childIds.stream()
                        .map(id -> endpointUtil.getFilterBuilder()
                                .attribute(Metacard.ID)
                                .is()
                                .equalTo()
                                .text(id))
                        .collect(Collectors.toList()));

    }

    private Map<String, Metacard> query(List<String> writableSourceIds, Filter filter)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        if (filter == null) {
            return Collections.emptyMap();
        }

        QueryRequestImpl queryRequest = new QueryRequestImpl(new QueryImpl(filter,
                1,
                querySize,
                SortBy.NATURAL_ORDER,
                false,
                TimeUnit.SECONDS.toMillis(30)), writableSourceIds);

        QueryResponse queryResponse = catalogFramework.query(queryRequest);

        return queryResponse.getResults()
                .stream()
                .map(Result::getMetacard)
                .collect(Collectors.toMap(Metacard::getId, Function.identity()));
    }

    private List<String> getRelationsToChild(Metacard parent, Metacard child) {
        List<String> relations = new ArrayList<>();
        for (String associationType : ASSOCIATION_TYPES) {
            if (Optional.of(parent)
                    .map(m -> m.getAttribute(associationType))
                    .map(Attribute::getValues)
                    .map(endpointUtil::getStringList)
                    .map(l -> l.contains(child.getId()))
                    .orElse(false)) {
                relations.add(associationType);
            }
        }

        return relations;
    }

    public class Edge {
        private Map<String, Object> parent;

        private Map<String, Object> child;

        private String relation;

        public Edge(Metacard parent, Metacard child, String relation) {
            this.parent = endpointUtil.getMetacardMap(parent);
            this.child = endpointUtil.getMetacardMap(child);
            this.relation = relation;
        }

        public int hashCode() {
            return new HashCodeBuilder().append(parent.get(Metacard.ID)
                    .toString())
                    .append(child.get(Metacard.ID)
                            .toString())
                    .append(relation)
                    .build();
        }

        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (o == this) {
                return true;
            }
            if (!(o instanceof Edge)) {
                return false;
            }
            Edge rhs = (Edge) o;
            return new EqualsBuilder().append(parent.get(Metacard.ID)
                            .toString(),
                    rhs.parent.get(Metacard.ID)
                            .toString())
                    .append(child.get(Metacard.ID)
                                    .toString(),
                            rhs.child.get(Metacard.ID)
                                    .toString())
                    .append(relation, rhs.relation)
                    .build();
        }

        @Override
        public String toString() {
            return String.format("%s [%s]-> %s",
                    parent.get("id")
                            .toString(),
                    relation,
                    child.get("id")
                            .toString());
        }
    }

}
