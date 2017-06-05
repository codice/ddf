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
package org.codice.ddf.admin.configurator.impl;

import static org.codice.ddf.admin.configurator.impl.OsgiUtils.getBundleContext;

import java.util.Set;
import java.util.stream.Collectors;

import org.codice.ddf.admin.configurator.ConfiguratorException;
import org.codice.ddf.internal.admin.configurator.actions.ServiceReader;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public class ServiceReaderImpl implements ServiceReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceReaderImpl.class);

    @Override
    public <S> S getServiceReference(Class<S> serviceClass) throws ConfiguratorException {
        BundleContext context = getBundleContext();
        ServiceReference<S> ref = context.getServiceReference(serviceClass);
        if (ref == null) {
            return null;
        }

        return context.getService(ref);
    }

    @Override
    public <S> Set<S> getServices(Class<S> serviceClass, String filter)
            throws ConfiguratorException {
        BundleContext context = getBundleContext();
        try {
            return context.getServiceReferences(serviceClass, filter)
                    .stream()
                    .map(context::getService)
                    .collect(Collectors.toSet());
        } catch (InvalidSyntaxException e) {
            LOGGER.debug("Invalid filter [{}].", filter, e);
            throw new ConfiguratorException(String.format("Received invalid filter [%s]", filter));
        }
    }
}
