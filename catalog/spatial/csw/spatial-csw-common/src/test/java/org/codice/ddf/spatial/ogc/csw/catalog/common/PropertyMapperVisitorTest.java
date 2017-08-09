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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.HashMap;
import java.util.Map;

import org.geotools.filter.AttributeExpression;
import org.geotools.filter.FilterFactoryImpl;
import org.junit.Test;
import org.opengis.filter.expression.PropertyName;

import ddf.catalog.filter.impl.PropertyNameImpl;

public class PropertyMapperVisitorTest {

    @Test
    public void testPropertyMapperVisit() {
        Map<String, String> map = new HashMap<>();
        map.put("prop", "newProp");
        PropertyMapperVisitor mapper = new PropertyMapperVisitor(map);
        PropertyName propertyName = new PropertyNameImpl("prop");
        AttributeExpression exp = (AttributeExpression) mapper.visit(propertyName,
                new FilterFactoryImpl());
        assertThat(exp.getPropertyName(), equalTo("newProp"));
    }

    @Test
    public void testPropertyMapperVisitNullProperty() {
        Map<String, String> map = new HashMap<>();
        PropertyMapperVisitor mapper = new PropertyMapperVisitor(map);

        AttributeExpression exp = (AttributeExpression) mapper.visit((PropertyName) null,
                new FilterFactoryImpl());
        assertThat(exp, nullValue());
    }

    @Test
    public void testPropertyMapperVisitPassThrough() {
        Map<String, String> map = new HashMap<>();
        map.put("prop", "newProp");
        PropertyMapperVisitor mapper = new PropertyMapperVisitor(map);
        PropertyName propertyName = new PropertyNameImpl("myprop");
        AttributeExpression exp = (AttributeExpression) mapper.visit(propertyName,
                new FilterFactoryImpl());
        assertThat(exp.getPropertyName(), equalTo("myprop"));
    }
}
