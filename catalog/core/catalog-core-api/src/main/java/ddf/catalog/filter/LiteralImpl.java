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
package ddf.catalog.filter;

import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.Literal;

/**
 * Simple implementation of filter that does not depend on GeoTools. Please use
 * {@link FilterBuilder} instead to create filters.
 * 
 * @author ddf.isgs@lmco.com
 * 
 */
public class LiteralImpl implements Literal {

    private Object value;

    /**
     * Create literal from object value.
     * 
     * @param value
     *            object value
     */
    public LiteralImpl(Object value) {
        this.value = value;
    }

    public Object evaluate(Object object) {
        return evaluate(object, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T evaluate(Object object, Class<T> context) {
        if (object == null) {
            return null;
        }

        if (value.getClass().equals(context)) {
            return (T) value;
        }

        if (String.class.equals(context)) {
            return context.cast(object.toString());
        }

        return null;
    }

    public Object accept(ExpressionVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    public Object getValue() {
        return value;
    }

}
