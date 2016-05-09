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

import static org.apache.karaf.shell.support.ansi.SimpleAnsi.COLOR_DEFAULT;
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.COLOR_RED;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.codice.ddf.commands.util.RuleSet;
import org.codice.ddf.commands.util.ValidatorConfigReader;
import org.codice.ddf.commands.util.ValidatorRuleSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.util.Describable;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ValidationException;

@Command(scope = CatalogCommands.NAMESPACE, name = "score", description = "Score an XML file according to a pre-defined rubric.")
@Service
public class ScoreCommand implements Action {

    @Argument(name = "fileName", description = "The path to the file that you want to validate", required = true)
    @Completion(FileCompleter.class)
    String filename;

    @Reference
    List<MetacardValidator> validators;

    private static final Logger LOGGER = LoggerFactory.getLogger(ScoreCommand.class);

    RuleSet ruleSet;

    private String ruleSetFilename = Paths.get(System.getProperty("ddf.home"), "etc", "validation-ruleset.xml").toString();

    // Using system out for println is caught by our githooks to prevent bad practice.
    // We actually need to use it here.
    protected PrintStream console = System.out;

    private static final String ERROR_COLOR = COLOR_RED;

    // Put all the formatting in one place so it's consistent and easy to change
    private static final int COLUMN_1_WIDTH = 20;
    private static final int COLUMN_2_WIDTH = 47;
    private static final int COLUMN_3_WIDTH = 5;
    private static final String TITLE = StringUtils.center("Metadata Quality Report", 80);
    private static final String SEPARATOR = StringUtils.repeat("-", COLUMN_1_WIDTH) + "-+-" + StringUtils.repeat("-", COLUMN_2_WIDTH) + "-+-" + StringUtils.repeat("-", COLUMN_3_WIDTH);
    private static final String TOTAL = StringUtils.repeat(" ", COLUMN_1_WIDTH + COLUMN_2_WIDTH + COLUMN_3_WIDTH - 7) + "Total  ";
    private static final String FORMAT = "%-" + COLUMN_1_WIDTH + "s | %-" + COLUMN_2_WIDTH + "s | %" + COLUMN_3_WIDTH + "s %n";


    @Override
    public Object execute() throws Exception {

        try {
            File file = new File(ruleSetFilename);
            InputStream ruleSetInputStream = new FileInputStream(file);
            ruleSet = new ValidatorConfigReader(ruleSetInputStream).getRuleSet();
        } catch (Exception e) {
            console.printf(ERROR_COLOR + "Ruleset not configured.%n" + COLOR_DEFAULT, filename);
            LOGGER.error("Error trying to configure ruleset {}", ruleSetFilename, e);
            return null;
        }

        if (fileExists() && hasValidators()) {
            Metacard metacard = createMetacard();
            if (metacard != null) {
                score(metacard);
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
            console.println(ERROR_COLOR + "Error reading file. Check the log for the stacktrace" + COLOR_DEFAULT);
            LOGGER.error("Error trying to read file {}", filename, e);
        }
        return metacard;
    }

    private MetacardValidator getValidator(String nameToFind) throws Exception {
        for (MetacardValidator validator : validators) {
            String name = validator.getClass().getName();
            if (validator instanceof Describable && ((Describable) validator).getId() != null) {
                name = ((Describable) validator).getId();
            }

            if (name.equals(nameToFind)) {
                return validator;
            }
        }

        throw new Exception(String.format("Validator with name %s not found. Check that the name is correct in the scoring configuration and that the validator is running.", nameToFind));
    }

    protected double score(Metacard metacard) throws Exception {
        console.println();
        console.println(TITLE);
        console.println(SEPARATOR);

        double score = 100;
        List<ValidatorRuleSet> ruleSets = ruleSet.getValidatorRuleSets();
        for (ValidatorRuleSet validatorRuleSet : ruleSets) {
            try {
                MetacardValidator validator = getValidator(validatorRuleSet.getValidatorName());
                validator.validate(metacard);
            } catch (ValidationException e) {
                List<String> errors = e.getErrors() != null ? e.getErrors() : new ArrayList<>();
                List<String> warnings = e.getWarnings() != null ? e.getWarnings() : new ArrayList<>();
                List<String> all = new ArrayList<>();
                all.addAll(errors);
                all.addAll(warnings);

                if (validatorRuleSet.getErrorWeight() > 0) {
                    score -= validatorRuleSet.getErrorWeight();
                    printWhole(validatorRuleSet.getValidatorName(), errors, validatorRuleSet.getErrorWeight());
                } else if (validatorRuleSet.getWarningWeight() > 0) {
                    score -= validatorRuleSet.getWarningWeight();
                    printWhole(validatorRuleSet.getValidatorName(), warnings, validatorRuleSet.getWarningWeight());
                } else if (validatorRuleSet.getAnyWeight() > 0) {
                    score -= validatorRuleSet.getAnyWeight();
                    printWhole(validatorRuleSet.getValidatorName(), all, validatorRuleSet.getAnyWeight());
                } else {
                    score -= printParts(validatorRuleSet.getValidatorName(), all, validatorRuleSet);
                }
            }
        }
        console.printf("%s %.2f%n", TOTAL, score);
        return score;
    }

    private void printWhole(String name, List<String> messages, double score) {
        printLine(name, "", String.format("%.2f", score));
        for (String message : messages) {
            printLine("", message, "");
        }
        console.println(SEPARATOR);
    }

    private double printParts(String name, List<String> messages, ValidatorRuleSet ruleSet) {
        printLine(name, "", "");
        double weight;
        double total = 0;
        for (String message : messages) {
            weight = ruleSet.getRuleWeight(message);
            total += weight;
            if (weight > 0) {
                printLine("", message, String.format("%.2f", weight));
            }
        }
        console.println(SEPARATOR);
        return total;
    }

    private void printLine(String name, String message, String score) {
        boolean moreToPrint = true;

        message = message.replaceAll("[\r\n\t ]+", " ").trim();

        while (moreToPrint) {
            moreToPrint = false;
            String nameSubstring = name.substring(0, Math.min(name.length(), COLUMN_1_WIDTH));
            String messageSubstring = message.substring(0, Math.min(message.length(), COLUMN_2_WIDTH));


            if (name.length() > COLUMN_1_WIDTH) {
                name = name.substring(COLUMN_1_WIDTH);
                moreToPrint = true;
            }

            if (message.length() > COLUMN_2_WIDTH) {
                message = message.substring(COLUMN_2_WIDTH);
                moreToPrint = true;
            }

            if (moreToPrint) {
                console.printf(FORMAT, nameSubstring, messageSubstring, "");
            } else {
                console.printf(FORMAT, nameSubstring, messageSubstring, score);
            }
        }
        console.printf(FORMAT, "", "", "");
    }

    public void setRuleSetFilename(String ruleSetFilename) {
        this.ruleSetFilename = ruleSetFilename;
    }
}
