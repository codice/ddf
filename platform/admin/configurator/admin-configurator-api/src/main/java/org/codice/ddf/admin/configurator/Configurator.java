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
import java.util.UUID;

/**
 * Provides pseudo-transactional semantics to system configuration tasks.
 * <p>
 * In order to use, invoke the various {@code start}, {@code stop}, {@code create}, {@code delete},
 * {@code update}, methods in the intended order of operation, then invoke one of the {@code commit}
 * methods to complete the transaction.
 * <p>
 * <b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface Configurator {
    /**
     * Sequentially invokes all the transaction's operations, committing their changes. If a failure
     * occurs during the processing, a rollback is attempted of those handlers that had already been
     * committed.
     * <p>
     * After commit is called, this {@code Configurator} should not be used again.
     * <p>
     * In the case of a successful commit, changes should be logged. {@code auditParams} are
     * interpolated into the {@code auditMessage} using the Log4J interpolation style.
     *
     * @param auditMessage In the case of a successful commit, the message to pass to be audited
     * @param auditParams  In the case of a successful commit, optional parameters to pass to be
     *                     interpolated into the message.
     * @return report of the commit status, whether successful, successfully rolled back, or partially
     * rolled back with errors
     */
    OperationReport commit(String auditMessage, String... auditParams);

    /**
     * Sequentially invokes all the transaction's operations, committing their changes. If a failure
     * occurs during the processing, a rollback is attempted of those handlers that had already been
     * committed.
     * <p>
     * After commit is called, this {@code Configurator} should not be used again.
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
    UUID startBundle(String bundleSymName);

    /**
     * Stops the bundle with the given name.
     *
     * @param bundleSymName the symbolic name of the bundle
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    UUID stopBundle(String bundleSymName);

    /**
     * Installs and starts the feature with the given name.
     *
     * @param featureName the name of the feature
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    UUID startFeature(String featureName);

    /**
     * Stops the feature with the given name.
     *
     * @param featureName the name of the feature
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    UUID stopFeature(String featureName);

    /**
     * Creates a property file in the system with the given set of new key:value pairs.
     *
     * @param propFile   the property file to create
     * @param properties the set of key:value pairs to save to the property file; a null map or
     *                   null values in the map will result in a runtime exception
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    UUID createPropertyFile(Path propFile, Map<String, String> properties);

    /**
     * Deletes a property file in the system.
     *
     * @param propFile the property file to delete
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    UUID deletePropertyFile(Path propFile);

    /**
     * Updates a property file in the system with the given set of new key:value pairs.
     *
     * @param propFile    the property file to update
     * @param properties  the set of key:value pairs to save to the property file; a null map or
     *                    null values in the map will result in a runtime exception
     * @param keepIgnored if true, then any keys already in the property file will retain their
     *                    initial values if they are excluded from the {@code properties} param; if
     *                    false, then the only properties that will be in the updated file are those
     *                    provided by the {@code properties} param
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    UUID updatePropertyFile(Path propFile, Map<String, String> properties, boolean keepIgnored);

    /**
     * Updates a bundle configuration file in the system with the given set of new key:value pairs.
     *
     * @param configPid   the configPid of the bundle configuration file to update
     * @param configs     the set of key:value pairs to save in the configuration
     * @param keepIgnored if true, then any keys already in the config file will retain their
     *                    initial values if they are excluded from the {@code properties} param; if
     *                    false, then the only config entires that will be in the updated file are those
     *                    provided by the {@code properties} param
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    UUID updateConfigFile(String configPid, Map<String, Object> configs, boolean keepIgnored);

    /**
     * Creates a new managed service for the given factory.
     *
     * @param factoryPid the factoryPid of the service to create
     * @param configs    the set of key:value pairs to save in the new managed service's configuration
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    UUID createManagedService(String factoryPid, Map<String, Object> configs);

    /**
     * Deletes a managed service.
     *
     * @param configPid the configPid of the instance of the service to delete
     * @return a lookup key that can be used to correlate this operation in the
     * final {@link OperationReport}
     */
    UUID deleteManagedService(String configPid);
}
