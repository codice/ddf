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
package ddf.catalog.source.opensearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.styling.UomOgcMapping;
import org.geotools.temporal.object.DefaultInstant;
import org.geotools.temporal.object.DefaultPeriod;
import org.geotools.temporal.object.DefaultPosition;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.geometry.Geometry;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.data.Metacard;
import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.Query;

public class MockQuery implements Query {
    // PLACEHOLDER for security
    private Subject user;

    private Integer startIndex;

    private Integer count;

    private long maxTimeout;

    private boolean isEnterprise;

    private Set<String> siteIds;

    private SortBy sortBy;

    public static final FilterFactory filterFactory = new FilterFactoryImpl();

    protected Filter filter;

    private List<Filter> filters;

    private static final XLogger logger = new XLogger(LoggerFactory.getLogger(MockQuery.class));

    private static String MODIFIED_DATE = "modifiedDate";

    public MockQuery() {
        this(null, 0, 10, "RELEVANCE", SortOrder.DESCENDING, 30000);
    }

    public MockQuery(Subject user, int startIndex, int count, String sortField,
            SortOrder sortOrder, long maxTimeout) {
        this.user = user;
        this.startIndex = startIndex;
        this.count = count;
        if (sortField != null && sortOrder != null) {
            this.sortBy = filterFactory.sort(sortField.toUpperCase(), sortOrder); // RELEVANCE or
                                                                                  // TEMPORAL
        }
        this.maxTimeout = maxTimeout;
        this.filters = new ArrayList<Filter>();
    }

    public void addContextualFilter(String searchTerm, String selector) {
        Filter filter = null;

        if (selector != null) {
            List<Filter> xpathFilters = new ArrayList<Filter>();
            String[] selectors = selector.split(",");
            for (int i = 0; i < selectors.length; i++) {
                Expression xpathRef = new AttributeExpressionImpl(selectors[i]);
                filter = filterFactory.like(xpathRef, searchTerm);
                xpathFilters.add(filter);
            }
            filter = filterFactory.or(xpathFilters);
        } else {
            filter = filterFactory.like(filterFactory.property(Metacard.ANY_TEXT), searchTerm);
        }

        if (filter != null) {
            filters.add(filter);
            this.filter = getFilter();
        }
    }

    public void addTemporalFilter(String dateStart, String dateEnd, String dateOffset) {
        TemporalFilter temporalFilter = null;

        if (dateStart != null || dateEnd != null) {
            temporalFilter = new TemporalFilter(dateStart, dateEnd);
        } else if (dateOffset != null) {
            temporalFilter = new TemporalFilter(Long.parseLong(dateOffset));
        }

        addTemporalFilter(temporalFilter);
    }

    public void addTemporalFilter(TemporalFilter temporalFilter) {
        if (temporalFilter != null) {
            // t1.start < timeType instance < t1.end
            Instant startInstant = new DefaultInstant(new DefaultPosition(
                    temporalFilter.getStartDate()));
            Instant endInstant = new DefaultInstant(
                    new DefaultPosition(temporalFilter.getEndDate()));
            Period period = new DefaultPeriod(startInstant, endInstant);

            Filter filter = filterFactory.during(filterFactory.property(MODIFIED_DATE),
                    filterFactory.literal(period));

            filters.add(filter);
            this.filter = getFilter();
        }
    }

    public void addSpatialFilter(String geometryWkt) {
        SpatialFilter spatialFilter = new SpatialFilter(geometryWkt);

        Geometry geometry = spatialFilter.getGeometry();

        if (geometry != null) {
            Filter filter = filterFactory.contains(Metacard.ANY_GEO, geometry);

            filters.add(filter);
            this.filter = getFilter();
        }
    }

    public void addSpatialDistanceFilter(String lon, String lat, String radius) {
        SpatialDistanceFilter distanceFilter = new SpatialDistanceFilter(lon, lat, radius);

        Geometry geometry = distanceFilter.getGeometry();

        if (geometry != null) {
            Filter filter = filterFactory.dwithin(Metacard.ANY_GEO, geometry,
                    Double.parseDouble(radius), UomOgcMapping.METRE.getSEString());

            filters.add(filter);
            this.filter = getFilter();
        }
    }

    public void addTypeFilter(String type, String versions) {
        Filter filter = null;

        if (versions != null && !versions.isEmpty()) {
            String[] typeVersions = versions.split(",");
            List<Filter> typeVersionPairsFilters = new ArrayList<Filter>();

            for (String version : typeVersions) {
                PropertyIsEqualTo typeFilter = filterFactory.equals(
                        filterFactory.property(Metacard.CONTENT_TYPE), filterFactory.literal(type));
                PropertyIsEqualTo versionFilter = filterFactory.equals(
                        filterFactory.property(Metacard.CONTENT_TYPE_VERSION),
                        filterFactory.literal(version));
                typeVersionPairsFilters.add(filterFactory.and(typeFilter, versionFilter));
            }

            if (!typeVersionPairsFilters.isEmpty()) {
                filter = filterFactory.or(typeVersionPairsFilters);
            } else {
                filter = filterFactory.equals(filterFactory.property(Metacard.CONTENT_TYPE),
                        filterFactory.literal(type));
            }
        } else {
            filter = filterFactory.equals(filterFactory.property(Metacard.CONTENT_TYPE),
                    filterFactory.literal(type));
        }

        if (filter != null) {
            filters.add(filter);
            this.filter = getFilter();
        }
    }

    @Override
    public Object accept(FilterVisitor visitor, Object obj) {
        logger.debug("accept");
        return filter.accept(visitor, obj);
    }

    @Override
    public boolean evaluate(Object object) {
        return filter.evaluate(object);
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

    public void setSiteIds(Set<String> siteIds) {
        this.siteIds = siteIds;
    }

    public void setIsEnterprise(boolean isEnterprise) {
        this.isEnterprise = isEnterprise;
    }

    @Override
    public SortBy getSortBy() {
        return sortBy;
    }

    public Filter getFilter() {
        // If multiple filters, then AND them all together
        if (filters.size() > 1) {
            return filterFactory.and(filters);
        }

        // If only one filter, then just return it
        // (AND'ing it would create an erroneous </ogc:and> closing tag)
        else if (filters.size() == 1) {
            return (Filter) filters.get(0);
        }

        // Otherwise, no filters
        else {
            return null;
        }
    }

}
