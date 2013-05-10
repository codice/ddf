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
package ddf.catalog.filter.proxy.builder.test;


import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Date;

import org.geotools.filter.visitor.DefaultExpressionVisitor;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Within;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.During;
import org.opengis.geometry.Geometry;
import org.opengis.temporal.Instant;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;


public class FilterBuilderTest
{

    final static String FOO_ATTRIBUTE = "foo";
    final static String POINT_WKT = "POINT (10 20)";
    private static final String MULTIPOLYGON_WKT = "MULTIPOLYGON (((40 40, 20 45, 45 30, 40 40)), ((20 35, 45 20, 30 5, 10 10, 10 30, 20 35), (30 20, 20 25, 20 15, 30 20)))";

    @Test
    public void propertyIsEqual()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute(FOO_ATTRIBUTE).equalTo().text("bar");
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).is().equalTo().text("bar");
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).is().text("bar");
        filter.accept(visitor, null);

        filter = builder.attribute(FOO_ATTRIBUTE).equalTo().bool(true);
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).equalTo().bytes(new byte[]
        {});
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).equalTo().date(new Date());
        filter.accept(visitor, null);

        filter = builder.attribute(FOO_ATTRIBUTE).equalTo().number((short) 5);
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).equalTo().number(new Integer(5));
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).equalTo().number(new Long(5));
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).equalTo().number(new Float(5));
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).equalTo().number(new Double(5));
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).equalTo().wkt("POINT (10, 30)");
        filter.accept(visitor, null);

        filter = builder.attribute(null).is().text(null);
        filter.accept(visitor, null);

        filter = builder.attribute(FOO_ATTRIBUTE).equalTo().dateRange(new Date(1), new Date(2));
        filter.accept(visitor, null);

        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(13)).visit(isA(PropertyIsEqualTo.class), anyObject());
        inOrder.verify(visitor, times(1)).visit(isA(During.class), anyObject());
    }

    @Test
    public void propertyIsNotEqual()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute(FOO_ATTRIBUTE).notEqualTo().text("bar");
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).is().notEqualTo().text("bar");
        filter.accept(visitor, null);

        filter = builder.attribute(null).is().notEqualTo().text(null);
        filter.accept(visitor, null);

        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(3)).visit(isA(PropertyIsNotEqualTo.class), anyObject());
    }

    @Test
    public void propertyIsBetween()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute(FOO_ATTRIBUTE).between().numbers(new Long(5), new Long(7));
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).is().between().numbers(new Long(5), new Long(7));
        filter.accept(visitor, null);

        filter = builder.attribute(FOO_ATTRIBUTE).between().numbers((short) 5, (short) 7);
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).between().numbers(new Integer(5), new Integer(7));
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).between().numbers(new Long(5), new Long(7));
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).between().numbers(new Float(5), new Float(7));
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).between().numbers(new Double(5), new Double(7));
        filter.accept(visitor, null);

        filter = builder.attribute(null).is().between().numbers(null, (Integer) null);
        filter.accept(visitor, null);

        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(8)).visit(isA(PropertyIsBetween.class), anyObject());
    }

    @Test
    public void propertyIsGreaterThan()
    {
        // TODO
    }

    @Test
    public void propertyIsGreaterThanOrEqualTo()
    {
        // TODO
    }

    @Test
    public void propertyIsLessThan()
    {
        // TODO
    }

    @Test
    public void propertyIsLessThanOrEqualTo()
    {
        // TODO
    }

    @Test
    public void andOrNull()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });

        // FilterBuilder builder = new GeotoolsFilterBuilder();
        //
        //

        FilterBuilder builder = new GeotoolsFilterBuilder();
        Filter filter = builder.allOf(builder.anyOf(builder.attribute(FOO_ATTRIBUTE).is().empty()));

        filter.accept(visitor, null);
        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(1)).visit(isA(And.class), anyObject());
        inOrder.verify(visitor).visit(isA(Or.class), anyObject());

        ArgumentCaptor<PropertyIsNull> propertyIsNullArgument = ArgumentCaptor.forClass(PropertyIsNull.class);
        verify(visitor).visit(propertyIsNullArgument.capture(), anyObject());
        ExpressionVisitor expVisitor = spy(new DefaultExpressionVisitor()
        {
        });
        propertyIsNullArgument.getValue().getExpression().accept(expVisitor, null);
        ArgumentCaptor<PropertyName> propertyNameArgument = ArgumentCaptor.forClass(PropertyName.class);
        verify(expVisitor).visit(propertyNameArgument.capture(), anyObject());
        assertEquals(FOO_ATTRIBUTE, propertyNameArgument.getValue().getPropertyName());
    }

    @Test
    public void andNull()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.allOf(builder.attribute(FOO_ATTRIBUTE).is().empty());
        filter.accept(visitor, null);

        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(1)).visit(isA(And.class), anyObject());
        inOrder.verify(visitor).visit(isA(PropertyIsNull.class), anyObject());
    }

    @Test
    public void orNull()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.anyOf(builder.attribute(FOO_ATTRIBUTE).is().empty());
        filter.accept(visitor, null);

        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(1)).visit(isA(Or.class), anyObject());
        inOrder.verify(visitor).visit(isA(PropertyIsNull.class), anyObject());
    }

    @Test
    public void notNull()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.not(builder.attribute(FOO_ATTRIBUTE).is().empty());
        filter.accept(visitor, null);

        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(1)).visit(isA(Not.class), anyObject());
        inOrder.verify(visitor).visit(isA(PropertyIsNull.class), anyObject());
    }

    @Test
    public void afterDate()
    {
        final Date date = new Date();
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute(FOO_ATTRIBUTE).is().after().date(date);

        filter.accept(visitor, null);

        ArgumentCaptor<After> expressionArgument = ArgumentCaptor.forClass(After.class);
        verify(visitor).visit(expressionArgument.capture(), anyObject());

        ExpressionVisitor expVisitor = spy(new DefaultExpressionVisitor()
        {
        });

        expressionArgument.getValue().getExpression1().accept(expVisitor, null);
        ArgumentCaptor<PropertyName> propertyNameArgument = ArgumentCaptor.forClass(PropertyName.class);
        verify(expVisitor).visit(propertyNameArgument.capture(), anyObject());
        assertEquals(FOO_ATTRIBUTE, propertyNameArgument.getValue().getPropertyName());

        expressionArgument.getValue().getExpression2().accept(expVisitor, null);
        ArgumentCaptor<Literal> literalArgument = ArgumentCaptor.forClass(Literal.class);
        verify(expVisitor).visit(literalArgument.capture(), anyObject());
        assertEquals(date, ((Instant) literalArgument.getValue().getValue()).getPosition().getDate());

    }

    @Test
    public void isDate()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute(FOO_ATTRIBUTE).is().date(new Date());

        filter.accept(visitor, null);
        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(1)).visit(isA(PropertyIsEqualTo.class), anyObject());
    }

    @Test
    public void after()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute(FOO_ATTRIBUTE).after().date(new Date());
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).is().after().date(new Date());
        filter.accept(visitor, null);

        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(2)).visit(isA(After.class), anyObject());
    }

    @Test
    public void before()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute(FOO_ATTRIBUTE).before().date(new Date());
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).is().before().date(new Date());
        filter.accept(visitor, null);

        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(2)).visit(isA(Before.class), anyObject());
    }

    @Test
    public void during()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute(FOO_ATTRIBUTE).during()
            .dates(new Date(), new Date(System.currentTimeMillis() + 10000000));
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).during().next(1000);
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).during().last(1000);
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).is().during()
            .dates(new Date(), new Date(System.currentTimeMillis() + 10000000));
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).is().during().next(1000);
        filter.accept(visitor, null);
        filter = builder.attribute(FOO_ATTRIBUTE).is().during().last(1000);
        filter.accept(visitor, null);

        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(6)).visit(isA(During.class), anyObject());
    }

    @Test
    public void like()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute(FOO_ATTRIBUTE).is().like().text("bar");
        filter.accept(visitor, null);

        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(1)).visit(isA(PropertyIsLike.class), anyObject());

        // TODO check case sensitivity

    }

    @Test
    public void likeCaseSensitive()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute(FOO_ATTRIBUTE).is().like().caseSensitiveText("bAr");
        filter.accept(visitor, null);

        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(1)).visit(isA(PropertyIsLike.class), anyObject());

        // TODO check case sensitivity
    }

    @Test
    public void likeFuzzy()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();
        Filter filter = builder.attribute(FOO_ATTRIBUTE).is().like().fuzzyText("bar");
        filter.accept(visitor, null);
        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(1)).visit(isA(PropertyIsLike.class), anyObject());

        // TODO check for fuzzy
    }

    @Test
    public void likeXPath()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.xpath("//foo").like().text("bar");
        filter.accept(visitor, null);
        filter = builder.xpath("//foo").is().like().text("bar");
        filter.accept(visitor, null);

        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(2)).visit(isA(PropertyIsLike.class), anyObject());
    }

    @Test
    public void xpathExists()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.xpath("//foo").exists();
        filter.accept(visitor, null);

        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(1)).visit(isA(PropertyIsLike.class), anyObject());
    }

    @Test( expected = IllegalArgumentException.class )
    public void likeXPathNull()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.xpath("//foo").is().like().text(null);
        filter.accept(visitor, null);

        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(1)).visit(isA(PropertyIsLike.class), anyObject());

        filter = builder.xpath(null).is().like().text(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void operatorBeforeNull()
    {
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute("something").before().date(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void operatorAfterNull()
    {
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute("something").after().date(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void operatorDuringNull()
    {
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute("something").during().dates(null, null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void operatorBeyondNull()
    {
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute("something").beyond().wkt(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void operatorWithinNull()
    {
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute("something").within().wkt(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void operatorWithinBufferNull()
    {
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute("something").withinBuffer().wkt(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void likeXPathInvalid()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.xpath("foo").is().like().text("bar");
    }

    @Test
    public void nearest()
    {
        // TODO
    }

    @Test
    public void contains()
    {
        // TODO
    }

    @Test
    public void intersects()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute(Metacard.GEOGRAPHY).intersecting().wkt(POINT_WKT);
        filter.accept(visitor, null);

        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(1)).visit(isA(Intersects.class), anyObject());
    }

    @Test
    public void beyond()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute(Metacard.GEOGRAPHY).beyond().wkt(POINT_WKT, 123.45d);
        filter.accept(visitor, null);
        filter = builder.attribute(Metacard.GEOGRAPHY).beyond().wkt(POINT_WKT);
        filter.accept(visitor, null);

        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(2)).visit(isA(Beyond.class), anyObject());
        // TODO check arguments
    }

    @Test
    public void within()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute(Metacard.GEOGRAPHY).within().wkt(POINT_WKT);
        filter.accept(visitor, null);
        filter = builder.attribute(Metacard.GEOGRAPHY).is().within().wkt(MULTIPOLYGON_WKT);
        filter.accept(visitor, null);

        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(2)).visit(isA(Within.class), anyObject());
    }

    @Test
    public void withinBuffer()
    {
        FilterVisitor visitor = spy(new DefaultFilterVisitor()
        {
        });
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute(Metacard.GEOGRAPHY).withinBuffer().wkt(POINT_WKT);
        filter.accept(visitor, null);
        filter = builder.attribute(Metacard.GEOGRAPHY).withinBuffer().wkt(POINT_WKT, 123.45d);
        filter.accept(visitor, null);
        filter = builder.attribute(Metacard.GEOGRAPHY).is().withinBuffer().wkt(POINT_WKT);
        filter.accept(visitor, null);
        filter = builder.attribute(Metacard.GEOGRAPHY).is().withinBuffer().wkt(POINT_WKT, 123.45d);
        filter.accept(visitor, null);

        InOrder inOrder = inOrder(visitor);
        inOrder.verify(visitor, times(4)).visit(isA(DWithin.class), anyObject());
    }

    /**
     *
     */
    @Test
    public void withinGeoTest()
    {
        FilterBuilder builder = new GeotoolsFilterBuilder();

        Filter filter = builder.attribute(Metacard.GEOGRAPHY).within().wkt(POINT_WKT);

        filter.accept(new DefaultFilterVisitor()
        {
            @Override
            public Object visit( Within filter, Object data )
            {
                Literal literalWrapper = (Literal) filter.getExpression2();

                Geometry geometry = (Geometry) literalWrapper.evaluate(null);
                geometry.getCentroid().getCoordinate();
                return super.visit(filter, data);
            }
        }, null);
    }

    // @Test
    public void test()
    {

        FilterBuilder builder = new GeotoolsFilterBuilder();
        Filter filter = builder.allOf(

        builder.allOf(builder.anyOf(builder.attribute(FOO_ATTRIBUTE).is().empty())), builder.allOf(builder
            .attribute(FOO_ATTRIBUTE).is().empty()), builder.allOf(

            builder.attribute("attr").is().greaterThan().number(new Short((short) 5)),
            builder.attribute("frequency").is().greaterThan().number(new Integer(5)),
            builder.attribute("frequency").is().greaterThan().number(new Long(5)),
            builder.attribute("frequency").is().greaterThan().number(new Float(5)),
            builder.attribute("frequency").is().greaterThan().number(new Double(5)),

            builder.attribute("frequency").is().greaterThanOrEqualTo().number(new Short((short) 5)),
            builder.attribute("frequency").is().greaterThanOrEqualTo().number(new Integer(5)),
            builder.attribute("frequency").is().greaterThanOrEqualTo().number(new Long(5)),
            builder.attribute("frequency").is().greaterThanOrEqualTo().number(new Float(5)),
            builder.attribute("frequency").is().greaterThanOrEqualTo().number(new Double(5)),

            builder.attribute("frequency").is().lessThan().number(new Short((short) 5)),
            builder.attribute("frequency").is().lessThan().number(new Integer(5)),
            builder.attribute("frequency").is().lessThan().number(new Long(5)),
            builder.attribute("frequency").is().lessThan().number(new Float(5)),
            builder.attribute("frequency").is().lessThan().number(new Double(5)),

            builder.attribute("frequency").is().lessThanOrEqualTo().number(new Short((short) 5)),
            builder.attribute("frequency").is().lessThanOrEqualTo().number(new Integer(5)),
            builder.attribute("frequency").is().lessThanOrEqualTo().number(new Long(5)),
            builder.attribute("frequency").is().lessThanOrEqualTo().number(new Float(5)),
            builder.attribute("frequency").is().lessThanOrEqualTo().number(new Double(5)),

            builder.attribute("frequency").is().between().numbers((short) 5, (short) 6),
            builder.attribute("frequency").is().between().numbers(new Integer(5), new Integer(5)),
            builder.attribute("frequency").is().between().numbers(new Long(5), new Long(5)),
            builder.attribute("frequency").is().between().numbers(new Float(5), new Float(5)),
            builder.attribute("frequency").is().between().numbers(new Double(5), new Double(5)),

            // (IN)EQUALITY
            builder.attribute(FOO_ATTRIBUTE).is().number(new Short((short) 5)),
            builder.attribute("frequency").is().number(new Integer(5)),
            builder.attribute("frequency").is().number(new Long(5)),
            builder.attribute("frequency").is().number(new Float(5)),
            builder.attribute("frequency").is().number(new Double(5)),
            builder.attribute("created").is().date(new Date()),
            builder.attribute("created").is().dateRange(new Date(), new Date()),
            builder.attribute("created").is().bool(true),
            builder.attribute("created").is().bytes(new byte[]
            {}),
            builder.attribute("created").is().wkt("POINT (10, 30)"),
            builder.attribute("created").is().text("POINT (10, 30)"),

            builder.attribute("frequency").is().equalTo().number(new Integer(5)),
            builder.attribute("frequency").is().equalTo().number(new Long(5)),
            builder.attribute("frequency").is().equalTo().number(new Float(5)),
            builder.attribute("frequency").is().equalTo().number(new Double(5)),
            builder.attribute("created").is().equalTo().date(new Date()),
            builder.attribute("created").is().equalTo().dateRange(new Date(), new Date()),
            builder.attribute("created").is().equalTo().bool(true),
            builder.attribute("created").is().equalTo().bytes(new byte[]
            {}),
            builder.attribute("created").is().equalTo().wkt("POINT (10, 30)"),
            builder.attribute("created").is().equalTo().text("POINT (10, 30)"),

            builder.attribute("frequency").is().notEqualTo().number(new Short((short) 5)),
            builder.attribute("frequency").is().notEqualTo().number(new Integer(5)),
            builder.attribute("frequency").is().notEqualTo().number(new Long(5)),
            builder.attribute("frequency").is().notEqualTo().number(new Float(5)),
            builder.attribute("frequency").is().notEqualTo().number(new Double(5)),
            builder.attribute("created").is().notEqualTo().date(new Date()),
            builder.attribute("created").is().notEqualTo().dateRange(new Date(), new Date()),
            builder.attribute("created").is().notEqualTo().bool(true),
            builder.attribute("created").is().notEqualTo().bytes(new byte[]
            {}),
            builder.attribute("created").is().notEqualTo().wkt("POINT (10, 30)"),
            builder.attribute("created").is().notEqualTo().text("POINT (10, 30)"),

            // NULLABLE
            builder.attribute("created").is().empty(),

            builder.attribute(FOO_ATTRIBUTE).is().number(50),

            // TEMPORAL
            builder.attribute("created").is().after().date(new Date()),
            builder.attribute("created").is().before().date(new Date()),
            builder.attribute("created").is().during().dates(new Date(), new Date()),
            builder.attribute("created").is().overlapping().dates(new Date(), new Date()),

            // CONTEXTUAL
            builder.attribute(FOO_ATTRIBUTE).is().like().text("bar*"),

            // implied equality
            builder.attribute("frequency").is().number((short) 5),
            builder.attribute("frequency").is().number(new Integer(5)),
            builder.attribute("frequency").is().number(new Long(5)),
            builder.attribute("frequency").is().number(new Float(5)),
            builder.attribute("frequency").is().number(new Double(5)),
            builder.attribute("created").is().date(new Date()),
            builder.attribute("created").is().dateRange(new Date(), new Date()),
            builder.attribute("created").is().bool(true),
            builder.attribute("created").is().bytes(new byte[]
            {}),
            builder.attribute("created").is().wkt("POINT (10, 30)"),
            builder.attribute("created").is().text("search*"),

            // SPATIAL
            builder.attribute("location").is().intersecting().wkt("POINT (10, 30)"),
            builder.attribute("location").is().containing().wkt("POINT (10, 30)"),
            builder.attribute("location").is().beyond().wkt("POINT (10, 30)"),
            builder.attribute("location").is().within().wkt("POINT (10, 30)"),
            builder.attribute("location").is().withinBuffer().wkt("POINT (10, 30)", (long) 5),

            // NO IS

            builder.attribute("frequency").greaterThan().number(new Short((short) 5)), builder.attribute("frequency")
                .greaterThan().number(new Integer(5)),
            builder.attribute("frequency").greaterThan().number(new Long(5)), builder.attribute("frequency")
                .greaterThan().number(new Float(5)),
            builder.attribute("frequency").greaterThan().number(new Double(5)),

            builder.attribute("frequency").greaterThanOrEqualTo().number(new Short((short) 5)),
            builder.attribute("frequency").greaterThanOrEqualTo().number(new Integer(5)), builder
                .attribute("frequency").greaterThanOrEqualTo().number(new Long(5)), builder.attribute("frequency")
                .greaterThanOrEqualTo().number(new Float(5)), builder.attribute("frequency").greaterThanOrEqualTo()
                .number(new Double(5)),

            builder.attribute("frequency").lessThan().number(new Short((short) 5)), builder.attribute("frequency")
                .lessThan().number(new Integer(5)),
            builder.attribute("frequency").lessThan().number(new Long(5)),
            builder.attribute("frequency").lessThan().number(new Float(5)),
            builder.attribute("frequency").lessThan().number(new Double(5)),

            builder.attribute("frequency").lessThanOrEqualTo().number(new Short((short) 5)),
            builder.attribute("frequency").lessThanOrEqualTo().number(5),
            builder.attribute("frequency").lessThanOrEqualTo().number(5L),
            builder.attribute("frequency").lessThanOrEqualTo().number(5f),
            builder.attribute("frequency").lessThanOrEqualTo().number(5d),

            builder.attribute("frequency").between().numbers((short) 5, (short) 6),
            builder.attribute("frequency").between().numbers(5, 6),
            builder.attribute("frequency").between().numbers(5L, 6L),
            builder.attribute("frequency").between().numbers(5f, 6f),
            builder.attribute("frequency").between().numbers(5d, 6d),

            // (IN)EQUALITY
            builder.attribute("frequency").number((short) 5), builder.attribute("frequency").number(5), builder
                .attribute("frequency").number(5L),
            builder.attribute("frequency").number(5f),
            builder.attribute("frequency").number(5d),
            builder.attribute("created").date(new Date()),
            builder.attribute("created").dateRange(new Date(), new Date()),
            builder.attribute("created").bool(true),
            builder.attribute("created").bytes(new byte[]
            {}),
            builder.attribute("created").wkt("POINT (10, 30)"),
            builder.attribute("created").text("POINT (10, 30)"),

            builder.attribute("frequency").notEqualTo().number(new Short((short) 5)),
            builder.attribute("frequency").notEqualTo().number(new Integer(5)),
            builder.attribute("frequency").notEqualTo().number(new Long(5)),
            builder.attribute("frequency").notEqualTo().number(new Float(5)),
            builder.attribute("frequency").notEqualTo().number(new Double(5)),
            builder.attribute("created").notEqualTo().date(new Date()),
            builder.attribute("created").notEqualTo().dateRange(new Date(), new Date()),
            builder.attribute("created").notEqualTo().bool(true),
            builder.attribute("created").notEqualTo().bytes(new byte[]
            {}),
            builder.attribute("created").notEqualTo().wkt("POINT (10, 30)"),
            builder.attribute("created").notEqualTo().text("POINT (10, 30)"),

            // NULLABLE
            builder.attribute("created").empty(),

            // TEMPORAL
            builder.attribute("created").after().date(new Date()),
            builder.attribute("created").before().date(new Date()),
            builder.attribute("created").during().dates(new Date(), new Date()),
            builder.attribute("created").overlapping().dates(new Date(), new Date()),

            // CONTEXTUAL
            builder.attribute(FOO_ATTRIBUTE).like().text("bar*"),

            // SPATIAL
            builder.attribute("location").intersecting().wkt("POINT (10, 30)"), builder.attribute("location")
                .containing().wkt("POINT (10, 30)"), builder.attribute("location").beyond().wkt("POINT (10, 30)"),
            builder.attribute("location").within().wkt("POINT (10, 30)"), builder.attribute("location").withinBuffer()
                .wkt("POINT (10, 30)", (long) 5),

            builder.xpath("//blah/blah").exists(),

            builder.xpath("//foo/bar").like().text("bat"), builder.xpath("//foo/bar").like().caseSensitiveText("bat"),
            builder.xpath("//foo/bar").like().fuzzyText("bat"), builder.xpath("//blah/blah").is().like().text("bat"),
            builder.xpath("//blah/blah").is().like().caseSensitiveText("bat"), builder.xpath("//blah/blah").is().like()
                .fuzzyText("bat"),

            builder.attribute(FOO_ATTRIBUTE).like().fuzzyText("bar"), builder.attribute(FOO_ATTRIBUTE).like()
                .caseSensitiveText("bar"), builder.attribute(FOO_ATTRIBUTE).like().text("bar"),
            builder.attribute(FOO_ATTRIBUTE).is().like().fuzzyText("bar"), builder.attribute(FOO_ATTRIBUTE).is().like()
                .caseSensitiveText("bar"), builder.attribute(FOO_ATTRIBUTE).is().like().text("bar"),

            builder.attribute(FOO_ATTRIBUTE).nearestTo().wkt("POINT(10,10)"),

            // Relative Time
            builder.attribute("created").during().last(500L), builder.attribute("created").during().next(500L)

        ));

    }
}
