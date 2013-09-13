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
package com.lmco.ddf.commands.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.lmco.ddf.commands.catalog.facade.CatalogFacade;
import com.lmco.ddf.commands.catalog.facade.Framework;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.MetacardImpl;
import ddf.catalog.data.Result;
import ddf.catalog.data.ResultImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class TestLatestCommand {

    /**
     * When no title is provided, output should still be displayed.
     * 
     * @throws Exception
     */
    @Test
    public void testNoTitle() throws Exception {

        ConsoleOutput consoleOutput = new ConsoleOutput();

        consoleOutput.interceptSystemOut();

        try {
            // given
            final CatalogFramework catalogFramework = givenCatalogFramework(getResultList("id1",
                    "id2"));

            LatestCommand latestCommand = new LatestCommand() {
                @Override
                protected CatalogFacade getCatalog() throws InterruptedException {
                    return new Framework(catalogFramework);
                }

                @Override
                protected FilterBuilder getFilterBuilder() throws InterruptedException {
                    return new GeotoolsFilterBuilder();
                }
            };

            // when
            latestCommand.doExecute();

            // then

            assertThat(consoleOutput.getOutput(), containsString("id1"));
            assertThat(consoleOutput.getOutput(), containsString("id2"));

        } finally {
            /* cleanup */
            consoleOutput.resetSystemOut();

            consoleOutput.closeBuffer();
        }

    }

    /**
     * @param list
     * @return
     * @throws UnsupportedQueryException
     * @throws SourceUnavailableException
     * @throws FederationException
     */
    private CatalogFramework givenCatalogFramework(List<Result> list)
        throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        final CatalogFramework catalogFramework = mock(CatalogFramework.class);

        QueryResponse queryResponse = mock(QueryResponse.class);

        when(queryResponse.getResults()).thenReturn(list);

        when(catalogFramework.query(isA(QueryRequest.class))).thenReturn(queryResponse);
        return catalogFramework;
    }

    private java.util.List<Result> getResultList(String... ids) {

        java.util.List<Result> results = new ArrayList<Result>();

        for (int i = 0; i < ids.length; i++) {

            String id = ids[i];
            MetacardImpl metacard = new MetacardImpl();
            metacard.setId(id);
            Result result = new ResultImpl(metacard);
            results.add(result);

        }

        return results;
    }

}
