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
package ddf.catalog.util.impl

import ddf.catalog.CatalogFramework
import ddf.catalog.data.Result
import ddf.catalog.operation.Query
import ddf.catalog.operation.QueryRequest
import spock.lang.Specification
import spock.lang.Unroll

class ResultListIterableSpec extends Specification {
    CatalogFramework catalogFramework
    Iterator<Result> resultsIterator

    def setup() {
        catalogFramework = Mock(CatalogFramework)
    }

    def "hasNext() when result iterator is empty"() {
        setup:
        initiliazeResultsIterator(0)

        Query queryMock = createQueryMock(1, 2)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        Iterator<List<Result>> resultListIterator = new ResultListIterableUnderTest(catalogFramework, queryRequestMock).iterator()

        when:
        def hasNext = resultListIterator.hasNext()

        then:
        hasNext.is false
    }

    def "hasNext() is true when result iterator has 1 result"() {
        setup:
        initiliazeResultsIterator(1)

        Query queryMock = createQueryMock(1, 2)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        Iterator<List<Result>> resultListIterator = new ResultListIterableUnderTest(catalogFramework, queryRequestMock).iterator()

        when:
        def hasNext = resultListIterator.hasNext()

        then:
        hasNext.is true
    }

    def "hasNext() is true when called twice in a row and result iterator has 2 results"() {
        setup:
        initiliazeResultsIterator(2)

        Query queryMock = createQueryMock(1, 2)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        Iterator<List<Result>> resultListIterator = new ResultListIterableUnderTest(catalogFramework, queryRequestMock).iterator()

        when:
        resultListIterator.hasNext()
        def hasNext = resultListIterator.hasNext()

        then:
        hasNext.is true
    }

    def "hasNext() is false when there are no more results"() {
        setup:
        initiliazeResultsIterator(4)

        Query queryMock = createQueryMock(1, 2)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        Iterator<List<Result>> resultListIterator = new ResultListIterableUnderTest(catalogFramework, queryRequestMock).iterator()

        when:
        resultListIterator.next()
        resultListIterator.next()
        def hasNext = resultListIterator.hasNext()

        then:
        hasNext.is false
    }

    def "hasNext() is true when using a query function and result iterator has 1 result"() {
        setup:
        initiliazeResultsIterator(1)

        Query queryMock = createQueryMock(1, 2)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        Iterator<List<Result>> resultListIterator = new ResultListIterableUnderTest(catalogFramework.&query, queryRequestMock).iterator()

        when:
        def hasNext = resultListIterator.hasNext()

        then:
        hasNext.is true
    }

    def "next() when result iterator is empty"() {
        setup:
        initiliazeResultsIterator(0)

        Query queryMock = createQueryMock(1, 2)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        Iterator<List<Result>> resultListIterator = new ResultListIterableUnderTest(catalogFramework, queryRequestMock).iterator()

        when:
        resultListIterator.next()

        then:
        thrown NoSuchElementException
    }

    @Unroll
    def "next() when result iterator has #numberOfResults results, start index is #startIndex and page size is #pageSize"(int numberOfResults, int startIndex, int pageSize) {
        setup:
        int expectedResults = Math.min(numberOfResults, pageSize)

        initiliazeResultsIterator(numberOfResults)

        Query queryMock = createQueryMock(1, pageSize)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        Iterator<List<Result>> resultListIterator = new ResultListIterableUnderTest(catalogFramework, queryRequestMock).iterator()

        when:
        def result = resultListIterator.next()

        then:
        result.size() == expectedResults
        1.upto(expectedResults, {
            result[it - 1].distanceInMeters == (Double) (it + startIndex - 1)
        })

        where:
        numberOfResults | startIndex | pageSize
        1               | 1          | 2
        1               | 2          | 2
        2               | 3          | 2
        3               | 4          | 2
    }

    @Unroll
    def "first two pages returned by next() when result iterator has #numberOfResults results, start index is #startIndex and page size is #pageSize"(int numberOfResults, int startIndex, int pageSize) {
        setup:
        int expectedResults = Math.min(numberOfResults - pageSize, pageSize)

        initiliazeResultsIterator(numberOfResults)

        Query queryMock = createQueryMock(1, pageSize)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        Iterator<List<Result>> resultListIterator = new ResultListIterableUnderTest(catalogFramework, queryRequestMock).iterator()

        when:
        def result1 = resultListIterator.next()
        def result2 = resultListIterator.next()

        then:
        result1.size() == pageSize
        result2.size() == expectedResults
        1.upto(expectedResults, {
            result2[it - 1].distanceInMeters == (Double) (it + pageSize + startIndex - 1)
        })

        where:
        numberOfResults | startIndex | pageSize
        3               | 1          | 2
        3               | 2          | 2
        4               | 3          | 2
        5               | 4          | 2
    }

    def "constructor with catalog framework when page size is less than 1"() {
        setup:
        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        when:
        new ResultListIterableUnderTest(catalogFramework, queryRequestMock).iterator()

        then:
        thrown IllegalArgumentException
    }

    def "constructor with query function when page size is less than 1"() {
        setup:
        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        when:
        new ResultListIterableUnderTest(catalogFramework.&query, queryRequestMock).iterator()

        then:
        thrown IllegalArgumentException
    }

    def "constructor when catalog framework is null"() {
        setup:
        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        when:
        new ResultListIterableUnderTest(null as CatalogFramework, queryRequestMock)

        then:
        thrown IllegalArgumentException
    }

    def "constructor when catalog framework is set but query request is null"() {
        when:
        new ResultListIterableUnderTest(catalogFramework, null)

        then:
        thrown IllegalArgumentException
    }

    def "constructor when query function is null"() {
        setup:
        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        when:
        new ResultListIterableUnderTest(null as QueryFunction, queryRequestMock)

        then:
        thrown IllegalArgumentException
    }

    def "constructor when query function is set but query request is null"() {
        when:
        new ResultListIterableUnderTest(Mock(QueryFunction), null)

        then:
        thrown IllegalArgumentException
    }

    private initiliazeResultsIterator(int numberOfResults) {
        if (numberOfResults == 0) {
            resultsIterator = [].iterator()
            return
        }

        def results = []

        1.upto(numberOfResults, {
            def result = Mock(Result)
            result.distanceInMeters >> (Double) it
            results << result
        })

        resultsIterator = results.iterator()
    }

    private Query createQueryMock(int startIndex, int pageSize) {
        def queryMock = Mock(Query.class)
        queryMock.getStartIndex() >> startIndex
        queryMock.getPageSize() >> pageSize
        queryMock.getSortBy() >> null
        queryMock.requestsTotalResultsCount() >> true
        queryMock.getTimeoutMillis() >> 0L
        return queryMock
    }

    private QueryRequest createQueryRequestMock(Query queryMock) {
        def queryRequestMock = Mock(QueryRequest.class)
        queryRequestMock.getQuery() >> queryMock
        return queryRequestMock
    }

    class ResultListIterableUnderTest extends ResultListIterable {

        ResultListIterableUnderTest(CatalogFramework catalogFramework, QueryRequest queryRequest) {
            super(catalogFramework, queryRequest)
        }

        ResultListIterableUnderTest(QueryFunction queryFunction, QueryRequest queryRequest) {
            super(queryFunction, queryRequest)
        }

        @Override
        Iterator<Result> createResultIterator(QueryFunction queryFunction, QueryRequest queryRequest) {
            return resultsIterator
        }
    }
}
