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
 */
package org.codice.ddf.commands.catalog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.karaf.shell.api.action.Action;
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
import org.codice.ddf.security.common.Security;
import org.geotools.filter.text.cql2.CQL;
import org.opengis.filter.Filter;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.validation.MetacardValidator;

/**
 * Custom Karaf command to validate XML files against services that implement MetacardValidator
 */
@Command(scope = CatalogCommands.NAMESPACE, name = "validate", description = "Validates an XML file against all installed validators.")
@Service
public class ValidateCommand implements Action {

    @Option(name = "--path", aliases = "-p", description = "The path to the file to be validated")
    @Completion(FileCompleter.class)
    private String path;

    @Option(name = "--cqlQuery", aliases = "-q", description =
            "Search using CQL Filter expressions.\n" + "CQL Examples:\n"
                    + "\tTextual:   catalog:validate --cqlQuery \"title like 'some text'\"\n"
                    + "\tTemporal:  catalog:validate --cqlQuery \"modified before 2012-09-01T12:30:00Z\"\n"
                    + "\tSpatial:   catalog:validate --cqlQuery \"DWITHIN(location, POINT (1 2) , 10, kilometers)\"\n"
                    + "\tComplex:   catalog:validate --cqlQuery \"title like 'some text' AND modified before 2012-09-01T12:30:00Z\"")
    private String cqlQuery;

    @Option(name = "--recurse", aliases = "-r", description = "Allows for searching subdirectories "
            + "of the specified directory to be searched for metacards.")
    private boolean recurse = false;

    @Option(name = "--include-extensions", multiValued = true, description =
            "List of file extensions to use in the path search. Leave blank for all "
                    + "file extensions to be included.")
    private List<String> filteredExtensions;

    @Reference
    private List<MetacardValidator> validators;

    @Reference
    private CatalogFramework catalog;

    private ValidatePrinter printer;

    public ValidateCommand() {
        printer = new ValidatePrinter();
    }

    ValidateCommand(ValidatePrinter printer) {
        this.printer = printer;
    }

    @Override
    public Object execute() throws Exception {
        int numMetacardsWithErrorsOrWarnings = 0;
        if (validators == null || validators.size() == 0) {
            printer.printError("No validators have been configured");
        } else {
            List<Metacard> metacards;
            if (path != null) {
                metacards = createMetacardsFromFiles();
            } else if (cqlQuery != null) {
                metacards = getMetacardsFromCatalog();
            } else {
                printer.printError(
                        "Usage: catalog:validate < --path filePath > < --cqlQuery cqlQuery >");
                return null;
            }

            List<ValidateReport> reports = ValidateExecutor.execute(metacards, validators);
            for (ValidateReport report : reports) {
                if (report.getEntries()
                        .size() > 0) {
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
            String metadata = IOUtils.toString(file.toURI());
            metacard.setAttribute(new AttributeImpl(Metacard.METADATA, metadata));
            metacard.setAttribute(new AttributeImpl(Metacard.TITLE, file.getName()));
            metacards.add(metacard);
        }
        return metacards;
    }

    private List<Metacard> getMetacardsFromCatalog() throws Exception {
        List<Metacard> results = new ArrayList<>();

        Filter cqlFilter = CQL.toFilter(cqlQuery);

        QueryImpl query = new QueryImpl(cqlFilter);
        ThreadContext.bind(Security.getInstance()
                .getSystemSubject());

        SourceResponse response = catalog.query(new QueryRequestImpl(query));
        List<Result> resultList = response.getResults();
        if (resultList != null) {
            results.addAll(resultList.stream()
                    .map(Result::getMetacard)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        }

        return results;
    }

    private Collection<File> getFiles() throws FileNotFoundException {
        File file = new File(path);

        if (!file.exists()) {
            printer.printError("File not found.");
            throw new FileNotFoundException(String.format("File or directory %s does not exist",
                    path));
        }

        Collection<File> files;
        if (file.isFile()) {
            files = Collections.singletonList(file);
        } else if (filteredExtensions == null) { //directory with any extensions
            files = FileUtils.listFiles(file, null, recurse);
        } else { //directory with restricted extensions
            files = FileUtils.listFiles(file,
                    filteredExtensions.toArray(new String[filteredExtensions.size()]),
                    recurse);
        }

        return files;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCqlQuery() {
        return cqlQuery;
    }

    public void setCqlQuery(String cqlQuery) {
        this.cqlQuery = cqlQuery;
    }

    public boolean isRecurse() {
        return recurse;
    }

    public void setRecurse(boolean recurse) {
        this.recurse = recurse;
    }

    public List<MetacardValidator> getValidators() {
        return validators;
    }

    public void setValidators(List<MetacardValidator> validators) {
        this.validators = validators;
    }

    public CatalogFramework getCatalog() {
        return catalog;
    }

    public void setCatalog(CatalogFramework catalog) {
        this.catalog = catalog;
    }
}