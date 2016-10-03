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
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.mappings;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.namespace.QName;

import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.CswQueryFactoryTest;
import org.geotools.feature.NameImpl;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.styling.UomOgcMapping;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Within;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.TEquals;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.impl.filter.FuzzyFunction;

public class TestCswRecordMapperFilterVisitor {

    private static final String UNMAPPED_PROPERTY = "not_mapped_to_anything";

    private static FilterFactoryImpl factory;

    private static Expression attrExpr;

    private static Expression created;

    private static CswRecordMapperFilterVisitor visitor;

    private static MetacardType metacardType;

    private static List<MetacardType> mockMetacardTypeList;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        factory = new FilterFactoryImpl();

        attrExpr = factory.property(new NameImpl(new QName(CswConstants.DUBLIN_CORE_SCHEMA,
                UNMAPPED_PROPERTY,
                CswConstants.DUBLIN_CORE_NAMESPACE_PREFIX)));

        created =
                new AttributeExpressionImpl(new NameImpl(new QName(CswConstants.DUBLIN_CORE_SCHEMA,
                        Core.CREATED,
                        CswConstants.DUBLIN_CORE_NAMESPACE_PREFIX)));
        metacardType = CswQueryFactoryTest.getCswMetacardType();

        mockMetacardTypeList = new ArrayList<>();
        mockMetacardTypeList.add(metacardType);

        visitor = new CswRecordMapperFilterVisitor(metacardType, mockMetacardTypeList);
    }

    @Test
    public void testVisitWithUnmappedName() {
        CswRecordMapperFilterVisitor visitor = new CswRecordMapperFilterVisitor(metacardType,
                mockMetacardTypeList);

        PropertyName propertyName = (PropertyName) visitor.visit(attrExpr, null);

        assertThat(propertyName.getPropertyName(), is(UNMAPPED_PROPERTY));
    }

    @Test
    public void testVisitWithBoundingBoxProperty() {
        AttributeExpressionImpl propName = new AttributeExpressionImpl(new NameImpl(new QName(
                CswConstants.DUBLIN_CORE_SCHEMA,
                CswConstants.OWS_BOUNDING_BOX,
                CswConstants.DUBLIN_CORE_NAMESPACE_PREFIX)));
        CswRecordMapperFilterVisitor visitor = new CswRecordMapperFilterVisitor(metacardType,
                mockMetacardTypeList);

        PropertyName propertyName = (PropertyName) visitor.visit(propName, null);

        assertThat(propertyName.getPropertyName(), is(Metacard.ANY_GEO));
    }

    @Test
    public void testVisitWithMappedName() {
        AttributeExpressionImpl propName = new AttributeExpressionImpl(new NameImpl(new QName(
                CswConstants.DUBLIN_CORE_SCHEMA,
                CswConstants.CSW_ALTERNATIVE,
                CswConstants.DUBLIN_CORE_NAMESPACE_PREFIX)));

        CswRecordMapperFilterVisitor visitor = new CswRecordMapperFilterVisitor(metacardType,
                mockMetacardTypeList);

        PropertyName propertyName = (PropertyName) visitor.visit(propName, null);

        assertThat(propertyName.getPropertyName(), is(Core.TITLE));
        assertThat(propertyName.getPropertyName(), not(is(CswConstants.CSW_ALTERNATIVE)));
    }

    @Test
    public void testVisitBeyond() {
        GeometryFactory geoFactory = new GeometryFactory();

        double val = 30;

        Expression pt1 = factory.literal(geoFactory.createPoint(new Coordinate(4, 5)));
        Expression pt2 = factory.literal(geoFactory.createPoint(new Coordinate(6, 7)));

        Beyond filter = factory.beyond(pt1, pt2, val, "kilometers");

        Beyond duplicate = (Beyond) visitor.visit(filter, null);

        assertThat(duplicate.getExpression1(), is(pt1));
        assertThat(duplicate.getExpression2(), is(pt2));
        assertThat(duplicate.getDistanceUnits(), is(UomOgcMapping.METRE.name()));
        assertThat(duplicate.getDistance(), is(1000 * val));
    }

    @Test
    public void testVisitBBox() {
        BBOX filter = factory.bbox(attrExpr, 0, 0, 10, 20, "EPSG:4269");
        String polygon = "POLYGON ((0 0, 0 20, 10 20, 10 0, 0 0))";

        Object obj = visitor.visit(filter, null);

        assertThat(obj, instanceOf(Within.class));

        Within duplicate = (Within) obj;

        assertThat(duplicate.getExpression1(), is(attrExpr));
        assertThat(duplicate.getExpression2()
                .toString(), is(polygon));

    }

    @Test
    public void testVisitDWithin() {
        GeometryFactory geoFactory = new GeometryFactory();

        double val = 10;

        Expression pt1 = factory.literal(geoFactory.createPoint(new Coordinate(4, 5)));
        Expression pt2 = factory.literal(geoFactory.createPoint(new Coordinate(6, 7)));

        DWithin filter = factory.dwithin(pt1, pt2, val, "meters");

        DWithin duplicate = (DWithin) visitor.visit(filter, null);

        assertThat(duplicate.getExpression1(), is(pt1));
        assertThat(duplicate.getExpression2(), is(pt2));
        assertThat(duplicate.getDistanceUnits(), is(UomOgcMapping.METRE.name()));
        assertThat(duplicate.getDistance(), is(val));
    }

    @Test
    public void testVisitPropertyIsBetween() {
        Expression lower = factory.literal(4);
        Expression upper = factory.literal(7);

        PropertyIsBetween filter = factory.between(attrExpr, lower, upper);

        PropertyIsBetween duplicate = (PropertyIsBetween) visitor.visit(filter, null);

        assertThat(duplicate.getExpression(), is(attrExpr));
        assertThat(duplicate.getLowerBoundary(), is(lower));
        assertThat(duplicate.getUpperBoundary(), is(upper));
    }

    @Test
    public void testVisitPropertyIsEqualTo() {
        Expression val = factory.literal("foo");

        PropertyIsEqualTo filter = factory.equals(attrExpr, val);

        PropertyIsEqualTo duplicate = (PropertyIsEqualTo) visitor.visit(filter, null);

        assertThat(duplicate.getExpression1(), is(attrExpr));
        assertThat(duplicate.getExpression2(), is(val));
        assertThat(duplicate.isMatchingCase(), is(true));
    }

    @Test
    public void testVisitPropertyIsFuzzy() {
        visitor = new CswRecordMapperFilterVisitor(metacardType, mockMetacardTypeList);
        Expression val1 = factory.property("fooProperty");
        Expression val2 = factory.literal("fooLiteral");

        //PropertyIsFuzzy maps to a propertyIsLike filter with a fuzzy function
        GeotoolsFilterBuilder builder = new GeotoolsFilterBuilder();
        PropertyIsLike fuzzySearch = (PropertyIsLike) builder.attribute(val1.toString())
                .is()
                .like()
                .fuzzyText(val2.toString());
        PropertyIsLike visitedFilter = (PropertyIsLike) visitor.visit(fuzzySearch, null);

        assertThat(visitedFilter.getExpression(), is(instanceOf(FuzzyFunction.class)));
        assertThat(visitedFilter.getLiteral(), is(val2.toString()));
    }

    @Test
    public void testVisitPropertyIsEqualToCaseInsensitive() {
        Expression val = factory.literal("foo");

        PropertyIsEqualTo filter = factory.equal(attrExpr, val, false);

        PropertyIsEqualTo duplicate = (PropertyIsEqualTo) visitor.visit(filter, null);

        assertThat(duplicate.getExpression1(), is(attrExpr));
        assertThat(duplicate.getExpression2(), is(val));
        assertThat(duplicate.isMatchingCase(), is(false));
    }

    @Test
    public void testVisitPropertyIsNotEqualToCaseInsensitive() {
        Expression val = factory.literal("foo");

        PropertyIsNotEqualTo filter = factory.notEqual(attrExpr, val, false);

        PropertyIsNotEqualTo duplicate = (PropertyIsNotEqualTo) visitor.visit(filter, null);

        assertThat(duplicate.getExpression1(), is(attrExpr));
        assertThat(duplicate.getExpression2(), is(val));
        assertThat(duplicate.isMatchingCase(), is(false));
    }

    @Test
    public void testVisitPropertyIsGreaterThan() {
        Expression val = factory.literal(8);

        PropertyIsGreaterThan filter = factory.greater(attrExpr, val);
        Object obj = visitor.visit(filter, null);

        assertThat(obj, instanceOf(PropertyIsGreaterThan.class));
        PropertyIsGreaterThan duplicate = (PropertyIsGreaterThan) obj;
        assertThat(duplicate.getExpression1(), is(attrExpr));
        assertThat(duplicate.getExpression2(), is(val));
    }


    @Test
    public void testVisitPropertyIsGreaterThanTemporal() {
        Expression val = factory.literal(new Date(System.currentTimeMillis() - 1000));
        Expression test =
                new AttributeExpressionImpl(new NameImpl(new QName(CswConstants.DUBLIN_CORE_SCHEMA,
                        "TestDate",
                        CswConstants.DUBLIN_CORE_NAMESPACE_PREFIX)));

        PropertyIsGreaterThan filter = factory.greater(test, val);
        Object obj = visitor.visit(filter, null);

        assertThat(obj, instanceOf(During.class));
        During duplicate = (During) obj;
        assertThat(duplicate.getExpression1(), is(test));
        assertThat(duplicate.getExpression2(), is(val));
    }

    @Test
    public void testVisitPropertyIsGreaterThanOrEqualTo() {
        Expression val = factory.literal(8);

        PropertyIsGreaterThanOrEqualTo filter = factory.greaterOrEqual(attrExpr, val);
        Object obj = visitor.visit(filter, null);

        assertThat(obj, instanceOf(PropertyIsGreaterThanOrEqualTo.class));
        PropertyIsGreaterThanOrEqualTo duplicate = (PropertyIsGreaterThanOrEqualTo) obj;
        assertThat(duplicate.getExpression1(), is(attrExpr));
        assertThat(duplicate.getExpression2(), is(val));
    }

    @Ignore("not supported by solr provider")
    @Test
    public void testVisitPropertyIsGreaterThanOrEqualToTemporal() {
        Expression val = factory.literal(new Date());

        PropertyIsGreaterThanOrEqualTo filter = factory.greaterOrEqual(created, val);
        Object obj = visitor.visit(filter, null);

        assertThat(obj, instanceOf(Or.class));
        Or duplicate = (Or) obj;

        for (Filter child : duplicate.getChildren()) {
            BinaryTemporalOperator binary = (BinaryTemporalOperator) child;
            assertThat(binary, anyOf(instanceOf(TEquals.class), instanceOf(After.class)));
            assertThat(binary.getExpression1(), is(created));
            assertThat(binary.getExpression2(), is(val));
        }
    }

    @Test
    public void testVisitPropertyIsLessThan() {
        Expression val = factory.literal(8);

        PropertyIsLessThan filter = factory.less(attrExpr, val);

        Object obj = visitor.visit(filter, null);

        assertThat(obj, instanceOf(PropertyIsLessThan.class));
        PropertyIsLessThan duplicate = (PropertyIsLessThan) obj;
        assertThat(duplicate.getExpression1(), is(attrExpr));
        assertThat(duplicate.getExpression2(), is(val));
    }

    @Test
    public void testVisitPropertyIsLessThanTemporal() {
        Expression val = factory.literal(new Date());

        PropertyIsLessThan filter = factory.less(created, val);

        Object obj = visitor.visit(filter, null);

        assertThat(obj, instanceOf(Before.class));
        Before duplicate = (Before) obj;
        assertThat(duplicate.getExpression1(), is(created));
        assertThat(duplicate.getExpression2(), is(val));
    }

    @Test
    public void testVisitPropertyIsLessThanOrEqualTo() {
        Expression val = factory.literal(8);

        PropertyIsLessThanOrEqualTo filter = factory.lessOrEqual(attrExpr, val);

        Object obj = visitor.visit(filter, null);

        assertThat(obj, instanceOf(PropertyIsLessThanOrEqualTo.class));
        PropertyIsLessThanOrEqualTo duplicate = (PropertyIsLessThanOrEqualTo) obj;
        assertThat(duplicate.getExpression1(), is(attrExpr));
        assertThat(duplicate.getExpression2(), is(val));
    }

    @Ignore("not supported by solr provider")
    @Test
    public void testVisitPropertyIsLessThanOrEqualToTemporal() {
        Expression val = factory.literal(new Date());

        PropertyIsLessThanOrEqualTo filter = factory.lessOrEqual(created, val);

        Object obj = visitor.visit(filter, null);

        assertThat(obj, instanceOf(Or.class));
        Or duplicate = (Or) obj;

        for (Filter child : duplicate.getChildren()) {
            BinaryTemporalOperator binary = (BinaryTemporalOperator) child;
            assertThat(binary, anyOf(instanceOf(TEquals.class), instanceOf(Before.class)));
            assertThat(binary.getExpression1(), is(created));
            assertThat(binary.getExpression2(), is(val));
        }
    }

    @Test
    public void testLiteralWithMappableType() {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        String dateString = formatter.format(date);
        Literal val = factory.literal(dateString);

        Literal literal = (Literal) visitor.visit(val, created);

        assertThat(literal.getValue(), instanceOf(Date.class));
        assertThat(literal.getValue(), is(date));
    }

    @Test
    public void testLiteralWithUnknownType() {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        String dateString = formatter.format(date);
        Literal val = factory.literal(dateString);

        Literal literal = (Literal) visitor.visit(val, attrExpr);

        assertThat(literal.getValue(), instanceOf(String.class));
        assertThat(literal.getValue(), is(dateString));
    }

    @Test
    public void testLiteralWithNoType() {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        String dateString = formatter.format(date);
        Literal val = factory.literal(dateString);

        Literal literal = (Literal) visitor.visit(val, null);

        assertThat(literal.getValue(), instanceOf(String.class));
        assertThat(literal.getValue(), is(dateString));
    }

    @Test
    public void testSourceIdFilter() {
        Expression val = factory.literal("source1");
        Expression val2 = factory.literal("source2");

        Expression sourceExpr = factory.property(Core.SOURCE_ID);

        PropertyIsEqualTo filter = factory.equal(sourceExpr, val, false);

        Filter filter2 = factory.equal(sourceExpr, val2, false);

        Filter likeFilter = factory.like(attrExpr, "something");

        Filter sourceFilter = factory.or(filter, filter2);

        Filter totalFilter = factory.and(sourceFilter, likeFilter);

        Object obj = totalFilter.accept(visitor, null);

        assertThat(obj, instanceOf(PropertyIsLike.class));
        PropertyIsLike duplicate = (PropertyIsLike) obj;
        assertThat(duplicate.getExpression(), is(attrExpr));
        assertThat(duplicate.getLiteral(), is("something"));
        assertThat(visitor.getSourceIds()
                .size(), is(2));
    }

}
