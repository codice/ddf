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
package ddf.catalog.transformer.response.query.atom;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardImpl;

public class MetacardStub extends MetacardImpl {

    public static final String DEFAULT_TITLE = "Flagstaff";

    public static final String DEFAULT_VERSION = "mockVersion";

    public static final String DEFAULT_TYPE = "simple";

    public static final String DEFAULT_LOCATION = "POINT (13.3 56.3)";

    public static final String DEFAULT_SOURCE_ID = "ddf";

    public static final HashMap<String, String> DEFAULT_SECURITY_ALL = new HashMap<String, String>();

    public static final HashMap<String, String> DEFAULT_SECURITY_ONE = new HashMap<String, String>();

    private static final long serialVersionUID = -189776439741244547L;

    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardStub.class);

    public MetacardStub(String metadata) {
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
        try {
            this.setResourceURI(new URI("http://example.com"));
        } catch (URISyntaxException e) {
            LOGGER.error("URI Syntax error", e);
        }
    }

    public static List<String> toStringList(List<Metacard> cards) {

        ArrayList<String> stringList = new ArrayList<String>();

        for (Metacard m : cards) {
            stringList.add(m.getId());
        }

        return stringList;
    }
}
