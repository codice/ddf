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

import org.codice.ddf.admin.configurator.ConfiguratorException;
import org.codice.ddf.admin.configurator.Result;
import org.codice.ddf.admin.configurator.Status;

public class ResultImpl<T> implements Result<T> {
    private final Status status;

    private final ConfiguratorException error;

    private final T data;

    private ResultImpl(Status status, ConfiguratorException error, T data) {
        this.status = status;
        this.error = error;
        this.data = data;
    }

    static Result<Void> pass() {
        return new ResultImpl<>(Status.COMMIT_PASSED, null, null);
    }

    static <S> Result<S> passWithData(S data) {
        return new ResultImpl<>(Status.COMMIT_PASSED, null, data);
    }

    static Result<Void> fail(ConfiguratorException error) {
        return new ResultImpl<>(Status.COMMIT_FAILED, error, null);
    }

    static Result<Void> rollback() {
        return new ResultImpl<>(Status.ROLLBACK_PASSED, null, null);
    }

    static <S> Result<S> rollbackWithData(S data) {
        return new ResultImpl<>(Status.ROLLBACK_PASSED, null, data);
    }

    static Result<Void> rollbackFail(ConfiguratorException error) {
        return new ResultImpl<>(Status.ROLLBACK_FAILED, error, null);
    }

    static <S> Result<S> rollbackFailWithData(ConfiguratorException error, S data) {
        return new ResultImpl<>(Status.ROLLBACK_FAILED, error, data);
    }

    static Result<Void> skip() {
        return new ResultImpl<>(Status.SKIPPED, null, null);
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
    public Optional<ConfiguratorException> getError() {
        return Optional.ofNullable(error);
    }

    @Override
    public Optional<T> getOperationData() {
        return Optional.ofNullable(data);
    }
}
