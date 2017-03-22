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
package org.codice.ddf.admin.configurator;

import java.nio.file.Path;
import java.util.Map;

/**
 * <b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface Configurator {
    /**
     * Sequentially invokes all the {@link Operation}s, committing their changes. If a failure
     * occurs during the processing, a rollback is attempted of those handlers that had already been
     * committed.
     * <p>
     * In the case of a successful commit, changes should be logged.
     *
     * @param auditMessage In the case of a successful commit, the message to pass to a
     *                     {@code SecurityLogger}
     * @param auditParams  In the case of a successful commit, the optional parameters to pass to a
     *                     {@code SecurityLogger} to be interpolated into the message
     * @return report of the commit status, whether successful, successfully rolled back, or partially
     * rolled back with errors
     */
    OperationReport commit(String auditMessage, String... auditParams);

    /**
     * Sequentially invokes all the {@link Operation}s, committing their changes. If a failure
     * occurs during the processing, a rollback is attempted of those handlers that had already been
     * committed.
     *
     * @return report of the commit status, whether successful, successfully rolled back, or partially
     * rolled back with errors
     */
    OperationReport commit();

    /**
     * Starts the bundle with the given name.
     *
     * @param bundleSymName the symbolic name of the bundle
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    String startBundle(String bundleSymName);

    /**
     * Stops the bundle with the given name.
     *
     * @param bundleSymName the symbolic name of the bundle
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    String stopBundle(String bundleSymName);

    /**
     * Determines if the bundle with the given name is started.
     *
     * @param bundleSymName the symbolic name of the bundle
     * @return true if started; else, false
     */
    boolean isBundleStarted(String bundleSymName);

    /**
     * Installs and starts the feature with the given name.
     *
     * @param featureName the name of the feature
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    String startFeature(String featureName);

    /**
     * Stops the feature with the given name.
     *
     * @param featureName the name of the feature
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    String stopFeature(String featureName);

    /**
     * Determines if the feature with the given name is started.
     *
     * @param featureName the name of the feature
     * @return true if started; else, false
     */
    boolean isFeatureStarted(String featureName);

    /**
     * Creates a property file in the system with the given set of new key:value pairs.
     *
     * @param propFile   the property file to create
     * @param properties the set of key:value pairs to save to the property file
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    String createPropertyFile(Path propFile, Map<String, String> properties);

    /**
     * Deletes a property file in the system.
     *
     * @param propFile the property file to delete
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    String deletePropertyFile(Path propFile);

    /**
     * Updates a property file in the system with the given set of new key:value pairs.
     *
     * @param propFile    the property file to update
     * @param properties  the set of key:value pairs to save to the property file
     * @param keepIgnored if true, then any keys already in the property file will retain their
     *                    initial values if they are excluded from the {@code properties} param; if
     *                    false, then the only properties that will be in the updated file are those
     *                    provided by the {@code properties} param
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    String updatePropertyFile(Path propFile, Map<String, String> properties, boolean keepIgnored);

    /**
     * Gets the current key:value pairs set in the given property file.
     *
     * @param propFile the property file to query
     * @return the current set of key:value pairs
     */
    Map<String, String> getProperties(Path propFile);

    /**
     * Updates a bundle configuration file in the system with the given set of new key:value pairs.
     *
     * @param configPid   the configId of the bundle configuration file to update
     * @param configs     the set of key:value pairs to save in the configuration
     * @param keepIgnored if true, then any keys already in the config file will retain their
     *                    initial values if they are excluded from the {@code properties} param; if
     *                    false, then the only config entires that will be in the updated file are those
     *                    provided by the {@code properties} param
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    String updateConfigFile(String configPid, Map<String, Object> configs, boolean keepIgnored);

    /**
     * Gets the current key:value pairs set in the given configuration file.
     *
     * @param configPid the configId of the bundle configuration file to query
     * @return the current set of key:value pairs
     */
    Map<String, Object> getConfig(String configPid);

    /**
     * Creates a new managed service for the given factory.
     *
     * @param factoryPid the factoryPid of the service to create
     * @param configs    the set of key:value pairs to save in the new managed service's configuration
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    String createManagedService(String factoryPid, Map<String, Object> configs);

    /**
     * Deletes a managed service.
     *
     * @param configPid the configPid of the instance of the service to delete
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    String deleteManagedService(String configPid);

    Map<String, Map<String, Object>> getManagedServiceConfigs(String factoryPid);

    /**
     * Retrieves the service reference. The reference should only be used for reading purposes,
     * any changes should be done through a commit
     *
     * @param serviceClass - Class of service to retrieve
     * @return first found service reference of serviceClass
     * @throws ConfiguratorException if any errors occur
     */
    <S> S getServiceReference(Class<S> serviceClass) throws ConfiguratorException;
}
