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
package org.codice.ddf.admin.application.service;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.apache.karaf.features.Feature;

/**
 * Service that keeps track and obtains status for applications running in the
 * system.
 * 
 */
public interface ApplicationService {

    /**
     * Retrieve a set of applications are currently installed in the system.
     * 
     * @return Set of applications.
     */
    Set<Application> getApplications();

    /**
     * Returns the application that has the given name
     * 
     * @param applicationName
     *            Name of the application to retrieve. Is case-insensitive.
     * @return The application that matches the name or null if no application
     *         matched.
     */
    Application getApplication(String applicationName);

    /**
     * Determine if an application is currently started.
     * 
     * @param application
     *            Application to check if started.
     * @return true if application is <b>ACTIVE</b>, false if it is
     *         <b>FAILED</b>, <b>INACTIVE</b>, or <b>UNKNOWN</b>.
     * @see org.codice.ddf.admin.application.service.ApplicationStatus.ApplicationState
     */
    boolean isApplicationStarted(Application application);

    /**
     * Starts an application, including any defined dependencies in the
     * application.
     * 
     * @param application
     *            Application instance to start.
     * @throws ApplicationServiceException
     *             If the application cannot start due to an error, the
     *             exception will be thrown with the error message.
     */
    void startApplication(Application application) throws ApplicationServiceException;

    /**
     * Starts an application, including any defined dependencies in the
     * application.
     * 
     * @param application
     *            Name of the application to start.
     * @throws ApplicationServiceException
     *             If the application cannot start due to an error, the
     *             exception will be thrown with the error message.
     */
    void startApplication(String application) throws ApplicationServiceException;

    /**
     * Stops an application, does not include any external transitive
     * dependencies as they may be needed by other applications.
     * 
     * @param application
     *            Application instance to stop.
     * @throws ApplicationServiceException
     *             If the application cannot stop due to an error (or it is not
     *             started), the exception will be thrown with the error
     *             message.
     */
    void stopApplication(Application application) throws ApplicationServiceException;

    /**
     * Stops an application, does not include any external transitive
     * dependencies as they may be needed by other applications.
     * 
     * @param application
     *            Name of the application to stop.
     * @throws ApplicationServiceException
     *             If the application cannot stop due to an error (or it is not
     *             started), the exception will be thrown with the error
     *             message.
     */
    void stopApplication(String application) throws ApplicationServiceException;

    /**
     * Adds a new application to the application list. <br/>
     * <br/>
     * <b>NOTE: This does NOT start the application</b>
     * 
     * @param applicationURL
     *            URL location of the application. Currently must be a features
     *            repository.
     * @throws ApplicationServiceException
     *             If there is an error trying to add the application.
     */
    void addApplication(URI applicationURL) throws ApplicationServiceException;

    /**
     * Removes an application that has the given URI.
     * 
     * @param applicationURL
     *            URL location of the application. Currently must be a features
     *            repository.
     * @throws ApplicationServiceException
     *             If there is an error trying to remove the application.
     */
    void removeApplication(URI applicationURL) throws ApplicationServiceException;

    /**
     * Removes the given application.
     * 
     * @param application
     *            Application instance to remove.
     * @throws ApplicationServiceException
     *             If there is an error trying to remove the application.
     */
    void removeApplication(Application application) throws ApplicationServiceException;

    /**
     * Removes an application that has the given name.
     * 
     * @param applicationName
     *            Name of the application to remove.
     * @throws ApplicationServiceException
     *             If there is an error trying to remove the application.
     */
    void removeApplication(String applicationName) throws ApplicationServiceException;

    /**
     * Retrieve the status for the given application.
     * 
     * @param application
     *            Application to obtain status for.
     * @return Status for the application.
     */
    ApplicationStatus getApplicationStatus(Application application);

    /**
     * Creates a hierarchy tree of application nodes that show the relationship
     * between applications.
     * 
     * @return set of the root application nodes that will contain all other
     *         applications as their children.
     */
    Set<ApplicationNode> getApplicationTree();

    /**
     * Creates an application list that has two attributes that describes relationships
     * between applications (parent and children dependencies).
     *
     * @return set of application nodes that will contain dependency information
     *          as attributes.
     */
    Set<ApplicationNode> getApplicationArray();

    /**
     * Determine which application contains a certain feature.
     * 
     * @param feature
     *            The feature to search for.
     * @return The application which contains that feature.
     */
    Application findFeature(Feature feature);

    /**
     * Gets the application Profile features on the system.
     * @return the inatllation profiles.
     */
    List<Feature> getInstallationProfiles();

}
