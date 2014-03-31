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

/**
 * Node for applications that is used to create a hierarchy tree that shows
 * application dependencies.
 * 
 */
public interface ApplicationNode {

    /**
     * Returns the application this node is referencing.
     * 
     * @return application for this node
     */
    Application getApplication();

    /**
     * Returns the parent of the application.
     * 
     * @return Parent node of this application or null if application has no
     *         parent.
     */
    ApplicationNode getParent();

    /**
     * Returns the children of this application. That is, the applications that
     * have a requirement on this application.
     * 
     * @return A set of children of this application.
     */
    Set<ApplicationNode> getChildren();

}
