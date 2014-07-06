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

import java.util.List;

import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;

/**
 * <p>
 * Service interface with which implementations will register in the OSGi Registry for use by
 * components needing to build {@link Filter}s in an easy way without a dependence on a particular
 * {@link Filter} implementation.
 * </p>
 * <p>
 * This method of creating {@link Filter} instances is strongly recommended.
 * </p>
 * 
 * @author Michael Menousek, Lockheed Martin
 * @author ddf.isgs@lmco.com
 * 
 */
public interface FilterBuilder {

    /**
     * <p>
     * Begin creating a {@link Filter} that will match {@link Metacard}s based on {@link Attribute}s
     * with the given name.
     * </p>
     * <p>
     * <em>Note</em> Because the Catalog's {@link Filter} profile uses attribute names and XPath
     * selectors interchangeable, the provided {@link Attribute} name must not include the forward
     * slash (/) or ampersand (@) characters, which are reserved for XPath selectors (see
     * {@link #xpath(String)}.
     * </p>
     * <p>
     * XPath {@link Filter}s will match on all {@link Metacard} {@link Attribute}s that have a
     * format of {@link AttributeFormat#XML} and match the XPath selector.
     * </p>
     * 
     * @param attributeName
     *            must not include "/" or "@"
     * @return {@link XPathBuilder} to continue building an XPath {@link Filter}
     */
    AttributeBuilder attribute(String attributeName);

    /**
     * <p>
     * Begin creating a {@link Filter} that will match {@link Metacard}s based on values selected
     * via an XPath selector.
     * </p>
     * <p>
     * <em>Note</em> Because the Catalog's {@link Filter} profile uses attribute names and XPath
     * selectors interchangeable, XPath must include either the forward slash (/) or ampersand (@)
     * character to indicate use of XPath.
     * </p>
     * <p>
     * XPath {@link Filter}s will match on all {@link Metacard} {@link Attribute}s that have a
     * format of {@link AttributeFormat#XML} and match the XPath selector.
     * </p>
     * 
     * @param xPathSelector
     *            must include "/" or "@"
     * @return {@link XPathBuilder} to continue building an XPath {@link Filter}
     */
    XPathBuilder xpath(String xPathSelector);

    /**
     * Create a new {@link Filter} that requires that all the provided {@link Filter}s be satisfied.
     * 
     * @param filters
     *            one or more {@link Filter}s
     * @return {@link And} {@link Filter}
     */
    And allOf(Filter... filters);

    /**
     * Create a new {@link Filter} that requires that all the provided {@link Filter}s be satisfied.
     * 
     * @param filters
     *            {@link List} of {@link Filter}s
     * @return {@link And} {@link Filter}
     */
    And allOf(List<Filter> filters);

    /**
     * Create a new {@link Filter} that requires at least one of the provided {@link Filter}s be
     * satisfied.
     * 
     * @param filters
     *            {@link List} of {@link Filter}s
     * @return {@link And} {@link Filter}
     */
    Or anyOf(Filter... filters);

    /**
     * Create a new {@link Filter} that requires at least one of the provided {@link Filter}s be
     * satisfied.
     * 
     * @param filters
     *            one or more {@link Filter}s
     * @return {@link And} {@link Filter}
     */
    Or anyOf(List<Filter> filters);

    /**
     * Create a Filter that matches on {@link Metacard}s that do <em>not</em> match the provided
     * {@link Filter}
     * 
     * @param filter
     *            the filter that should not be matched
     * @return {@link Not} {@link Filter}
     */
    Not not(Filter filter);

    // Filter cql(String cqlFilter);
    // Filter xml(String xmlFilter);
}
