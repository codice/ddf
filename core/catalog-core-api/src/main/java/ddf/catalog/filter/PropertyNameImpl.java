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
import org.opengis.filter.expression.PropertyName;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Simple implementation of filter that does not depend on GeoTools. Please use
 * {@link FilterBuilder} instead to create filters.
 * 
 * @deprecated As of release 2.3.0, replaced by
 *             ddf.catalog.filter.impl.PropertyNameImpl
 */
@Deprecated
public class PropertyNameImpl implements PropertyName {

    private String propertyName;

    /**
     * Create property name.
     * 
     * @param propertyName
     *            property name
     */
    public PropertyNameImpl(String propertyName) {
        this.propertyName = propertyName;
    }

    public Object evaluate(Object object) {
        return evaluate(object, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T evaluate(Object object, Class<T> context) {
        if (object == null) {
            return null;
        }

        if (propertyName.getClass().equals(context)) {
            return (T) propertyName;
        }

        if (String.class.equals(context)) {
            return context.cast(object.toString());
        }

        return null;
    }

    public Object accept(ExpressionVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    public String getPropertyName() {
        return propertyName;
    }

    public NamespaceSupport getNamespaceContext() {
        // Not supported
        return null;
    }

}
