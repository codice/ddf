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
package org.codice.ddf.catalog.plugin.metacard.backup.common;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.UpdateImpl;

public class ResponseSplitterTest {

    private List<Metacard> metacards = getTestMetacards();

    @Test
    public void testSplitCreate() {
        CreateResponse mockResponse = mock(CreateResponse.class);
        when(mockResponse.getCreatedMetacards()).thenReturn(metacards);
        ResponseMetacardActionSplitter splitter = new ResponseMetacardActionSplitter();
        List<Metacard> splitCards = splitter.split(mockResponse);
        assertThat(ListUtils.isEqualList(splitCards, metacards), is(true));
    }

    @Test
    public void testSplitUpdate() {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setId("test");

        UpdateResponse mockResponse = mock(UpdateResponse.class);
        List<Update> updates = new ArrayList<>();
        Update update = new UpdateImpl(null, metacard);
        updates.add(update);

        when(mockResponse.getUpdatedMetacards()).thenReturn(updates);
        ResponseMetacardActionSplitter splitter = new ResponseMetacardActionSplitter();
        List<Metacard> splitCards = splitter.split(mockResponse);
        assertThat(splitCards, hasSize(1));
    }

    @Test
    public void testSplitDelete() {
        DeleteResponse mockResponse = mock(DeleteResponse.class);
        when(mockResponse.getDeletedMetacards()).thenReturn(metacards);
        ResponseMetacardActionSplitter splitter = new ResponseMetacardActionSplitter();
        List<Metacard> splitCards = splitter.split(mockResponse);
        assertThat(ListUtils.isEqualList(splitCards, metacards), is(true));
    }

    private List<Metacard> getTestMetacards() {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setId("test");
        List<Metacard> testMetacards = new ArrayList<>();
        testMetacards.add(metacard);
        return testMetacards;
    }
}
