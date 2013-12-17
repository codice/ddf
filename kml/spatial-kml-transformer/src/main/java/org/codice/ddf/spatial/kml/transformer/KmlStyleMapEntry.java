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
package org.codice.ddf.spatial.kml.transformer;

import ddf.catalog.data.Metacard;

/**
 * Used to define a mapping between {@link Metacard} {@link Attribute}s and a URL to the KML Style.
 * 
 * @author Keith C Wire
 * 
 */
public interface KmlStyleMapEntry {

    String getAttributeName();

    String getAttributeValue();

    String getStyleUrl();

    /**
     * Determines if a {@link Metacard} has an {@link Attribute} and Value that match this
     * {@link KmlStyleMapEntry}.
     * 
     * @param metacard
     *            - the {@link Metacard} to check for the {@link Attribute}
     * @return - true if a match was found, false otherwise
     */
    boolean metacardMatch(Metacard metacard);
}
