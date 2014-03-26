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

import java.util.HashSet;
import java.util.Set;

import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationNode;

public class ApplicationNodeImpl implements ApplicationNode, Comparable<ApplicationNode> {

    private Application application;

    private ApplicationNode parent;

    private Set<ApplicationNode> children;

    public ApplicationNodeImpl(Application application) {
        this.application = application;
        this.children = new HashSet<ApplicationNode>();
    }

    public Application getApplication() {
        return application;
    }

    public void setParent(ApplicationNode parent) {
        this.parent = parent;
    }

    public ApplicationNode getParent() {
        return parent;
    }

    public Set<ApplicationNode> getChildren() {
        return children;
    }

    @Override
    public int hashCode() {
        return application.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return application.equals(obj);
    }

    @Override
    public int compareTo(ApplicationNode otherApp) {
        int nameCompare = application.getName().compareTo(otherApp.getApplication().getName());
        if (nameCompare == 0) {
            return application.getVersion().compareTo(otherApp.getApplication().getVersion());
        } else {
            return nameCompare;
        }
    }

}
