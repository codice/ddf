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
package org.codice.ddf.admin.application.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Handles testing the different aspects of the applicationconfiguraitonplugin.
 * @author Jeren
 *
 */
public class ApplicationConfigurationPluginTest {
	/** static for the display name test.*/
	private static final String DISPLAY_NAME_TEST = "I'm a display name";
	/** static for the iframe test.*/
	private static final String IFRAME_LOCATION_TEST = "/iframelocation/";
	/** static for the assocation test.*/
	private static final String TEST_ASSOCATION_1 = "Test1";
	/** static for the assocation test.*/
	private static final String TEST_ASSOCATION_2 = "Test2";
	/** static for the assocations test.*/
	private static final List<String> ORIGINAL_APP_ASSOCIATIONS = new LinkedList<String>(Arrays.asList(TEST_ASSOCATION_1, TEST_ASSOCATION_2));
	
	private static final Integer ORDER_TEST = 1337;
	

	/**
	 * Test class we will utilize for the tests.
	 * @author Jeren
	 *
	 */
	private class TestPlugin extends AbstractApplicationPlugin {
		/**
		 * Constructor.
		 */
		public TestPlugin() {
			this.displayName = DISPLAY_NAME_TEST;
			this.iframeLocation = URI.create(IFRAME_LOCATION_TEST);
			this.order = ORDER_TEST;
			setAssociations(ORIGINAL_APP_ASSOCIATIONS);
		}
		
	}
	
	/**
	 * Will test the abstractconfigurationplugin by creating a new class that extends it, setting values to it
	 * and then testing that they are all constructed correctly.
	 */
	@Test
    public void testPluginWithAbstractBackEnd() {
		TestPlugin plugin = new TestPlugin();
		TestPlugin plugin2 = new TestPlugin();
		
		assertEquals(plugin.getDisplayName(), DISPLAY_NAME_TEST);
		assertFalse(0 == plugin.getID().compareTo(plugin2.getID()));
		assertEquals(plugin.getIframeLocation().toString(), IFRAME_LOCATION_TEST);
		assertEquals(plugin.getAssocations(), ORIGINAL_APP_ASSOCIATIONS);
		assertEquals(plugin.getOrder(), ORDER_TEST);
		
		Map<String, Object> constructedJSON = new HashMap<String, Object>();
		constructedJSON.put(ApplicationPlugin.DISPLAY_NAME_KEY, plugin.getDisplayName());
		constructedJSON.put(ApplicationPlugin.ID_KEY, plugin.getID().toString());
		constructedJSON.put(ApplicationPlugin.IFRAME_LOCATION_KEY, plugin.getIframeLocation());
		constructedJSON.put(ApplicationPlugin.JAVASCRIPT_LOCATION_KEY, plugin.getJavascriptLocation());
		constructedJSON.put(ApplicationPlugin.APPLICATION_ASSOCIATION_KEY, plugin.getAssocations());
		constructedJSON.put(ApplicationPlugin.ORDER_KEY, plugin.getOrder());
		
		//compare the maps.
		Map<String, Object> pluginMap = plugin.toJSON();
		assertTrue(compareMap(constructedJSON, pluginMap));
		
		String newAppAssocation = "NewGuy";
		List<String> modifiedCopy = new ArrayList<String>(ORIGINAL_APP_ASSOCIATIONS);
		modifiedCopy.add(newAppAssocation);
		plugin.addAssociations(newAppAssocation);
		
		assertEquals(plugin.getAssocations(), modifiedCopy);
		
		assertTrue(plugin.matchesAssocationName(TEST_ASSOCATION_1));
		assertFalse(plugin.matchesAssocationName("Fail"));
		assertTrue(plugin.matchesAssocationName(TEST_ASSOCATION_2));
		assertFalse(plugin.matchesAssocationName(ApplicationPlugin.ALL_ASSOCATION_KEY));
		
		plugin.addAssociations(ApplicationPlugin.ALL_ASSOCATION_KEY);
		
		assertTrue(plugin.matchesAssocationName(TEST_ASSOCATION_1));
		assertTrue(plugin.matchesAssocationName("Fail"));
		assertTrue(plugin.matchesAssocationName(TEST_ASSOCATION_2));
		assertTrue(plugin.matchesAssocationName(ApplicationPlugin.ALL_ASSOCATION_KEY));
	}
	
	/**
	 * Compares two maps and checks to make sure their values equal.
	 * @param original - our good copy.
	 * @param toCheck - what we'll check against.
	 * @return true if they are equal, false if they aren't.
	 */
	private boolean compareMap(Map<String, Object> original, Map<String, Object> toCheck) {
		//compare keys here.
		Set<String> originalKeys = original.keySet();
		Set<String> toCheckKeys = toCheck.keySet();
		if (!Arrays.equals(originalKeys.toArray(), toCheckKeys.toArray())) {
			return false;
		}
		//end compare key shere.
		
		for (String key : original.keySet()) {
			Object orginalValue = original.get(key);
			Object toCheckValue = toCheck.get(key);
			if (orginalValue == null || toCheckValue == null) {
				if (orginalValue != toCheckValue) {
					return false;
				}
			} else if (!(orginalValue.equals(toCheckValue))) {
				return false;
			}
		}
		
		return true;
	}
	    
}

