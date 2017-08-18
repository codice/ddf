/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.source.opensearch.impl;

import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;

public class OpenSearchFilterVisitorObject {

    private ContextualSearch contextualSearch;

    private TemporalFilter temporalSearch;

    private SpatialFilter spatialSearch;

    private String id;

    private NestedTypes currentNest = null;

    public NestedTypes getCurrentNest() {
        return currentNest;
    }

    public void setCurrentNest(NestedTypes currentNest) {
        this.currentNest = currentNest;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setContextualSearch(ContextualSearch contextualSearch) {
        this.contextualSearch = contextualSearch;
    }

    public void setSpatialSearch(SpatialFilter spatialSearch) {
        this.spatialSearch = spatialSearch;
    }

    public ContextualSearch getContextualSearch() {
        return contextualSearch;
    }

    public TemporalFilter getTemporalSearch() {
        return temporalSearch;
    }

    public SpatialFilter getSpatialSearch() {
        return spatialSearch;
    }

    public String getId() {
        return id;
    }

    public void setTemporalSearch(TemporalFilter temporalSearch) {
        this.temporalSearch = temporalSearch;
    }

}
