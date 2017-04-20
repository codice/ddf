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
package org.codice.ddf.admin.api.configurator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class OperationReport {
    private final Map<String, Result> results = new LinkedHashMap<>();

    public boolean txactSucceeded() {
        return results.values()
                .stream()
                .allMatch(Result::isTxactSucceeded);
    }

    public Result getResult(String key) {
        return results.get(key);
    }

    public List<Result> getFailedResults() {
        return results.values()
                .stream()
                .filter(((Predicate<Result>) Result::isTxactSucceeded).negate())
                .collect(Collectors.toList());
    }

    public boolean containsFailedResults() {
        return getFailedResults().size() != 0;
    }

    public void putResult(String key, Result result) {
        results.put(key, result);
    }

    enum Status {
        COMMIT_PASSED, COMMIT_FAILED, SKIPPED, ROLLBACK_PASSED, ROLLBACK_FAILED;
    }

    static class Result {
        private final Status status;

        private final Throwable badOutcome;

        private final String configId;

        private Result(Status status, Throwable badOutcome, String configId) {
            this.status = status;
            this.badOutcome = badOutcome;
            this.configId = configId;
        }

        static Result pass() {
            return new Result(Status.COMMIT_PASSED, null, null);
        }

        static Result passManagedService(String configId) {
            return new Result(Status.COMMIT_PASSED, null, configId);
        }

        static Result fail(Throwable throwable) {
            return new Result(Status.COMMIT_FAILED, throwable, null);
        }

        static Result rollback() {
            return new Result(Status.ROLLBACK_PASSED, null, null);
        }

        static Result rollbackFail(Throwable throwable) {
            return new Result(Status.ROLLBACK_FAILED, throwable, null);
        }

        static Result rollbackFailManagedService(Throwable throwable, String configId) {
            return new Result(Status.ROLLBACK_FAILED, throwable, configId);
        }

        static Result skip() {
            return new Result(Status.SKIPPED, null, null);
        }

        public boolean isTxactSucceeded() {
            return status == Status.COMMIT_PASSED;
        }

        public Status getStatus() {
            return status;
        }

        public Optional<Throwable> getBadOutcome() {
            return Optional.ofNullable(badOutcome);
        }

        public String getConfigId() {
            return configId;
        }
    }
}
