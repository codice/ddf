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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.admin.insecure.defaults.service.Alert.Level;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlatformGlobalConfigurationValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(PlatformGlobalConfigurationValidator.class);

    private static final String PLATFORM_GLOBAL_CONFIGURATION_PID = "ddf.platform.config";

    private static final String PROTOCOL_PROPERTY = "protocol";

    private static final String HTTP_PROTOCOL = "http://";

    private ConfigurationAdmin configAdmin;

    private List<Alert> alerts;

    static final String PROTCOL_IN_PLATFORM_GLOBAL_CONFIG_IS_HTTP = "The [%s] in Platform Global Configuration is set to [http].";

    public PlatformGlobalConfigurationValidator(ConfigurationAdmin configAdmin) {
        alerts = new ArrayList<>();
        this.configAdmin = configAdmin;
    }

    public List<Alert> validate() {
        alerts = new ArrayList<>();
        validateHttpIsDisabled();
        return alerts;
    }

    private void validateHttpIsDisabled() {
        try {
            if (configAdmin != null) {
                Configuration config = configAdmin
                        .getConfiguration(PLATFORM_GLOBAL_CONFIGURATION_PID);
                Dictionary<String, Object> properties = config.getProperties();
                LOGGER.debug("props: {}", properties.toString());
                String protocol = (String) properties.get(PROTOCOL_PROPERTY);
                if (StringUtils.equalsIgnoreCase(protocol, HTTP_PROTOCOL)) {
                    alerts.add(new Alert(Level.WARN, String.format(PROTCOL_IN_PLATFORM_GLOBAL_CONFIG_IS_HTTP, PROTOCOL_PROPERTY)));
                }
            } else {
                String msg = "Unable to determine if Platform Global Configuration has insecure defaults. Cannot access Configuration Admin.";
                alerts.add(new Alert(Level.WARN, msg));
            }
        } catch (IOException e) {
            String msg = "Unable to determine if Platform Global Configuration has insecure defaults. ";
            LOGGER.warn(msg, e);
            alerts.add(new Alert(Level.WARN, msg + e.getMessage()));
        }
    }
}
