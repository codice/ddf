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

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import ddf.catalog.data.impl.MetacardImpl;

public class MockMetacard extends MetacardImpl {
    public static final String DEFAULT_TITLE = "Flagstaff";

    public static final String DEFAULT_VERSION = "mockVersion";

    public static final String DEFAULT_TYPE = "simple";

    public static final String DEFAULT_LOCATION = "POINT (1 0)";

    public static final String DEFAULT_SOURCE_ID = "ddf";

    public static final String DEFAULT_METADATA = "<xml>Metadata</xml>";

    public static final Date DEFAULT_DATE = Calendar.getInstance().getTime();

    private static final long serialVersionUID = -189776439741244547L;

    public MockMetacard() {
        this(null, null);
    }

    public MockMetacard(String name, Serializable value) {
        // make a simple metacard
        this.setCreatedDate(DEFAULT_DATE);
        this.setEffectiveDate(DEFAULT_DATE);
        this.setExpirationDate(DEFAULT_DATE);
        this.setModifiedDate(DEFAULT_DATE);
        this.setMetadata(DEFAULT_METADATA);
        this.setContentTypeName(DEFAULT_TYPE);
        this.setContentTypeVersion(DEFAULT_VERSION);
        this.setLocation(DEFAULT_LOCATION);
        byte[] buffer = {-86};
        this.setThumbnail(buffer);
        this.setSourceId(DEFAULT_SOURCE_ID);
        this.setTitle(DEFAULT_TITLE);
        this.setSecurity(new HashMap<String, List<String>>());
        this.setType(new MockMetacardType());
        if (name != null && value != null) {
            this.setAttribute(name, value);
        }
    }
}
