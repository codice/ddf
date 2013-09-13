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

import org.opengis.filter.Filter;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;

/**
 * Builds XPath {@link Filter}s
 * 
 * @author Michael Menousek, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public interface XPathBuilder extends XPathBasicBuilder {

    /**
     * Builds a {@link Filter} that matches {@link Metacard}s where the XML node indicated by the
     * XPath exists. Searches across all {@link Metacard} {@link Attribute}s of type
     * {@link AttributeFormat#XML}.
     * 
     * @return {@link Filter} for indicated XPath
     */
    public Filter exists();

    /**
     * Continue building the {@link Filter} with an implied EQUALS
     * 
     * @return {@link XPathBasicBuilder}, to continue building the {@link Filter}
     */
    public XPathBasicBuilder is();

}
