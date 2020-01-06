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
package org.codice.ddf.persistence;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;

@SuppressWarnings("unchecked")
public class PersistentItem extends HashMap<String, Object> {

  public static final String ID = "id_txt";

  public static final String TEXT_SUFFIX = "_txt";

  public static final String XML_SUFFIX = "_xml";

  public static final String INT_SUFFIX = "_int";

  public static final String LONG_SUFFIX = "_lng";

  public static final String DATE_SUFFIX = "_tdt";

  public static final String BINARY_SUFFIX = "_bin";

  public static final String[] SUFFIXES =
      new String[] {TEXT_SUFFIX, XML_SUFFIX, INT_SUFFIX, LONG_SUFFIX, DATE_SUFFIX, BINARY_SUFFIX};

  private static final long serialVersionUID = 6030726429622527480L;

  public static Map<String, Object> stripSuffixes(Map<String, Object> inMap) {
    Map<String, Object> outMap = new HashMap<>();
    for (Map.Entry<String, Object> entry : inMap.entrySet()) {
      int index = StringUtils.lastIndexOfAny(entry.getKey(), SUFFIXES);
      if (index > 0) {
        String newKey = entry.getKey().substring(0, index);
        outMap.put(newKey, entry.getValue());
      } else { // should this ever be executed?
        outMap.put(entry.getKey(), entry.getValue());
      }
    }

    return outMap;
  }

  public void addIdProperty(Object value) {
    if (value != null) {
      put(ID, value);
    }
  }

  public void addProperty(String name, String value) {
    addProperty(name, TEXT_SUFFIX, value);
  }

  public void addXmlProperty(String name, String value) {
    addProperty(name, XML_SUFFIX, value);
  }

  public void addProperty(String name, int value) {
    addProperty(name, INT_SUFFIX, value);
  }

  public void addProperty(String name, long value) {
    addProperty(name, LONG_SUFFIX, value);
  }

  public void addProperty(String name, Set<String> value) {
    addProperty(name, TEXT_SUFFIX, value);
  }

  public void addProperty(String name, Date value) {
    addProperty(name, DATE_SUFFIX, value);
  }

  public void addProperty(String name, byte[] value) {
    addProperty(name, BINARY_SUFFIX, value);
  }

  public void addProperty(String name, Object value) {
    if (value instanceof String) {
      addProperty(name, (String) value);
    } else if (value instanceof Date) {
      addProperty(name, (Date) value);
    } else if (value instanceof Set) {
      addProperty(name, (Set) value);
    } else if (value instanceof Long) {
      addProperty(name, (long) value);
    } else if (value instanceof Integer) {
      addProperty(name, (int) value);
    } else if (value instanceof byte[]) {
      addProperty(name, (byte[]) value);
    } else if (value != null) {
      addProperty(name, value.toString());
    }
  }

  public void addProperty(String name, String suffix, Object value) {
    if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(suffix)) {
      if (name.endsWith(suffix)) {
        put(name, value);
      } else {
        put(name + suffix, value);
      }
    }
  }

  public String getIdProperty() {
    return (String) getProperty(ID);
  }

  public String getTextProperty(String name) {
    return (String) getProperty(name + TEXT_SUFFIX);
  }

  public String getXmlProperty(String name) {
    return (String) getProperty(name + XML_SUFFIX);
  }

  public Integer getIntProperty(String name) {
    return (Integer) getProperty(name + INT_SUFFIX);
  }

  public Long getLongProperty(String name) {
    return (Long) getProperty(name + LONG_SUFFIX);
  }

  public Date getDateProperty(String name) {
    return (Date) getProperty(name + DATE_SUFFIX);
  }

  public void encodeBinaryProperties() {
    List<String> binKey =
        this.keySet()
            .stream()
            .filter(key -> key.contains(BINARY_SUFFIX))
            .collect(Collectors.toList());

    for (String key : binKey) {
      put(key, Base64.getEncoder().encodeToString((byte[]) get(key)));
    }
  }

  public String getBinaryProperty(String name) {
    return (String) getProperty(name + BINARY_SUFFIX);
  }

  public Set<String> getTextSetProperty(String name) {
    return (Set<String>) getProperty(name + TEXT_SUFFIX);
  }

  public Object getProperty(String name) {
    if (StringUtils.isNotBlank(name)) {
      return get(name);
    }
    return null;
  }

  public Set<String> getPropertyNames() {
    return keySet();
  }
}
