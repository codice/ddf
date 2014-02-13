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
package org.codice.ddf.ui.searchui.query.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.QueryResponse;

public class TestSearch {
    private MetacardImpl first = new MetacardImpl();

    private MetacardImpl second = new MetacardImpl();

    private MetacardImpl third = new MetacardImpl();

    private QueryResponse mockQueryResponse = mock(QueryResponse.class);

    @Before
    public void before() {
        first = new MetacardImpl();
        second = new MetacardImpl();
        third = new MetacardImpl();

        Calendar c = Calendar.getInstance();
        first.setModifiedDate(c.getTime());
        c.add(Calendar.DAY_OF_YEAR, -1);
        second.setModifiedDate(c.getTime());
        c.add(Calendar.DAY_OF_YEAR, -1);
        third.setModifiedDate(c.getTime());
    }

    @Test
    public void testAddQueryResponseDefaultSort() throws InterruptedException {
        List<Result> results = Arrays.<Result> asList(new ResultImpl(third), new ResultImpl(first),
                new ResultImpl(second));
        when(mockQueryResponse.getResults()).thenReturn(results);
        Search search = new Search();
        search.addQueryResponse(mockQueryResponse);
        assertThat(search.getCompositeQueryResponse(), notNullValue());
        QueryResponse response = search.getCompositeQueryResponse();
        assertThat(response.getResults().isEmpty(), is(false));
        assertThat((MetacardImpl) response.getResults().get(0).getMetacard(), is(equalTo(first)));
        assertThat((MetacardImpl) response.getResults().get(1).getMetacard(), is(equalTo(second)));
        assertThat((MetacardImpl) response.getResults().get(2).getMetacard(), is(equalTo(third)));
    }
}
