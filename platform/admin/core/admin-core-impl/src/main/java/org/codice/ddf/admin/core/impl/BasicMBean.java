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
 */
package org.codice.ddf.admin.core.impl;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BasicMBean implements the standard init and destroy methods all of our mbeans use. Other mbean
 * implementations can extend this BasicMBean and only need to implement their interface without the
 * need to worry about the bean registration.
 */
public class BasicMBean extends StandardMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicMBean.class);

    private MBeanServer mbeanServer;

    private ObjectName objectName;

    private final String objectNameString;

    private final Class classInterface;

    protected BasicMBean(Class classInterface, String objectNameString)
            throws NotCompliantMBeanException {
        super(classInterface);
        this.classInterface = classInterface;
        this.objectNameString = objectNameString;
    }

    public void init() {
        mbeanServer = ManagementFactory.getPlatformMBeanServer();

        try {
            objectName = new ObjectName(objectNameString);
        } catch (MalformedObjectNameException e) {
            LOGGER.debug("Exception while creating object name: " + objectNameString, e);
        }

        try {
            mbeanServer.registerMBean(new StandardMBean(this, classInterface), objectName);
        } catch (InstanceAlreadyExistsException e) {
            try {
                mbeanServer.unregisterMBean(objectName);
                mbeanServer.registerMBean(new StandardMBean(this, classInterface), objectName);
            } catch (Exception ex) {
                LOGGER.info("Could not register mbean.", ex);
            }
        } catch (Exception e) {
            LOGGER.info("Could not register mbean.", e);
        }
    }

    public void destroy() {
        try {
            if (objectName != null && mbeanServer != null) {
                mbeanServer.unregisterMBean(objectName);
            }
        } catch (Exception e) {
            LOGGER.debug("Exception unregistering mbean: ", e);
            throw new RuntimeException(e);
        }
    }
}
