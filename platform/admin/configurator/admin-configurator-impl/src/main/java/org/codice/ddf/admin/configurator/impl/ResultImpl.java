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

import java.util.Optional;

import org.codice.ddf.admin.configurator.Result;
import org.codice.ddf.admin.configurator.Status;

public class ResultImpl implements Result {

    private final Status status;

    private final Throwable badOutcome;

    private final String configId;

    private ResultImpl(Status status, Throwable badOutcome, String configId) {
        this.status = status;
        this.badOutcome = badOutcome;
        this.configId = configId;
    }

    static Result pass() {
        return new ResultImpl(Status.COMMIT_PASSED, null, null);
    }

    static Result passManagedService(String configId) {
        return new ResultImpl(Status.COMMIT_PASSED, null, configId);
    }

    static Result fail(Throwable throwable) {
        return new ResultImpl(Status.COMMIT_FAILED, throwable, null);
    }

    static Result rollback() {
        return new ResultImpl(Status.ROLLBACK_PASSED, null, null);
    }

    static Result rollbackFail(Throwable throwable) {
        return new ResultImpl(Status.ROLLBACK_FAILED, throwable, null);
    }

    static Result rollbackFailManagedService(Throwable throwable, String configId) {
        return new ResultImpl(Status.ROLLBACK_FAILED, throwable, configId);
    }

    static Result skip() {
        return new ResultImpl(Status.SKIPPED, null, null);
    }

    @Override
    public boolean isOperationSucceeded() {
        return status == Status.COMMIT_PASSED;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public Optional<Throwable> getBadOutcome() {
        return Optional.ofNullable(badOutcome);
    }

    @Override
    public String getConfigId() {
        return configId;
    }
}
