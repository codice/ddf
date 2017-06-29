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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;

public class QueryResultPaginatorTest {

    private CatalogFramework catalogFrameworkMock;

    private QueryResponseImpl queryResponseMock;

    private QueryImpl queryMock;

    private QueryResultPaginator queryResultPaginator;

    @Before
    public void setUp() throws Exception {

        catalogFrameworkMock = mock(CatalogFramework.class);
        QueryRequestImpl queryRequestMock = mock(QueryRequestImpl.class);
        queryResponseMock = mock(QueryResponseImpl.class);
        queryMock = mock(QueryImpl.class);
        when(catalogFrameworkMock.query(any(QueryRequest.class))).thenReturn(queryResponseMock);
        when(queryRequestMock.getQuery()).thenReturn(queryMock);
    }

    @Test(expected = Exception.class)
    public void testNegativeStartIndexThrowsException() throws Exception {

        setStartIndexAndPageSize(-1, 1);
        queryResultPaginator.next();
    }

    @Test(expected = Exception.class)
    public void testZeroStartIndexThrowsException() throws Exception {

        setStartIndexAndPageSize(0, 1);
        queryResultPaginator.next();

    }

    @Test
    public void testHasNextNoNext() throws Exception {
        setStartIndexAndPageSize(1, 1);
        setResponses(1, 0);
        assertThat(queryResultPaginator.hasNext(), equalTo(true));
        assertThat(queryResultPaginator.hasNext(), equalTo(true));
        assertThat(queryResultPaginator.hasNext(), equalTo(true));
        queryResultPaginator.next();
        assertThat(queryResultPaginator.hasNext(), equalTo(false));
        assertThat(queryResultPaginator.hasNext(), equalTo(false));
        assertThat(queryResultPaginator.hasNext(), equalTo(false));
        assertThat(queryResultPaginator.getBufferSize(), equalTo(0));

    }

    @Test
    public void testHasNext() throws Exception {
        setStartIndexAndPageSize(1, 1);
        setResponses(1, 0);
        assertThat(queryResultPaginator.hasNext(), equalTo(true));
        assertThat(queryResultPaginator.hasNext(), equalTo(true));
        assertThat(queryResultPaginator.hasNext(), equalTo(true));
        assertThat(queryResultPaginator.hasNext(), equalTo(true));
        assertThat(queryResultPaginator.hasNext(), equalTo(true));
        assertThat(queryResultPaginator.hasNext(), equalTo(true));
        queryResultPaginator.next();
        assertThat(queryResultPaginator.getBufferSize(), equalTo(0));

        verify(queryResponseMock, atMost(1)).getResults();
    }

    @Test
    public void testSimpleNextPage() {
        setStartIndexAndPageSize(1, 1);
        setResponses(2, 0);
        assertThat(queryResultPaginator.next()
                .size(), equalTo(1));
        assertThat(queryResultPaginator.next()
                .size(), equalTo(1));
        assertThat(queryResultPaginator.hasNext(), equalTo(false));
        assertThat(queryResultPaginator.getBufferSize(), equalTo(0));

    }

    @Test(expected = NoSuchElementException.class)
    public void testNoSuchElementException() {
        setStartIndexAndPageSize(1, 1);
        setResponses(0);
        queryResultPaginator.next();
    }

    @Test
    public void testDifferentSizePages() {
        int pageSize = 3;
        setStartIndexAndPageSize(1, pageSize);
        setResponses(2, 2, 0);
        assertThat(queryResultPaginator.hasNext(), equalTo(true));
        assertThat(queryResultPaginator.next()
                .size(), equalTo(pageSize));
        assertThat(queryResultPaginator.next()
                .size(), equalTo(1));
        assertThat(queryResultPaginator.hasNext(), equalTo(false));
        assertThat(queryResultPaginator.getBufferSize(), equalTo(0));
    }

    @Test
    public void testIndexingAcrossPages() {
        int pageSize = 5;
        setStartIndexAndPageSize(10, pageSize);
        setResponses(5, 5, 0);
        List<Result> pageOne = queryResultPaginator.next();
        assertThat(pageOne, hasSize(5));
        assertThat(queryResultPaginator.getIndexOffset(), equalTo(15));
        List<Result> pageTwo = queryResultPaginator.next();
        assertThat(pageTwo, hasSize(5));
        assertThat(queryResultPaginator.hasNext(), equalTo(false));
        assertThat(queryResultPaginator.getBufferSize(), equalTo(0));
        assertThat(queryResultPaginator.getIndexOffset(), equalTo(20));

        try {
            queryResultPaginator.next();
        } catch (NoSuchElementException e) {
            //do nothing
        }

        assertThat(queryResultPaginator.hasNext(), equalTo(false));
        assertThat(queryResultPaginator.getBufferSize(), equalTo(0));
        assertThat(queryResultPaginator.getIndexOffset(), equalTo(20));
    }

    private void setStartIndexAndPageSize(int startIndex, int pageSize) {
        when(queryMock.getStartIndex()).thenReturn(startIndex);
        when(queryMock.getPageSize()).thenReturn(pageSize);
        when(queryMock.getSortBy()).thenReturn(null);
        when(queryMock.requestsTotalResultsCount()).thenReturn(true);
        when(queryMock.getTimeoutMillis()).thenReturn(0L);
        when(queryMock.getFilter()).thenReturn(mock(Filter.class));
        queryResultPaginator = new QueryResultPaginator(catalogFrameworkMock, queryMock);
    }

    private void setResponses(int... arguments) {
        int index = 1;
        List<List<Result>> mockResponses = new ArrayList<>();
        for (int numberofResults : arguments) {
            mockResponses.add(getResultListOfSize(numberofResults, index));
            index += numberofResults;
        }
        List[] args = new List[arguments.length];
        mockResponses.toArray(args);
        List[] vaargs = Arrays.copyOfRange(args, 1, arguments.length);
        when(queryResponseMock.getResults()).thenReturn(args[0], vaargs);
    }

    private List<Result> getResultListOfSize(int pageSize, int indexOffset) {
        List<Result> resultList = new ArrayList<>();
        for (int i = 0; i < pageSize; i++) {
            ResultImpl newResult = new ResultImpl();
            newResult.setDistanceInMeters((double) i + indexOffset);
            resultList.add(newResult);
        }
        return resultList;
    }
}
