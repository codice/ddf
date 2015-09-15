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
 **/

package ddf.catalog.util.impl;

import org.osgi.framework.ServiceReference;

/**
 *
 * The ServiceFactory encapsulates the logic required to retrieve the highest-ranked service from the
 * set of currently installed services of a given type.  Implementations of this interface will be typically
 * be registered as Blueprint reference-listeners.
 *
 * Example:
 * <pre>
 *     {@code
 *    <bean id="geoCoderFactory" class="org.codice.ddf.spatial.geocoder.endpoint.GeoCoderFactoryImpl"/>
 *
 *    <reference-list id="geoCoderList" interface="org.codice.ddf.spatial.geocoder.GeoCoder" availability="optional">
 *       <reference-listener bind-method="bindService" unbind-method="unbindService" ref="geoCoderFactory"/>
 *    </reference-list>
 *    }
 * </pre>
 *
 * @param <T> - The type of the service to be served by the ServiceFactory.
 */

public interface ServiceFactory<T> {

    /**
     *
     * @return - the "preferred" service implementation currently bound in the service registry.
     */
    T getService();

    /**
     * This method is intended to be called by the OSGi container.
     *
     * @param service - a ServiceReference that is being freshly bound to the service registry.
     */
    void bindService(ServiceReference<T> service);

    /**
     * This method is intended to be called by the OSGi container.
     *
     * @param service - a ServiceReference that is being freshly unbound from the service registry.
     */
    void unbindService(ServiceReference<T> service);
}
