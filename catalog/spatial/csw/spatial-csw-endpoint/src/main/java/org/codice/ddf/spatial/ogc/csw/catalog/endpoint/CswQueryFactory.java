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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.common.util.CollectionUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.PropertyIsFuzzyFunction;
import org.codice.ddf.spatial.ogc.csw.catalog.common.converter.DefaultCswRecordMap;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.mappings.CswRecordMapperFilterVisitor;
import org.geotools.feature.NameImpl;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.IsEqualsToImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ddf.catalog.data.MetacardType;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.delegate.TagsFilterDelegate;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.permission.Permissions;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.cat.csw.v_2_0_2.ResultType;
import net.opengis.filter.v_1_1_0.FilterType;
import net.opengis.filter.v_1_1_0.SortByType;

/**
 * CswQueryFactory provides utility methods for creating a {@Link QueryRequest}
 */
public class CswQueryFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CswQueryFactory.class);

    private static final Configuration PARSER_CONFIG =
            new org.geotools.filter.v1_1.OGCConfiguration();

    private static JAXBContext jaxBContext;

    private final FilterBuilder builder;

    private final FilterAdapter adapter;

    private MetacardType metacardType;

    private Map<String, Set<String>> schemaToTagsMapping = new HashMap<>();

    private List<MetacardType> metacardTypes;

    public CswQueryFactory(FilterBuilder filterBuilder, FilterAdapter adapter,
            MetacardType metacardType, List<MetacardType> metacardTypes) {
        this.builder = filterBuilder;
        this.adapter = adapter;
        this.metacardType = metacardType;
        this.metacardTypes = metacardTypes;
    }

    public QueryRequest getQueryById(List<String> ids) {
        List<Filter> filters = ids.stream()
                .map(id -> builder.attribute(Core.ID)
                        .is()
                        .equalTo()
                        .text(id))
                .collect(Collectors.toList());

        Filter anyOfFilter = builder.anyOf(filters);
        return new QueryRequestImpl(new QueryImpl(anyOfFilter), false);
    }

    public QueryRequest getQuery(GetRecordsType request) throws CswException {

        QueryType query = (QueryType) request.getAbstractQuery()
                .getValue();

        CswRecordMapperFilterVisitor filterVisitor = buildFilter(query.getConstraint());
        QueryImpl frameworkQuery = new QueryImpl(filterVisitor.getVisitedFilter());
        frameworkQuery.setSortBy(buildSort(query.getSortBy()));

        if (ResultType.HITS.equals(request.getResultType()) || request.getMaxRecords()
                .intValue() < 1) {
            frameworkQuery.setStartIndex(1);
            frameworkQuery.setPageSize(1);
        } else {
            frameworkQuery.setStartIndex(request.getStartPosition()
                    .intValue());
            frameworkQuery.setPageSize(request.getMaxRecords()
                    .intValue());
        }
        QueryRequest queryRequest;
        boolean isDistributed = request.getDistributedSearch() != null && (
                request.getDistributedSearch()
                        .getHopCount()
                        .longValue() > 1);

        if (isDistributed && CollectionUtils.isEmpty(filterVisitor.getSourceIds())) {
            queryRequest = new QueryRequestImpl(frameworkQuery, true);
        } else if (isDistributed && !CollectionUtils.isEmpty(filterVisitor.getSourceIds())) {
            queryRequest = new QueryRequestImpl(frameworkQuery, filterVisitor.getSourceIds());
        } else {
            queryRequest = new QueryRequestImpl(frameworkQuery, false);
        }
        return queryRequest;
    }

    public QueryRequest getQuery(QueryConstraintType constraint) throws CswException {
        Filter filter = buildFilter(constraint).getVisitedFilter();
        QueryImpl query = new QueryImpl(filter);
        query.setPageSize(-1);

        return new QueryRequestImpl(query);
    }

    private CswRecordMapperFilterVisitor buildFilter(QueryConstraintType constraint)
            throws CswException {
        CswRecordMapperFilterVisitor visitor = new CswRecordMapperFilterVisitor(metacardType, metacardTypes);
        Filter filter = null;
        if (constraint != null) {
            if (constraint.isSetCqlText()) {
                try {
                    filter = CQL.toFilter(constraint.getCqlText());
                } catch (CQLException e) {
                    throw new CswException("Unable to parse CQL Constraint: " + e.getMessage(), e);
                }
            } else if (constraint.isSetFilter()) {
                FilterType constraintFilter = constraint.getFilter();
                filter = parseFilter(constraintFilter);
            }
        } else {
            // not supported by catalog:
            //filter = Filter.INCLUDE;
            filter = builder.attribute(Core.ID)
                    .is()
                    .like()
                    .text(FilterDelegate.WILDCARD_CHAR);
        }

        if (filter == null) {
            throw new CswException("Invalid Filter Expression",
                    CswConstants.NO_APPLICABLE_CODE,
                    null);
        }

        filter = transformCustomFunctionToFilter(filter);

        try {
            visitor.setVisitedFilter((Filter) filter.accept(visitor, null));
        } catch (UnsupportedOperationException ose) {
            throw new CswException(ose.getMessage(), CswConstants.INVALID_PARAMETER_VALUE, null);
        }

        return visitor;
    }

    /**
     * Transforms the filter if it contains a custom function from the
     * {@link org.codice.ddf.spatial.ogc.csw.catalog.common.ExtendedGeotoolsFunctionFactory}. If
     * the filter does not contain a custom function then the original filter is returned.
     *
     * @param filter
     * @return
     */
    private Filter transformCustomFunctionToFilter(Filter filter) {
        if (filter instanceof IsEqualsToImpl
                && ((IsEqualsToImpl) filter).getExpression1() instanceof PropertyIsFuzzyFunction) {

            PropertyIsFuzzyFunction fuzzyProperty =
                    (PropertyIsFuzzyFunction) ((IsEqualsToImpl) filter).getExpression1();

            return builder.attribute(fuzzyProperty.getPropertyName()
                    .toString())
                    .is()
                    .like()
                    .fuzzyText(fuzzyProperty.getLiteral()
                            .toString());
        }

        return filter;
    }

    private SortBy buildSort(SortByType sort) throws CswException {
        if (sort == null || sort.getSortProperty() == null) {
            return null;
        }

        SortBy[] sortByArr = parseSortBy(sort);

        if (sortByArr.length > 1) {
            LOGGER.debug("Query request has multiple sort criteria, only primary will be used");
        }

        SortBy sortBy = sortByArr[0];

        if (sortBy.getPropertyName() == null) {
            LOGGER.debug("No property name in primary sort criteria");
            return null;
        }

        if (!DefaultCswRecordMap.hasDefaultMetacardFieldForPrefixedString(sortBy.getPropertyName()
                        .getPropertyName(),
                sortBy.getPropertyName()
                        .getNamespaceContext())) {
            throw new CswException("Property " + sortBy.getPropertyName()
                    .getPropertyName() + " is not a valid SortBy Field",
                    CswConstants.INVALID_PARAMETER_VALUE,
                    "SortProperty");
        }

        String name =
                DefaultCswRecordMap.getDefaultMetacardFieldForPrefixedString(sortBy.getPropertyName()
                                .getPropertyName(),
                        sortBy.getPropertyName()
                                .getNamespaceContext());

        PropertyName propName = new AttributeExpressionImpl(new NameImpl(name));

        return new SortByImpl(propName, sortBy.getSortOrder());
    }

    private SortBy[] parseSortBy(SortByType sortByType) throws CswException {
        JAXBElement<SortByType> sortByElement =
                new net.opengis.filter.v_1_1_0.ObjectFactory().createSortBy(sortByType);

        return (SortBy[]) parseJaxB(sortByElement);
    }

    private Object parseJaxB(JAXBElement<?> element) throws CswException {
        Parser parser = new Parser(PARSER_CONFIG);
        InputStream inputStream;

        try {
            inputStream = marshalJaxB(element);
            return parser.parse(inputStream);
        } catch (JAXBException | IOException | SAXException | ParserConfigurationException | RuntimeException e) {
            throw new CswException(String.format("Failed to parse Element: (%s): %s",
                    e.getClass()
                            .getSimpleName(),
                    e.getMessage()), CswConstants.INVALID_PARAMETER_VALUE, null);
        }
    }

    private InputStream marshalJaxB(JAXBElement<?> filterElement) throws JAXBException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        getJaxBContext().createMarshaller()
                .marshal(filterElement, os);
        ByteArrayInputStream input = new ByteArrayInputStream(os.toByteArray());
        IOUtils.closeQuietly(os);

        return input;
    }

    public static synchronized JAXBContext getJaxBContext() throws JAXBException {
        if (jaxBContext == null) {

            jaxBContext = JAXBContext.newInstance("net.opengis.cat.csw.v_2_0_2:"
                            + "net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1:net.opengis.ows.v_1_0_0",
                    CswQueryFactory.class.getClassLoader());
        }
        return jaxBContext;
    }

    private Filter parseFilter(FilterType filterType) throws CswException {
        if (!filterType.isSetComparisonOps() && !filterType.isSetId() && !filterType.isSetLogicOps()
                && !filterType.isSetSpatialOps()) {
            throw new CswException("Empty Filter provided. Unable to perform query.",
                    CswConstants.INVALID_PARAMETER_VALUE,
                    "Filter");
        }
        JAXBElement<FilterType> filterElement =
                new net.opengis.filter.v_1_1_0.ObjectFactory().createFilter(filterType);

        return (Filter) parseJaxB(filterElement);
    }

    public QueryRequest updateQueryRequestTags(QueryRequest queryRequest, String schema)
            throws UnsupportedQueryException {
        QueryRequest newRequest = queryRequest;
        Set<String> tags = schemaToTagsMapping.get(schema);
        if (CollectionUtils.isEmpty(tags)) {
            return queryRequest;
        }
        Query origQuery = queryRequest.getQuery();
        if (!adapter.adapt(queryRequest.getQuery(), new TagsFilterDelegate(tags, true))) {
            List<Filter> filters = new ArrayList<>(tags.size());
            for (String tag : tags) {
                filters.add(builder.attribute(Core.METACARD_TAGS)
                        .is()
                        .like()
                        .text(tag));
            }
            QueryImpl newQuery = new QueryImpl(builder.allOf(builder.anyOf(filters), origQuery),
                    origQuery.getStartIndex(),
                    origQuery.getPageSize(),
                    origQuery.getSortBy(),
                    origQuery.requestsTotalResultsCount(),
                    origQuery.getTimeoutMillis());
            newRequest = new QueryRequestImpl(newQuery,
                    queryRequest.isEnterprise(),
                    queryRequest.getSourceIds(),
                    queryRequest.getProperties());
        }
        return newRequest;
    }

    public void setSchemaToTagsMapping(String[] schemaToTagsMappingStrings) {
        if (schemaToTagsMappingStrings != null) {
            schemaToTagsMapping.clear();
            schemaToTagsMapping.putAll(Permissions.parsePermissionsFromString(
                    schemaToTagsMappingStrings));
        }
    }
}
