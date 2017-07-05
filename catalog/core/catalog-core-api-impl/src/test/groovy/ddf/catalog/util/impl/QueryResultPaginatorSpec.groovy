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

    void setup() {
        catalogFramework = Mock(CatalogFramework.class)
    }

    def "hasNext() is false when catalog returns no results"() {
        setup:
        1 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 0, 0)
        }

        Query queryMock = createQueryMock(1, 1)

        when:
        def queryResultPaginator = new QueryResultPaginator(catalogFramework, queryMock)
        def hasNext = queryResultPaginator.hasNext()

        then:
        !hasNext
    }

    def "hasNext() is true when catalog returns results"() {
        setup:
        1 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 1, 1)
        }

        Query queryMock = createQueryMock(1, 1)

        when:
        def queryResultPaginator = new QueryResultPaginator(catalogFramework, queryMock)
        def hasNext = queryResultPaginator.hasNext()

        then:
        hasNext
    }

    def "hasNext() is true when called twice in a row"() {
        setup:
        1 * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 1, 1)
        }

        Query queryMock = createQueryMock(1, 1)

        when:
        def queryResultPaginator = new QueryResultPaginator(catalogFramework, queryMock)
        queryResultPaginator.hasNext()
        def hasNext = queryResultPaginator.hasNext()

        then:
        hasNext == true
    }

    @Unroll
    def "next() returns #expected results when page size is #pageSize and catalog returns #resultSize results"(int pageSize, int resultSize, int expected) {
        setup:
        (_..2) * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, resultSize, 5)
        }

        Query queryMock = createQueryMock(1, pageSize)

        when:
        def queryResultPaginator = new QueryResultPaginator(catalogFramework, queryMock)
        List<Result> results = queryResultPaginator.next()

        then:
        results.size() == expected

        1.upto(expected, { results.get(it - 1).distanceInMeters == (Double) it })

        where:
        pageSize | resultSize | expected
        2        | 3          | 2
        2        | 2          | 2
        2        | 1          | 2
    }

    @Unroll
    def "second next() call returns #expected results when page size is #pageSize, catalog returns #resultSize per call and total of results is #totalResults"(int pageSize, int resultSize, int totalResults, int expected) {
        setup:
        (_..3) * catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, resultSize, totalResults)
        }

        Query queryMock = createQueryMock(1, pageSize)

        when:
        def queryResultPaginator = new QueryResultPaginator(catalogFramework, queryMock)
        List<Result> firstPage = queryResultPaginator.next()
        List<Result> secondPage = queryResultPaginator.next()

        then:
        firstPage.size() == pageSize
        secondPage.size() == expected

        1.upto(expected, { secondPage.get(it - 1).distanceInMeters == (Double) (it + pageSize) })

        where:
        pageSize | resultSize | totalResults | expected
        3        | 3          | 4            | 1
        3        | 3          | 5            | 2
        3        | 3          | 6            | 3
    }

    def "next() when number of results from catalog varies"() {
        setup:
        catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 2, 6)
        } >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 3, 6)
        } >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 1, 6)
        }

        def pageSize = 3
        Query queryMock = createQueryMock(1, pageSize)

        when:
        def queryResultPaginator = new QueryResultPaginator(catalogFramework, queryMock)
        List<Result> firstPage = queryResultPaginator.next()
        List<Result> secondPage = queryResultPaginator.next()

        then:
        firstPage.size() == 3
        1.upto(pageSize, { firstPage.get(it - 1).distanceInMeters == (Double) it })
        secondPage.size() == 3
        1.upto(pageSize, { secondPage.get(it - 1).distanceInMeters == (Double) (it + pageSize) })
    }

    def "next() throws exception when no more results"() {
        setup:
        catalogFramework.query(_ as QueryRequest) >> {
            QueryRequest queryRequest -> buildQueryResponse(queryRequest, 1, 1)
        }

        Query queryMock = createQueryMock(1, 1)

        when:
        def queryResultPaginator = new QueryResultPaginator(catalogFramework, queryMock)
        queryResultPaginator.next()
        queryResultPaginator.next()

        then:
        thrown NoSuchElementException
    }

    def "test catalog query throws UnsupportedQueryException"() {
        setup:
        catalogFramework.query(_ as QueryRequest) >> { throw new UnsupportedQueryException() }
        Query queryMock = createQueryMock(1, 1)

        when:
        def queryResultPaginator = new QueryResultPaginator(catalogFramework, queryMock)
        queryResultPaginator.next()

        then:
        thrown CatalogQueryException
    }

    def "test catalog query throws SourceUnavailableException"() {
        setup:
        catalogFramework.query(_ as QueryRequest) >> { throw new SourceUnavailableException() }
        Query queryMock = createQueryMock(1, 1)

        when:
        def queryResultPaginator = new QueryResultPaginator(catalogFramework, queryMock)
        queryResultPaginator.next()

        then:
        thrown CatalogQueryException
    }

    def "test catalog query throws FederationException"() {
        setup:
        catalogFramework.query(_ as QueryRequest) >> { throw new FederationException() }
        Query queryMock = createQueryMock(1, 1)

        when:
        def queryResultPaginator = new QueryResultPaginator(catalogFramework, queryMock)
        queryResultPaginator.next()

        then:
        thrown CatalogQueryException
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

    private void setCatalogFrameworkResponses(int resultListsSize, int totalResults)
            throws Exception {
        catalogFramework.query(_ as QueryRequest) >> { queryRequest -> buildQueryResponse(queryRequest, resultListsSize, totalResults) }
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
        List<Result> resultList = new ArrayList<>()
        def endIndex = resultListSize + startIndex - 1

        for (int i = startIndex; (i <= endIndex) && (i <= totalResults); i++) {
            ResultImpl newResult = new ResultImpl()
            newResult.setDistanceInMeters((double) i)
            resultList.add(newResult)
        }

        return resultList
    }
}
