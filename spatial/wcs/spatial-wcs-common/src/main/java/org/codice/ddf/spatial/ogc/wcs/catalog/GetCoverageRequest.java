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
package org.codice.ddf.spatial.ogc.wcs.catalog;

public class GetCoverageRequest {

    private String request = WcsConstants.GET_COVERAGE;

    private String service = WcsConstants.WCS;

    private String version = WcsConstants.VERSION_1_0_0;

    private String id;

    private String format;

    public GetCoverageRequest(String id, String format) {
        this.id = id;
        this.format = format;
    }

    public String getId() {
        return id;
    }

    public String getCoverageFormat() {
        return format;
    }

}
