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

package org.codice.ddf.opensearch.query;

import org.codice.ddf.endpoints.ASTNode;
import org.codice.ddf.endpoints.KeywordFilterGenerator;
import org.codice.ddf.endpoints.KeywordTextParser;
import org.codice.ddf.opensearch.query.filter.BBoxSpatialFilter;
import org.codice.ddf.opensearch.query.filter.PolygonSpatialFilter;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.Query;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.styling.UomOgcMapping;
import org.geotools.temporal.object.DefaultInstant;
import org.geotools.temporal.object.DefaultPeriod;
import org.geotools.temporal.object.DefaultPosition;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.geometry.Geometry;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.parboiled.Parboiled;
import org.parboiled.buffers.InputBuffer;
import org.parboiled.errors.InvalidInputError;
import org.parboiled.errors.ParseError;
import org.parboiled.errors.ParsingException;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OpenSearchQuery implements Query {
    public static final String CARET = "^";

    private Subject user;

    private Integer startIndex;

    private Integer count;

    private long maxTimeout;

    private boolean isEnterprise;

    private Set<String> siteIds;

    private SortBy sortBy;

    // TODO remove this and only use filterbuilder
    public static final FilterFactory FILTER_FACTORY = new FilterFactoryImpl();

    private List<Filter> filters;

    private static XLogger logger = new XLogger(LoggerFactory.getLogger(OpenSearchQuery.class));

    private final FilterBuilder filterBuilder;

    /**
     * Creates an Implementation of a DDF Query interface. This object is passed from the endpoint
     * to DDF and will be used by sites to perform queries on their respective systems.
     * 
     * @param user
     *            Credentials of the user performing the query
     * @param startIndex
     *            Offset of the returned results.
     * @param count
     *            Number of results to return.
     * @param sortField
     *            Area that the results should be sorted by. Possible values: 'date' and 'relevance'
     * @param sortOrderIn
     *            Order of the results. Possible values: 'asc', 'desc'
     * @param maxTimeout
     *            Maximum amount of time for a query to respond.
     * @param filterBuilder
     *            FilterBuilder object to use for filter creation.
     */
    public OpenSearchQuery(Subject user, Integer startIndex, Integer count, String sortField,
            String sortOrderIn, long maxTimeout, FilterBuilder filterBuilder) {
        String methodName = "OpenSearchQuery constructor";
        logger.entry(methodName);

        this.user = user;
        this.startIndex = startIndex;
        this.count = count;
        this.filterBuilder = filterBuilder;
        SortOrder sortOrder;

        // Query must specify a valid sort order if a sort field was specified, i.e., query
        // cannot specify just "date:", must specify "date:asc"
        if ("asc".equalsIgnoreCase(sortOrderIn)) {
            sortOrder = SortOrder.ASCENDING;
        } else if ("desc".equalsIgnoreCase(sortOrderIn)) {
            sortOrder = SortOrder.DESCENDING;
        } else {
            logger.exit(methodName);
            throw new IllegalArgumentException(
                    "Incorrect sort order received, must be 'asc' or 'desc'");
        }

        if (sortField.equalsIgnoreCase("relevance")) {
            // this.sortPolicy = new SortPolicyImpl( true, Constants.DDF_SORT_QUALIFIER,
            // Constants.SORT_POLICY_VALUE_FULLTEXT, this.sortOrder );
            this.sortBy = FILTER_FACTORY.sort(sortField.toUpperCase(), sortOrder);
        } else if (sortField.equalsIgnoreCase("date")) {
            // this.sortPolicy = new SortPolicyImpl( true, Constants.DDF_SORT_QUALIFIER,
            // Constants.SORT_POLICY_VALUE_TEMPORAL, this.sortOrder );
            this.sortBy = FILTER_FACTORY.sort(Result.TEMPORAL, sortOrder);
        } else {
            logger.exit(methodName);
            throw new IllegalArgumentException(
                    "Incorrect sort field received, must be 'relevance' or 'date'");
        }

        this.maxTimeout = maxTimeout;
        this.filters = new ArrayList<Filter>();
        this.siteIds = new HashSet<String>();

        logger.exit(methodName);
    }

    public void addContextualFilter(String searchTerm, String selectors) throws ParsingException {
        String methodName = "addContextualFilter";
        logger.entry(methodName);

        Filter filter = null;
        KeywordFilterGenerator keywordFilterGenerator = new KeywordFilterGenerator(filterBuilder);

        KeywordTextParser parser = Parboiled.createParser(KeywordTextParser.class);

        // translate the search terms into an abstract syntax tree
        ParsingResult<ASTNode> result = new RecoveringParseRunner(parser.InputPhrase())
                .run(searchTerm);

        // make sure it's a good result before using it
        if (result.matched && !result.hasErrors()) {
            filter = generateContextualFilter(selectors, keywordFilterGenerator, result);
        } else if (result.hasErrors()) {
            throw new ParsingException("Unable to parse keyword search phrase. "
                    + generateParsingError(result));
        }

        if (filter != null) {
            filters.add(filter);
        }

        logger.exit(methodName);
    }

    private String generateParsingError(ParsingResult<ASTNode> result) {
        StringBuilder parsingErrorBuilder = new StringBuilder("Parsing error"
                + ((result.parseErrors.size() > 1) ? "s" : "") + ": \n");
        InputBuffer inputBuffer = result.inputBuffer;
        String parsedLine = inputBuffer.extract(0, Integer.MAX_VALUE);

        StringBuilder invalidInputLineBuilder = null;
        for (ParseError parseError : result.parseErrors) {
            StringBuilder otherErrorLineBuilder = getCaratLineStringBuilder(parsedLine);

            // NOTE for some reason, these indexes start at 1, not 0
            int originalEndIndex = inputBuffer.getOriginalIndex(parseError.getEndIndex()) - 1;
            int originalStartIndex = inputBuffer.getOriginalIndex(parseError.getStartIndex()) - 1;

            if (parseError.getClass().isAssignableFrom(InvalidInputError.class)) {
                // Combine all InvalidInputError's
                if (invalidInputLineBuilder == null) {
                    invalidInputLineBuilder = getCaratLineStringBuilder(parsedLine);
                }

                addCaretsToStringBuilder(invalidInputLineBuilder, originalEndIndex,
                        originalStartIndex);
            } else {
                // output other types of errors separately
                parsingErrorBuilder.append("\nError found in: \n");

                addCaretsToStringBuilder(otherErrorLineBuilder, originalEndIndex,
                        originalStartIndex);

                parsingErrorBuilder.append("\n\t");
                parsingErrorBuilder.append(parsedLine);
                parsingErrorBuilder.append("\n\t");
                parsingErrorBuilder.append(otherErrorLineBuilder);
            }
        }

        if (invalidInputLineBuilder != null) {
            // if the first and last occurrence of CARET aren't the same, there are more than one in
            // the string
            parsingErrorBuilder.append("\nInvalid character"
                    + ((invalidInputLineBuilder.indexOf(CARET) != invalidInputLineBuilder
                            .lastIndexOf(CARET)) ? "s" : "") + " found in: \n");
            parsingErrorBuilder.append("\n\t");
            parsingErrorBuilder.append(parsedLine);
            parsingErrorBuilder.append("\n\t");
            parsingErrorBuilder.append(invalidInputLineBuilder);
        }
        return parsingErrorBuilder.toString();
    }

    private Filter generateContextualFilter(String selectors,
            KeywordFilterGenerator keywordFilterGenerator, ParsingResult<ASTNode> result)
        throws ParsingException {
        Filter filter = null;

        try {
            if (selectors != null) {
                // generate a filter for each selector
                for (String selector : selectors.split(",")) {
                    if (filter == null) {
                        filter = keywordFilterGenerator.getFilterFromASTNode(result.resultValue,
                                selector);
                    } else {
                        filter = filterBuilder.anyOf(filter, keywordFilterGenerator
                                .getFilterFromASTNode(result.resultValue, selector));
                    }
                }
            } else {
                filter = keywordFilterGenerator.getFilterFromASTNode(result.resultValue);
            }

        } catch (IllegalStateException e) {
            throw new ParsingException("Unable to parse keyword search phrase. ", e);
        }

        return filter;
    }

    private void addCaretsToStringBuilder(StringBuilder stringBuilder, int endIndex, int startIndex) {
        for (int insertCaretIndex = startIndex + 1; insertCaretIndex <= endIndex; insertCaretIndex++) {
            stringBuilder.replace(insertCaretIndex, insertCaretIndex + 1, CARET);
        }
    }

    private StringBuilder getCaratLineStringBuilder(String parsedLine) {
        StringBuilder caratLineBuilder = new StringBuilder();
        for (int index = 0; index < parsedLine.length(); index++) {
            caratLineBuilder.append(" ");
        }
        return caratLineBuilder;
    }

    public void addTemporalFilter(String dateStart, String dateEnd, String dateOffset) {
        String methodName = "addTemporalFilter";
        logger.entry(methodName);

        TemporalFilter temporalFilter = null;

        // If either start date OR end date is specified and non-empty, then
        // a temporal filter can be created
        if ((dateStart != null && !dateStart.trim().isEmpty())
                || (dateEnd != null && !dateEnd.trim().isEmpty())) {
            temporalFilter = new TemporalFilter(dateStart, dateEnd);
        } else if (dateOffset != null && !dateOffset.trim().isEmpty()) {
            temporalFilter = new TemporalFilter(Long.parseLong(dateOffset));
        }

        addTemporalFilter(temporalFilter);

        logger.exit(methodName);
    }

    public void addTemporalFilter(TemporalFilter temporalFilter) {
        String methodName = "addTemporalFilter";
        logger.entry(methodName);

        if (temporalFilter != null) {
            // t1.start < timeType instance < t1.end
            Instant startInstant = new DefaultInstant(new DefaultPosition(
                    temporalFilter.getStartDate()));
            Instant endInstant = new DefaultInstant(
                    new DefaultPosition(temporalFilter.getEndDate()));
            Period period = new DefaultPeriod(startInstant, endInstant);

            Filter filter = FILTER_FACTORY.during(FILTER_FACTORY.property(Metacard.MODIFIED),
                    FILTER_FACTORY.literal(period));
            logger.debug("Adding temporal filter");
            filters.add(filter);
        }

        logger.exit(methodName);
    }

    public void addGeometrySpatialFilter(String geometryWkt) {
        SpatialFilter spatialFilter = new SpatialFilter(geometryWkt);
        addSpatialFilter(spatialFilter);
    }

    public void addBBoxSpatialFilter(String bbox) {
        BBoxSpatialFilter bboxFilter = new BBoxSpatialFilter(bbox);
        addSpatialFilter(bboxFilter);
    }

    public void addPolygonSpatialFilter(String polygon) {
        PolygonSpatialFilter polygonFilter = new PolygonSpatialFilter(polygon);
        addSpatialFilter(polygonFilter);
    }

    public void addSpatialDistanceFilter(String lon, String lat, String radius) {
        SpatialDistanceFilter distanceFilter = new SpatialDistanceFilter(lon, lat, radius);

        Geometry geometry = distanceFilter.getGeometry();

        if (geometry != null) {
            Filter filter = FILTER_FACTORY.dwithin(Metacard.ANY_GEO, geometry,
                    Double.parseDouble(radius), UomOgcMapping.METRE.name());
            logger.debug("Adding spatial filter");
            filters.add(filter);
        }
    }

    private void addSpatialFilter(SpatialFilter spatialFilter) {
        Geometry geometry = spatialFilter.getGeometry();

        if (geometry != null) {
            Filter filter = FILTER_FACTORY.intersects(Metacard.ANY_GEO, geometry);
            logger.debug("Adding spatial filter");
            filters.add(filter);
        }
    }

    public void addTypeFilter(String type, String versions) {
        Filter filter;

        Filter typeFilter = null;
        if (type.contains("*")) {
            typeFilter = FILTER_FACTORY.like(FILTER_FACTORY.property(Metacard.CONTENT_TYPE), type);
        } else {
            typeFilter = FILTER_FACTORY.equals(FILTER_FACTORY.property(Metacard.CONTENT_TYPE),
                    FILTER_FACTORY.literal(type));
        }

        if (versions != null && !versions.isEmpty()) {
            logger.debug("Received versions from client.");
            String[] typeVersions = versions.split(",");
            List<Filter> typeVersionPairsFilters = new ArrayList<Filter>();

            for (String version : typeVersions) {
                Filter versionFilter = null;
                if (version.contains("*")) {
                    versionFilter = FILTER_FACTORY.like(
                            FILTER_FACTORY.property(Metacard.CONTENT_TYPE_VERSION), version);
                } else {
                    versionFilter = FILTER_FACTORY.equals(
                            FILTER_FACTORY.property(Metacard.CONTENT_TYPE_VERSION),
                            FILTER_FACTORY.literal(version));
                }
                typeVersionPairsFilters.add(FILTER_FACTORY.and(typeFilter, versionFilter));
            }

            if (!typeVersionPairsFilters.isEmpty()) {
                filter = FILTER_FACTORY.or(typeVersionPairsFilters);
            } else {
                filter = typeFilter;
            }
        } else {
            filter = typeFilter;
        }

        if (filter != null) {
            logger.debug("Adding type filter");
            filters.add(filter);
        }
    }

    @Override
    public Object accept(FilterVisitor visitor, Object obj) {
        Filter filter = getFilter();

        if (logger.isDebugEnabled()) {
            logger.debug("filter being visited: " + filter);
        }

        if (filter != null) {
            return filter.accept(visitor, obj);
        }

        return null;
    }

    @Override
    public boolean evaluate(Object object) {
        Filter filter = getFilter();
        if (logger.isDebugEnabled()) {
            logger.debug("filter being evaluated: " + filter);
        }

        if (filter != null) {
            return filter.evaluate(object);
        }

        return false;
    }

    @Override
    public int getStartIndex() {
        return startIndex;
    }

    @Override
    public int getPageSize() {
        return count;
    }

    @Override
    public boolean requestsTotalResultsCount() {
        // always send back total count
        return true;
    }

    @Override
    public long getTimeoutMillis() {
        return maxTimeout;
    }

    public Set<String> getSiteIds() {
        return this.siteIds;
    }

    public void setSiteIds(Set<String> siteIds) {
        this.siteIds = siteIds;
    }

    public boolean isEnterprise() {
        return this.isEnterprise;
    }

    public void setIsEnterprise(boolean isEnterprise) {
        this.isEnterprise = isEnterprise;
    }

    public FederationStrategy getStrategy() {
        return null;
    }

    @Override
    public SortBy getSortBy() {
        return sortBy;
    }

    public Filter getFilter() {
        if (filters.size() > 1) {
            // If multiple filters, then AND them all together
            return FILTER_FACTORY.and(filters);
        } else if (filters.size() == 1) {
            // If only one filter, then just return it
            // (AND'ing it would create an erroneous </ogc:and> closing tag)
            return filters.get(0);
        } else {
            // Otherwise, no filters
            return null;
        }
    }

    @Override
    public String toString() {
        Filter queryFilter = getFilter();
        if (queryFilter == null) {
            return "OpenSearchQuery: FILTERS:{ NULL }";
        } else {
            return "OpenSearchQuery: " + "FILTERS:{" + queryFilter.toString() + "}";
        }
    }

}
