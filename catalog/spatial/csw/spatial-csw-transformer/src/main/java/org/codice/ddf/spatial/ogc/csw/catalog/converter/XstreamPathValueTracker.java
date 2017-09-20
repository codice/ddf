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
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import com.thoughtworks.xstream.io.path.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;

public class XstreamPathValueTracker {

  private LinkedHashMap<Path, List<String>> pathMap = new LinkedHashMap<>();

  public void buildPaths(LinkedHashSet<Path> paths) {
    paths.forEach(path -> pathMap.put(path, null));
  }

  public String getFirstValue(Path key) {

    if (key != null) {
      List<String> value = pathMap.get(key);
      if (CollectionUtils.isNotEmpty(value)) {
        return value.get(0);
      }
    }
    return null;
  }

  public List<String> getAllValues(Path key) {
    return pathMap.get(key);
  }

  public Set<Path> getPaths() {
    return pathMap.keySet();
  }

  public void add(Path key, List<String> value) {

    if (key != null) {
      List<String> originalValue = pathMap.get(key);
      if (originalValue == null) {
        pathMap.put(key, value);
      } else {
        List<String> joinedList = new ArrayList<>(originalValue);
        joinedList.addAll(value);
        pathMap.put(key, joinedList);
      }
    }
  }

  public void add(Path key, String value) {

    this.add(key, Arrays.asList(value));
  }
}
