/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.ui.query.monitor.api;

import java.io.Serializable;
import java.util.Map;

import ddf.security.Subject;

public interface SecurityService {

    /**
     * Get the system subject.
     *
     * @return system subject
     */
    Subject getSystemSubject();

    /**
     * Add the system subject to a properties map and return the result. The returned map is not
     * guaranteed to be the same instance.
     *
     * @param properties must be non-null
     * @return new properties (non-null)
     */
    Map<String, Serializable> addSystemSubject(Map<String, Serializable> properties);

}
