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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.junit.Test;

public class PersistenceItemTest {

  @Test
  public void testStripSuffixes() {
    Map<String, Object> inMap = new HashMap<String, Object>();
    inMap.put("key1" + PersistentItem.TEXT_SUFFIX, "key1_value");
    inMap.put("key2" + PersistentItem.DATE_SUFFIX, new Date());

    Map<String, Object> outMap = PersistentItem.stripSuffixes(inMap);
    assertTrue(outMap.size() == inMap.size());
    assertTrue(outMap.containsKey("key1"));
    assertFalse(outMap.containsKey("key1_" + PersistentItem.TEXT_SUFFIX));
    assertTrue(outMap.containsKey("key2"));
    assertFalse(outMap.containsKey("key2_" + PersistentItem.DATE_SUFFIX));
  }

  @Test
  public void testSuffixCreation() {
    Map<String, Object> inMap = new HashMap<>();
    inMap.put("string", "value");
    inMap.put("int", 1);
    inMap.put("long", 1L);
    inMap.put("binary", new byte[1]);
    inMap.put("date", new Date());
    HashSet<String> set = new HashSet<>();
    set.add("foo");
    set.add("bar");
    inMap.put("set", set);
    PersistentItem item = new PersistentItem();
    inMap.forEach((name, value) -> item.addProperty(name, value));
    assertTrue(item.getPropertyNames().contains("string_txt"));
    assertTrue(item.getPropertyNames().contains("int_int"));
    assertTrue(item.getPropertyNames().contains("long_lng"));
    assertTrue(item.getPropertyNames().contains("binary_bin"));
    assertTrue(item.getPropertyNames().contains("date_tdt"));
    assertTrue(item.getPropertyNames().contains("set_txt"));
  }

  @Test
  public void testEncodeBinaryProperties() {
    Map<String, Object> inMap = new HashMap<>();
    inMap.put("string", "value");
    inMap.put("int", 1);
    inMap.put("long", 1L);
    inMap.put("binary", new byte[1]);
    inMap.put("date", new Date());
    HashSet<String> set = new HashSet<>();
    set.add("foo");
    set.add("bar");
    inMap.put("set", set);
    PersistentItem item = new PersistentItem();
    inMap.forEach((name, value) -> item.addProperty(name, value));
    item.encodeBinaryProperties();
    assertTrue(item.getPropertyNames().contains("string_txt"));
    assertTrue(item.getPropertyNames().contains("int_int"));
    assertTrue(item.getPropertyNames().contains("long_lng"));
    assertTrue(item.getPropertyNames().contains("binary_bin"));
    assertTrue(item.getPropertyNames().contains("date_tdt"));
    assertTrue(item.getPropertyNames().contains("set_txt"));
    String encodedValue = Base64.getEncoder().encodeToString((byte[]) inMap.get("binary"));
    assertTrue(encodedValue.equalsIgnoreCase(item.getBinaryProperty("binary")));
    assertTrue(item.getTextProperty("string").equalsIgnoreCase(inMap.get("string").toString()));
  }
}
