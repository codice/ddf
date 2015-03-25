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
package ddf.catalog.source.solr;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.impl.QueryImpl;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.SortByImpl;
import org.geotools.geometry.jts.spatialschema.geometry.DirectPositionImpl;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.UomOgcMapping;
import org.geotools.temporal.object.DefaultInstant;
import org.geotools.temporal.object.DefaultPeriod;
import org.geotools.temporal.object.DefaultPosition;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.geometry.Geometry;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class CommonQueryBuilder {

    public final FilterFactory filterFactory = new FilterFactoryImpl();

    /**
     * Builds an OGC {@link PropertyIsEqualTo} filter and returns a generic {@link QueryImpl} with a
     * start index of 1 and request for total count set to true
     * 
     * @param property
     *            - the property you are searching
     * @param value
     *            - the value that you are comparing against the property
     * @return {@link QueryImpl}
     */
    public QueryImpl queryByProperty(String property, String value) {

        QueryImpl query = new QueryImpl(filterFactory.equals(filterFactory.property(property),
                filterFactory.literal(value)));

        query.setStartIndex(1);

        query.setRequestsTotalResultsCount(true);

        return query;

    }

    public QueryImpl read(String id) {
        return queryByProperty(Metacard.ID, id);
    }

    /**
     * Builds {@link QueryImpl} with a start index of 1 and request for total count set to true that
     * contains all the filter logic to request documents from the given List of ids.
     * 
     * @param ids
     * @return {@link QueryImpl}
     */
    public QueryImpl read(List<String> ids) {

        LinkedList<Filter> orFilters = new LinkedList<Filter>();

        for (String id : ids) {

            orFilters.add(filterFactory.equals(filterFactory.property(Metacard.ID),
                    filterFactory.literal(id)));

        }

        QueryImpl query = new QueryImpl(filterFactory.or(orFilters));

        query.setStartIndex(1);

        query.setRequestsTotalResultsCount(true);

        return query;
    }

    /**
     * Builds an OGC {@link PropertyIsLike} filter and returns a generic {@link QueryImpl} with a
     * start index of 1 and request for total count set to true.
     * 
     * <p>
     * The wildcard is *, single character wildcard is '?', and the escape is "\"
     * </p>
     * 
     * @param property
     * @param searchPhrase
     * @return
     */
    public QueryImpl like(String property, String searchPhrase, boolean isCaseSensitive,
            boolean isFuzzy) {

        QueryImpl query = new QueryImpl(filterFactory.like(filterFactory.property(property),
                searchPhrase, "*", "?", "\\", isCaseSensitive));

        return query;

    }

    public Filter equalTo(String property, String searchPhrase, boolean isCaseSensitive) {

        return filterFactory.equal(filterFactory.property(property),
                filterFactory.literal(searchPhrase), isCaseSensitive);

    }

    public QueryImpl contextual(String searchPhrase, boolean isCaseSensitive, boolean isFuzzy) {

        QueryImpl query = new QueryImpl(filterFactory.like(
                filterFactory.property(Metacard.ANY_TEXT), searchPhrase, "*", "?", "\\",
                isCaseSensitive));

        query.setStartIndex(1);

        query.setRequestsTotalResultsCount(true);

        return query;

    }

    public QueryImpl within(Geometry geometry) {

        QueryImpl query = new QueryImpl(filterFactory.within(Metacard.GEOGRAPHY, geometry));

        query.setStartIndex(1);

        query.setRequestsTotalResultsCount(true);

        return query;

    }

    // public QueryImpl within(double x, double y) {
    // double[] coords = {x, y} ;
    //
    // QueryImpl query = new QueryImpl( filterFactory.within(Metacard.ANY_GEO, new PointImpl(new
    // DirectPositionImpl(coords), DefaultGeographicCRS.WGS84)));
    //
    // query.setStartIndex(1) ;
    //
    // query.setRequestsTotalResultsCount(true);
    //
    // return query;
    // }

    /**
     * Creates a {@link QueryImpl} that sorts by distance, startIndex = 1, and nearest neighbor at
     * the coordinates given with the WGS84 CRS.
     * 
     * @param x
     *            - the x coordinate
     * @param y
     *            - the y coordinate
     * @return {@link QueryImpl}
     */
    // public QueryImpl nn(double x, double y) {
    //
    // double[] coords = {x, y} ;
    //
    // QueryImpl query = new QueryImpl(
    // filterFactory.beyond(
    // Metacard.ANY_GEO,
    // new PointImpl(new DirectPositionImpl(coords), DefaultGeographicCRS.WGS84),
    // 0.0,
    // UomOgcMapping.METRE.name()));
    //
    // query.setStartIndex(1) ;
    //
    // SortByImpl sortby = new SortByImpl(filterFactory.property(Result.DISTANCE),
    // org.opengis.filter.sort.SortOrder.ASCENDING);
    // query.setSortBy(sortby) ;
    //
    // return query;
    // }

    /**
     * Creates a {@link QueryImpl} that sorts by distance, startIndex = 1, and nearest neighbor at
     * the coordinates given with the WGS84 CRS.
     * 
     * @param x
     *            - the x coordinate
     * @param y
     *            - the y coordinate
     * @return {@link QueryImpl}
     */
    public QueryImpl nn(Geometry geometry) {

        QueryImpl query = new QueryImpl(filterFactory.beyond(Metacard.ANY_GEO, geometry, 0.0,
                UomOgcMapping.METRE.name()));

        query.setStartIndex(1);

        SortByImpl sortby = new SortByImpl(filterFactory.property(Result.DISTANCE),
                org.opengis.filter.sort.SortOrder.ASCENDING);
        query.setSortBy(sortby);

        return query;
    }

    /**
     * Creates a point radius {@link QueryImpl} with units of measurement of meters.
     * 
     * @param x
     * @param y
     * @param distance
     * @return
     */
    // public QueryImpl pointRadius(double x, double y, double distance) {
    //
    // double[] coords = {x, y} ;
    //
    // QueryImpl query = new QueryImpl(
    // filterFactory.dwithin(
    // Metacard.ANY_GEO,
    // new PointImpl(new DirectPositionImpl(coords), DefaultGeographicCRS.WGS84),
    // distance,
    // UomOgcMapping.METRE.name()));
    //
    // query.setStartIndex(1) ;
    //
    // SortByImpl sortby = new SortByImpl(filterFactory.property(Result.DISTANCE),
    // org.opengis.filter.sort.SortOrder.ASCENDING);
    // query.setSortBy(sortby) ;
    //
    // return query;
    // }

    // public QueryImpl intersects(double x, double y) {
    //
    // double[] coords = {x, y} ;
    //
    // QueryImpl query = new QueryImpl( filterFactory.intersects(Metacard.ANY_GEO, new PointImpl(new
    // DirectPositionImpl(coords), DefaultGeographicCRS.WGS84)));
    //
    // query.setStartIndex(1) ;
    //
    // query.setRequestsTotalResultsCount(true);
    //
    // return query;
    //
    // }
    /**
     * Creates a point radius {@link QueryImpl} with units of measurement of meters.
     * 
     * @param x
     * @param y
     * @param distance
     * @return
     */
    public QueryImpl pointRadius(double x, double y, double distance) {

        double[] coords = {x, y};

        QueryImpl query = new QueryImpl(filterFactory.dwithin(Metacard.ANY_GEO, new PointImpl(
                new DirectPositionImpl(coords), DefaultGeographicCRS.WGS84), distance,
                UomOgcMapping.METRE.name()));

        query.setStartIndex(1);

        SortByImpl sortby = new SortByImpl(filterFactory.property(Result.DISTANCE),
                org.opengis.filter.sort.SortOrder.ASCENDING);
        query.setSortBy(sortby);

        return query;
    }

    public QueryImpl intersects(Geometry geometry) {

        QueryImpl query = new QueryImpl(filterFactory.intersects(Metacard.ANY_GEO, geometry));

        query.setStartIndex(1);

        query.setRequestsTotalResultsCount(true);

        return query;

    }

    // public QueryImpl fuzzy(String searchPhrase, boolean isCaseSensitive) {
    //
    // QueryImpl query = new QueryImpl(
    // filterFactory.like(
    // new FuzzyFunction(
    // Arrays.asList((Expression)(filterFactory.property(Metacard.ANY_TEXT))),
    // filterFactory.literal("")),
    // searchPhrase,
    // "*", "?", "\\",
    // isCaseSensitive) ) ;
    //
    // query.setStartIndex(1) ;
    //
    // query.setRequestsTotalResultsCount(true);
    //
    // return query;
    // }

    public QueryImpl during(String property, Date start, Date end) {

        Instant startInstant = new DefaultInstant(new DefaultPosition(start));

        Instant endInstant = new DefaultInstant(new DefaultPosition(end));

        Period period = new DefaultPeriod(startInstant, endInstant);

        Filter filter = filterFactory.during(filterFactory.property(property),
                filterFactory.literal(period));

        QueryImpl query = new QueryImpl(filter);

        query.setStartIndex(1);

        query.setRequestsTotalResultsCount(true);

        return query;

    }

}
