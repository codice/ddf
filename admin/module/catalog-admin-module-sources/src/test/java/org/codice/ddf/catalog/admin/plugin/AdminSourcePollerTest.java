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

package org.codice.ddf.catalog.admin.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.shiro.util.CollectionUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.Source;
import ddf.catalog.source.opensearch.OpenSearchSource;

public class AdminSourcePollerTest {

    public static final String CONFIG_PID = "properPid";

    public static final String EXCEPTION_PID = "throwsAnException";

    public static final String FPID = "OpenSearchSource";

    public static MockedAdminSourcePoller poller;

    @BeforeClass
    public static void setup() {
        poller = new AdminSourcePollerTest().new MockedAdminSourcePoller(null);
    }

    @Test
    public void testAllSourceInfo() {
        List<Map<String, Object>> sources = poller.allSourceInfo();
        assertNotNull(sources);
        assertEquals(2, sources.size());

        assertFalse(sources.get(0).containsKey("configurations"));
        assertTrue(sources.get(1).containsKey("configurations"));
    }

    @Test
    public void testSourceStatus() {
        assertTrue(poller.sourceStatus(CONFIG_PID));
        assertFalse(poller.sourceStatus(EXCEPTION_PID));
        assertFalse(poller.sourceStatus("FAKE SOURCE"));
    }

    private class MockedAdminSourcePoller extends AdminSourcePollerServiceBean {
        public MockedAdminSourcePoller(ConfigurationAdmin configAdmin) {
            super(configAdmin);
        }

        @Override
        protected AdminSourceHelper getHelper() {
            AdminSourceHelper helper = mock(AdminSourceHelper.class);
            try {
                // Mock out the configuration
                Configuration config = mock(Configuration.class);
                when(config.getPid()).thenReturn(CONFIG_PID);
                when(config.getFactoryPid()).thenReturn(FPID);
                Dictionary<String, Object> dict = new Hashtable<>();
                dict.put("service.pid", CONFIG_PID);
                dict.put("service.factoryPid", FPID);
                when(config.getProperties()).thenReturn(dict);
                when(helper.getConfigurations(anyMap()))
                        .thenReturn(CollectionUtils.asList(config), null);

                // Mock out the sources
                OpenSearchSource source = mock(OpenSearchSource.class);
                when(source.isAvailable()).thenReturn(true);

                OpenSearchSource badSource = mock(OpenSearchSource.class);
                when(badSource.isAvailable()).thenThrow(new RuntimeException());

                //CONFIG_PID, EXCEPTION_PID, FAKE_SOURCE
                when(helper.getConfiguration(any(ConfiguredService.class)))
                        .thenReturn(config, config, config);
                when(helper.getSources())
                        .thenReturn(CollectionUtils.asList((Source) source, badSource));

                // Mock out the metatypes
                Map<String, Object> metatype = new HashMap<>();
                metatype.put("id", "OpenSearchSource");
                metatype.put("metatype", new ArrayList<Map<String, Object>>());

                Map<String, Object> noConfigMetaType = new HashMap<>();
                noConfigMetaType.put("id", "No Configurations");
                noConfigMetaType.put("metatype", new ArrayList<Map<String, Object>>());

                when(helper.getMetatypes())
                        .thenReturn(CollectionUtils.asList(metatype, noConfigMetaType));
            } catch (Exception e) {

            }

            return helper;
        }
    }
}
