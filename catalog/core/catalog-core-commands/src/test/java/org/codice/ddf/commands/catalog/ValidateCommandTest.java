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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ValidationException;
import ddf.security.Subject;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.karaf.shell.api.console.Session;
import org.codice.ddf.commands.catalog.validation.ValidatePrinter;
import org.codice.ddf.security.Security;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ValidateCommandTest {

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  ValidateCommand validateCommand;

  MetacardValidator goodValidator;

  MetacardValidator badValidator;

  CatalogFramework mockCatalog = mock(CatalogFramework.class);

  ValidatePrinter mockPrinter = mock(ValidatePrinter.class);

  @Mock Subject subject;

  @Mock Security security;

  @Mock Session session;

  @Before
  public void setup() throws Exception {
    // mock out validators
    // good validator will mock out passing validation
    goodValidator = mock(MetacardValidator.class);
    doNothing().when(goodValidator).validate(any());

    // bad validator will mock out failing validation
    badValidator = mock(MetacardValidator.class);
    doThrow(
            new ValidationException() { // configure bad validator to throw exception
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
            })
        .when(badValidator)
        .validate(any());

    // mock out catalog
    validateCommand = new ValidateCommandUnderTest(mockPrinter);
    validateCommand.catalogFramework = mockCatalog;

    when(security.getSystemSubject()).thenReturn(subject);
    when(subject.execute(any(Callable.class))).thenAnswer(this::executeCommand);
  }

  private Object executeCommand(InvocationOnMock invocationOnMock) throws Throwable {
    return ((Callable) invocationOnMock.getArguments()[0]).call();
  }

  // test having no validators
  @Test
  public void testNoValidators() throws Exception {
    validateCommand.path = testFolder.getRoot().getAbsolutePath() + "aFileThatDoesntExist.xml";
    validateCommand.executeWithSubject();

    validateCommand.validators = Collections.emptyList();
    validateCommand.executeWithSubject(); // execute with empty validators list

    verify(mockPrinter, times(2)).printError("No validators have been configured");
  }

  // test invalid file
  @Test(expected = FileNotFoundException.class)
  public void testInvalidFile() throws Exception {
    validateCommand.path = testFolder.getRoot().getAbsolutePath() + "aFileThatDoesntExist.xml";
    validateCommand.validators = new ArrayList<>();
    validateCommand.validators.add(goodValidator);

    validateCommand.executeWithSubject();
  }

  // test a single valid file
  @Test
  public void testValidFile() throws Exception {
    File newFile = testFolder.newFile("temp.xml");
    validateCommand.path = newFile.getAbsolutePath();

    validateCommand.validators = new ArrayList<>();
    validateCommand.validators.add(goodValidator);

    validateCommand.executeWithSubject();
    verify(mockPrinter).printSummary(0, 1); // 0 errors, one message saying so
  }

  // test valid file that fails validation
  @Test
  public void testValidFileFailedValidation() throws Exception {
    File newFile = testFolder.newFile("temp.xml");
    validateCommand.path = newFile.getAbsolutePath();

    validateCommand.validators = new ArrayList<>();
    validateCommand.validators.add(badValidator);

    validateCommand.executeWithSubject();
    verify(mockPrinter).printSummary(1, 1);
  }

  // test empty directory
  @Test
  public void testEmptyDirectory() throws Exception {
    validateCommand.path = testFolder.getRoot().getAbsolutePath();

    validateCommand.validators = new ArrayList<>();
    validateCommand.validators.add(goodValidator);

    validateCommand.executeWithSubject();
    verify(mockPrinter).printSummary(0, 0);
  }

  // test directory with one file
  @Test
  public void testSingleFileDirectory() throws Exception {
    testFolder.newFile("temp.xml");
    validateCommand.path = testFolder.getRoot().getAbsolutePath();

    validateCommand.validators = new ArrayList<>();
    validateCommand.validators.add(goodValidator);

    validateCommand.executeWithSubject();
    verify(mockPrinter).printSummary(0, 1);
  }

  // test directory with multiple files
  @Test
  public void testMultipleFileDirectory() throws Exception {
    testFolder.newFile("temp1.xml");
    testFolder.newFile("temp2.xml");
    testFolder.newFile("temp3.xml");
    testFolder.newFile("temp4.xml");
    validateCommand.path = testFolder.getRoot().getAbsolutePath();
    validateCommand.validators = new ArrayList<>();
    validateCommand.validators.add(goodValidator);

    validateCommand.executeWithSubject();
    verify(mockPrinter).printSummary(0, 4);
  }

  // test directory with multiple files with a good and bad validator
  @Test
  public void testMultipleFileDirectoryGoodBadValidation() throws Exception {
    testFolder.newFile("temp1.xml");
    testFolder.newFile("temp2.xml");
    testFolder.newFile("temp3.xml");
    testFolder.newFile("temp4.xml");
    validateCommand.path = testFolder.getRoot().getAbsolutePath();

    validateCommand.validators = new ArrayList<>();
    validateCommand.validators.add(goodValidator);
    validateCommand.validators.add(badValidator);

    validateCommand.executeWithSubject();
    verify(mockPrinter).printSummary(4, 4);
  }

  // helper methods to set up mock catalog for cqlQuery tests
  private void setupEmptyCatalog() throws Exception {
    QueryResponse mockResponse = mock(QueryResponseImpl.class);
    doReturn(null).when(mockResponse).getResults();
    doReturn(mockResponse).when(mockCatalog).query(any());
  }

  private void setupNonEmptyCatalog() throws Exception {
    QueryResponse mockResponse = mock(QueryResponseImpl.class);
    ArrayList<Result> results = new ArrayList<>();

    ResultImpl fakeResult = new ResultImpl();
    MetacardImpl fakeMetacard = new MetacardImpl();
    fakeMetacard.setTitle("fakeMetacard");
    fakeResult.setMetacard(fakeMetacard);
    results.add(fakeResult);

    doReturn(results).when(mockResponse).getResults();
    doReturn(mockResponse).when(mockCatalog).query(any());
  }

  // test cqlQuery with no results
  @Test
  public void testQueryNoResults() throws Exception {
    setupEmptyCatalog();

    validateCommand.cqlFilter = "title like 'fake'";
    validateCommand.validators = new ArrayList<>();
    validateCommand.validators.add(badValidator);

    validateCommand.executeWithSubject();
    verify(mockPrinter).printSummary(0, 0);
  }

  // test cqlQuery with results
  @Test
  public void testQueryWithResults() throws Exception {
    setupNonEmptyCatalog();

    validateCommand.cqlFilter = "title like 'fake'";
    validateCommand.validators = new ArrayList<>();
    validateCommand.validators.add(badValidator);

    validateCommand.executeWithSubject();
    verify(mockPrinter).printSummary(1, 1);
  }

  private class ValidateCommandUnderTest extends ValidateCommand {

    ValidateCommandUnderTest(ValidatePrinter printer) {
      super(printer);
      this.session = ValidateCommandTest.this.session;
      this.security = ValidateCommandTest.this.security;
    }
  }
}
