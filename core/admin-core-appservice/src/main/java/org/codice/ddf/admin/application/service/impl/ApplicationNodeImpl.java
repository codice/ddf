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

import java.util.Set;
import java.util.TreeSet;

import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationNode;
import org.codice.ddf.admin.application.service.ApplicationStatus;

/**
 * Implementation of an ApplicationNode. This Node is used to help form a
 * hierarchy tree for application relationships.
 * 
 */
public class ApplicationNodeImpl implements ApplicationNode, Comparable<ApplicationNode> {

    private Application application;

    private ApplicationStatus status;

    private ApplicationNode parent;

    private Set<ApplicationNode> children;

    /**
     * Creates a new instance of an ApplicationNode.
     * 
     * @param application
     *            The application that this node corresponds to.
     */
    public ApplicationNodeImpl(Application application) {
        if (application == null) {
            throw new IllegalArgumentException("Input application cannot be null.");
        }
        this.application = application;
        this.children = new TreeSet<ApplicationNode>();
    }

    /**
     * Creates a new instance of an ApplicationNode.
     * 
     * @param application
     *            The application that this node corresponds to.
     * @param status
     *            Current status for the application
     */
    public ApplicationNodeImpl(Application application, ApplicationStatus status) {
        if (application == null || status == null) {
            throw new IllegalArgumentException("Input application and status cannot be null.");
        }
        this.application = application;
        this.children = new TreeSet<ApplicationNode>();
        this.status = status;
    }

    @Override
    public Application getApplication() {
        return application;
    }

    @Override
    public ApplicationStatus getStatus() {
        return status;
    }

    /**
     * Sets the status of this application.
     * 
     * @param status
     *            Current status for this application
     */
    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    /**
     * Sets the parent for this application.
     * 
     * @param parent
     *            Application Node that this application depends on.
     */
    public void setParent(ApplicationNode parent) {
        this.parent = parent;
    }

    @Override
    public ApplicationNode getParent() {
        return parent;
    }

    @Override
    public Set<ApplicationNode> getChildren() {
        return children;
    }

    @Override
    public int hashCode() {
        return application.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (!(obj instanceof ApplicationNode)) {
            return false;
        }
        return application.equals(((ApplicationNode) obj).getApplication());
    }

    @Override
    public int compareTo(ApplicationNode otherApp) {
        if (otherApp == null) {
            throw new IllegalArgumentException("ApplicationNode parameter cannot be null.");
        }
        int nameCompare = application.getName().compareTo(otherApp.getApplication().getName());
        if (nameCompare == 0) {
            return application.getVersion().compareTo(otherApp.getApplication().getVersion());
        } else {
            return nameCompare;
        }

    }

}
