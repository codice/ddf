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
package ddf.catalog.operation;

import static org.junit.Assert.*;

import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.FilterFactoryImpl;
import org.junit.BeforeClass;
import org.junit.Test;

import ddf.catalog.data.Metacard;

/**
 * @author willisod
 * 
 */
public class TestQueryImpl {
    private static Filter filter1 = null;

    private static Filter filter2 = null;

    private static final String DEFAULT_TEST_SINGLE_WILDCARD = "?";

    private static final String DEFAULT_TEST_WILDCARD = "*";

    /**
     * Create the filter2 one time to use for all of the tests
     */
    @BeforeClass
    public static void setUp() {

        FilterFactory filterFactory = new FilterFactoryImpl();

        // Dummy filter copied from another test
        filter1 = filterFactory.like(filterFactory.property(Metacard.METADATA),
                "million", DEFAULT_TEST_WILDCARD, DEFAULT_TEST_SINGLE_WILDCARD,
                "^", false);

        filter2 = filterFactory.like(filterFactory.property(Metacard.METADATA),
                "zillion", DEFAULT_TEST_WILDCARD, DEFAULT_TEST_SINGLE_WILDCARD,
                "^", false);
    }

    /**
     * Test method for
     * {@link ddf.catalog.operation.QueryImpl#QueryImpl(org.opengis.filter.Filter)}
     * .
     */
    @Test
    public void testQueryImplFilter() {
        QueryImpl qi = new QueryImpl(filter1);

        assertEquals(filter1, qi.getFilter());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQueryImpleFilterWithNullFilter() {
        Filter filter = null;

        QueryImpl qi = new QueryImpl(filter);
    }

    /**
     * Test method for
     * {@link ddf.catalog.operation.QueryImpl#setFilter(org.opengis.filter.Filter)}
     * .
     */
    @Test
    public void testSetFilter() {
        QueryImpl qi = new QueryImpl(filter1);

        assertEquals(filter1, qi.getFilter());

        qi.setFilter(filter2);

        assertEquals(filter2, qi.getFilter());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetFilterWithNullFilter() {
        FilterFactory filterFactory = new FilterFactoryImpl();

        // Dummy filter copied from another test
        Filter filter1 = filterFactory
                .like(filterFactory.property(Metacard.METADATA), "million",
                        DEFAULT_TEST_WILDCARD, DEFAULT_TEST_SINGLE_WILDCARD,
                        "^", false);

        Filter filter2 = null;

        QueryImpl qi = new QueryImpl(filter1);

        assertEquals(filter1, qi.getFilter());

        qi.setFilter(filter2);
    }

}
