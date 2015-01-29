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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.mappings;

import java.io.Serializable;
import java.util.Date;

import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswRecordConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.DefaultCswRecordMap;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.styling.UomOgcMapping;
import org.geotools.temporal.object.DefaultInstant;
import org.geotools.temporal.object.DefaultPeriod;
import org.geotools.temporal.object.DefaultPosition;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.DWithin;
import org.opengis.temporal.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.helpers.NamespaceSupport;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BasicTypes;
import ddf.measure.Distance;
import ddf.measure.Distance.LinearUnit;

/**
 * CswRecordMapperFilterVisitor extends {@link DuplicatingFilterVisitor} to create a new filter
 * where PropertyName expressions are converted from CswRecord terminology to the framework's
 * Metacard terminology
 * 
 */
public class CswRecordMapperFilterVisitor extends DuplicatingFilterVisitor {

    protected static final CswRecordMetacardType CSW_METACARD_TYPE = new CswRecordMetacardType();

    protected static final CswRecordConverter CONVERTER = new CswRecordConverter(null);

    protected static final FilterFactory FILTER_FACTORY = new FilterFactoryImpl();
    
    protected static final String SPATIAL_QUERY_TAG = "spatialQueryExtraData";
    
    @Override
    // convert BBOX queries to Within filters.
    public Object visit(BBOX filter, Object extraData) {
        Expression geometry1 = visit(filter.getExpression1(), SPATIAL_QUERY_TAG);
        Expression geometry2 = visit(filter.getExpression2(), extraData);
        return getFactory(extraData).within(geometry1, geometry2);
    }

    @Override
    public Object visit(Beyond filter, Object extraData) {
        double distance = getDistanceInMeters(filter.getDistance(), filter.getDistanceUnits());

        Expression geometry1 = visit(filter.getExpression1(), SPATIAL_QUERY_TAG);
        Expression geometry2 = visit(filter.getExpression2(), extraData);

        return getFactory(extraData).beyond(geometry1, geometry2, distance,
                UomOgcMapping.METRE.name());
    }

    @Override
    public Object visit(DWithin filter, Object extraData) {
        double distance = getDistanceInMeters(filter.getDistance(), filter.getDistanceUnits());

        Expression geometry1 = visit(filter.getExpression1(), SPATIAL_QUERY_TAG);
        Expression geometry2 = visit(filter.getExpression2(), extraData);

        return getFactory(extraData).dwithin(geometry1, geometry2, distance,
                UomOgcMapping.METRE.name());
    }

    private static final Logger LOGGER = LoggerFactory
            .getLogger(CswRecordMapperFilterVisitor.class);

    @Override
    public Object visit(PropertyName expression, Object extraData) {
        if (expression == null) {
            LOGGER.warn("Attempting to visit a null expression");
            return null;
        }
        String propertyName = expression.getPropertyName();
        String name;
        if (CswConstants.BBOX_PROP.equals(propertyName)
                || CswRecordMetacardType.OWS_BOUNDING_BOX.equals(propertyName)) {
            name = Metacard.ANY_GEO;
        } else {

            NamespaceSupport namespaceSupport = expression.getNamespaceContext();

            name = DefaultCswRecordMap.getDefaultCswRecordMap()
                    .getDefaultMetacardFieldForPrefixedString(propertyName, namespaceSupport);

            if (SPATIAL_QUERY_TAG.equals(extraData)) {
                AttributeDescriptor attrDesc = CSW_METACARD_TYPE.getAttributeDescriptor(name);
                if (attrDesc != null && !BasicTypes.GEO_TYPE.equals(attrDesc.getType())) {
                    throw new UnsupportedOperationException(
                            "Attempted a spatial query on a non-geometry-valued attribute ("
                                    + propertyName + ")");
                }
            }
        }
        LOGGER.debug("Converting \"{}\" to \"{}\"", propertyName, name);

        return getFactory(extraData).property(name);
    }

    @Override
    public Object visit(PropertyIsBetween filter, Object extraData) {
        Expression expr = visit(filter.getExpression(), extraData);
        Expression lower = visit(filter.getLowerBoundary(), expr);
        Expression upper = visit(filter.getUpperBoundary(), expr);
        return getFactory(extraData).between(expr, lower, upper);
    }

    @Override
    public Object visit(PropertyIsEqualTo filter, Object extraData) {
        Expression expr1 = visit(filter.getExpression1(), extraData);
        Expression expr2 = visit(filter.getExpression2(), expr1);
        boolean matchCase = filter.isMatchingCase();
        return getFactory(extraData).equal(expr1, expr2, matchCase);
    }

    @Override
    public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
        Expression expr1 = visit(filter.getExpression1(), extraData);
        Expression expr2 = visit(filter.getExpression2(), expr1);
        boolean matchCase = filter.isMatchingCase();
        return getFactory(extraData).notEqual(expr1, expr2, matchCase);
    }

    @Override
    public Object visit(PropertyIsGreaterThan filter, Object extraData) {
        Expression expr1 = visit(filter.getExpression1(), extraData);
        Expression expr2 = visit(filter.getExpression2(), expr1);
        
        // work around since Solr Provider doesn't support greater on temporal  (DDF-311)
        if(isTemporalQuery(expr1, expr2)) {
            // also not supported by provider (DDF-311)
            //TODO: work around 1: return getFactory(extraData).after(expr1, expr2); 
            Object val = null;
            Expression other = null;
            if(expr2 instanceof Literal) {
                val = ((Literal) expr2).getValue();
                other = expr1;
            } else if(expr1 instanceof Literal) {
                val = ((Literal) expr1).getValue();
                other = expr2;
            }
                    
            if(val != null) {
                Date orig = (Date) val;
                orig.setTime(orig.getTime() + 1);
                Instant start = new DefaultInstant(new DefaultPosition(orig));
                Instant end = new DefaultInstant(new DefaultPosition(new Date()));
                DefaultPeriod period = new DefaultPeriod(start, end);
                Literal literal = getFactory(extraData).literal(period);
                return getFactory(extraData).during(other, literal);
            }
        }
        return getFactory(extraData).greater(expr1, expr2);
    }

    @Override
    public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
        Expression expr1 = visit(filter.getExpression1(), extraData);
        Expression expr2 = visit(filter.getExpression2(), expr1);

        // work around since Solr Provider doesn't support greaterOrEqual on temporal (DDF-311)
        if(isTemporalQuery(expr1, expr2)) {
            // also not supported by provider (DDF-311)
            //TEquals tEquals = getFactory(extraData).tequals(expr1, expr2);
            //After after = getFactory(extraData).after(expr1, expr2);
            //return getFactory(extraData).or(tEquals, after);
            
            Object val = null;
            Expression other = null;
            if(expr2 instanceof Literal) {
                val = ((Literal) expr2).getValue();
                other = expr1;
            } else if(expr1 instanceof Literal) {
                val = ((Literal) expr1).getValue();
                other = expr2;
            }
                    
            if(val != null) {
                Date orig = (Date) val;
                Instant start = new DefaultInstant(new DefaultPosition(orig));
                Instant end = new DefaultInstant(new DefaultPosition(new Date()));
                DefaultPeriod period = new DefaultPeriod(start, end);
                Literal literal = getFactory(extraData).literal(period);
                return getFactory(extraData).during(other, literal);
            }

        }
        return getFactory(extraData).greaterOrEqual(expr1, expr2);
    }

    @Override
    public Object visit(PropertyIsLessThan filter, Object extraData) {
        Expression expr1 = visit(filter.getExpression1(), extraData);
        Expression expr2 = visit(filter.getExpression2(), expr1);

        // work around since solr provider doesn't support lessthan on temporal (DDF-311)
        if(isTemporalQuery(expr1, expr2)) {
            return getFactory(extraData).before(expr1, expr2);
        }        
        return getFactory(extraData).less(expr1, expr2);
    }

    @Override
    public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
        Expression expr1 = visit(filter.getExpression1(), extraData);
        Expression expr2 = visit(filter.getExpression2(), expr1);

        // work around since solr provider doesn't support lessOrEqual on temporal (DDF-311)
        if(isTemporalQuery(expr1, expr2)) {
            // work around #1 fails, solr provider doesn't support tEquals either (DDF-311)
            //TEquals tEquals = getFactory(extraData).tequals(expr1, expr2);
            //Before before = getFactory(extraData).before(expr1, expr2);            
            //return getFactory(extraData).or(tEquals, before);
            
            
            Object val = null;
            Expression other = null;

            if(expr2 instanceof Literal) {
                val = ((Literal) expr2).getValue();
                other = expr1;
            } else if(expr1 instanceof Literal) {
                val = ((Literal) expr1).getValue();
                other = expr2;
            }
                    
            if(val != null) {
                Date orig = (Date) val;
                orig.setTime(orig.getTime() + 1);
                Literal literal = getFactory(extraData).literal(orig);
                return getFactory(extraData).before(other, literal);
            }
        }        
        return getFactory(extraData).lessOrEqual(expr1, expr2);
    }

    @Override
    public Object visit(Literal expression, Object extraData) {
        if (extraData != null && extraData instanceof PropertyName
                && expression.getValue() instanceof String) {
            String propName = ((PropertyName) extraData).getPropertyName();
            AttributeDescriptor attrDesc = CSW_METACARD_TYPE.getAttributeDescriptor(propName);
            if (attrDesc != null && attrDesc.getType() != null) {
                String value = (String) expression.getValue();
                Serializable convertedValue = CONVERTER.convertStringValueToMetacardValue(attrDesc
                        .getType().getAttributeFormat(), value);
                return getFactory(extraData).literal(convertedValue);
            }
        }
        return getFactory(extraData).literal(expression.getValue());
    }
    
    private boolean isTemporalQuery(Expression expr1, Expression expr2) {
        return isTemporalProperty(expr1) || isTemporalProperty(expr2);
    }
    
    private boolean isTemporalProperty(Expression expr) {
        if(expr instanceof PropertyName) {
            AttributeDescriptor attrDesc = CSW_METACARD_TYPE
                    .getAttributeDescriptor(((PropertyName) expr).getPropertyName());
            if(attrDesc != null) {
                return attrDesc.getType().equals(BasicTypes.DATE_TYPE);
            }
        }
        return false;
    }

    private double getDistanceInMeters(double distance, String units) {
        LinearUnit linearUnit = null;
        if ("meters".equals(units)) {
            linearUnit = LinearUnit.METER;
        } else if ("feet".equals(units)) {
            linearUnit = LinearUnit.FOOT_U_S;
        } else if ("statute miles".equals(units)) {
            linearUnit = LinearUnit.MILE;
        } else if ("nautical miles".equals(units)) {
            linearUnit = LinearUnit.NAUTICAL_MILE;
        } else if ("kilometers".equals(units)) {
            linearUnit = LinearUnit.KILOMETER;
        }
        return new Distance(distance, linearUnit).getAs(LinearUnit.METER);
    }

    protected Expression visit(Expression expression, Object extraData) {
        if (expression == null) {
            return null;
        }
        return (Expression) expression.accept(this, extraData);
    }
}
