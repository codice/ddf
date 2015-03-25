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
package ddf.catalog.source.solr;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class MockMetacard extends MetacardImpl {

    public static final String DEFAULT_TITLE = "Flagstaff";

    public static final String DEFAULT_VERSION = "mockVersion";

    public static final String DEFAULT_TYPE = "simple";

    public static final String DEFAULT_LOCATION = "POINT (1 0)";

    public static final String DEFAULT_SOURCE_ID = "ddf";

    private static final long serialVersionUID = -189776439741244547L;

    public MockMetacard(String metadata) {
        // make a simple metacard
        this.setCreatedDate(Calendar.getInstance().getTime());
        this.setEffectiveDate(Calendar.getInstance().getTime());
        this.setExpirationDate(Calendar.getInstance().getTime());
        this.setModifiedDate(Calendar.getInstance().getTime());
        this.setMetadata(metadata);
        this.setContentTypeName(DEFAULT_TYPE);
        this.setContentTypeVersion(DEFAULT_VERSION);
        this.setLocation(DEFAULT_LOCATION);
        byte[] buffer = {-86};
        this.setThumbnail(buffer);
        // this.setSourceId(DEFAULT_SOURCE_ID) ;
        this.setTitle(DEFAULT_TITLE);
        this.setSecurity(new HashMap<String, List<String>>());
    }

    public static List<String> toStringList(List<Metacard> cards) {

        ArrayList<String> stringList = new ArrayList<String>();

        for (Metacard m : cards) {
            stringList.add(m.getId());
        }

        return stringList;
    }
}
