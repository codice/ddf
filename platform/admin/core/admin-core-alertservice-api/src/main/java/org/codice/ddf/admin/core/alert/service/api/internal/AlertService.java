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
 * The {@link AlertService} is used to get, add, and dismiss {@link Alert}s.
 */
public interface AlertService {

    /**
     * Adds an {@link Alert}
     *
     * @param alert
     */
    void addAlert(@Nonnull Alert alert);

    /**
     * @return the current {@link Alert}s
     */
    @Nonnull
    List<Alert> getAlerts();

    /**
     * Dismisses the first {@link Alert} with the specified key from the current {@link Alert}s
     *
     * @param keyToDismiss the {@param keyToDismiss} of the {@link Alert} to be dismissed
     * @return {@code true} if an {@link Alert} with the specified {@param keyToDismiss} was successfully removed from the current collection of {@link Alert}s
     */
    Boolean dismissAlert(@Nonnull String keyToDismiss);
}