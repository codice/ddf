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
package org.codice.ddf.commands.catalog;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.validation.MetacardValidator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.commands.catalog.validation.ValidateExecutor;
import org.codice.ddf.commands.catalog.validation.ValidatePrinter;
import org.codice.ddf.commands.catalog.validation.ValidateReport;
import org.codice.ddf.commands.util.CatalogCommandException;
import org.codice.ddf.security.common.Security;
import org.geotools.filter.text.cql2.CQLException;

/** Custom Karaf command to validate XML files against services that implement MetacardValidator */
@Service
@Command(
  scope = CatalogCommands.NAMESPACE,
  name = "validate",
  description = "Validates an XML file against all installed validators."
)
public class ValidateCommand extends CqlCommands {

  @Option(name = "--path", aliases = "-p", description = "The path to the file to be validated")
  @Completion(FileCompleter.class)
  String path;

  @Option(
    name = "--recurse",
    aliases = "-r",
    description =
        "Allows for searching subdirectories "
            + "of the specified directory to be searched for metacards."
  )
  boolean recurse = false;

  @Option(
    name = "--include-extensions",
    multiValued = true,
    description =
        "List of file extensions to use in the path search. Leave blank for all "
            + "file extensions to be included."
  )
  List<String> filteredExtensions;

  @Reference List<MetacardValidator> validators;

  private ValidatePrinter printer;

  public ValidateCommand() {
    printer = new ValidatePrinter();
  }

  public ValidateCommand(ValidatePrinter printer) {
    this.printer = printer;
  }

  @Override
  public Object executeWithSubject() throws Exception {
    int numMetacardsWithErrorsOrWarnings = 0;
    if (validators == null || validators.size() == 0) {
      printer.printError("No validators have been configured");
    } else {
      List<Metacard> metacards;
      if (path != null) {
        metacards = createMetacardsFromFiles();
      } else if (hasFilter()) {
        metacards = getMetacardsFromCatalog();
      } else {
        printer.printError("Usage: catalog:validate < --path filePath > < --cql cqlQuery >");
        return null;
      }

      List<ValidateReport> reports = ValidateExecutor.execute(metacards, validators);
      for (ValidateReport report : reports) {
        if (report.getEntries().size() > 0) {
          numMetacardsWithErrorsOrWarnings++;
          printer.print(report);
        }
      }
      printer.printSummary(numMetacardsWithErrorsOrWarnings, reports.size());
    }

    return null;
  }

  private List<Metacard> createMetacardsFromFiles() throws IOException {
    Collection<File> files = getFiles();
    List<Metacard> metacards = new ArrayList<>();
    for (File file : files) {
      Metacard metacard = new MetacardImpl();
      String metadata = IOUtils.toString(file.toURI(), StandardCharsets.UTF_8);
      metacard.setAttribute(new AttributeImpl(Metacard.METADATA, metadata));
      metacard.setAttribute(new AttributeImpl(Metacard.TITLE, file.getName()));
      metacards.add(metacard);
    }
    return metacards;
  }

  private List<Metacard> getMetacardsFromCatalog() throws CatalogCommandException {
    List<Metacard> results = new ArrayList<>();
    try {
      QueryImpl query = new QueryImpl(getFilter());
      ThreadContext.bind(Security.getInstance().getSystemSubject());

      SourceResponse response = getCatalog().query(new QueryRequestImpl(query));
      List<Result> resultList = response.getResults();
      if (resultList != null) {
        results.addAll(
            resultList
                .stream()
                .map(Result::getMetacard)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
      }

    } catch (UnsupportedQueryException
        | SourceUnavailableException
        | FederationException
        | CQLException
        | ParseException e) {
      throw new CatalogCommandException("Error executing catalog:validate", e);
    }
    return results;
  }

  private Collection<File> getFiles() throws FileNotFoundException {
    File file = new File(path);

    if (!file.exists()) {
      printer.printError("File not found.");
      throw new FileNotFoundException(String.format("File or directory %s does not exist", path));
    }

    Collection<File> files;
    if (file.isFile()) {
      files = Collections.singletonList(file);
    } else if (filteredExtensions == null) { // directory with any extensions
      files = FileUtils.listFiles(file, null, recurse);
    } else { // directory with restricted extensions
      files =
          FileUtils.listFiles(
              file, filteredExtensions.toArray(new String[filteredExtensions.size()]), recurse);
    }

    return files;
  }
}
