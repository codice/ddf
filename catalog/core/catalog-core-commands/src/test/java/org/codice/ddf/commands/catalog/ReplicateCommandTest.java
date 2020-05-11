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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.api.console.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ReplicateCommandTest extends ConsoleOutputCommon {

  private static final Set<String> SOURCE_IDS =
      new HashSet<>(Arrays.asList("sourceId1", "sourceId2"));

  private static final int HITS = 1000;

  private CatalogFramework catalogFramework;

  private CreateResponse mockCreateResponse = mock(CreateResponse.class);

  private CommandSession mockSession = mock(CommandSession.class);

  private InputStream mockIS = IOUtils.toInputStream("sourceId1", Charset.defaultCharset());

  private ReplicateCommand replicateCommand;

  @Before
  public void setUp() throws Exception {
    catalogFramework = mock(CatalogFramework.class);
    replicateCommand = new ReplicateCommand();

    final Session session = mock(Session.class);
    when(session.readLine(anyString(), isNull(Character.class))).thenReturn("sourceId1");
    replicateCommand.session = session;

    replicateCommand.catalogFramework = catalogFramework;
    replicateCommand.filterBuilder = new GeotoolsFilterBuilder();

    when(mockSession.getKeyboard()).thenReturn(mockIS);

    when(catalogFramework.getSourceIds()).thenReturn(SOURCE_IDS);
    when(catalogFramework.query(isA(QueryRequest.class)))
        .thenAnswer(
            invocation -> {
              Object[] args = invocation.getArguments();
              QueryRequest request = (QueryRequest) args[0];
              QueryResponse mockQueryResponse = mock(QueryResponse.class);
              when(mockQueryResponse.getHits()).thenReturn(Long.valueOf(HITS));
              when(mockQueryResponse.getResults())
                  .thenReturn(
                      getResultList(
                          Math.min(
                              replicateCommand.batchSize,
                              HITS - request.getQuery().getStartIndex() + 1)));

              return mockQueryResponse;
            });

    when(catalogFramework.create(isA(CreateRequest.class)))
        .thenAnswer(
            invocation -> {
              Object[] args = invocation.getArguments();
              CreateRequest request = (CreateRequest) args[0];
              when(mockCreateResponse.getCreatedMetacards()).thenReturn(request.getMetacards());
              return mockCreateResponse;
            });
  }

  @Test
  public void testBadBatchSize() throws Exception {
    replicateCommand.isUseTemporal = false;
    replicateCommand.sourceId = "sourceId1";
    replicateCommand.temporalProperty = Metacard.EFFECTIVE;
    replicateCommand.batchSize = -1;

    replicateCommand.executeWithSubject();
    verifyConsoleOutput("Batch Size must be between 1 and 1000.");
  }

  @Test
  public void testInvalidTemporalProperty() {
    replicateCommand.temporalProperty = "invalidTemporalProperty";

    assertThat(replicateCommand.getTemporalProperty(), is(Core.CREATED));
  }

  @Test
  public void testPrintSourceIds() throws Exception {
    replicateCommand.isUseTemporal = false;
    replicateCommand.sourceId = "";
    replicateCommand.temporalProperty = Metacard.EFFECTIVE;

    replicateCommand.executeWithSubject();
    verifyConsoleOutput(
        "Please enter the Source ID you would like to replicate:", "sourceId1", "sourceId2");
  }

  @Test
  public void testDefaultQuery() throws Exception {
    replicateCommand.isUseTemporal = false;
    replicateCommand.sourceId = "sourceId1";
    replicateCommand.temporalProperty = Metacard.EFFECTIVE;

    replicateCommand.executeWithSubject();
    verifyReplicate(HITS, Metacard.EFFECTIVE, 2);
    verifyConsoleOutput(HITS + " record(s) replicated; " + 0 + " record(s) failed;");
  }

  @Test
  public void testSmallBatchSize() throws Exception {
    replicateCommand.isUseTemporal = false;
    replicateCommand.sourceId = "sourceId1";
    replicateCommand.batchSize = 10;
    replicateCommand.temporalProperty = Metacard.EFFECTIVE;

    replicateCommand.executeWithSubject();
    verifyReplicate(HITS, Metacard.EFFECTIVE);
    verifyConsoleOutput(HITS + " record(s) replicated; " + 0 + " record(s) failed;");
  }

  @Test
  public void testMaxMetacard() throws Exception {
    replicateCommand.isUseTemporal = false;
    replicateCommand.sourceId = "sourceId1";
    replicateCommand.batchSize = 10;
    replicateCommand.maxMetacards = 20;
    replicateCommand.temporalProperty = Metacard.EFFECTIVE;

    replicateCommand.executeWithSubject();
    verifyReplicate(Math.min(HITS, replicateCommand.maxMetacards), Metacard.EFFECTIVE);
    verifyConsoleOutput(
        Math.min(HITS, replicateCommand.maxMetacards)
            + " record(s) replicated; "
            + 0
            + " record(s) failed;");
  }

  @Test
  public void testMultithreaded() throws Exception {
    replicateCommand.isUseTemporal = false;
    replicateCommand.sourceId = "sourceId1";
    replicateCommand.batchSize = 10;
    replicateCommand.multithreaded = 4;
    replicateCommand.temporalProperty = Metacard.EFFECTIVE;

    replicateCommand.executeWithSubject();

    verifyReplicate(HITS, Metacard.EFFECTIVE);
    verifyConsoleOutput(HITS + " record(s) replicated; " + 0 + " record(s) failed;");
  }

  @Test
  public void testTemporalFlag() throws Exception {
    replicateCommand.isUseTemporal = true;
    replicateCommand.sourceId = "sourceId1";
    replicateCommand.temporalProperty = Core.CREATED;
    replicateCommand.lastMinutes = 30;

    replicateCommand.executeWithSubject();
    verifyReplicate(HITS, Metacard.EFFECTIVE, 2);
    verifyConsoleOutput(HITS + " record(s) replicated; " + 0 + " record(s) failed;");
    // TODO - How do I validate the actual filter
  }

  @Test
  public void testFailedtoIngestHalf() throws Exception {
    when(catalogFramework.create(isA(CreateRequest.class)))
        .thenAnswer(
            invocation -> {
              Object[] args = invocation.getArguments();
              CreateRequest request = (CreateRequest) args[0];
              when(mockCreateResponse.getCreatedMetacards())
                  .thenReturn(request.getMetacards().subList(0, request.getMetacards().size() / 2));
              return mockCreateResponse;
            });

    replicateCommand.isUseTemporal = false;
    replicateCommand.sourceId = "sourceId1";
    replicateCommand.temporalProperty = Metacard.EFFECTIVE;

    replicateCommand.executeWithSubject();
    verifyReplicate(HITS, Metacard.EFFECTIVE, 2);
    verifyConsoleOutput(
        (int) Math.floor(HITS / 2)
            + " record(s) replicated; "
            + (int) (HITS - Math.floor(HITS / 2))
            + " record(s) failed;");
  }

  private List<Result> getResultList(int size) {
    return Stream.generate(
            () -> {
              MetacardImpl metacard = new MetacardImpl();
              metacard.setId(UUID.randomUUID().toString());
              return new ResultImpl(metacard);
            })
        .limit(size)
        .collect(Collectors.toList());
  }

  private void verifyReplicate(int actualMaxMetacards, String sortBy) throws Exception {
    verifyReplicate(
        actualMaxMetacards,
        sortBy,
        (int) Math.ceil((double) actualMaxMetacards / (double) replicateCommand.batchSize));
  }

  private void verifyReplicate(int actualMaxMetacards, String sortBy, int catalogCallTimes)
      throws Exception {
    ArgumentCaptor<QueryRequest> argument = ArgumentCaptor.forClass(QueryRequest.class);
    verify(catalogFramework, times(catalogCallTimes)).query(argument.capture());
    QueryRequest request = argument.getValue();
    assertThat(request, notNullValue());
    Query query = request.getQuery();
    assertThat(query, notNullValue());
    assertThat(query.getPageSize(), is(replicateCommand.batchSize));
    assertThat(query.getSortBy().getPropertyName().getPropertyName(), is(sortBy));
  }

  private void verifyConsoleOutput(String... message) {
    Arrays.stream(message).forEach(s -> assertThat(consoleOutput.getOutput(), containsString(s)));
  }
}
