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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.codice.ddf.admin.configurator.OperationReport;
import org.codice.ddf.admin.configurator.Result;

public class OperationReportImpl implements OperationReport {
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

    @Override
    public void putResult(String key, Result result) {
        results.put(key, result);
    }
}
