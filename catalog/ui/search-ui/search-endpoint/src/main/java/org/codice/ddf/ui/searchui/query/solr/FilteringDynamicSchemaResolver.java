/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.ui.searchui.query.solr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.solr.common.SolrInputDocument;

import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.DynamicSchemaResolver;
import ddf.catalog.source.solr.SchemaFields;
import ddf.catalog.source.solr.SolrFilterDelegate;
import ddf.catalog.source.solr.SolrFilterDelegateFactory;

public class FilteringDynamicSchemaResolver extends DynamicSchemaResolver {

    public static final String SOURCE_ID = "source-id";

    private final Set<String> usedFields = new HashSet<>();

    public FilteringDynamicSchemaResolver(FilterAdapter filterAdapter,
            SolrFilterDelegateFactory filterDelegateFactory, QueryRequest request) {
        super();

        usedFields.add(Metacard.ID + SchemaFields.TEXT_SUFFIX);
        usedFields.add(SchemaFields.METACARD_TYPE_FIELD_NAME);

        SolrFilterDelegate solrFilterDelegate = filterDelegateFactory
                .newInstance(this);
        solrFilterDelegate.setSortPolicy(request.getQuery().getSortBy());

        try {
            filterAdapter.adapt(request.getQuery(), solrFilterDelegate);
        } catch (UnsupportedQueryException e) {
            throw new IllegalArgumentException("Unable to parse query for index filtering", e);
        }
    }

    @Override
    public String getField(String propertyName, AttributeType.AttributeFormat format,
            boolean isSearchedAsExactValue) {
        String field = super.getField(propertyName, format, isSearchedAsExactValue);
        usedFields.add(field);
        return field;
    }

    @Override
    public String getCaseSensitiveField(String mappedPropertyName) {
        String field = super.getCaseSensitiveField(mappedPropertyName);
        usedFields.add(field);
        return field;
    }

    @Override
    public List<String> getGuestField(String field) {
        List<String> fields = super.getGuestField(field);
        usedFields.addAll(fields);
        return fields;
    }

    @Override
    public String getWhitespaceTokenizedField(String mappedPropertyName) {
        String field = super.getWhitespaceTokenizedField(mappedPropertyName);
        usedFields.add(field);
        return field;
    }

    @Override
    public void addFields(Metacard metacard, SolrInputDocument solrInputDocument)
            throws MetacardCreationException {
        super.addFields(metacard, solrInputDocument);

        List<String> fieldsToRemove = new ArrayList<>();
        for (String field : solrInputDocument.getFieldNames()) {
            if (!solrFieldUsed(field)) {
                fieldsToRemove.add(field);
            }
        }

        for (String field : fieldsToRemove) {
            solrInputDocument.removeField(field);
        }

        solrInputDocument.addField(SOURCE_ID + SchemaFields.TEXT_SUFFIX, metacard.getSourceId());
    }

    private boolean solrFieldUsed(String field) {
        for (String usedField : usedFields) {
            if (usedField.contains(field)) {
                return true;
            }
        }
        return false;
    }
}
