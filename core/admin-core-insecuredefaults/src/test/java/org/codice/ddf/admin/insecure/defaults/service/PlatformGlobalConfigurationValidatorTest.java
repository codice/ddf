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
package org.codice.ddf.admin.insecure.defaults.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.junit.Test;
import org.osgi.service.cm.ConfigurationAdmin;

public class PlatformGlobalConfigurationValidatorTest {

    private static final String PLATFORM_GLOBAL_CONFIGURATION_PID = "ddf.platform.config";

    private static final String PROTOCOL_PROPERTY = "protocol";

    private static final String HTTP_PROTOCOL = "http://";

    private static final String HTTPS_PROTOCOL = "https://";

    @Test
    public void testValidateWhenHttpIsEnabled() throws Exception {
        // Setup
        ConfigurationAdmin mockConfigAdmin = mock(ConfigurationAdmin.class, RETURNS_DEEP_STUBS);
        Dictionary<String, Object> mockProperties = new Hashtable<>();
        mockProperties.put(PROTOCOL_PROPERTY, HTTP_PROTOCOL);
        when(mockConfigAdmin.getConfiguration(PLATFORM_GLOBAL_CONFIGURATION_PID).getProperties())
                .thenReturn(mockProperties);
        PlatformGlobalConfigurationValidator pgc = new PlatformGlobalConfigurationValidator(
                mockConfigAdmin);

        // Perform Test
        List<Alert> alerts = pgc.validate();

        // Verify
        assertThat(alerts.size(), is(1));
        assertThat(alerts.get(0).getMessage(),
                is("The [protocol] in Platform Global Configuration is set to [http]."));
    }

    @Test
    public void testValidateWhenHttpIsDisabled() throws Exception {
        // Setup
        ConfigurationAdmin mockConfigAdmin = mock(ConfigurationAdmin.class, RETURNS_DEEP_STUBS);
        Dictionary<String, Object> mockProperties = new Hashtable<>();
        mockProperties.put(PROTOCOL_PROPERTY, HTTPS_PROTOCOL);
        when(mockConfigAdmin.getConfiguration(PLATFORM_GLOBAL_CONFIGURATION_PID).getProperties())
                .thenReturn(mockProperties);
        PlatformGlobalConfigurationValidator pgc = new PlatformGlobalConfigurationValidator(
                mockConfigAdmin);

        // Perform Test
        List<Alert> alerts = pgc.validate();

        // Verify
        assertThat(alerts.size(), is(0));
    }

}
