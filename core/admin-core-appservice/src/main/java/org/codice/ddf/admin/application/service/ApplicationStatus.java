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

import java.util.Set;

import org.apache.karaf.features.Feature;
import org.osgi.framework.Bundle;

/**
 * Class that describes the status of an application. If application has errored
 * out, it shows which components of the application did not start correctly.
 *
 */
public interface ApplicationStatus {

    /**
     * State describing the Application.
     * <p>
     * Possible States:
     * <p>
     * <b>ACTIVE</b>: no errors, started successfully <br/>
     * <b>FAILED</b>: errors occurred while trying to start <br/>
     * <b>INACTIVE</b>: nothing from the app is installed <br/>
     * <b>UNKNOWN</b>: could not determine status <br/>
     *
     *
     */
    public enum ApplicationState {
        ACTIVE, FAILED, INACTIVE, UNKNOWN
    }

    /**
     * Application this status is for.
     *
     * @return The application associated to this status.
     */
    public Application getApplication();

    /**
     * Retrieves the state of the current application.
     *
     * @return {@link ApplicationState} for the current application
     */
    public ApplicationState getState();

    /**
     * Gets the features for this application that are not properly installed.
     *
     * @return Set of features that did not install.
     */
    public Set<Feature> getErrorFeatures();

    /**
     * Gets the bundles for this application that are not properly installed.
     *
     * @return Set of bundles that did not install.
     */
    public Set<Bundle> getErrorBundles();

}
