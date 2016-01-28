/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.data.dynamic.api;

import java.util.List;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;

public interface DynamicMetacard extends Metacard, MetacardType {
    // the base metacard for dynamic metacards
    String DYNAMIC = "ddf";
    // Constants used in addition to those from Metacard
    String LOCATION = "location";
    String SOURCE_ID = "sourceId";
    String METACARD_TYPE = "metacardType";
    String CONTENT_TYPE_NAMESPACE = "contentTypeNamespace";
    String CONTENT_TYPE_NAME = "contentTypeName";
    String CONTENT_TYPE_VERSION = "contentTypeVersion";

    void addAttribute(String name, Object value);

    void setAttribute(String name, Object value);

    List<String> getMetacardTypes();

    boolean isType(String name);
}
