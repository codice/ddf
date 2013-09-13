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
package ddf.catalog.filter.proxy.builder;

import org.opengis.filter.Filter;

import ddf.catalog.filter.ContextualExpressionBuilder;
import ddf.catalog.filter.XPathBuilder;

public class GeotoolsXPathBuilder extends GeotoolsBuilder implements XPathBuilder {

    public GeotoolsXPathBuilder(String xPathSelector) {
        if (xPathSelector == null
                || (xPathSelector.indexOf('/') == -1 && xPathSelector.indexOf('@') == -1)) {
            throw new IllegalArgumentException(
                    "XPath must contain either / or @ to be recognized by the system");
        }
        setAttribute(xPathSelector);
        setOperator(Operator.LIKE);
        setSecondaryValue(false);
    }

    @Override
    public Filter exists() {
        return build("");
    }

    @Override
    public ContextualExpressionBuilder like() {
        return new GeotoolsContextualExpressionBuilder(this);
    }

    @Override
    public XPathBuilder is() {
        return this;
    }

}
