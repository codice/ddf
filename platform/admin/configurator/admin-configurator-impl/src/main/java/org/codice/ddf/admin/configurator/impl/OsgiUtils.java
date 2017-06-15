/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.admin.configurator.impl;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.codice.ddf.admin.configurator.ConfiguratorException;
import org.codice.ddf.admin.core.api.jmx.AdminConsoleServiceMBean;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OsgiUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(OsgiUtils.class);

    private static final String INTERNAL_ERROR = "Internal error";

    private OsgiUtils() {
        // Disable instantiation
    }

    static BundleContext getBundleContext() throws ConfiguratorException {
        Bundle bundle = FrameworkUtil.getBundle(OsgiUtils.class);
        if (bundle == null) {
            LOGGER.info("Unable to access bundle context");
            throw new ConfiguratorException(INTERNAL_ERROR);
        }

        return bundle.getBundleContext();
    }

    static AdminConsoleServiceMBean getConfigAdminMBean() throws ConfiguratorException {
        ObjectName objectName = null;
        try {
            objectName = new ObjectName(AdminConsoleServiceMBean.OBJECTNAME);
        } catch (MalformedObjectNameException e) {
            LOGGER.info("Unable to access config admin mbean");
            throw new ConfiguratorException(INTERNAL_ERROR);
        }

        return MBeanServerInvocationHandler.newProxyInstance(ManagementFactory.getPlatformMBeanServer(),
                objectName,
                AdminConsoleServiceMBean.class,
                false);
    }
}
