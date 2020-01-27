/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.source.solr.provider;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MockMetacard extends MetacardImpl {

  public static final String DEFAULT_TITLE = "Flagstaff";

  public static final String DEFAULT_VERSION = "mockVersion";

  public static final String DEFAULT_TYPE = "simple";

  public static final String DEFAULT_LOCATION = "POINT (1 0)";

  public static final String DEFAULT_TAG = "resource";

  public static final byte[] DEFAULT_THUMBNAIL = {-86};

  private static final long serialVersionUID = -189776439741244547L;

  public MockMetacard(String metadata, MetacardType type, Calendar calendar) {
    super(type);
    // make a simple metacard
    this.setCreatedDate(calendar.getTime());
    this.setEffectiveDate(calendar.getTime());
    this.setExpirationDate(calendar.getTime());
    this.setModifiedDate(calendar.getTime());
    this.setMetadata(metadata);
    this.setContentTypeName(DEFAULT_TYPE);
    this.setContentTypeVersion(DEFAULT_VERSION);
    this.setLocation(DEFAULT_LOCATION);
    this.setThumbnail(DEFAULT_THUMBNAIL);
    this.setTitle(DEFAULT_TITLE);
    this.setSecurity(new HashMap<>());
    this.setTags(Collections.singleton(DEFAULT_TAG));
  }

  public MockMetacard(String metadata, MetacardType type) {
    this(metadata, type, Calendar.getInstance());
  }

  public MockMetacard(String metadata) {
    this(metadata, MetacardImpl.BASIC_METACARD);
  }

  public static List<String> toStringList(List<Metacard> cards) {

    ArrayList<String> stringList = new ArrayList<>();

    for (Metacard m : cards) {
      stringList.add(m.getId());
    }

    return stringList;
  }
}
