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
 **/
package ddf.catalog.nato.stanag4559.common;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.impl.ContentTypeImpl;

public class Stanag4559Constants {

    public static final String STANAG_VERSION = "STANAG 4559";

    public static final String NSIL_ALL_VIEW = "NSIL_ALL_VIEW";

    public static final List<String> CONTENT_STRINGS = Arrays.asList("COLLECTION/EXPLOITATION PLAN",
            "DOCUMENT",
            "GEOGRAPHIC AREA OF INTEREST",
            "GMTI",
            "IMAGERY",
            "INTELLIGENCE REQUIREMENT",
            "MESSAGE",
            "OPERATIONAL",
            "ROLES",
            "ORBAT",
            "REPORT",
            "RFI",
            "SYSTEM ASSIGNMENTS",
            "SYSTEM DEPLOYMENT STATUS",
            "SYSTEM SPECIFICATIONS",
            "TACTICAL SYMBOL",
            "TASK",
            "TDL DATA",
            "VIDEO");

    public static final Set<ContentType> CONTENT_TYPES = setContentStrings();

    public static Set<ContentType> setContentStrings() {
        Set<ContentType> contentTypes = new HashSet<>();

        for (String string : CONTENT_STRINGS) {
            contentTypes.add(new ContentTypeImpl(string, STANAG_VERSION));
        }
        return contentTypes;
    }
}