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

import org.apache.commons.lang.StringUtils;
import org.geotools.filter.visitor.NullExpressionVisitor;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

public class ExpressionValueVisitor extends NullExpressionVisitor {
    @Override
    public Object visit(Literal expression, Object extraData) {
        Object value = expression.getValue();
        if (value == null) {
            throw new UnsupportedOperationException("PropertyName and Literal is required for search.");
        }
        return value;
    }

    @Override
    public Object visit(PropertyName expr, Object extraData) {
        String value = expr.getPropertyName();
        if (StringUtils.isBlank(value)) {
            throw new UnsupportedOperationException("PropertyName and Literal is required for search.");
        }
        return value;
    }
}
