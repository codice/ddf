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

import static org.apache.karaf.shell.support.ansi.SimpleAnsi.COLOR_CYAN;
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.COLOR_DEFAULT;
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.COLOR_RED;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.util.Describable;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ValidationException;

/**
 * Custom Karaf command to validate XML files against services that implement MetacardValidator
 */
@Command(scope = CatalogCommands.NAMESPACE, name = "validate", description = "Validates an XML file against all installed validators.")
@Service
public class ValidateCommand implements Action {

    @Argument(index = 0, name = "fileName", description = "The path to the file that you want to validate", required = true, multiValued = false)
    @Completion(FileCompleter.class)
    String filename;

    @Reference
    List<MetacardValidator> validators;

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateCommand.class);

    // Using system out for println is caught by our githooks to prevent bad practice.
    // We actually need to use it here.
    protected PrintStream console = System.out;

    // These constants are to help in trying to make the output
    // both uniform and readable.
    private static final String INDENT = "|   ";

    private static final String LAST_INDENT = "    ";

    private static final String ELEMENT = "+-- ";

    private static final String LAST_ELEMENT = "`-- ";

    private static final String WARNING_TITLE = "Warnings";

    private static final String ERROR_TITLE = "Errors";

    private static final String WARNING_COLOR = COLOR_CYAN;

    private static final String ERROR_COLOR = COLOR_RED;

    @Override
    public Object execute() throws Exception {

        if (fileExists() && hasValidators()) {
            Metacard metacard = createMetacard();
            if (metacard != null) {
                runValidators(metacard);
            }
        }

        // We must return an object per the Action contract which
        // this class implements. It is sufficient to return null.
        return null;
    }

    private boolean fileExists() {
        File fileToValidate = new File(filename);
        if (!fileToValidate.exists()) {
            console.printf(ERROR_COLOR +
                    "Unable to locate file '%s'. Double check the path is correct and the file exists.%n"
                    + COLOR_DEFAULT, filename);
            return false;
        }

        if (!fileToValidate.isFile()) {
            console.printf(ERROR_COLOR + "'%s' is not a file.%n" + COLOR_DEFAULT, filename);
            return false;
        }
        return true;
    }

    private boolean hasValidators() {
        if (validators == null || validators.size() < 1) {
            console.println(ERROR_COLOR + "No validators have been configured" + COLOR_DEFAULT);
            return false;
        }
        return true;
    }

    private Metacard createMetacard() {

        // Although we are given XML, we need to put it in a metacard to work with the validators.
        MetacardImpl metacard = null;
        try {
            String metadata = IOUtils.toString(new File(filename).toURI());
            metacard = new MetacardImpl();
            metacard.setMetadata(metadata);
        } catch (IOException e) {
            console.println(ERROR_COLOR + "Error reading file. Check the log for the stacktrace"
                    + COLOR_DEFAULT);
            LOGGER.error("Error trying to read file {}", filename, e);
        }
        return metacard;
    }

    private void runValidators(Metacard metacard) {

        // In an effort to make the output of this command more readable on the console,
        // we model it after the 'tree' command in Linux. We are printing a hierarchy of lists,
        // so we want to make the last element of the list pronounced so that we can more
        // easily see the groupings of the hierarchy. This is why we use an iterator
        // and if-else rather than a more simple for-loop.
        Iterator<MetacardValidator> iterator = validators.iterator();
        while (iterator.hasNext()) {
            MetacardValidator validator = iterator.next();
            if (iterator.hasNext()) {
                printValidator(validator, ELEMENT);
                printErrorsAndWarnings(validator, metacard, INDENT);
            } else {
                printValidator(validator, LAST_ELEMENT);
                printErrorsAndWarnings(validator, metacard, LAST_INDENT);
            }
        }
    }

    private void printValidator(MetacardValidator validator, String prefix) {
        String name = validator.getClass()
                .getName();
        if (validator instanceof Describable && ((Describable) validator).getId() != null) {
            name = ((Describable) validator).getId();
        }
        prettyPrint(prefix, name);
    }

    private void printErrorsAndWarnings(MetacardValidator validator, Metacard metacard,
            String prefix) {
        try {
            validator.validate(metacard);
            prettyPrint(prefix + LAST_ELEMENT, "No errors or warnings");
        } catch (ValidationException e) {

            // The seemingly unnecessary complexity here is because we want the end result
            // on the console to look good. We know that errors will come before warnings,
            // so the whole error block uses the "indent" prefix. Further, we print
            // errors and warnings with different
            prettyPrint(prefix + ELEMENT, ERROR_TITLE);
            if (e.getErrors() == null) {
                prettyPrint(prefix + INDENT + LAST_ELEMENT, "None");
            } else {
                printList(prefix + INDENT, e.getErrors(), ERROR_COLOR);
            }

            // We know that warnings come last, so we format each line knowing that it's the last
            // in its hierarchy.
            prettyPrint(prefix + LAST_ELEMENT, WARNING_TITLE);
            if (e.getWarnings() == null) {
                prettyPrint(prefix + LAST_INDENT + LAST_ELEMENT, "None");
            } else {
                printList(prefix + LAST_INDENT, e.getWarnings(), WARNING_COLOR);
            }

        } catch (RuntimeException e) {
            prettyPrint(prefix + LAST_ELEMENT,
                    "Unhandled exception while running validator. Check the log for the stack trace and fix the validator.",
                    ERROR_COLOR);
            LOGGER.error("Unhandled exception while trying to validate.", e);
        }
    }

    private void prettyPrint(String prefix, String message) {
        prettyPrint(prefix, message, COLOR_DEFAULT);
    }

    private void prettyPrint(String prefix, String message, String color) {

        // We want to get rid of all whitespace and newlines to make everything look uniform.
        console.println(prefix + color + message.replaceAll("[\r\n\t ]+", " ") + COLOR_DEFAULT);
    }

    private void printList(String prefix, List<String> messages, String color) {

        // Using an iterator instead of a for loop to make sure the last element
        // in the list is printed differently for console readability.
        Iterator<String> iterator = messages.iterator();
        while (iterator.hasNext()) {
            String message = iterator.next();
            if (iterator.hasNext()) {
                prettyPrint(prefix + ELEMENT, message, color);
            } else {
                prettyPrint(prefix + LAST_ELEMENT, message, color);
            }
        }
    }
}