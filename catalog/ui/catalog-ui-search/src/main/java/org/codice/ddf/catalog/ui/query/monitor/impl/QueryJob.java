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
 */
package org.codice.ddf.catalog.ui.query.monitor.impl;

import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quartz Job that calls {@link WorkspaceQueryService#run()}. Requires that a WorkspaceQueryService
 * be registered as a service reference.
 */
public class QueryJob implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryJob.class);

    private static final Lock LOCK = new ReentrantLock();

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        LOCK.lock();
        try {
            getWorkspaceQueryService().ifPresent(WorkspaceQueryService::run);
        } finally {
            LOCK.unlock();
        }
    }

    @Override
    public String toString() {
        return "QueryJob{}";
    }

    private Bundle getBundle() {
        return FrameworkUtil.getBundle(getClass());
    }

    private Optional<WorkspaceQueryService> getWorkspaceQueryService() {
        BundleContext bundleContext = getBundle().getBundleContext();
        if (bundleContext == null) {
            LOGGER.warn("unable to get the bundle context");
            return Optional.empty();
        }
        return Optional.of(bundleContext.getService(bundleContext.getServiceReference(
                WorkspaceQueryService.class)));
    }

}
