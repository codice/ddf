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
package org.codice.ddf.platform.status;

import java.util.Set;

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
    public Set<Application> getApplications();

    /**
     * Determine if an application is currently started.
     *
     * @param application
     *            Application to check if started.
     * @return true if application is <b>ACTIVE</b>, false if it is
     *         <b>FAILED</b>, <b>INACTIVE</b>, or <b>UNKNOWN</b>.
     * @see org.codice.ddf.platform.status.ApplicationStatus.ApplicationState
     */
    public boolean isApplicationStarted(Application application);

    /**
     * Retrieve the status for the given application.
     *
     * @param application
     *            Application to obtain status for.
     * @return Status for the application.
     */
    public ApplicationStatus getApplicationStatus(Application application);

}
