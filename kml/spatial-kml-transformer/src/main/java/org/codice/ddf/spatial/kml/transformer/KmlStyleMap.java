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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;

/**
 * Bean to provide a mapping based on {@link Metacard} {@link Attribute}s to supply custom style
 * configuration.
 * 
 * @author kcwire
 * 
 */
public class KmlStyleMap {

    private List<KmlStyleMapEntry> styleMap = new ArrayList<KmlStyleMapEntry>();

    private static final Logger LOGGER = LoggerFactory.getLogger(KmlStyleMap.class);
    
    public KmlStyleMap() {
    }

    public void addMapEntry(KmlStyleMapEntry entry) {
        if (entry != null) {
            LOGGER.debug("Adding KmlStyleMapEntry");
            styleMap.add(entry);
        }
    }

    public void removeMapEntry(KmlStyleMapEntry entry){
        if (entry != null) {
            LOGGER.debug("Removing KmlStyleMapEntry with {}, {}", entry.getAttributeName(),
                    entry.getAttributeValue());
            styleMap.remove(entry);
        }
    }

    public String getStyleForMetacard(Metacard metacard) {
        for (KmlStyleMapEntry mapEntry : styleMap) {
            if (mapEntry.metacardMatch(metacard)) {
                return mapEntry.getStyleUrl();
            }
        }
        return "";
    }
}
