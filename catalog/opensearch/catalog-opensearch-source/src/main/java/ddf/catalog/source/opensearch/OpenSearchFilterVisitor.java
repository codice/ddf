/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.source.opensearch;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.LikeFilterImpl;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.TOverlaps;
import org.opengis.temporal.Period;
import org.opengis.temporal.PeriodDuration;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import com.vividsolutions.jts.geom.Coordinate;

import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;


public class OpenSearchFilterVisitor extends DefaultFilterVisitor
{
    private static XLogger logger = new XLogger(LoggerFactory.getLogger(OpenSearchFilterVisitor.class));

    private static final String ONLY_AND_MSG = "Opensearch only supports AND operations for non-contextual criteria.";

    private enum NestedTypes
    {
        AND, OR, NOT
    }

    private List<Filter> filters;

    // Can only have one each of each type of filter in an OpenSearch query
    private ContextualSearch contextualSearch;
    private TemporalFilter temporalSearch;
    private SpatialFilter spatialSearch;

    private NestedTypes curNest = null;

    public OpenSearchFilterVisitor()
    {
        filters = new ArrayList<Filter>();

        contextualSearch = null;
        temporalSearch = null;
        spatialSearch = null;
    }

    @Override
    public Object visit( Not filter, Object data )
    {
        Object newData;
        NestedTypes parentNest = curNest;
        logger.debug("ENTERING: NOT filter");
        curNest = NestedTypes.NOT;
        filters.add(filter);
        newData = super.visit(filter, data);
        curNest = parentNest;
        logger.debug("EXITING: NOT filter");

        return newData;
    }

    @Override
    public Object visit( Or filter, Object data )
    {
        Object newData;
        NestedTypes parentNest = curNest;
        logger.debug("ENTERING: OR filter");
        curNest = NestedTypes.OR;
        filters.add(filter);
        newData = super.visit(filter, data);
        curNest = parentNest;
        logger.debug("EXITING: OR filter");

        return newData;
    }

    @Override
    public Object visit( And filter, Object data )
    {
        Object newData;
        NestedTypes parentNest = curNest;
        logger.debug("ENTERING: AND filter");
        curNest = NestedTypes.AND;
        filters.add(filter);
        newData = super.visit(filter, data);
        curNest = parentNest;
        logger.debug("EXITING: AND filter");

        return newData;
    }

    /**
     * DWithin filter maps to a Point/Radius distance Spatial search criteria.
     */
    @Override
    public Object visit( DWithin filter, Object data )
    {
        logger.debug("ENTERING: DWithin filter");
        if (curNest == null || NestedTypes.AND.equals(curNest))
        {
            // The geometric point is wrapped in a <Literal> element, so have to
            // get geometry expression as literal and then evaluate it to get
            // the geometry.
            // Example:
            // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl@dc33f184</ogc:Literal>
            Literal literalWrapper = (Literal) filter.getExpression2();

            // Luckily we know what type the geometry expression should be, so
            // we
            // can cast it
            PointImpl point = (PointImpl) literalWrapper.evaluate(null);
            double[] coords = point.getCentroid().getCoordinate();
            double distance = filter.getDistance();

            logger.debug("point: coords[0] = " + coords[0] + ",   coords[1] = " + coords[1]);
            logger.debug("radius = " + distance);

            this.spatialSearch = new SpatialDistanceFilter(coords[0], coords[1], distance);

            filters.add(filter);
        }
        else
        {
            logger.warn(ONLY_AND_MSG);
        }

        logger.debug("EXITING: DWithin filter");

        return super.visit(filter, data);
    }

    /**
     * Contains filter maps to a Polygon or BBox Spatial search criteria.
     */
    @Override
    public Object visit( Contains filter, Object data )
    {
        logger.debug("ENTERING: Contains filter");
        if (curNest == null || NestedTypes.AND.equals(curNest))
        {
            // The geometric point is wrapped in a <Literal> element, so have to
            // get geometry expression as literal and then evaluate it to get
            // the geometry.
            // Example:
            // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl@64a7c45e</ogc:Literal>
            Literal literalWrapper = (Literal) filter.getExpression2();
            Object geometryExpression = literalWrapper.getValue();

            StringBuffer geometryWkt = new StringBuffer();

            if (geometryExpression instanceof SurfaceImpl)
            {
                SurfaceImpl polygon = (SurfaceImpl) literalWrapper.evaluate(null);

                Coordinate[] coords = polygon.getJTSGeometry().getCoordinates();

                geometryWkt.append("POLYGON((");
                for ( int i = 0; i < coords.length; i++ )
                {
                    geometryWkt.append(coords[i].x);
                    geometryWkt.append(" ");
                    geometryWkt.append(coords[i].y);

                    if (i != (coords.length - 1))
                    {
                        geometryWkt.append(",");
                    }
                }
                geometryWkt.append("))");
                this.spatialSearch = new SpatialFilter(geometryWkt.toString());

                logger.debug("geometryWkt = [" + geometryWkt.toString() + "]");

                filters.add(filter);

            }
            else
            {
                logger.warn("Only POLYGON geometry WKT for Contains filter is supported");
            }
        }
        else
        {
            logger.warn(ONLY_AND_MSG);
        }

        logger.debug("EXITING: Contains filter");

        return super.visit(filter, data);
    }

    /**
     * Intersects filter maps to a Polygon or BBox Spatial search criteria.
     */
    @Override
    public Object visit( Intersects filter, Object data )
    {
        logger.debug("ENTERING: Intersects filter");
        if (curNest == null || NestedTypes.AND.equals(curNest))
        {
            // The geometric point is wrapped in a <Literal> element, so have to
            // get geometry expression as literal and then evaluate it to get
            // the geometry.
            // Example:
            // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl@64a7c45e</ogc:Literal>
            Literal literalWrapper = (Literal) filter.getExpression2();
            Object geometryExpression = literalWrapper.getValue();

            StringBuffer geometryWkt = new StringBuffer();

            if (geometryExpression instanceof SurfaceImpl)
            {
                SurfaceImpl polygon = (SurfaceImpl) literalWrapper.evaluate(null);

                Coordinate[] coords = polygon.getJTSGeometry().getCoordinates();

                geometryWkt.append("POLYGON((");
                for ( int i = 0; i < coords.length; i++ )
                {
                    geometryWkt.append(coords[i].x);
                    geometryWkt.append(" ");
                    geometryWkt.append(coords[i].y);

                    if (i != (coords.length - 1))
                    {
                        geometryWkt.append(",");
                    }
                }
                geometryWkt.append("))");
                this.spatialSearch = new SpatialFilter(geometryWkt.toString());

                logger.debug("geometryWkt = [" + geometryWkt.toString() + "]");

                filters.add(filter);
            }
            else
            {
                logger.warn("Only POLYGON geometry WKT for Intersects filter is supported");
            }
        }
        else
        {
            logger.warn(ONLY_AND_MSG);
        }

        logger.debug("EXITING: Intersects filter");

        return super.visit(filter, data);
    }

    /**
     * TOverlaps filter maps to a Temporal (Absolute and Offset) search
     * criteria.
     */
    @Override
    public Object visit( TOverlaps filter, Object data )
    {
        logger.debug("ENTERING: TOverlaps filter");
        if (curNest == null || NestedTypes.AND.equals(curNest))
        {
            handleTemporal(filter);
        }
        else
        {
            logger.warn(ONLY_AND_MSG);
        }
        logger.debug("EXITING: TOverlaps filter");

        return super.visit(filter, data);
    }

    /**
     * During filter maps to a Temporal (Absolute and Offset) search criteria.
     */
    @Override
    public Object visit( During filter, Object data )
    {
        logger.debug("ENTERING: TOverlaps filter");
        if (curNest == null || NestedTypes.AND.equals(curNest))
        {
            handleTemporal(filter);
        }
        else
        {
            logger.warn(ONLY_AND_MSG);
        }
        logger.debug("EXITING: TOverlaps filter");

        return super.visit(filter, data);
    }

    private void handleTemporal( BinaryTemporalOperator filter )
    {

        Literal literalWrapper = (Literal) filter.getExpression2();
        logger.debug("literalWrapper.getValue() = " + literalWrapper.getValue());

        Object literal = literalWrapper.evaluate(null);
        if (literal instanceof Period)
        {
            Period period = (Period) literal;

            // Extract the start and end dates from the filter
            Date start = period.getBeginning().getPosition().getDate();
            Date end = period.getEnding().getPosition().getDate();

            temporalSearch = new TemporalFilter(start, end);

            filters.add(filter);
        }
        else if (literal instanceof PeriodDuration)
        {

            DefaultPeriodDuration duration = (DefaultPeriodDuration) literal;

            // Extract the start and end dates from the filter
            Date end = Calendar.getInstance().getTime();
            Date start = new Date(end.getTime() - duration.getTimeInMillis());

            temporalSearch = new TemporalFilter(start, end);

            filters.add(filter);
        }

    }

    /**
     * PropertyIsEqualTo filter maps to a Type/Version(s) search criteria.
     */
    @Override
    public Object visit( PropertyIsEqualTo filter, Object data )
    {
        logger.debug("ENTERING: PropertyIsEqualTo filter");

        filters.add(filter);

        logger.debug("EXITING: PropertyIsEqualTo filter");

        return super.visit(filter, data);
    }

    /**
     * PropertyIsLike filter maps to a Contextual search criteria.
     */
    @Override
    public Object visit( PropertyIsLike filter, Object data )
    {
        logger.debug("ENTERING: PropertyIsLike filter");

        LikeFilterImpl likeFilter = (LikeFilterImpl) filter;

        AttributeExpressionImpl expression = (AttributeExpressionImpl) likeFilter.getExpression();
        String selectors = expression.getPropertyName();
        logger.debug("selectors = " + selectors);

        String searchPhrase = likeFilter.getLiteral();
        logger.debug("searchPhrase = [" + searchPhrase + "]");
        if (contextualSearch != null)
        {
            contextualSearch.setSearchPhrase(contextualSearch.getSearchPhrase() + " " + curNest.toString() + " "
                    + searchPhrase);
        }
        else
        {
            contextualSearch = new ContextualSearch(selectors, searchPhrase, likeFilter.isMatchingCase());
        }

        logger.debug("EXITING: PropertyIsLike filter");

        return super.visit(filter, data);
    }

    @Override
    public Object visit( PropertyName expression, Object data )
    {
        logger.debug("ENTERING: PropertyName expression");

        // countOccurrence( expression );

        logger.debug("EXITING: PropertyName expression");

        return data;
    }

    @Override
    public Object visit( Literal expression, Object data )
    {
        logger.debug("ENTERING: Literal expression");

        // countOccurrence( expression );

        logger.debug("EXITING: Literal expression");

        return data;
    }

    public List<Filter> getFilters()
    {
        return filters;
    }

    public ContextualSearch getContextualSearch()
    {
        return contextualSearch;
    }

    public TemporalFilter getTemporalSearch()
    {
        return temporalSearch;
    }

    public SpatialFilter getSpatialSearch()
    {
        return spatialSearch;
    }

}
