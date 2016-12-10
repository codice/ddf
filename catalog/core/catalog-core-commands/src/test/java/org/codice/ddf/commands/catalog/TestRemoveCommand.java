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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import ddf.catalog.cache.SolrCacheMBean;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;

public class TestRemoveCommand {

    private List<Metacard> metacardList = getMetacardList(5);

    private static ConsoleOutput consoleOutput;

    @Before
    public void setup() {
        metacardList = getMetacardList(5);

        consoleOutput = new ConsoleOutput();
        consoleOutput.interceptSystemOut();
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        consoleOutput.resetSystemOut();
        consoleOutput.closeBuffer();
    }

    @Test
    public void testSingleItemList() throws Exception {
        final SolrCacheMBean mbean = mock(SolrCacheMBean.class);

        List<String> ids = new ArrayList<>();
        ids.add(metacardList.get(0)
                .getId());

        RemoveCommand removeCommand = new RemoveCommand() {
            @Override
            protected SolrCacheMBean getCacheProxy() {
                return mbean;
            }

            @Override
            protected Object doExecute() throws Exception {
                return executeWithSubject();
            }
        };

        removeCommand.ids = ids;

        removeCommand.cache = true;

        removeCommand.doExecute();

        String[] idsArray = new String[ids.size()];
        idsArray = ids.toArray(idsArray);
        verify(mbean, times(1)).removeById(idsArray);
    }

    @Test
    public void testMultipleItemList() throws Exception {
        final SolrCacheMBean mbean = mock(SolrCacheMBean.class);

        List<String> ids = new ArrayList<>();
        ids.add(metacardList.get(0)
                .getId());
        ids.add(metacardList.get(1)
                .getId());
        ids.add(metacardList.get(2)
                .getId());

        RemoveCommand removeCommand = new RemoveCommand() {
            @Override
            protected SolrCacheMBean getCacheProxy() {
                return mbean;
            }

            @Override
            protected Object doExecute() throws Exception {
                return executeWithSubject();
            }
        };

        removeCommand.ids = ids;

        removeCommand.cache = true;

        removeCommand.doExecute();

        String[] idsArray = new String[ids.size()];
        idsArray = ids.toArray(idsArray);
        verify(mbean, times(1)).removeById(idsArray);
    }

    /**
     * Tests the {@Link RemoveCommand} when passed
     * a null list of ids
     *
     * @throws Exception
     */
    @Test
    public void testNullList() throws Exception {
        final SolrCacheMBean mbean = mock(SolrCacheMBean.class);

        RemoveCommand removeCommand = new RemoveCommand() {
            @Override
            protected SolrCacheMBean getCacheProxy() {
                return mbean;
            }

            @Override
            protected Object doExecute() throws Exception {
                return executeWithSubject();
            }
        };

        removeCommand.ids = null;

        removeCommand.doExecute();

        assertThat(consoleOutput.getOutput(), containsString("Nothing to remove."));

        consoleOutput.reset();
    }

    private java.util.List<Metacard> getMetacardList(int amount) {

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
