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
import ddf.catalog.data.impl.ResultImpl
import ddf.catalog.federation.FederationException
import ddf.catalog.operation.Query
import ddf.catalog.operation.QueryRequest
import ddf.catalog.operation.QueryResponse
import ddf.catalog.source.SourceUnavailableException
import ddf.catalog.source.UnsupportedQueryException
import spock.lang.Specification
import spock.lang.Unroll

class QueryResultPaginatorSpec extends Specification {

    CatalogFramework catalogFramework

    def setup() {
        catalogFramework = Mock(CatalogFramework.class)
    }

    def "hasNext() is false when catalog returns no results"() {
        setup:
        1 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 0, 0)
        }

        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = new ResultIterable(catalogFramework, queryRequestMock).iterator()

        when:
        def hasNext = resultIterator.hasNext()

        then:
        hasNext.is false
    }

    def "hasNext() is true when catalog returns results"() {
        setup:
        1 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 1, 1)
        }

        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = new ResultIterable(catalogFramework, queryRequestMock).iterator()

        when:
        def hasNext = resultIterator.hasNext()

        then:
        hasNext.is true
    }

    def "hasNext() is true when called twice in a row and catalog returns results"() {
        setup:
        1 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 1, 1)
        }

        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = new ResultIterable(catalogFramework, queryRequestMock).iterator()

        when:
        resultIterator.hasNext()
        def hasNext = resultIterator.hasNext()

        then:
        hasNext.is true
    }

    def "hasNext() is true when using a query function and catalog returns results"() {
        setup:
        1 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 1, 1)
        }

        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = new ResultIterable(catalogFramework.&query, queryRequestMock).iterator()

        when:
        def hasNext = resultIterator.hasNext()

        then:
        hasNext.is true
    }

    def "hasNext() doesn't query the catalog after all the results have been retrieved"() {
        setup:
        2 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 1, 1)
        } >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 0, 1)
        }

        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = new ResultIterable(catalogFramework, queryRequestMock).iterator()

        when:
        resultIterator.next()
        resultIterator.hasNext()
        def hasNext = resultIterator.hasNext()

        then:
        hasNext.is false
    }

    @Unroll
    def '''#nextCalls results are returned and catalog is queried #expectedQueries times
           when next() is called #nextCalls times, page size is #pageSize and catalog returns
           #resultSize results at a time'''(int nextCalls, int pageSize, int resultSize, int expectedQueries) {
        setup:
        expectedQueries * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, resultSize, 5)
        }

        Query queryMock = createQueryMock(1, pageSize)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = new ResultIterable(catalogFramework, queryRequestMock).iterator()
        def results = []

        when:
        1.upto(nextCalls, { results << resultIterator.next() })

        then:
        1.upto(nextCalls, { results.get(it - 1).distanceInMeters == (Double) it })

        where:
        nextCalls | pageSize | resultSize | expectedQueries
        1         | 1        | 1          | 1
        2         | 1        | 2          | 1
        3         | 1        | 1          | 3
        2         | 2        | 1          | 2
        2         | 0        | 1          | 2
    }

    def "next() returns the proper results when start index is not 1"() {
        setup:
        int startIndex = 10
        1 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 1, 1)
        }

        Query queryMock = createQueryMock(startIndex, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = new ResultIterable(catalogFramework.&query, queryRequestMock).iterator()

        when:
        def result = resultIterator.next()

        then:
        result.distanceInMeters == (Double) startIndex
    }

    def "next() properly pages when default page size is used"() {
        setup:
        def resultSize = 20

        2 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, resultSize, resultSize + 5)
        }

        Query queryMock = createQueryMock(1, 0)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = new ResultIterable(catalogFramework, queryRequestMock).iterator()
        def results = []

        when:
        1.upto(resultSize + 1, { results << resultIterator.next() })

        then:
        1.upto(resultSize + 1, { results.get(it - 1).distanceInMeters == (Double) it })
    }

    def "next() when number of results from catalog varies"() {
        setup:
        def totalResults = 6
        3 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 2, totalResults)
        } >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 3, totalResults)
        } >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 1, totalResults)
        }

        def pageSize = 3
        Query queryMock = createQueryMock(1, pageSize)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = new ResultIterable(catalogFramework, queryRequestMock).iterator()
        def results = []

        when:
        1.upto(totalResults, { results << resultIterator.next() })

        then:
        1.upto(totalResults, { results.get(it - 1).distanceInMeters == (Double) it })
    }

    def "next() doesn't query the catalog after all the results have been retrieved"() {
        setup:
        2 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 1, 1)
        } >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 0, 1)
        }

        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = new ResultIterable(catalogFramework, queryRequestMock).iterator()

        when:
        resultIterator.next()
        resultIterator.hasNext()
        resultIterator.next()

        then:
        thrown NoSuchElementException
    }

    def "next() when using a query function and catalog returns results"() {
        setup:
        1 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 1, 1)
        }

        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = new ResultIterable(catalogFramework.&query, queryRequestMock).iterator()

        when:
        def result = resultIterator.next()

        then:
        result.distanceInMeters == 1.0
    }

    def "next() throws exception when no more results"() {
        setup:
        catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 1, 1)
        } >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 0, 1)
        }

        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = new ResultIterable(catalogFramework, queryRequestMock).iterator()

        when:
        resultIterator.next()
        resultIterator.next()

        then:
        thrown NoSuchElementException
    }

    def "catalog query() throws UnsupportedQueryException"() {
        setup:
        catalogFramework.query(_ as QueryRequest) >> { throw new UnsupportedQueryException() }
        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = new ResultIterable(catalogFramework, queryRequestMock).iterator()

        when:
        resultIterator.next()

        then:
        thrown CatalogQueryException
    }

    def "catalog query() throws SourceUnavailableException"() {
        setup:
        catalogFramework.query(_ as QueryRequest) >> { throw new SourceUnavailableException() }
        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = new ResultIterable(catalogFramework, queryRequestMock).iterator()

        when:
        resultIterator.next()

        then:
        thrown CatalogQueryException
    }

    def "catalog() query throws FederationException"() {
        setup:
        catalogFramework.query(_ as QueryRequest) >> { throw new FederationException() }
        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = new ResultIterable(catalogFramework, queryRequestMock).iterator()

        when:
        resultIterator.next()

        then:
        thrown CatalogQueryException
    }

    def "constructor when catalog framework is null"() {
        setup:
        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        when:
        new ResultIterable(null as CatalogFramework, queryRequestMock)

        then:
        thrown IllegalArgumentException
    }

    def "constructor when catalog framework is set but query request is null"() {
        when:
        new ResultIterable(catalogFramework, null)

        then:
        thrown IllegalArgumentException
    }

    def "constructor when query function is null"() {
        setup:
        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        when:
        new ResultIterable(null as QueryFunction, queryRequestMock)

        then:
        thrown IllegalArgumentException
    }

    def "constructor when query function is set but query request is null"() {
        when:
        new ResultIterable(Mock(QueryFunction), null)

        then:
        thrown IllegalArgumentException
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

    private buildQueryResponse(QueryRequest queryRequest, int resultListsSize, int totalResults) {
        int startIndex = queryRequest.getQuery()
                .getStartIndex()
        QueryResponse queryResponse = Mock(QueryResponse.class)

        queryResponse.getResults() >> {
            return getResultListOfSize(startIndex,
                    resultListsSize,
                    totalResults)
        }

        return queryResponse
    }

    // Gets a list of results with each Result's distanceInMeters set to a value equal to
    // startIndex up to the resultListSize or totalResults, whichever is reached first.
    private List<Result> getResultListOfSize(int startIndex, int resultListSize, int totalResults) {
        def resultList = []
        def endIndex = Math.min(resultListSize + startIndex - 1, totalResults + startIndex - 1)

        for (int i = startIndex; i <= endIndex; i++) {
            def newResult = new ResultImpl()
            newResult.setDistanceInMeters((double) i)
            resultList << newResult
        }

        return resultList
    }
}