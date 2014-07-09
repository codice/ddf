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
package org.codice.ddf.persistence;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashMap;
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

}
