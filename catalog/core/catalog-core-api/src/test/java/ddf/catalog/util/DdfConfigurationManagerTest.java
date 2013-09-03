/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class DdfConfigurationManagerTest {
	MockDdfConfigurationWatcher mockWatcher;
	DdfConfigurationManager ddfConfigMgr;
	String key;
	Map<Object,Object> config1;
	Map<Object,Object> config2;

	@Before
	public void setUp() throws Exception {
		key = DdfConfigurationManagerTest.class.getSimpleName() + "Key";
		mockWatcher = new MockDdfConfigurationWatcher();
		List<DdfConfigurationWatcher> watchers = new ArrayList<DdfConfigurationWatcher>();
		watchers.add(mockWatcher);
		ddfConfigMgr = new DdfConfigurationManager(watchers, null);
		config1 = new Hashtable<Object, Object>();
		config1.put(key, "config1");
		config2 = new Hashtable<Object, Object>();
		config2.put(key, "config2");
		mockWatcher.ddfConfigurationUpdated(config1);
	}

	@Ignore
	@Test
	public void testDdfConfigurationManager() {
		assertNotNull(ddfConfigMgr);
		assertTrue(null == ddfConfigMgr.getConfigurationAdmin());
	}

	@Ignore
	@Test
	public void testUpdated() {
		assertEquals(mockWatcher.getConfigValue(key), config1.get(key));
/*
        Not Sure what these should do, but they fail currently. 
        May be a bugs. May be bad tests.
*/
        
		ddfConfigMgr.updated(null);
		assertEquals(mockWatcher.getConfigValue(key), config1.get(key));

		ddfConfigMgr.updated(new HashMap());
		assertEquals(mockWatcher.getConfigValue(key), config1.get(key));

		ddfConfigMgr.updated(config2);
		assertEquals(mockWatcher.getConfigValue(key), config2.get(key));
		
		ddfConfigMgr = new DdfConfigurationManager(null, null);
		try{
			ddfConfigMgr.updated(config1);
		}catch(NullPointerException npe){
			fail();
		}
		
		
	}

	@Ignore
	@Test
	public void testBind() {
		assertEquals(mockWatcher.getConfigValue(key), config1.get(key));

  		ddfConfigMgr.bind(mockWatcher, null);
 
		assertEquals(mockWatcher.getConfigValue(key), config1.get(key));
		ddfConfigMgr.bind(null, config2);
		assertEquals(mockWatcher.getConfigValue(key), config1.get(key));
		ddfConfigMgr.bind(null, null);
		assertEquals(mockWatcher.getConfigValue(key), config1.get(key));
		ddfConfigMgr.bind(mockWatcher, config2);
		assertEquals(mockWatcher.getConfigValue(key), config2.get(key));
		assertTrue(config2.size()>1); // should have read-only props too.

		ddfConfigMgr = new DdfConfigurationManager(null, null);
		try{
			ddfConfigMgr.bind(mockWatcher, config1);
			assertEquals(mockWatcher.getConfigValue(key), config1.get(key));
			ddfConfigMgr.bind(mockWatcher, config2);
			assertEquals(mockWatcher.getConfigValue(key), config2.get(key));
		}catch(NullPointerException npe){
			fail();
		}

	}

	@Ignore
	@Test
	public void testGetConfigurationAdmin() {
		assertEquals(null, ddfConfigMgr.getConfigurationAdmin());
		//TODO: Make a mock config admin.
	}

	@Ignore
	@Test
	public void testSetConfigurationAdmin() {
		//TODO: Make a mock config admin.
	}

	@Ignore
	@Test
	public void testGetConfigurationValue() {
		ddfConfigMgr.getConfigurationValue("1234", key);
	}

}
