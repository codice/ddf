/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.admin.core.alert.service.api.internal;

import java.util.List;

import javax.annotation.Nonnull;

/**
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library.</b>
 * </p>
 * <p>
 * <b> This API will be modified and built upon as the need for Admin UI notifications evolves.
 * See <a href="https://codice.atlassian.net/wiki/display/DDF/Design+Admin+UI+Notifications">Design Admin UI Notifications</a>.</b>
 * </p>
 * <p>
 * While some {@link Alert}s will be pushed to the {@link AlertService}, other {@link Alert}s should
 * be polled each time the current {@link Alert}s is retrieved. An {@link AlertsSupplier} will
 * return a list of {@link Alert}s every time that the {@link #getAlerts} method is called.
 */
public interface AlertsSupplier {

    /**
     * Returns a list of {@link Alert}s specific to the {@link AlertsSupplier}
     *
     * @return list of {@link Alert}s
     */
    @Nonnull
    List<Alert> getAlerts();
}