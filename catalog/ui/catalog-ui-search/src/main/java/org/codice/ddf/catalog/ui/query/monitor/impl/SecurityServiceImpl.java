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
package org.codice.ddf.catalog.ui.query.monitor.impl;

import java.io.Serializable;
import java.util.Map;

import org.codice.ddf.catalog.ui.query.monitor.api.SecurityService;
import org.codice.ddf.security.common.Security;

import ddf.security.SecurityConstants;
import ddf.security.Subject;

public class SecurityServiceImpl implements SecurityService {
    @Override
    public Subject getSystemSubject() {
        return Security.getInstance()
                .getSystemSubject();
    }

    @Override
    public Map<String, Serializable> addSystemSubject(Map<String, Serializable> properties) {
        properties.put(SecurityConstants.SECURITY_SUBJECT, getSystemSubject());
        return properties;
    }

    @Override
    public String toString() {
        return "SecurityServiceImpl{}";
    }
}
