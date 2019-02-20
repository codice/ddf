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
import ddf.catalog.data.Metacard
import ddf.catalog.data.Result
import ddf.catalog.data.impl.ResultImpl
import ddf.catalog.federation.FederationException
import ddf.catalog.operation.Query
import ddf.catalog.operation.QueryRequest
import ddf.catalog.operation.QueryResponse
import ddf.catalog.operation.impl.QueryRequestImpl
import ddf.catalog.operation.impl.QueryResponseImpl
import ddf.catalog.source.SourceUnavailableException
import ddf.catalog.source.UnsupportedQueryException
import spock.lang.Specification
import spock.lang.Unroll

import static ddf.catalog.util.impl.ResultIterable.resultIterable
import static java.util.stream.Collectors.toList

class ResultIterableSpec extends Specification {

    CatalogFramework catalogFramework

    def metacardIdInt = 1

    def setup() {
        catalogFramework = Mock(CatalogFramework.class)
    }

    def "hasNext() is false when catalog returns no results"() {
        setup:
        List<Result> actualResults = []
        1 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 0, 0)
        }

        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = resultIterable(catalogFramework, queryRequestMock).iterator()

        when:
        def hasNext = resultIterator.hasNext()

        then:
        hasNext.is false
    }

    def "hasNext() is true when catalog returns results"() {
        setup:
        1 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 1, 1)
        } >> {
            QueryRequest queryRequest -> buildEmptyQueryResponse([null])
        }

        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = resultIterable(catalogFramework, queryRequestMock).iterator()

        when:
        def hasNext = resultIterator.hasNext()

        then:
        hasNext.is true
    }

    def "hasNext() is true when called twice in a row and catalog returns results"() {
        setup:
        1 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 1, 1)
        } >> {
            QueryRequest queryRequest -> buildEmptyQueryResponse([null])
        }

        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = resultIterable(catalogFramework, queryRequestMock).iterator()

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
        } >> {
            QueryRequest queryRequest -> buildEmptyQueryResponse([null])
        }

        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = resultIterable(catalogFramework.&query, queryRequestMock).iterator()

        when:
        def hasNext = resultIterator.hasNext()

        then:
        hasNext.is true
    }

    def "hasNext() doesn't query the catalog after all the results have been retrieved"() {
        setup:
        1 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 1, 1)
        } >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 0, 1)
        }

        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = resultIterable(catalogFramework, queryRequestMock).iterator()

        when:
        resultIterator.next()
        resultIterator.hasNext()
        def hasNext = resultIterator.hasNext()

        then:
        hasNext.is false
    }

    def "next() returns the proper results when start index is not 1"() {
        setup:
        List<Result> actualResults = (1..10).collect { new ResultImpl() }
        int startIndex = 10
        1 * catalogFramework.query(_ as QueryRequest) >> {
                // Return the last result (aka startIndex = 10, actual index of 9)
            QueryRequest queryRequest -> buildQueryResponse(actualResults, 9)
        } >> {
            QueryRequest queryRequest -> buildEmptyQueryResponse(actualResults)
        }

        Query queryMock = createQueryMock(startIndex, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = resultIterable(catalogFramework.&query, queryRequestMock).iterator()

        when:
        def result = resultIterator.next()

        then:
        result == actualResults.last()
    }

    def "next() properly pages when default page size is used"() {
        setup:
        List<Result> actualResults = (1..70).collect { new ResultImpl() }
        def totalResults = actualResults.size()

        2 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(actualResults, 0..63)
        } >> {
            QueryRequest queryRequest -> buildQueryResponse(actualResults, 64..69)
        } >> {
            QueryRequest queryRequest -> buildEmptyQueryResponse(actualResults)
        }

        Query queryMock = createQueryMock(1, 0)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterable = resultIterable(catalogFramework, queryRequestMock)

        when:
        def results = resultIterable.stream()
                .collect(toList())

        then:
        // Test assumption: we're currently using more results than the current page size
        totalResults > ResultIterable.DEFAULT_PAGE_SIZE
        results == actualResults
    }

    def "next() properly pages when first page is filtered out"() {
        setup:
        List<Result> actualResults = (1..70).collect { new ResultImpl() }
        def totalResults = actualResults.size()

        2 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest ->
                def response = new QueryResponseImpl(queryRequest,
                        [],
                        true,
                        (long) actualResults.size(),
                        ["actualResultSize": 64])
                return response

        } >> {
            QueryRequest queryRequest -> buildQueryResponse(actualResults, 64..69)
        } >> {
            QueryRequest queryRequest -> buildEmptyQueryResponse(actualResults)
        }

        Query queryMock = createQueryMock(1, 0)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterable = resultIterable(catalogFramework, queryRequestMock)

        when:
        def results = resultIterable.stream()
                .collect(toList())

        then:
        // Test assumption: the total real results index is greater than the page size
        totalResults > ResultIterable.DEFAULT_PAGE_SIZE
        //  the first 64 results are filtered (0..63), but should still get the unfiltered results
        results == actualResults[64..69]
    }

    def "next() properly pages when some results are filtered out"() {
        setup:
        List<Result> actualResults = (1..70).collect { new ResultImpl() }
        def totalResults = actualResults.size()

        2 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest ->
                def response = new QueryResponseImpl(queryRequest,
                        actualResults[0..6] + actualResults[8..63],
                        true,
                        (long) actualResults.size(),
                        ["actualResultSize": 64])
                return response

        } >> {
            QueryRequest queryRequest -> buildQueryResponse(actualResults, 64..69)
        } >> {
            QueryRequest queryRequest -> buildEmptyQueryResponse(actualResults)
        }

        Query queryMock = createQueryMock(1, 0)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterable = resultIterable(catalogFramework, queryRequestMock)

        when:
        def results = resultIterable.stream()
                .collect(toList())

        then:
        // Test assumption: the total real results index is greater than the page size
        totalResults > ResultIterable.DEFAULT_PAGE_SIZE
        //  the 7th result is filtered out,  but should still get all others, no duplicates.
        results == actualResults[0..6] + actualResults[8..69]
    }

    def "Properly pages when default page size is used with no maxResults and no hit count"() {
        setup:
        def actualResults = (1..70).collect { new ResultImpl() }
        def totalResults = actualResults.size()

        3 * catalogFramework.query(_ as QueryRequest) >>
                { QueryRequest queryRequest ->
                    def response = Mock(QueryResponse)
                    response.getHits() >> -1
                    response.getResults() >> actualResults[0..63]
                    return response
                } >>
                { QueryRequest queryRequest ->
                    def response = Mock(QueryResponse)
                    response.getHits() >> -1
                    response.getResults() >> actualResults[64..69]
                    return response
                } >>
                { QueryRequest queryRequest ->
                    def response = Mock(QueryResponse)
                    response.getHits() >> -1
                    response.getResults() >> []
                    return response
                }

        Query queryMock = createQueryMock(1, 0)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        ResultIterable resultIterable = resultIterable(catalogFramework, queryRequestMock)

        when:
        def results = resultIterable.stream().collect(toList())

        then:
        results.size() == totalResults
        results == actualResults
    }

    def "Properly pages when default page size is used with no maxResults"() {
        setup:
        def actualResults = (1..25).collect { new ResultImpl() }
        def totalResults = actualResults.size()

        2 * catalogFramework.query(_ as QueryRequest) >>
                { QueryRequest queryRequest -> buildQueryResponse(actualResults, 0..19) } >>
                { QueryRequest queryRequest -> buildQueryResponse(actualResults, 20..24) } >>
                { QueryRequest queryRequest -> buildEmptyQueryResponse(actualResults) }

        Query queryMock = createQueryMock(1, 0)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        ResultIterable resultIterable = resultIterable(catalogFramework, queryRequestMock)

        when:
        def results = resultIterable.stream().collect(toList())

        then:
        results.size() == totalResults
        results == actualResults
    }

    @Unroll
    def "Properly pages when default page size is used with maxResults"() {
        setup:
        def actualResults = (1..25).collect { new ResultImpl() }
        def totalResults = actualResults.size()

        // pageSize = 20
        expectedQueriesMade * catalogFramework.query(_ as QueryRequest) >>
                { QueryRequest queryRequest -> buildQueryResponse(actualResults, 0..19) } >>
                { QueryRequest queryRequest -> buildQueryResponse(actualResults, 20..24) } >>
                { QueryRequest queryRequest -> buildEmptyQueryResponse(actualResults) }

        Query queryMock = createQueryMock(1, 20)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        ResultIterable resultIterable = ResultIterable.resultIterable(catalogFramework, queryRequestMock, maxResultCount)

        when:
        def queryResults = resultIterable.stream()
                .collect(toList())

        then:
        def resultCount = Math.min(maxResultCount, totalResults)
        // Either expect the maxResults it was capped to, or the actual result count if it was less
        queryResults.size() == resultCount
        queryResults == actualResults.subList(0, resultCount)

        where:
        maxResultCount | expectedQueriesMade
        7              | 1
        22             | 2
        25             | 2
        35             | 2
    }

    @Unroll
    def 'Dedupes results when paging'() {
        setup:
        def metacardCounter = 1
        def actualResults = (1..25).collect {
            def metacard = Mock(Metacard)
            metacard.getId() >> (metacardCounter++ as String)
            metacardCounter %= 5

            def result = new ResultImpl()
            result.metacard = metacard
            result
        }
        def id = actualResults*.metacard*.id
        def dedupedCount = id.unique().size()

        catalogFramework.query(_ as QueryRequest) >>
                { qr -> buildQueryResponse(actualResults, 0..4) } >>
                { qr -> buildQueryResponse(actualResults, 5..9) } >>
                { qr -> buildQueryResponse(actualResults, 10..14) } >>
                { qr -> buildQueryResponse(actualResults, 15..19) } >>
                { qr -> buildQueryResponse(actualResults, 20..24) } >>
                { qr -> buildEmptyQueryResponse(actualResults) }

        // Use a very small page size to trigger a large number of pages
        Query queryMock = createQueryMock(1, 3)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        ResultIterable resultIterable = resultIterable(catalogFramework, queryRequestMock)

        when:
        def queryResults = resultIterable.stream()
                .collect(toList())

        then:
        queryResults.size() == dedupedCount
    }

    def "next() when number of results from catalog varies"() {
        setup:
        def actualResults = (1..6).collect { new ResultImpl() }
        def totalResults = actualResults.size()
        3 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(actualResults, 0..1)
        } >> {
            QueryRequest queryRequest -> buildQueryResponse(actualResults, 2..4)
        } >> {
            QueryRequest queryRequest -> buildQueryResponse(actualResults, 5)
        } >> {
            QueryRequest queryRequest -> buildEmptyQueryResponse(actualResults)
        }

        def pageSize = 3
        Query queryMock = createQueryMock(1, pageSize)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = resultIterable(catalogFramework, queryRequestMock)

        when:
        def results = resultIterator.stream()
                .collect(toList())

        then:
        results.size() == totalResults
        results == actualResults
    }

    def "next() doesn't query the catalog after all the results have been retrieved"() {
        setup:
        1 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 1, 1)
        } >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 0, 1)
        }

        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterator = resultIterable(catalogFramework, queryRequestMock).iterator()

        when:
        resultIterator.next()
        resultIterator.hasNext()
        resultIterator.next()

        then:
        thrown NoSuchElementException
    }

    def "next() when using a query function and catalog returns results"() {
        setup:
        def actualResults = [new ResultImpl()]
        1 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(actualResults, 0)
        } >> {
            QueryRequest queryRequest -> buildEmptyQueryResponse(actualResults)
        }

        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        def resultIterable = resultIterable(catalogFramework.&query, queryRequestMock)

        when:
        def result = resultIterable.iterator().next()

        then:
        result == actualResults.first()
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

        def resultIterator = resultIterable(catalogFramework, queryRequestMock).iterator()

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

        def resultIterator = resultIterable(catalogFramework, queryRequestMock).iterator()

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

        def resultIterator = resultIterable(catalogFramework, queryRequestMock).iterator()

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

        def resultIterator = resultIterable(catalogFramework, queryRequestMock).iterator()

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
        resultIterable(null as CatalogFramework, queryRequestMock)

        then:
        thrown IllegalArgumentException
    }

    def "constructor when catalog framework is set but query request is null"() {
        when:
        resultIterable(catalogFramework, null)

        then:
        thrown IllegalArgumentException
    }

    def "constructor when query function is null"() {
        setup:
        Query queryMock = createQueryMock(1, 1)
        QueryRequest queryRequestMock = createQueryRequestMock(queryMock)

        when:
        resultIterable(null as QueryFunction, queryRequestMock)

        then:
        thrown IllegalArgumentException
    }

    def "constructor when query function is set but query request is null"() {
        when:
        resultIterable(Mock(QueryFunction), null)

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

    private QueryResponse buildEmptyQueryResponse(List<Result> resultList) {
        def response = Mock(QueryResponse)
        response.getHits() >> resultList.size()
        response.getResults() >> { [] }
        response.getProperties() >> { ["actualResultSize": 0] }
        response.getPropertyValue("actualResultSize") >> 0
        return response
    }

    private QueryResponse buildQueryResponse(List<Result> resultList, int resultIndex) {
        return buildQueryResponse(resultList, resultIndex..resultIndex)
    }

    private QueryResponse buildQueryResponse(List<Result> resultList,
                                             Range resultRange) {
        QueryResponse response = new QueryResponseImpl(new QueryRequestImpl(null),
                resultList[resultRange],
                true,
                (long) resultList.size(),
                ["actualResultSize": resultRange.size()])

        return response
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

            def metacard = Mock(Metacard)
            metacard.getId() >> (metacardIdInt++ as String)
            newResult.metacard = metacard

            newResult.setDistanceInMeters((double) i)
            resultList << newResult
        }

        return resultList
    }
}