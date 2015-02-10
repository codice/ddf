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

import java.util.Arrays;
import java.util.List;

import org.geotools.filter.FilterFactoryImpl;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Not;
import org.opengis.filter.Or;

import ddf.catalog.filter.AttributeBuilder;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.XPathBuilder;

/**
 * @author Michael Menousek, Lockheed Martin
 * @author ddf.isgs@lmco.com
 * 
 */
public class GeotoolsFilterBuilder implements FilterBuilder {
    private FilterFactory factory = new FilterFactoryImpl();

    // public Filter cql(String cql) {
    // // TODO
    // return null;
    // }
    //
    // public Filter xml(String xml) {
    // // TODO
    // return null;
    // }

    public And allOf(Filter... filters) {
        return allOf(Arrays.asList(filters));
    }

    public Or anyOf(Filter... filters) {
        return anyOf(Arrays.asList(filters));
    }

    public AttributeBuilder attribute(String string) {
        return new GeotoolsAttributeBuilder(string);
    }

    @Override
    public XPathBuilder xpath(String xPathSelector) {
        return new GeotoolsXPathBuilder(xPathSelector);
    }

    @Override
    public Not not(Filter filter) {
        return factory.not(filter);
    }

    @Override
    public And allOf(List<Filter> filters) {
        return factory.and(filters);
    }

    @Override
    public Or anyOf(List<Filter> filters) {
        return factory.or(filters);
    }

}