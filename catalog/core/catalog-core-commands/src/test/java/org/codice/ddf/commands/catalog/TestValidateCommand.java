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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.impl.CatalogFrameworkImpl;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ValidationException;

public class TestValidateCommand extends TestAbstractCommand {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    ValidateCommand command;

    MetacardValidator goodValidator;

    MetacardValidator badValidator;

    CatalogFramework mockCatalog;

    @Before
    public void setup() throws Exception {
        //mock out validators
        //good validator will mock out passing validation
        goodValidator = mock(MetacardValidator.class);
        doNothing().when(goodValidator)
                .validate(anyObject());

        //bad validator will mock out failing validation
        badValidator = mock(MetacardValidator.class);
        doThrow(new ValidationException() { //configure bad validator to throw exception
            @Override
            public List<String> getErrors() {
                List<String> errors = new ArrayList<String>();
                errors.add("someError");
                errors.add("someOtherError");
                return errors;
            }

            @Override
            public List<String> getWarnings() {
                List<String> warnings = new ArrayList<String>();
                warnings.add("someWarning");
                warnings.add("someOtherWarning");
                return warnings;
            }
        }).when(badValidator)
                .validate(anyObject());

        //mock out catalog
        command = new ValidateCommand();
        mockCatalog = mock(CatalogFrameworkImpl.class);
        command.catalog = mockCatalog;
    }

    //test having no validators
    @Test
    public void testNoValidators() throws Exception {
        //capture console output
        ConsoleOutput consoleOutput = new ConsoleOutput();
        consoleOutput.interceptSystemOut();

        command.path = testFolder.getRoot()
                .getAbsolutePath() + "aFileThatDoesntExist.xml";
        command.execute(); //execute with null validators list
        assertThat(consoleOutput.getOutput(), containsString("No validators have been configured"));

        command.validators = Collections.emptyList();
        command.execute(); //execute with empty validators list
        assertThat(consoleOutput.getOutput(), containsString("No validators have been configured"));

        consoleOutput.resetSystemOut();
        consoleOutput.closeBuffer();
    }

    //test invalid file
    @Test(expected = FileNotFoundException.class)
    public void testInvalidFile() throws Exception {
        command.path = testFolder.getRoot()
                .getAbsolutePath() + "aFileThatDoesntExist.xml";
        command.validators = new ArrayList<>();
        command.validators.add(goodValidator);

        command.execute();
    }

    //test a single valid file
    @Test
    public void testValidFile() throws Exception {
        int fileCount = 1;
        File newFile = testFolder.newFile("temp.xml");
        command.path = newFile.getAbsolutePath();

        command.validators = new ArrayList<>();
        command.validators.add(goodValidator);

        assertThat(fileCount - (int) command.execute(), equalTo(fileCount));
    }

    //test valid file that fails validation
    @Test
    public void testValidFileFailedValidation() throws Exception {
        int fileCount = 1;
        File newFile = testFolder.newFile("temp.xml");
        command.path = newFile.getAbsolutePath();

        command.validators = new ArrayList<>();
        command.validators.add(badValidator);

        assertThat(fileCount - (int) command.execute(), equalTo(0));
    }

    //test empty directory
    @Test
    public void testEmptyDirectory() throws Exception {
        int fileCount = 0;
        command.path = testFolder.getRoot()
                .getAbsolutePath();

        command.validators = new ArrayList<>();
        command.validators.add(goodValidator);

        assertThat(fileCount - (int) command.execute(), equalTo(fileCount));
    }

    //test directory with one file
    @Test
    public void testSingleFileDirectory() throws Exception {
        int fileCount = 1;
        testFolder.newFile("temp.xml");
        command.path = testFolder.getRoot()
                .getAbsolutePath();

        command.validators = new ArrayList<>();
        command.validators.add(goodValidator);

        assertThat(fileCount - (int) command.execute(), equalTo(fileCount));
    }

    //test directory with multiple files
    @Test
    public void testMultipleFileDirectory() throws Exception {
        int fileCount = 4;
        testFolder.newFile("temp1.xml");
        testFolder.newFile("temp2.xml");
        testFolder.newFile("temp3.xml");
        testFolder.newFile("temp4.xml");
        command.path = testFolder.getRoot()
                .getAbsolutePath();

        command.validators = new ArrayList<>();
        command.validators.add(goodValidator);

        assertThat(fileCount - (int) command.execute(), equalTo(fileCount));
    }

    //test directory with multiple files with a good and bad validator
    @Test
    public void testMultipleFileDirectoryGoodBadValidation() throws Exception {
        int fileCount = 4;
        testFolder.newFile("temp1.xml");
        testFolder.newFile("temp2.xml");
        testFolder.newFile("temp3.xml");
        testFolder.newFile("temp4.xml");
        command.path = testFolder.getRoot()
                .getAbsolutePath();

        command.validators = new ArrayList<>();
        command.validators.add(goodValidator);
        command.validators.add(badValidator);

        assertThat(fileCount - (int) command.execute(), equalTo(0));
    }

    //helper methods to set up mock catalog for cqlQuery tests
    private void setupEmptyCatalog() throws Exception {
        QueryResponse mockResponse = mock(QueryResponseImpl.class);
        doReturn(null).when(mockResponse)
                .getResults();
        doReturn(mockResponse).when(mockCatalog)
                .query(anyObject());
    }

    private void setupNonEmptyCatalog() throws Exception {
        QueryResponse mockResponse = mock(QueryResponseImpl.class);
        ArrayList<Result> results = new ArrayList<>();

        ResultImpl fakeResult = new ResultImpl();
        MetacardImpl fakeMetacard = new MetacardImpl();
        fakeMetacard.setTitle("fakeMetacard");
        fakeResult.setMetacard(fakeMetacard);
        results.add(fakeResult);

        doReturn(results).when(mockResponse)
                .getResults();
        doReturn(mockResponse).when(mockCatalog)
                .query(anyObject());
    }

    //test cqlQuery with no results
    @Test
    public void testQueryNoResults() throws Exception {
        setupEmptyCatalog();

        command.cqlQuery = "title like 'fake'";
        command.validators = new ArrayList<>();
        command.validators.add(badValidator);

        assertThat(command.execute(), equalTo(0));
    }

    //test cqlQuery with results
    @Test
    public void testQueryWithResults() throws Exception {
        setupNonEmptyCatalog();

        command.cqlQuery = "title like 'fake'";
        command.validators = new ArrayList<>();
        command.validators.add(badValidator);

        assertThat(command.execute(), equalTo(1));
    }
}
