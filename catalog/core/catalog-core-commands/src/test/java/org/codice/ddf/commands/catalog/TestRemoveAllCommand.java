/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.commands.catalog;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.catalog.facade.Framework;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardImpl;
import ddf.catalog.data.Result;
import ddf.catalog.data.ResultImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;

/**
 * Tests the {@link RemoveAllCommand} output.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class TestRemoveAllCommand {

    static final String DEFAULT_CONSOLE_COLOR = Ansi.ansi().reset().toString();

    static final String RED_CONSOLE_COLOR = Ansi.ansi().fg(Ansi.Color.RED).toString();

    /**
     * If it is possible to give bad batch size, this test checks the proper outcome.
     * 
     * @throws Exception
     */
    @Test
    public void testBadBatchSize() throws Exception {

        ConsoleOutput consoleOutput = new ConsoleOutput();

        consoleOutput.interceptSystemOut();

        // given
        RemoveAllCommand command = new RemoveAllCommand();

        command.batchSize = 0;

        // when
        command.doExecute();

        /* cleanup */
        consoleOutput.resetSystemOut();

        // then
        try {
            String message = String.format(RemoveAllCommand.BATCH_SIZE_ERROR_MESSAGE_FORMAT, 0);
            String expectedPrintOut = RED_CONSOLE_COLOR + message + DEFAULT_CONSOLE_COLOR;
            assertThat(consoleOutput.getOutput(), startsWith(expectedPrintOut));

        } finally {
            consoleOutput.closeBuffer();
        }

    }

    /**
     * Checks the forced (-f) generic case.
     * 
     * @throws Exception
     */
    @Test
    public void testDoExecute() throws Exception {
        final CatalogFramework catalogFramework = mock(CatalogFramework.class);

        QueryResponse queryResponse = mock(QueryResponse.class);

        when(queryResponse.getResults()).thenReturn(getResultList(10));

        when(catalogFramework.query(isA(QueryRequest.class))).thenReturn(queryResponse);

        DeleteResponse deleteResponse = mock(DeleteResponse.class);

        when(deleteResponse.getDeletedMetacards()).thenReturn(getMetacardList(10));

        when(catalogFramework.delete(isA(DeleteRequest.class))).thenReturn(deleteResponse);

        RemoveAllCommand removeAllCommand = new RemoveAllCommand() {
            @Override
            protected CatalogFacade getCatalog() throws InterruptedException {
                return new Framework(catalogFramework);
            }

            @Override
            protected FilterBuilder getFilterBuilder() throws InterruptedException {
                return new GeotoolsFilterBuilder();
            }
        };

        removeAllCommand.batchSize = 11;

        removeAllCommand.force = true;

        removeAllCommand.doExecute();

        verify(catalogFramework, times(1)).delete(isA(DeleteRequest.class));

    }

    private java.util.List<Result> getResultList(int amount) {

        java.util.List<Result> results = new ArrayList<Result>();

        for (int i = 0; i < amount; i++) {

            String id = UUID.randomUUID().toString();
            MetacardImpl metacard = new MetacardImpl();
            metacard.setId(id);
            Result result = new ResultImpl(metacard);
            results.add(result);

        }

        return results;
    }

    private java.util.List<Metacard> getMetacardList(int amount) {

        List<Metacard> metacards = new ArrayList<Metacard>();

        for (int i = 0; i < amount; i++) {

            String id = UUID.randomUUID().toString();
            MetacardImpl metacard = new MetacardImpl();
            metacard.setId(id);

            metacards.add(metacard);

        }

        return metacards;
    }

    private BundleContext getBundleContextStub(CatalogFramework framework) {

        BundleContext context = mock(BundleContext.class);

        when(context.getServiceReference(eq(CatalogFramework.class.getName()))).thenReturn(
                mock(ServiceReference.class));

        when(context.getService(isA(ServiceReference.class))).thenReturn(framework);

        return context;

    }

}
