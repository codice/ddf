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

import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

/**
 *
 * This implementation of ServiceFactory uses an instance of ServiceComparator to determine which service should be
 * returned by a call to getService().  Internally, it maintains a sorted list of bound services, always returning the
 * first in that list.
 *
 * @param <T> - The type of the service to be served up by this implementation of ServiceFactory
 * @see ServiceFactory
 * @see ddf.catalog.util.impl.ServiceComparator
 *
 */

public class ServiceFactoryImpl<T> implements ServiceFactory<T> {
    private SortedSet<ServiceReference<T>> serviceSet = new ConcurrentSkipListSet<ServiceReference<T>>(
            new ServiceComparator());

    private T service;

    private Class bundleClass;

    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(ServiceFactoryImpl.class));

    /**
     *
     * @param bundleClass - A class in a bundle that contains services of type T.  May not be null.
     *
     */

    public ServiceFactoryImpl(Class bundleClass) {

        if (bundleClass == null) {
            throw new IllegalArgumentException(
                    "ServiceFactoryImpl(): constructor argument 'bundleClass' may not be null.");
        }

        this.bundleClass = bundleClass;
    }

    BundleContext getBundleContext() {
        Bundle cxfBundle = FrameworkUtil.getBundle(bundleClass);

        if (cxfBundle != null) {
            return cxfBundle.getBundleContext();
        }

        return null;
    }

    public T getService() {
        return this.service;
    }

    public void bindService(ServiceReference serviceReference) {
        LOGGER.trace("Entering: bindService(ServiceReference)");

        if (serviceReference != null) {
            serviceSet.add(serviceReference);
            resetService();
        }

        LOGGER.trace("Exiting: bindService(ServiceReference)");
    }

    public void unbindService(ServiceReference geoCoderServiceReference) {
        LOGGER.trace("Entering: unbindService(ServiceReference)");

        if (geoCoderServiceReference != null) {
            serviceSet.remove(geoCoderServiceReference);
            resetService();
        }

        LOGGER.trace("Exiting: unbindServer(ServiceReference)");
    }

    private void resetService() {
        if (serviceSet.isEmpty()) {
            this.service = null;
            return;
        }

        ServiceReference preferredServiceReference = serviceSet.first();

        if (preferredServiceReference != null) {

            //extract the preferred GeoCoder from the stored ServiceReferences;
            this.service = (T) getBundleContext().getService(preferredServiceReference);
        } else {
            this.service = null;
        }
    }
}
