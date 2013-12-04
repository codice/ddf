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

import org.opengis.annotation.XmlElement;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

/**
 * Simple implementation of filter that does not depend on GeoTools. Please use
 * {@link FilterBuilder} instead to create filters.
 * 
 * @deprecated As of release 2.3.0, replaced by
 *             ddf.catalog.filter.impl.PropertyIsEqualToEqual
 * 
 */
@Deprecated
public class PropertyIsEqualToLiteral implements PropertyIsEqualTo {

    private PropertyName propertyName;

    private Literal literal;

    /**
     * Create PropertyIsEqualTo filter with property name as expression 1 and literal as expression
     * 2.
     * 
     * @param propertyName
     *            property name
     * @param literal
     *            literal
     */
    public PropertyIsEqualToLiteral(PropertyName propertyName, Literal literal) {
        this.propertyName = propertyName;
        this.literal = literal;
    }

    @XmlElement("expression")
    public Expression getExpression1() {
        return propertyName;
    }

    @XmlElement("expression")
    public Expression getExpression2() {
        return literal;
    }

    @XmlElement("matchCase")
    public boolean isMatchingCase() {
        return true;
    }

    @XmlElement("matchAction")
    public MatchAction getMatchAction() {
        return MatchAction.ANY;
    }

    public boolean evaluate(Object object) {
        if (object == null || literal == null || literal.getValue() == null) {
            return false;
        }

        if (object.equals(literal.getValue())) {
            return true;
        }

        return false;
    }

    public Object accept(FilterVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

}
