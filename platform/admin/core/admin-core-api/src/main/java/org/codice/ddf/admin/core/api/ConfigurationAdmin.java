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
package org.codice.ddf.admin.core.api;

import java.io.IOException;
import java.util.List;

import org.apache.shiro.subject.Subject;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.Configuration;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * This service provides extra operations that can be used with respect to ConfigurationAdmin
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 */
public interface ConfigurationAdmin {

    // Used when we want a filter to match with nothing
    String NO_MATCH_FILTER = "(service.pid=0)";

    /**
     * Returns the {@link Configuration} associated with the given PID
     * @param pid - service pid for the configuration
     * @return Configuration
     */
    Configuration getConfiguration(String pid);

    /**
     * Returns the {@link ObjectClassDefinition} associated with the given {@link Configuration}.
     * The ObjectClassDefinition is the actual configuration part of the metatype that holds all
     * of the configurable pieces.
     * @param config - The configuration associated with the metatype
     * @return ObjectClassDefinition
     */
    ObjectClassDefinition getObjectClassDefinition(Configuration config);

    /**
     * Returns the {@link ObjectClassDefinition} associated with the given {@link Configuration}.
     * The ObjectClassDefinition is the actual configuration part of the metatype that holds all
     * of the configurable pieces.
     * @param bundle - The bundle for which metatype information is requested.
     * @param pid - Service pid for the metatype
     * @return ObjectClassDefinition
     */
    ObjectClassDefinition getObjectClassDefinition(Bundle bundle, String pid);

    /**
     * Returns the {@link ObjectClassDefinition} associated with the given {@link Configuration}.
     * The ObjectClassDefinition is the actual configuration part of the metatype that holds all
     * of the configurable pieces.
     * @param pid - Service pid for the metatype
     * @return ObjectClassDefinition
     */
    ObjectClassDefinition getObjectClassDefinition(String pid);

    /**
     * Returns all services and managed service factories that satisfy the provided LDAP filters
     * @param serviceFactoryFilter - LDAP filter for managed service factories
     * @param serviceFilter - LDAP filter for managed services
     * @return List<Service>
     */
    List<Service> listServices(String serviceFactoryFilter, String serviceFilter);

    /**
     * Returns true if the subject is permitted to view the service corresponding to the provided
     * service.pid.
     * @param servicePid - PID of the service in question
     * @param subject - Subject to imply
     * @return True if the subject is allowed to view the service
     */
    boolean isPermittedToViewService(String servicePid, Subject subject);

    /**
     * Returns the complete Metatype with all available information for the provided {@link Configuration}
     * @param config - Configuration to retrieve Metatype for
     * @return Metatype
     */
    Metatype findMetatypeForConfig(Configuration config);

    /**
     * Returns a default filter that can be used by a client when calling listServices(...)
     * This filter would pertain to managed service factories
     * @return a default filter that can be used by a client when calling listServices(...)
     */
    String getDefaultFactoryLdapFilter();

    /**
     * Returns a default filter that can be used by a client when calling listServices(...)
     * This filter would pertain to managed services
     * @return a default filter that can be used by a client when calling listServices(...)
     */
    String getDefaultLdapFilter();

    /**
     * Enables a currently disabled configuration.
     * @param servicePid - PID corresponding to the configuration
     * @param disabledConfig - The configuration that is disabled
     * @return object representing the change in state from enabled to disabled
     * @throws IOException
     */
    ConfigurationStatus enableManagedServiceFactoryConfiguration(String servicePid,
            Configuration disabledConfig) throws IOException;

    /**
     * Disables a currently enabled configuration.
     * @param servicePid - PID corresponding to the configuration
     * @param enabledConfig - The configuration that is enabled
     * @return object representing the change in state from disabled to enabled
     * @throws IOException
     */
    ConfigurationStatus disableManagedServiceFactoryConfiguration(String servicePid,
            Configuration enabledConfig) throws IOException;

    /**
     * Returns the name of a bundle.
     * @param bundle - Bundle to get the name of
     * @return name of the bundle
     */
    String getName(Bundle bundle);
}
