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

import static java.util.stream.Collectors.toList;
import static org.codice.ddf.commands.catalog.CommandSupport.ERROR_COLOR;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.cache.SolrCacheMBean;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.karaf.shell.api.console.Session;
import org.fusesource.jansi.Ansi;
import org.junit.Before;
import org.junit.Test;

public class RemoveAllCommandTest extends ConsoleOutputCommon {

  private int batchSize;

  private int numCatalogCalls;

  private boolean forceCommand;

  private CatalogFramework catalogFrameworkMock;

  private QueryResponse queryResponse;

  private DeleteResponse deleteResponse;

  static final String DEFAULT_CONSOLE_COLOR = Ansi.ansi().reset().toString();

  static final String RED_CONSOLE_COLOR = Ansi.ansi().fg(ERROR_COLOR).toString();

  @Before
  public void setUp() throws Exception {

    catalogFrameworkMock = mock(CatalogFramework.class);
    queryResponse = mock(QueryResponse.class);
    deleteResponse = mock(DeleteResponse.class);
  }

  /**
   * If it is possible to give bad batch size, this test checks the proper outcome.
   *
   * @throws Exception
   */
  @Test
  public void testBadBatchSize() throws Exception {

    // given
    RemoveAllCommand command = new RemoveAllCommand();
    command.batchSize = 0;

    // when
    command.executeWithSubject();

    // then
    String message = String.format(RemoveAllCommand.BATCH_SIZE_ERROR_MESSAGE_FORMAT, 0);
    String expectedPrintOut = RED_CONSOLE_COLOR + message + DEFAULT_CONSOLE_COLOR;
    assertThat(consoleOutput.getOutput(), startsWith(expectedPrintOut));
  }

  /**
   * Tests condition where number of results returned by each catalog framework query is less than
   * batchSize being used.
   *
   * <p>Response of size 0 for final call lets the command exit as it simulates no more results to
   * process.
   *
   * <p>Cumulative results by each batch: First: 53 (53 results returned) Second: 110 (57 results
   * returned) Third: 164 (54 results returned) Fourth: 200 (36 results returned)
   *
   * <p>Final: 0 results returned, therefore done.
   *
   * @throws Exception
   */
  @Test
  public void testDeleteFewerThanBatchSizeExpectMultipleCatalogCalls() throws Exception {

    batchSize = 101;
    numCatalogCalls = 4;
    forceCommand = true;

    setQueryAndDeleteResponseMocks(53, 57, 54, 36, 0);

    setCatalogQueryAndDeleteResponses();

    newRemoveAllCommand(new RemoveAllCommand()).executeWithSubject();
    verify(catalogFrameworkMock, times(numCatalogCalls)).delete(isA(DeleteRequest.class));
  }

  /**
   * Checks the forced (-f) generic case.
   *
   * <p>Response of size 0 for second call lets the command exit as it simulates no more results to
   * process.
   *
   * @throws Exception
   */
  @Test
  public void testExecuteWithSubject() throws Exception {

    batchSize = 11;
    numCatalogCalls = 1;
    forceCommand = true;

    setQueryAndDeleteResponseMocks(11, 0);

    setCatalogQueryAndDeleteResponses();

    newRemoveAllCommand(new RemoveAllCommand()).executeWithSubject();
    verify(catalogFrameworkMock, times(numCatalogCalls)).delete(isA(DeleteRequest.class));
  }

  @Test
  public void testExecuteWithSubjectWithoutForceOption() throws Exception {
    // given
    final Session mockSession = mock(Session.class);
    when(mockSession.readLine(anyString(), isNull())).thenReturn("yes");

    batchSize = 11;
    numCatalogCalls = 1;
    forceCommand = false;

    setQueryAndDeleteResponseMocks(11, 0);

    setCatalogQueryAndDeleteResponses();

    final RemoveAllCommand command = new RemoveAllCommand();
    command.session = mockSession;

    // when
    newRemoveAllCommand(command).executeWithSubject();

    // then
    verify(catalogFrameworkMock, times(numCatalogCalls)).delete(isA(DeleteRequest.class));
  }

  /**
   * Checks the forced (-f) generic case with (--cache) option
   *
   * @throws Exception
   */
  @Test
  public void testExecuteWithSubjectWithCache() throws Exception {

    int numCatalogCalls = 1;

    final SolrCacheMBean mbean = mock(SolrCacheMBean.class);

    RemoveAllCommand removeAllCommand =
        new RemoveAllCommand() {
          @Override
          protected SolrCacheMBean getCacheProxy() {
            return mbean;
          }
        };

    removeAllCommand.force = true;
    removeAllCommand.cache = true;
    removeAllCommand.executeWithSubject();

    verify(mbean, times(numCatalogCalls)).removeAll();
  }

  @Test
  public void testInvalidWarningPromptInput() throws Exception {
    // given
    final Session mockSession = mock(Session.class);
    when(mockSession.readLine(anyString(), isNull())).thenReturn("something that isn't yes nor no");

    batchSize = 11;
    numCatalogCalls = 1;
    forceCommand = false;

    setQueryAndDeleteResponseMocks(11, 0);

    setCatalogQueryAndDeleteResponses();

    final RemoveAllCommand command = new RemoveAllCommand();
    command.session = mockSession;

    // when
    newRemoveAllCommand(command).executeWithSubject();

    // then
    verifyZeroInteractions(catalogFrameworkMock);
  }

  @Test
  public void testWarningPromptInputOfNo() throws Exception {
    // given
    final Session mockSession = mock(Session.class);
    when(mockSession.readLine(anyString(), isNull())).thenReturn("no");

    batchSize = 11;
    numCatalogCalls = 1;
    forceCommand = false;

    setQueryAndDeleteResponseMocks(11, 0);

    setCatalogQueryAndDeleteResponses();

    final RemoveAllCommand command = new RemoveAllCommand();
    command.session = mockSession;

    // when
    newRemoveAllCommand(command).executeWithSubject();

    // then
    verifyZeroInteractions(catalogFrameworkMock);
  }

  @Test
  public void testWarningPromptException() throws Exception {
    // given
    final Session mockSession = mock(Session.class);
    when(mockSession.readLine(anyString(), isNull())).thenThrow(IOException.class);

    batchSize = 11;
    numCatalogCalls = 1;
    forceCommand = false;

    setQueryAndDeleteResponseMocks(11, 0);

    setCatalogQueryAndDeleteResponses();

    final RemoveAllCommand command = new RemoveAllCommand();
    command.session = mockSession;

    // when
    newRemoveAllCommand(command).executeWithSubject();

    // then
    verifyZeroInteractions(catalogFrameworkMock);
  }

  private RemoveAllCommand newRemoveAllCommand(RemoveAllCommand removeAllCommand) {

    removeAllCommand.catalogFramework = catalogFrameworkMock;
    removeAllCommand.filterBuilder = new GeotoolsFilterBuilder();
    removeAllCommand.batchSize = batchSize;
    removeAllCommand.force = forceCommand;

    return removeAllCommand;
  }

  private void setCatalogQueryAndDeleteResponses() throws Exception {
    when(catalogFrameworkMock.query(isA(QueryRequest.class))).thenReturn(queryResponse);
    when(catalogFrameworkMock.delete(isA(DeleteRequest.class))).thenReturn(deleteResponse);
  }

  private void setQueryAndDeleteResponseMocks(int... numResultsPerQuery) {
    setQueryResponseMockReturn(numResultsPerQuery);
    setDeleteResponseMockReturn(numResultsPerQuery);
  }

  /**
   * Given the internal implementation of RemoveAllCommand:executeRemoveAllFromStore(),
   * queryResponse.getResults() needs to be called three times per catalog framework query. For this
   * reason, three mock responses are added for each argument.
   *
   * @param numResultsPerQueryList
   */
  private void setQueryResponseMockReturn(int... numResultsPerQueryList) {

    List<List<Result>> queryResponseMockList = new ArrayList<>();

    for (int numResultsForQuery : numResultsPerQueryList) {

      List<Result> resultList = populateResultList(numResultsForQuery);
      queryResponseMockList.add(resultList);

      // add two copies of first result list as implementation of RemoveAllCommand
      // calls queryResponse.getResults() three times per catalog framework query
      // Calling getResults does NOT advance index of queryResponse
      queryResponseMockList.add(resultList);
      queryResponseMockList.add(resultList);
    }

    when(queryResponse.getResults())
        .thenReturn(queryResponseMockList.get(0), getRemainingArguments(queryResponseMockList));
  }

  private void setDeleteResponseMockReturn(int... numResultsPerQueryList) {

    List<List<Metacard>> deleteResponseMockList = new ArrayList<>();

    for (int numResultsForQuery : numResultsPerQueryList) {
      deleteResponseMockList.add(populateMetacardList(numResultsForQuery));
    }

    when(deleteResponse.getDeletedMetacards())
        .thenReturn(deleteResponseMockList.get(0), getRemainingArguments(deleteResponseMockList));
  }

  private <T> List[] getRemainingArguments(List<List<T>> mockResponses) {
    List[] argumentListArr = new List[mockResponses.size()];
    mockResponses.toArray(argumentListArr);
    return Arrays.copyOfRange(argumentListArr, 1, mockResponses.size());
  }

  private List<Result> populateResultList(int size) {
    return Stream.generate(() -> new ResultImpl(newRandomMetacard())).limit(size).collect(toList());
  }

  private List<Metacard> populateMetacardList(int size) {
    return Stream.generate(() -> newRandomMetacard()).limit(size).collect(toList());
  }

  private MetacardImpl newRandomMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId(UUID.randomUUID().toString());
    return metacard;
  }
}
