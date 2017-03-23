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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.codice.ddf.admin.configurator.OperationReport;
import org.codice.ddf.admin.configurator.Result;

public class OperationReportImpl implements OperationReport {
    private final Map<UUID, Result> results = new LinkedHashMap<>();

    public boolean hasTransactionSucceeded() {
        return results.values()
                .stream()
                .allMatch(Result::isOperationSucceeded);
    }

    public Result getResult(UUID key) {
        return results.get(key);
    }

    public List<Result> getFailedResults() {
        return Collections.unmodifiableList(results.values()
                .stream()
                .filter(((Predicate<Result>) Result::isOperationSucceeded).negate())
                .collect(Collectors.toList()));
    }

    public boolean containsFailedResults() {
        return getFailedResults().size() != 0;
    }

    @Override
    public void putResult(UUID key, Result result) {
        results.put(key, result);
    }
}
