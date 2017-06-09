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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.codice.ddf.admin.configurator.Configurator;
import org.codice.ddf.admin.configurator.ConfiguratorException;
import org.codice.ddf.admin.configurator.Operation;
import org.codice.ddf.admin.configurator.OperationReport;
import org.codice.ddf.admin.configurator.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.common.audit.SecurityLogger;

/**
 * Transactional orchestrator for persisting configuration changes.
 * <p>
 * Sequentially processes {@link Operation}s, committing their changes. If a failure occurs
 * during the processing, a rollback is attempted of those handlers that had already been committed.
 * When the {@link #commit()} operation completes - either successfully, with a successful rollback,
 * or with a failure to rollback - it returns a {@link OperationReport} of the outcome. In the case of
 * rollback failures, callers of this class should inform users of those failures so they may manually
 * intercede.
 * <p>
 * This class does not guarantee that it can reliably rollback changes in the case of failure. It
 * makes a best-effort to revert changes and reports the outcome.
 * <p>
 * To use this class, first instantiate then invoke the various methods for feature, bundle, config,
 * etc. updates in the order they should be applied. When all have been completed, call the
 * {@link #commit()} method to write the changes to the system. The resulting {@link OperationReport}
 * will have the outcome.
 */
public class ConfiguratorImpl implements Configurator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfiguratorImpl.class);

    private final Map<UUID, Operation> configHandlers = new LinkedHashMap<>();

    /**
     * Sequentially invokes all the {@link Operation}s, committing their changes. If a failure
     * occurs during the processing, a rollback is attempted of those handlers that had already been
     * committed.
     * <p>
     * In the case of a successful commit, the {@link SecurityLogger} will be invoked to log changes.
     *
     * @param auditMessage In the case of a successful commit, the message to pass to the
     *                     {@link SecurityLogger}
     * @param auditParams  In the case of a successful commit, the optional parameters to pass to the
     *                     {@link SecurityLogger} to be interpolated into the message
     * @return report of the commit status, whether successful, successfully rolled back, or partially
     * rolled back with errors
     */
    public OperationReport commit(String auditMessage, String... auditParams) {
        OperationReport report = commit();
        if (report.hasTransactionSucceeded()) {
            SecurityLogger.audit(auditMessage, (Object[]) auditParams);
        }

        return report;
    }

    @Override
    public UUID add(Operation operation) {
        return registerHandler(operation);
    }

    /**
     * Sequentially invokes all the {@link Operation}s, committing their changes. If a failure
     * occurs during the processing, a rollback is attempted of those handlers that had already been
     * committed.
     *
     * @return report of the commit status, whether successful, successfully rolled back, or partially
     * rolled back with errors
     */
    private OperationReport commit() {
        OperationReport configReport = new OperationReportImpl();
        for (Map.Entry<UUID, Operation> row : configHandlers.entrySet()) {
            try {
                Result result = row.getValue()
                        .commit();
                configReport.putResult(row.getKey(), result);
            } catch (ConfiguratorException e) {
                LOGGER.debug("Error committing configuration change", e);

                // On failure, attempt to rollback any config changes that have already been made
                // and then break out of loop processing, only reporting the remaining as skipped
                rollback(row.getKey(), configReport, e);
                break;
            }
        }

        return configReport;
    }

    private UUID registerHandler(Operation handler) {
        UUID key = UUID.randomUUID();
        configHandlers.put(key, handler);
        return key;
    }

    private void rollback(UUID failedStep, OperationReport configReport,
            ConfiguratorException exception) {
        configReport.putResult(failedStep, ResultImpl.fail(exception));

        Deque<Map.Entry<UUID, Operation>> undoStack = new ArrayDeque<>();
        boolean skipRest = false;

        for (Map.Entry<UUID, Operation> row : configHandlers.entrySet()) {
            if (failedStep.equals(row.getKey())) {
                skipRest = true;
            }

            if (!skipRest) {
                undoStack.push(row);
            } else if (!failedStep.equals(row.getKey())) {
                configReport.putResult(row.getKey(), ResultImpl.skip());
            }
        }

        for (Map.Entry<UUID, Operation> row : undoStack) {
            try {
                Result result = row.getValue()
                        .rollback();

                configReport.putResult(row.getKey(), result);
            } catch (ConfiguratorException e) {
                Optional operationData = configReport.getResult(row.getKey())
                        .getOperationData();
                if (operationData.isPresent()) {
                    configReport.putResult(row.getKey(),
                            ResultImpl.rollbackFailWithData(e, operationData.get()));
                } else {
                    configReport.putResult(row.getKey(), ResultImpl.rollbackFail(e));
                }
            }
        }
    }
}
