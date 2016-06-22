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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ddf.catalog.data.Metacard;
import ddf.catalog.util.Describable;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ValidationException;

public class TestValidateCommand {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setup() throws IOException {
        testFolder.newFile("empty.xml");
    }

    @Test
    public void testWithPassingValidator() throws Exception {
        List<MetacardValidator> validators = new ArrayList<>();
        validators.add(mock(MetacardValidator.class));
        assertThat(execute(validators), containsString("No errors or warnings"));
    }

    @Test
    public void testWithInvalidPath() throws Exception {
        List<MetacardValidator> validators = new ArrayList<>();
        validators.add(mock(MetacardValidator.class));
        assertThat(execute(validators, "not_a_real_file.xml"), containsString("Unable to locate"));
    }

    @Test
    public void testWithWarning() throws Exception {
        List<MetacardValidator> validators = new ArrayList<>();
        MockValidatorWithException mockValidatorWithException = new MockValidatorWithException();
        mockValidatorWithException.warnings = new ArrayList<>(Arrays.asList("sample warning"));
        validators.add(mockValidatorWithException.getValidator());
        assertThat(execute(validators), containsString("sample warning"));
    }

    @Test
    public void testWithError() throws Exception {
        List<MetacardValidator> validators = new ArrayList<>();
        MockValidatorWithException mockValidatorWithException = new MockValidatorWithException();
        mockValidatorWithException.errors = new ArrayList<>(Arrays.asList("sample error"));
        validators.add(mockValidatorWithException.getValidator());
        assertThat(execute(validators), containsString("sample error"));
    }

    @Test
    public void testWithMultipleValidators() throws Exception {
        List<MetacardValidator> validators = new ArrayList<>();
        MockValidatorWithException mockValidatorWithException1 = new MockValidatorWithException();
        MockValidatorWithException mockValidatorWithException2 = new MockValidatorWithException();
        mockValidatorWithException1.id = "validator 1";
        mockValidatorWithException2.id = "validator 2";
        validators.add(mockValidatorWithException1.getValidator());
        validators.add(mockValidatorWithException2.getValidator());
        assertThat(execute(validators), containsString("validator 1"));
        assertThat(execute(validators), containsString("validator 2"));
    }

    @Test
    public void testWithErrorsAndWarnings() throws Exception {
        List<MetacardValidator> validators = new ArrayList<>();
        MockValidatorWithException mockValidatorWithException = new MockValidatorWithException();
        mockValidatorWithException.errors = new ArrayList<>(Arrays.asList("error 1", "error 2"));
        mockValidatorWithException.warnings = new ArrayList<>(Arrays.asList("warning 1", "warning 2"));
        validators.add(mockValidatorWithException.getValidator());
        assertThat(execute(validators), containsString("error 1"));
        assertThat(execute(validators), containsString("error 2"));
        assertThat(execute(validators), containsString("warning 1"));
        assertThat(execute(validators), containsString("warning 2"));
    }

    @Test
    public void testWithValidatorsEqualNull() throws Exception {
        assertThat(execute(null), containsString("No validators have been configured"));
    }

    @Test
    public void testWithValidatorsSizeZero() throws Exception {
        assertThat(execute(new ArrayList<>()), containsString("No validators have been configured"));
    }

    private String execute(List<MetacardValidator> validators) throws Exception {
        return execute(validators, "empty.xml");
    }

    private String execute(List<MetacardValidator> validators, String filename) throws Exception {
        ConsoleOutput consoleOutput = new ConsoleOutput();
        consoleOutput.interceptSystemOut();

        ValidateCommand command = new ValidateCommand();
        command.filename = Paths.get(testFolder.getRoot().getAbsolutePath(), filename).toString();
        command.validators = validators;
        command.execute();
        consoleOutput.resetSystemOut();
        return consoleOutput.getOutput();
    }

    private class MockValidatorWithException {
        String id;
        List<String> warnings;
        List<String> errors;

        MetacardValidator getValidator() throws ValidationException {
            ValidationException validationException = mock(ValidationException.class);
            when(validationException.getErrors()).thenReturn(errors);
            when(validationException.getWarnings()).thenReturn(warnings);
            MetacardValidator metacardValidator = mock(MetacardValidator.class, withSettings().extraInterfaces(Describable.class));
            when(((Describable) metacardValidator).getId()).thenReturn(id);
            doThrow(validationException).when(metacardValidator).validate(any(Metacard.class));
            return metacardValidator;
        }
    }
}
