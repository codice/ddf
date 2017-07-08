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
package org.codice.solr.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

public class ExpressionValueVisitorTest {

    @Test
    public void testLiteral() {
        ExpressionValueVisitor visitor = new ExpressionValueVisitor();
        Literal literal = mock(Literal.class);
        when(literal.getValue()).thenReturn("value");
        assertThat(visitor.visit(literal, null), equalTo("value"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testLiteralEmptyExpression() {
        ExpressionValueVisitor visitor = new ExpressionValueVisitor();
        Literal literal = mock(Literal.class);
        when(literal.getValue()).thenReturn(null);
        visitor.visit(literal, null);
    }

    @Test
    public void testPropertyName() {
        ExpressionValueVisitor visitor = new ExpressionValueVisitor();
        PropertyName prop = mock(PropertyName.class);
        when(prop.getPropertyName()).thenReturn("name");
        assertThat(visitor.visit(prop, null), equalTo("name"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPropertyNameEmptyExpression() {
        ExpressionValueVisitor visitor = new ExpressionValueVisitor();
        PropertyName prop = mock(PropertyName.class);
        when(prop.getPropertyName()).thenReturn("");
        visitor.visit(prop, null);
    }
}
