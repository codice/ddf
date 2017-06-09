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
 **/
package org.codice.ddf.internal.admin.configurator.actions;

import java.util.Set;

import org.codice.ddf.admin.configurator.ConfiguratorException;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface ServiceReader {
    /**
     * Retrieves the first found service reference. The reference should only be used for reading purposes,
     * any changes should be done through a commit
     *
     * @param serviceClass Class of service to retrieve
     * @param <S>          type to be returned
     * @return first found service reference of serviceClass
     * @throws ConfiguratorException if any errors occur
     */
    <S> S getServiceReference(Class<S> serviceClass) throws ConfiguratorException;

    /**
     * Retrieves all service references matching the class and LDAP filter.
     *
     * @param serviceClass Class of service to retrieve
     * @param filter       LDAP filter to limit results
     * @param <S>          type to be returned
     * @return all services matching the parameters
     * @throws ConfiguratorException if any errors occur
     */
    <S> Set<S> getServices(Class<S> serviceClass, String filter) throws ConfiguratorException;
}
