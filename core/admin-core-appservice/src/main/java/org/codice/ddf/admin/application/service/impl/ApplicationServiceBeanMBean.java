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

import java.util.List;
import java.util.Map;

/**
 * Interface for the Application Service MBean. Allows exposing the application
 * service out via a JMX MBean interface.
 * 
 */
public interface ApplicationServiceBeanMBean {

    /**
     * Creates an application hierarchy tree that shows relationships between
     * applications.
     * 
     * @return A list of the root applications expressed as maps.
     */
    List<Map<String, Object>> getApplicationTree();

    /**
     * Starts an application with the given name.
     * 
     * @param appName
     *            Name of the application to start.
     * 
     * @return true if the application was successfully started, false if not.
     */
    boolean startApplication(String appName);

    /**
     * Stops an application with the given name.
     * 
     * @param appName
     *            Name of the application to stop.
     * 
     * @return true if the application was successfully stopped, false if not.
     */
    boolean stopApplication(String appName);

    /**
     * Adds a list of application that are specified by their URL.
     * 
     * @param applicationURLList
     */
    void addApplications(List<String> applicationURLList);

}
