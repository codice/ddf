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

package org.codice.ddf.spatial.ogc.wfs.catalog.common;

import java.util.ArrayList;
import java.util.List;

import ddf.catalog.data.Metacard;

public class WfsFeatureCollection {
    // Should reconstruct the FeatureCollection as defined in the schema

    // TODO namespaces? schemalocations?
    // TODO boundedBy?

    // XXX - IDEAS for methods to add to this object
    // Method to retrieve a set of namespaces
    // Method to get a bounding box of all metacards
    // Provide a map of the metacardTypes needed to marshal / unmarshal

    private List<Metacard> featureMembers = new ArrayList<Metacard>();

    public List<Metacard> getFeatureMembers() {
        return featureMembers;
    }

    public void setFeatureMembers(List<Metacard> featureMembers) {
        this.featureMembers = featureMembers;
    }

}
