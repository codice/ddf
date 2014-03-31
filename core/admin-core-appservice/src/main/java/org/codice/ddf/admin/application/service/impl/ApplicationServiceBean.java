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
package org.codice.ddf.admin.application.service.impl;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationNode;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the Application Service MBean. Provides an MBean interface
 * for the application service api.
 * 
 */
public class ApplicationServiceBean implements ApplicationServiceBeanMBean {

    private ObjectName objectName;

    private MBeanServer mBeanServer;

    private ApplicationService appService;

    private static final String MAP_NAME = "name";

    private static final String MAP_VERSION = "version";

    private static final String MAP_DESCRIPTION = "description";

    private static final String MAP_CHILDREN = "children";

    private Logger logger = LoggerFactory.getLogger(ApplicationServiceBeanMBean.class);

    /**
     * Creates an instance of an ApplicationServiceBean
     * 
     * @param appService
     *            ApplicationService that is running in the system.
     * @throws ApplicationServiceException
     *             If an error occurs when trying to construct the MBean
     *             objects.
     */
    public ApplicationServiceBean(ApplicationService appService) throws ApplicationServiceException {
        this.appService = appService;
        try {
            objectName = new ObjectName(
                    "org.codice.ddf.admin.application.service.ApplicationService:service=application,version=1.0.0");
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        } catch (MalformedObjectNameException mone) {
            throw new ApplicationServiceException("Could not create objectname.", mone);
        }
    }

    /**
     * Initializes the initial variables and registers the class to the MBean
     * server. <br/>
     * <br/>
     * <b>NOTE: This should be run before any other operations are performed.
     * Operations will NOT be usable until this is called (and until destroy()
     * is called).</b>
     * 
     * @throws ApplicationServiceException
     *             if an error occurs during registration.
     */
    public void init() throws ApplicationServiceException {
        try {
            try {
                logger.debug("Registering application service MBean under object name: {}",
                        objectName.toString());
                mBeanServer.registerMBean(this, objectName);
            } catch (InstanceAlreadyExistsException iaee) {
                // Try to remove and re-register
                logger.info("Re-registering Application Service MBean");
                mBeanServer.unregisterMBean(objectName);
                mBeanServer.registerMBean(this, objectName);
            }
        } catch (Exception e) {
            logger.warn("Could not register mbean.", e);
            throw new ApplicationServiceException(e);
        }
    }

    /**
     * Destroys the application service bean by unregistering it from the MBean
     * server. <br/>
     * <br/>
     * <b>NOTE: This should be run after all operations are completed and the
     * bean is no longer needed. Operations will NOT be usable after this is
     * called (until init() is called). </b>
     * 
     * @throws ApplicationServiceException
     *             if an error occurs during unregistration.
     */
    public void destroy() throws ApplicationServiceException {
        try {
            if (objectName != null && mBeanServer != null) {
                mBeanServer.unregisterMBean(objectName);
            }
        } catch (Exception e) {
            logger.warn("Exception unregistering mbean: ", e);
            throw new ApplicationServiceException(e);
        }
    }

    @Override
    public List<Map<String, Object>> getApplicationTree() {
        Set<ApplicationNode> rootApplications = appService.getApplicationTree();
        List<Map<String, Object>> applications = new ArrayList<Map<String, Object>>();
        for (ApplicationNode curRoot : rootApplications) {
            applications.add(convertApplicationNode(curRoot));
        }
        logger.debug("Returning {} root applications.", applications.size());
        return applications;
    }

    private Map<String, Object> convertApplicationNode(ApplicationNode application) {
        logger.debug("Converting {} to a map", application.getApplication().getName());
        Map<String, Object> appMap = new HashMap<String, Object>();
        Application internalApplication = application.getApplication();
        appMap.put(MAP_NAME, internalApplication.getName());
        appMap.put(MAP_VERSION, internalApplication.getVersion());
        appMap.put(MAP_DESCRIPTION, internalApplication.getDescription());
        List<Map<String, Object>> children = new ArrayList<Map<String, Object>>();
        for (ApplicationNode curNode : application.getChildren()) {
            children.add(convertApplicationNode(curNode));
        }
        appMap.put(MAP_CHILDREN, children);
        return appMap;
    }

    @Override
    public boolean startApplication(String appName) {
        try {
            appService.startApplication(appName);
            return true;
        } catch (ApplicationServiceException ase) {
            logger.warn("Application " + appName + " was not successfully started.", ase);
            return false;
        }
    }

    @Override
    public boolean stopApplication(String appName) {
        try {
            appService.stopApplication(appName);
            return true;
        } catch (ApplicationServiceException ase) {
            logger.warn("Application " + appName + " was not successfully stopped.", ase);
            return false;
        }
    }

}
