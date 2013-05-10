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
package ddf.catalog.filter;

import org.opengis.filter.Filter;

import ddf.catalog.data.Attribute;

/**
 * Starts the fluent API to create {@link Filter} based on a particular
 * {@link Attribute}
 * 
 * @author Michael Menousek, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public interface AttributeBuilder extends ExpressionBuilder {

    /**
     * Continue building the {@link Filter} with an implied equality operator.
     * Also used for syntactic completeness (readability).
     * 
     * @return ExpressionBuilder to continue building this {@link Filter}
     */
    public abstract ExpressionBuilder is();

}