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
package ddf.security.permission.impl;

import ddf.security.permission.Permissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;

public class PermissionsImpl implements Permissions {

  private static final Pattern PATTERN = Pattern.compile(",");

  public PermissionsImpl() {}

  public Map<String, Set<String>> parsePermissionsFromString(List<String> permStrings) {
    return parsePermissionsFromString(permStrings.toArray(new String[permStrings.size()]));
  }

  /**
   * Parses a string array representation of permission attributes into a map
   *
   * @param permStrings array of permission strings in the format "permName=val1,val2"
   */
  public Map<String, Set<String>> parsePermissionsFromString(String... permStrings) {
    Map<String, Set<String>> permissions = new HashMap<>();
    if (permStrings != null) {
      for (String perm : permStrings) {
        if (perm != null) {
          String[] parts = perm.split("=");
          if (parts.length == 2) {
            String attributeName = parts[0];
            Set<String> attributeValues =
                PATTERN.splitAsStream(parts[1]).map(String::trim).collect(Collectors.toSet());
            permissions.put(attributeName, attributeValues);
          }
        }
      }
    }
    return permissions;
  }

  public List<String> getPermissionsAsStrings(Map<String, Set<String>> attributes) {
    if (attributes == null) {
      return Collections.emptyList();
    }
    List<String> stringAttributes = new ArrayList<>(attributes.size());
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Set<String>> entry : attributes.entrySet()) {
      sb.append(entry.getKey());
      sb.append("=");
      sb.append(StringUtils.join(entry.getValue(), ","));
      stringAttributes.add(sb.toString());
      sb.setLength(0);
    }
    return stringAttributes;
  }
}
