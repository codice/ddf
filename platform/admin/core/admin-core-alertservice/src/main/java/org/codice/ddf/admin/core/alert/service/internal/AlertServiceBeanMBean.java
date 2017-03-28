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
package org.codice.ddf.admin.core.alert.service.internal;

import java.util.List;
import java.util.Map;

/**
 * Interface to expose {@link AlertServiceBean} as an MBean.
 */
public interface AlertServiceBeanMBean {

    String OBJECT_NAME = AlertServiceBean.class.getName() + ":service=alert-service";

    /**
     * @return a list of the current {@link org.codice.ddf.admin.core.alert.service.data.internal.Alert}s
     */
    List<Map<String, Object>> getAlerts();

    /**
     * Dismisses an alert from the current {@link org.codice.ddf.admin.core.alert.service.data.internal.Alert}s by {@param key}
     *
     * @param key of the alert to dismiss
     * @return if the alert with the {@param key} was succesfully dismissed
     */
    Boolean dismissAlert(String key);
}