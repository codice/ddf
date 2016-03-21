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
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.thoughtworks.xstream.io.path.Path;

public class XstreamPathValueTracker {

    private MultivaluedMap<Path, String> pathMap = new MultivaluedHashMap<>();

    public void buildPaths(List<Path> paths) {
        paths.forEach(path -> pathMap.add(path, null));
    }

    public String getPathValue(Path key) {
        return pathMap.getFirst(key);
    }

    public List<String> getAllValues(Path key) {
        return pathMap.get(key);
    }

    public Set<Path> getPaths() {
        return pathMap.keySet();
    }

    public void add(Path key, String value) {
        pathMap.add(key, value);
    }

}
