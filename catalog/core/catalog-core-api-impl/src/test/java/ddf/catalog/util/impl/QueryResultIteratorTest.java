/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.util.impl;

import static org.apache.commons.lang3.math.NumberUtils.min;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Result;
import ddf.catalog.util.impl.QueryResultIterable.QueryResultIterator;

public class QueryResultIteratorTest {

    private QueryResultPaginator paginatorMock;

    private QueryResultIterator queryIterator;

    private List<Result> totalPaginatorResultsList;

    private Result resultMock;

    @Before
    public void setUp() throws Exception {

        paginatorMock = mock(QueryResultPaginator.class);
        resultMock = mock(Result.class);

        queryIterator = new QueryResultIterator(paginatorMock);
        totalPaginatorResultsList = new ArrayList<>();
    }

    @Test
    public void testEmptyPaginator() {

        when(paginatorMock.hasNext()).thenReturn(false);
        assertThat("Results available", !queryIterator.hasNext());
    }

    @Test
    public void testHasNextNonEmptyPaginator() {

        int totalPaginatorResults = 11;

        totalPaginatorResultsList = populateResultList(totalPaginatorResults);
        when(paginatorMock.hasNext()).thenReturn(!totalPaginatorResultsList.isEmpty());

        assertThat("Results not available", queryIterator.hasNext());
    }

    @Test
    public void testSingleCallToCatalogFramework() {

        int totalPaginatorResults = 3;
        int totalCallsToCF = 1;
        int pageSizeOfQuery = 4;

        totalPaginatorResultsList = populateResultList(totalPaginatorResults);

        when(paginatorMock.next()).thenReturn(populateResultList(min(pageSizeOfQuery,
                totalPaginatorResultsList.size())));

        decrementPaginatorResultsList(pageSizeOfQuery);

        assertThat("Results not available", queryIterator.hasNext(), is(false));
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));

        assertThat("Results not available", queryIterator.hasNext(), is(true));
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));

        assertThat("Results not available", queryIterator.hasNext(), is(true));
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));

        assertThat("Results not available", queryIterator.hasNext(), is(false));

        verify(paginatorMock, times(totalCallsToCF)).next();
    }

    @Test(expected = NoSuchElementException.class)
    public void testTooManyCallsToNextThrowsNoSuchElementException() {

        int totalPaginatorResults = 3;
        int pageSizeOfQuery = 4;

        totalPaginatorResultsList = populateResultList(totalPaginatorResults);

        when(paginatorMock.next()).thenReturn(populateResultList(min(pageSizeOfQuery,
                totalPaginatorResultsList.size())));

        decrementPaginatorResultsList(pageSizeOfQuery);

        assertThat("Results not available", queryIterator.hasNext(), is(false));
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));

        assertThat("Results not available", queryIterator.hasNext(), is(true));
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));

        assertThat("Results not available", queryIterator.hasNext(), is(true));
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));

        assertThat("Results not available", queryIterator.hasNext(), is(false));
        when(paginatorMock.next()).thenThrow(NoSuchElementException.class);

        queryIterator.next();
    }

    @Test
    public void testMultipleCallsToCatalogFramework() {

        int totalPaginatorResults = 11;
        int totalCallsToCF = 4;
        int pageSizeOfQuery = 3;

        totalPaginatorResultsList = populateResultList(totalPaginatorResults);

        when(paginatorMock.next()).thenReturn(populateResultList(min(pageSizeOfQuery,
                totalPaginatorResultsList.size())));
        decrementPaginatorResultsList(pageSizeOfQuery);

        assertThat("Results not available", !queryIterator.hasNext());

        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));
        assertThat("Results not available", queryIterator.hasNext());
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));
        assertThat("Results not available", queryIterator.hasNext());
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));

        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));
        assertThat("Results not available", queryIterator.hasNext());
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));
        assertThat("Results not available", queryIterator.hasNext());
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));

        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));
        assertThat("Results not available", queryIterator.hasNext());
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));
        assertThat("Results not available", queryIterator.hasNext());
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));

        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));
        assertThat("Results not available", queryIterator.hasNext());
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));
        assertThat("Results not available", queryIterator.hasNext());
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));

        assertThat("Results not available", !queryIterator.hasNext());

        verify(paginatorMock, times(totalCallsToCF)).next();
    }

    @Test
    public void testMultipleCallsToHasNext() {

        int totalPaginatorResults = 11;
        int totalCallsToCF = 4;
        int pageSizeOfQuery = 3;

        totalPaginatorResultsList = populateResultList(totalPaginatorResults);

        when(paginatorMock.next()).thenReturn(populateResultList(min(pageSizeOfQuery,
                totalPaginatorResultsList.size())));
        decrementPaginatorResultsList(pageSizeOfQuery);

        assertThat("Results not available", !queryIterator.hasNext());

        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));
        assertThat("Results not available", queryIterator.hasNext());
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));
        assertThat("Results not available", queryIterator.hasNext());
        assertThat("Results not available", queryIterator.hasNext());
        assertThat("Results not available", queryIterator.hasNext());
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));

        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));
        assertThat("Results not available", queryIterator.hasNext());
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));
        assertThat("Results not available", queryIterator.hasNext());
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));

        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));
        assertThat("Results not available", queryIterator.hasNext());
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));
        assertThat("Results not available", queryIterator.hasNext());
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));

        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));
        assertThat("Results not available", queryIterator.hasNext());
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));
        assertThat("Results not available", queryIterator.hasNext());
        assertThat(" Results not returned from iterator", queryIterator.next(), is(resultMock));

        assertThat("Results not available", !queryIterator.hasNext());

        verify(paginatorMock, times(totalCallsToCF)).next();
    }

    private void decrementPaginatorResultsList(int numResultsToRemove) {

        int lastIndex = totalPaginatorResultsList.size() - 1;

        for (int i = lastIndex; i < numResultsToRemove; i--) {

            if (totalPaginatorResultsList.isEmpty()) {
                return;
            }

            totalPaginatorResultsList.remove(i);
        }
    }

    private List<Result> populateResultList(int size) {

        List<Result> resultList = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            resultList.add(resultMock);
        }

        return resultList;
    }

}