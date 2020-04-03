/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.commands.catalog.validation;

import ddf.catalog.data.Metacard;
import ddf.catalog.util.Describable;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ValidationException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class ValidateExecutor {

  public static List<ValidateReport> execute(
      List<Metacard> metacards, List<MetacardValidator> validators)
      throws ExecutionException, InterruptedException {

    ForkJoinPool forkJoinPool = new ForkJoinPool();
    List<ValidateReport> results;
    results =
        forkJoinPool
            .submit(
                () ->
                    metacards.stream()
                        .parallel()
                        .map(metacard -> generateReport(metacard, validators))
                        .collect(Collectors.toList()))
            .get();
    return results;
  }

  private static ValidateReport generateReport(
      Metacard metacard, List<MetacardValidator> validators) {

    ValidateReport report = new ValidateReport(metacard.getTitle());

    for (MetacardValidator validator : validators) {
      try {
        validator.validate(metacard);
      } catch (ValidationException e) {
        String name = validator.getClass().getName();
        if (validator instanceof Describable && ((Describable) validator).getId() != null) {
          name = ((Describable) validator).getId();
        }
        List<String> warnings = e.getWarnings() == null ? Collections.emptyList() : e.getWarnings();
        List<String> errors = e.getErrors() == null ? Collections.emptyList() : e.getErrors();
        report.addEntry(new ValidateReportEntry(name, errors, warnings));
      }
    }

    return report;
  }
}
