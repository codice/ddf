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

import static org.codice.ddf.commands.catalog.CommandSupport.ERROR_COLOR;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.fusesource.jansi.Ansi;
import org.junit.Test;

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

public class RemoveAllCommandTest extends ConsoleOutputCommon {

    static final String DEFAULT_CONSOLE_COLOR = Ansi.ansi()
            .reset()
            .toString();

    static final String RED_CONSOLE_COLOR = Ansi.ansi()
            .fg(ERROR_COLOR)
            .toString();

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
     * Checks the forced (-f) generic case.
     *
     * @throws Exception
     */
    @Test
    public void testExecuteWithSubject() throws Exception {
        // given
        RemoveAllCommand removeAllCommand = new RemoveAllCommand();

        final CatalogFramework catalogFramework = mock(CatalogFramework.class);

        QueryResponse queryResponse = mock(QueryResponse.class);
        when(queryResponse.getResults()).thenReturn(getResultList(10));
        when(catalogFramework.query(isA(QueryRequest.class))).thenReturn(queryResponse);

        DeleteResponse deleteResponse = mock(DeleteResponse.class);
        when(deleteResponse.getDeletedMetacards()).thenReturn(getMetacardList(10));
        when(catalogFramework.delete(isA(DeleteRequest.class))).thenReturn(deleteResponse);

        removeAllCommand.catalogFramework = catalogFramework;
        removeAllCommand.filterBuilder = new GeotoolsFilterBuilder();
        removeAllCommand.batchSize = 11;
        removeAllCommand.force = true;

        // when
        removeAllCommand.executeWithSubject();

        // then
        verify(catalogFramework, times(1)).delete(isA(DeleteRequest.class));
    }

    /**
     * Checks the forced (-f) generic case with (--cache) option
     *
     * @throws Exception
     */
    @Test
    public void testExecuteWithSubjectWithCache() throws Exception {
        // given
        final SolrCacheMBean mbean = mock(SolrCacheMBean.class);

        RemoveAllCommand removeAllCommand = new RemoveAllCommand() {
            @Override
            protected SolrCacheMBean getCacheProxy() {
                return mbean;
            }
        };
        removeAllCommand.force = true;
        removeAllCommand.cache = true;

        // when
        removeAllCommand.executeWithSubject();

        // then
        verify(mbean, times(1)).removeAll();
    }

    private List<Result> getResultList(int amount) {
        List<Result> results = new ArrayList<>();

        for (int i = 0; i < amount; i++) {

            String id = UUID.randomUUID()
                    .toString();
            MetacardImpl metacard = new MetacardImpl();
            metacard.setId(id);
            Result result = new ResultImpl(metacard);
            results.add(result);

        }

        return results;
    }

    private List<Metacard> getMetacardList(int amount) {
        List<Metacard> metacards = new ArrayList<>();

        for (int i = 0; i < amount; i++) {

            String id = UUID.randomUUID()
                    .toString();
            MetacardImpl metacard = new MetacardImpl();
            metacard.setId(id);

            metacards.add(metacard);

        }
        return metacards;
    }
}