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
package ddf.catalog.source.solr;

import java.util.Date;

import org.geotools.filter.FilterFactoryImpl;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;

public class TestSolrFilterBuilder extends GeotoolsFilterBuilder {

    private FilterFactory factory = new FilterFactoryImpl();

    public Filter dateLessThan(String attribute, Date literal) {
        return factory.less(factory.property(attribute), factory.literal(literal));
    }

    public Filter dateLessThanOrEqual(String attribute, Date literal) {
        return factory.lessOrEqual(factory.property(attribute), factory.literal(literal));
    }

    public Filter dateGreaterThan(String attribute, Date literal) {
        return factory.greater(factory.property(attribute), factory.literal(literal));
    }

    public Filter dateGreaterThanOrEqual(String attribute, Date literal) {
        return factory.greaterOrEqual(factory.property(attribute), factory.literal(literal));
    }
}
