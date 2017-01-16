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
package org.codice.ddf.admin.api.configurator;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.codice.ddf.ui.admin.api.ConfigurationAdmin;
import org.codice.ddf.ui.admin.api.ConfigurationAdminMBean;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: tbatie - 1/16/17 - Rename to operation and rename all implementations
public interface Operation<T, S> {
    Logger LOGGER = LoggerFactory.getLogger(Operation.class);

    T commit() throws ConfiguratorException;

    T rollback() throws ConfiguratorException;

    S readState() throws ConfiguratorException;

    default BundleContext getBundleContext() throws ConfiguratorException {
        Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        if (bundle == null) {
            LOGGER.info("Unable to access bundle context");
            throw new ConfiguratorException("Internal error");
        }

        return bundle.getBundleContext();
    }

    default ConfigurationAdmin getConfigAdmin() {
        BundleContext context = getBundleContext();
        ServiceReference<org.osgi.service.cm.ConfigurationAdmin> serviceReference =
                context.getServiceReference(org.osgi.service.cm.ConfigurationAdmin.class);
        return new ConfigurationAdmin(context.getService(serviceReference));
    }

    default ConfigurationAdminMBean getConfigAdminMBean() throws MalformedObjectNameException {
        ObjectName objectName = new ObjectName(ConfigurationAdminMBean.OBJECTNAME);

        return MBeanServerInvocationHandler.newProxyInstance(ManagementFactory.getPlatformMBeanServer(),
                objectName,
                ConfigurationAdminMBean.class,
                false);
    }
}
