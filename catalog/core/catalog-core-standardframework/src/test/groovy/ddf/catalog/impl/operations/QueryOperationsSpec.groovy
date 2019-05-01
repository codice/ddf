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
package ddf.catalog.impl.operations

import ddf.catalog.Constants
import ddf.catalog.content.StorageProvider
import ddf.catalog.data.ContentType
import ddf.catalog.data.MetacardType
import ddf.catalog.data.Result
import ddf.catalog.data.impl.MetacardImpl
import ddf.catalog.federation.FederationException
import ddf.catalog.federation.FederationStrategy
import ddf.catalog.filter.FilterAdapter
import ddf.catalog.filter.FilterBuilder
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder
import ddf.catalog.impl.FrameworkProperties
import ddf.catalog.impl.QueryResponsePostProcessor
import ddf.catalog.operation.Query
import ddf.catalog.operation.QueryRequest
import ddf.catalog.operation.QueryResponse
import ddf.catalog.operation.impl.QueryImpl
import ddf.catalog.plugin.PolicyPlugin
import ddf.catalog.plugin.PolicyResponse
import ddf.catalog.source.CatalogProvider
import ddf.catalog.source.ConnectedSource
import ddf.catalog.source.FederatedSource
import ddf.catalog.source.UnsupportedQueryException
import ddf.security.SecurityConstants
import ddf.security.Subject
import org.apache.commons.collections.CollectionUtils
import spock.lang.Shared
import spock.lang.Specification

class QueryOperationsSpec extends Specification {
    private static final String SOURCE_ID = "test_source"

    @Shared
    private FrameworkProperties frameworkProperties

    private List<CatalogProvider> catalogProviders
    private List<StorageProvider> storageProviders
    private Collection<FederatedSource> fedSources
    private List<ConnectedSource> connSources
    private SourceOperations sourceOperations
    private OperationsSecuritySupport opsSecurity
    private OperationsMetacardSupport opsMetacard
    private QueryOperations queryOperations
    private FilterAdapter filterAdapter

    def setup() {
        frameworkProperties = new FrameworkProperties()
        catalogProviders = (1..3).collect { mockCatalogProvider(it) }
        storageProviders = [Mock(StorageProvider)]
        fedSources = [mockFedSource('fed1'), mockFedSource('fed2')]
        connSources = ['conn1', 'conn2'].collect { mockConnectedSource(it) }
        filterAdapter = Mock(FilterAdapter)

        frameworkProperties.with {
            catalogProviders = this.catalogProviders
            storageProviders = this.storageProviders
            federatedSources = fedSources
            connectedSources = connSources
            queryResponsePostProcessor = Mock(QueryResponsePostProcessor)
            federationStrategy = Mock(FederationStrategy)
        }

        sourceOperations = Mock(SourceOperations)
        sourceOperations.getId() >> SOURCE_ID
        sourceOperations.getCatalog() >> { catalogProviders.first() }

        opsSecurity = Mock(OperationsSecuritySupport)
        opsMetacard = Mock(OperationsMetacardSupport)

        queryOperations = new QueryOperations(frameworkProperties, sourceOperations,
                opsSecurity, opsMetacard)
        queryOperations.setId(SOURCE_ID)
    }

    def 'test querysources init default'() {
        setup:
        def mockForQueryOps = Mock(QueryOperations)
        mockForQueryOps.hasCatalogProvider() >> { true }
        def request = Mock(QueryRequest)
        request.isEnterprise() >> { false }

        when:
        def sources = new QueryOperations.QuerySources(frameworkProperties)
        sources = sources.initializeSources(mockForQueryOps, request, sourceIds)

        then:
        sources.isEmpty()
        sources.exceptions.isEmpty()
        sources.needToAddConnectedSources == CollectionUtils.isNotEmpty(frameworkProperties.connectedSources)
        sources.needToAddCatalogProvider == mockForQueryOps.hasCatalogProvider()

        where:
        sourceIds << [null, [] as Set]
    }

    def 'test querysources enterprise'() {
        setup:
        def mockSourceOps = Mock(SourceOperations)
        mockSourceOps.isSourceAvailable(_) >> { true }
        def mockForQueryOps = Mock(QueryOperations,
                constructorArgs: [frameworkProperties, mockSourceOps, opsSecurity, opsMetacard])
        mockForQueryOps.hasCatalogProvider() >> { true }
        mockForQueryOps.canAccessSource(_, _) >> { src, req ->
            true
        }
        def request = Mock(QueryRequest)
        request.isEnterprise() >> { true }

        when:
        def sources = new QueryOperations.QuerySources(frameworkProperties)
        sources = sources.initializeSources(mockForQueryOps, request, null)

        then:
        sources.needToAddConnectedSources
        sources.needToAddCatalogProvider == mockForQueryOps.hasCatalogProvider()
        sources.sourcesToQuery as Set == frameworkProperties.federatedSources as Set
        sources.exceptions.isEmpty()
    }

    def 'test querysources enterprise with unavailable sources'() {
        setup:
        def mockSourceOps = Mock(SourceOperations)
        mockSourceOps.isSourceAvailable(_) >> { true }
        def mockForQueryOps = Mock(QueryOperations,
                constructorArgs: [frameworkProperties, mockSourceOps, opsSecurity, opsMetacard])
        mockForQueryOps.hasCatalogProvider() >> { true }
        mockForQueryOps.canAccessSource(_, _) >> { src, req ->
            false
        }
        def request = Mock(QueryRequest)
        request.isEnterprise() >> { true }

        when:
        def sources = new QueryOperations.QuerySources(frameworkProperties)
        sources = sources.initializeSources(mockForQueryOps, request, null)

        then:
        sources.needToAddConnectedSources
        sources.needToAddCatalogProvider == mockForQueryOps.hasCatalogProvider()
        sources.isEmpty()
        sources.exceptions.size() == frameworkProperties.federatedSources.size()
    }

    def 'test querysources enterprise with one unavailable source'() {
        setup:
        def mockSourceOps = Mock(SourceOperations)
        mockSourceOps.isSourceAvailable(_) >> { true }
        def mockForQueryOps = Mock(QueryOperations,
                constructorArgs: [frameworkProperties, mockSourceOps, opsSecurity, opsMetacard])
        mockForQueryOps.hasCatalogProvider() >> { true }
        mockForQueryOps.canAccessSource(_, _) >> { src, req ->
            src.id == 'fed1'
        }
        def request = Mock(QueryRequest)
        request.isEnterprise() >> { true }

        when:
        def sources = new QueryOperations.QuerySources(frameworkProperties)
        sources = sources.initializeSources(mockForQueryOps, request, null)

        then:
        sources.needToAddConnectedSources
        sources.needToAddCatalogProvider == mockForQueryOps.hasCatalogProvider()
        sources.sourcesToQuery as Set == [frameworkProperties.federatedSources.find { it.getId() == 'fed1' }] as Set
        sources.exceptions.size() == frameworkProperties.federatedSources.size() - 1
    }

    def 'test querysources with only local sourceId'() {
        setup:
        def mockSourceOps = Mock(SourceOperations)
        mockSourceOps.isSourceAvailable(_) >> { true }
        def mockForQueryOps = Mock(QueryOperations,
                constructorArgs: [frameworkProperties, mockSourceOps, opsSecurity, opsMetacard])
        mockForQueryOps.getId() >> { SOURCE_ID }
        mockForQueryOps.hasCatalogProvider() >> { true }
        mockForQueryOps.includesLocalSources(_) >> { true }
        mockForQueryOps.canAccessSource(_, _) >> { src, req ->
            true
        }
        def request = Mock(QueryRequest)
        request.isEnterprise() >> { false }

        when:
        def sources = new QueryOperations.QuerySources(frameworkProperties)
        sources = sources.initializeSources(mockForQueryOps, request, [SOURCE_ID] as Set)

        then:
        sources.needToAddConnectedSources == CollectionUtils.isNotEmpty(frameworkProperties.connectedSources)
        sources.needToAddCatalogProvider == mockForQueryOps.hasCatalogProvider()
        sources.isEmpty()
        sources.exceptions.isEmpty()
    }

    def 'test querysources with fed sourceId'() {
        setup:
        def mockSourceOps = Mock(SourceOperations)
        mockSourceOps.isSourceAvailable(_) >> { true }
        def mockForQueryOps = Mock(QueryOperations,
                constructorArgs: [frameworkProperties, mockSourceOps, opsSecurity, opsMetacard])
        mockForQueryOps.getId() >> { SOURCE_ID }
        mockForQueryOps.hasCatalogProvider() >> { true }
        mockForQueryOps.includesLocalSources(_) >> { true }
        mockForQueryOps.canAccessSource(_, _) >> { src, req ->
            true
        }
        def request = Mock(QueryRequest)
        request.isEnterprise() >> { false }

        when:
        def sources = new QueryOperations.QuerySources(frameworkProperties)
        sources = sources.initializeSources(mockForQueryOps, request, ["fed1"] as Set)

        then:
        sources.needToAddConnectedSources == CollectionUtils.isNotEmpty(frameworkProperties.connectedSources)
        sources.needToAddCatalogProvider == mockForQueryOps.hasCatalogProvider()
        sources.sourcesToQuery as Set == [frameworkProperties.federatedSources.find { it.getId() == 'fed1' }] as Set
        sources.exceptions.isEmpty()
    }

    def 'test querysources with unknown fed sourceId'() {
        setup:
        def mockSourceOps = Mock(SourceOperations)
        mockSourceOps.isSourceAvailable(_) >> { true }
        def mockForQueryOps = Mock(QueryOperations,
                constructorArgs: [frameworkProperties, mockSourceOps, opsSecurity, opsMetacard])
        mockForQueryOps.getId() >> { SOURCE_ID }
        mockForQueryOps.hasCatalogProvider() >> { true }
        mockForQueryOps.includesLocalSources(_) >> { true }
        mockForQueryOps.canAccessSource(_, _) >> { src, req ->
            true
        }
        def request = Mock(QueryRequest)
        request.isEnterprise() >> { false }

        when:
        def sources = new QueryOperations.QuerySources(frameworkProperties)
        sources = sources.initializeSources(mockForQueryOps, request, ["unknown"] as Set)

        then:
        sources.needToAddConnectedSources == CollectionUtils.isNotEmpty(frameworkProperties.connectedSources)
        sources.needToAddCatalogProvider == mockForQueryOps.hasCatalogProvider()
        sources.isEmpty()
        sources.exceptions.size() == 1
    }

    def 'test querysources with unavailable fed sourceId'() {
        setup:
        def mockSourceOps = Mock(SourceOperations)
        mockSourceOps.isSourceAvailable(_) >> { true }
        def mockForQueryOps = Mock(QueryOperations,
                constructorArgs: [frameworkProperties, mockSourceOps, opsSecurity, opsMetacard])
        mockForQueryOps.getId() >> { SOURCE_ID }
        mockForQueryOps.hasCatalogProvider() >> { true }
        mockForQueryOps.includesLocalSources(_) >> { true }
        mockForQueryOps.canAccessSource(_, _) >> { src, req ->
            false
        }
        def request = Mock(QueryRequest)
        request.isEnterprise() >> { false }

        when:
        def sources = new QueryOperations.QuerySources(frameworkProperties)
        sources = sources.initializeSources(mockForQueryOps, request, ["fed1"] as Set)

        then:
        sources.needToAddConnectedSources == CollectionUtils.isNotEmpty(frameworkProperties.connectedSources)
        sources.needToAddCatalogProvider == mockForQueryOps.hasCatalogProvider()
        sources.isEmpty()
        sources.exceptions.size() == 1
    }

    def 'do not add connected sources when not to be added'() {
        setup:
        def mockSourceOps = Mock(SourceOperations)
        mockSourceOps.isSourceAvailable(_) >> { true }
        def mockForQueryOps = Mock(QueryOperations,
                constructorArgs: [frameworkProperties, mockSourceOps, opsSecurity, opsMetacard])
        mockForQueryOps.getId() >> { SOURCE_ID }
        def sources = new QueryOperations.QuerySources(frameworkProperties)
        sources.sourcesToQuery = []
        sources.exceptions = []

        when:
        sources = sources.addConnectedSources(mockForQueryOps, frameworkProperties)

        then:
        0 * mockSourceOps.isSourceAvailable(_)
        sources.isEmpty()
        sources.exceptions.isEmpty()
    }

    def 'add connected sources'() {
        setup:
        def mockSourceOps = Mock(SourceOperations)
        def mockForQueryOps = Mock(QueryOperations,
                constructorArgs: [frameworkProperties, mockSourceOps, opsSecurity, opsMetacard])
        mockForQueryOps.getId() >> { SOURCE_ID }
        def sources = new QueryOperations.QuerySources(frameworkProperties)
        sources.sourcesToQuery = []
        sources.exceptions = []
        def connectedCount = frameworkProperties.getConnectedSources().size()
        sources.needToAddConnectedSources = true

        when:
        sources = sources.addConnectedSources(mockForQueryOps, frameworkProperties)

        then:
        connectedCount * mockSourceOps.isSourceAvailable(_) >> { true }
        sources.sourcesToQuery as Set == frameworkProperties.connectedSources as Set
        sources.exceptions.isEmpty()
    }

    def 'do not add catalog provider when not to be added'() {
        setup:
        def mockSourceOps = Mock(SourceOperations)
        mockSourceOps.isSourceAvailable(_) >> { true }
        def mockForQueryOps = Mock(QueryOperations,
                constructorArgs: [frameworkProperties, mockSourceOps, opsSecurity, opsMetacard])
        mockForQueryOps.getId() >> { SOURCE_ID }
        def sources = new QueryOperations.QuerySources(frameworkProperties)
        sources.sourcesToQuery = []
        sources.exceptions = []

        when:
        sources = sources.addCatalogProvider(mockForQueryOps)

        then:
        0 * mockSourceOps.isSourceAvailable(_)
        sources.isEmpty()
        sources.exceptions.isEmpty()
    }

    def 'add catalog provider'() {
        setup:
        def mockSourceOps = Mock(SourceOperations)
        mockSourceOps.getCatalog() >> mockCatalogProvider('mockCat')
        def mockForQueryOps = Mock(QueryOperations,
                constructorArgs: [frameworkProperties, mockSourceOps, opsSecurity, opsMetacard])
        mockForQueryOps.getId() >> { SOURCE_ID }
        def sources = new QueryOperations.QuerySources(frameworkProperties)
        sources.sourcesToQuery = []
        sources.exceptions = []
        sources.needToAddCatalogProvider = true

        when:
        sources = sources.addCatalogProvider(mockForQueryOps)

        then:
        1 * mockSourceOps.isSourceAvailable(_) >> { true }
        sources.sourcesToQuery as Set == [mockSourceOps.getCatalog()] as Set
        sources.exceptions.isEmpty()
    }

    def 'add catalog provider with exception'() {
        setup:
        def mockSourceOps = Mock(SourceOperations)
        mockSourceOps.getCatalog() >> mockCatalogProvider('mockCat')
        def mockForQueryOps = Mock(QueryOperations,
                constructorArgs: [frameworkProperties, mockSourceOps, opsSecurity, opsMetacard])
        mockForQueryOps.getId() >> { SOURCE_ID }
        def sources = new QueryOperations.QuerySources(frameworkProperties)
        sources.sourcesToQuery = []
        sources.exceptions = []
        sources.needToAddCatalogProvider = true

        when:
        sources = sources.addCatalogProvider(mockForQueryOps)

        then:
        1 * mockSourceOps.isSourceAvailable(_) >> { false }
        sources.isEmpty()
        sources.exceptions.size() == 1
    }

    def 'test replace source id'() {
        setup:
        def results = ['res1', 'res2', 'res3'].collect { mockResult(it, 'old_source') }
        def response = Mock(QueryResponse)
        def request = Mock(QueryRequest)
        def query = Mock(Query)

        query.getTimeoutMillis() >> { 100 }
        request.getQuery() >> { query }

        response.getRequest() >> { request }
        response.getProperties() >> { [:] }
        response.getResults() >> { results }

        when:
        def newResponse = queryOperations.replaceSourceId(response)
        def newResults = newResponse.getResults()

        then:
        newResults.size() == results.size()
        newResults.each {
            assert it.metacard.sourceId == queryOperations.id
        }
    }

    def 'can access source no security attributes'() {
        setup:
        def fedSource = Mock(FederatedSource)
        fedSource.getSecurityAttributes() >> [:]

        expect:
        queryOperations.canAccessSource(fedSource, Mock(QueryRequest))
    }

    def 'cannot access source not permitted'() {
        setup:
        def fedSource = Mock(FederatedSource)
        fedSource.getSecurityAttributes() >> [abc: ['def'] as Set]
        def subject = Mock(Subject)
        subject.isPermitted(_) >> { false }
        def request = Mock(QueryRequest)
        request.getProperties() >> [(SecurityConstants.SECURITY_SUBJECT): subject]

        expect:
        !queryOperations.canAccessSource(fedSource, request)
    }

    def 'can access source is permitted'() {
        setup:
        def fedSource = Mock(FederatedSource)
        fedSource.getSecurityAttributes() >> [abc: ['def'] as Set]
        def subject = Mock(Subject)
        subject.isPermitted(_) >> { true }
        def request = Mock(QueryRequest)
        request.getProperties() >> [(SecurityConstants.SECURITY_SUBJECT): subject]

        expect:
        queryOperations.canAccessSource(fedSource, request)
    }

    def 'test includes local sources'() {
        expect:
        result == queryOperations.includesLocalSources(sourceIds as Set)

        where:
        sourceIds                | result
        null                     | false
        []                       | false
        ['not_local']            | false
        [SOURCE_ID]              | true
        [""]                     | true
        [null]                   | true
        ['not_local', SOURCE_ID] | true
        ['not_local', ""]        | true
        ['not_local', null]      | true
    }

    def 'test setting flags on null request'() {
        expect:
        !queryOperations.setFlagsOnRequest(null)
    }

    def 'test setting flags on QueryRequest with no store or source ids'() {
        setup:
        def request = Mock(QueryRequest)

        def props = [:]
        request.getProperties() >> { props }

        when:
        def result = queryOperations.setFlagsOnRequest(request)

        then:
        result instanceof QueryRequest
        result.properties == props
        props.containsKey(Constants.LOCAL_DESTINATION_KEY)
        props.containsKey(Constants.REMOTE_DESTINATION_KEY)
        props.get(Constants.LOCAL_DESTINATION_KEY)
        !props.get(Constants.REMOTE_DESTINATION_KEY)
    }

    def 'test setting flags on QueryRequest with local source id only'() {
        setup:
        def request = Mock(QueryRequest)
        request.getSourceIds() >> [SOURCE_ID]

        def props = [:]
        request.getProperties() >> { props }

        when:
        def result = queryOperations.setFlagsOnRequest(request)

        then:
        result instanceof QueryRequest
        result.properties == props
        props.containsKey(Constants.LOCAL_DESTINATION_KEY)
        props.containsKey(Constants.REMOTE_DESTINATION_KEY)
        !props.get(Constants.LOCAL_DESTINATION_KEY)
        !props.get(Constants.REMOTE_DESTINATION_KEY)
    }

    def 'test setting flags on QueryRequest with store and source ids'() {
        setup:
        def request = Mock(QueryRequest)
        request.getStoreIds() >> ['store1', 'store2']
        request.getSourceIds() >> ['source1', 'source2']

        def props = [:]
        request.getProperties() >> { props }

        when:
        def result = queryOperations.setFlagsOnRequest(request)

        then:
        result instanceof QueryRequest
        result.properties == props
        props.containsKey(Constants.LOCAL_DESTINATION_KEY)
        props.containsKey(Constants.REMOTE_DESTINATION_KEY)
        !props.get(Constants.LOCAL_DESTINATION_KEY)
        props.get(Constants.REMOTE_DESTINATION_KEY)
    }

    def 'test setting flags on QueryRequest with source ids'() {
        setup:
        def request = Mock(QueryRequest)
        request.getSourceIds() >> ['source1', 'source2']

        def props = [:]
        request.getProperties() >> { props }

        when:
        def result = queryOperations.setFlagsOnRequest(request)

        then:
        result instanceof QueryRequest
        result.properties == props
        props.containsKey(Constants.LOCAL_DESTINATION_KEY)
        props.containsKey(Constants.REMOTE_DESTINATION_KEY)
        !props.get(Constants.LOCAL_DESTINATION_KEY)
        props.get(Constants.REMOTE_DESTINATION_KEY)
    }

    def 'test setting flags on QueryRequest with source ids including local source'() {
        setup:
        def request = Mock(QueryRequest)
        request.getSourceIds() >> ['source1', 'source2', SOURCE_ID]

        def props = [:]
        request.getProperties() >> { props }

        when:
        def result = queryOperations.setFlagsOnRequest(request)

        then:
        result instanceof QueryRequest
        result.properties == props
        props.containsKey(Constants.LOCAL_DESTINATION_KEY)
        props.containsKey(Constants.REMOTE_DESTINATION_KEY)
        !props.get(Constants.LOCAL_DESTINATION_KEY)
        props.get(Constants.REMOTE_DESTINATION_KEY)
    }

    def 'test setting flags on QueryRequest with store ids'() {
        setup:
        def request = Mock(QueryRequest)
        request.getStoreIds() >> ['store1', 'store2']

        def props = [:]
        request.getProperties() >> { props }

        when:
        def result = queryOperations.setFlagsOnRequest(request)

        then:
        result instanceof QueryRequest
        result.properties == props
        props.containsKey(Constants.LOCAL_DESTINATION_KEY)
        props.containsKey(Constants.REMOTE_DESTINATION_KEY)
        !props.get(Constants.LOCAL_DESTINATION_KEY)
        props.get(Constants.REMOTE_DESTINATION_KEY)
    }

    def 'test query with null request'() {
        when:
        queryOperations.query(null, null, false, false)

        then:
        thrown(UnsupportedQueryException)
    }

    def 'test query with null query'() {
        setup:
        def request = Mock(QueryRequest)
        request.getProperties() >> [:]
        request.query >> null

        when:
        queryOperations.query(request, null, false, false)

        then:
        thrown(UnsupportedQueryException)
    }

    def 'test query with null fed strategy'() {
        setup:
        def request = Mock(QueryRequest)
        request.getProperties() >> [:]
        request.query >> Mock(Query)

        when:
        queryOperations.query(request, null, false, false)

        then:
        thrown(FederationException)
    }

    def 'test query with null fed strategy, one in framework properties, but empty sources'() {
        setup:
        def request = Mock(QueryRequest)
        request.getProperties() >> [:]
        request.query >> Mock(Query)

        when:
        queryOperations.query(request, null, false, false)

        then:
        thrown(FederationException)
    }

    def 'test query with null fed strategy, one in framework properties, non empty sources'() {
        setup:
        def request = Mock(QueryRequest)
        def query = Mock(Query)
        def response = Mock(QueryResponse)

        query.getTimeoutMillis() >> { 100 }
        request.query >> { query }
        request.getQuery() >> { query }
        request.getProperties() >> [:]
        request.getSourceIds() >> { ['fed1', 'fed2'] }
        response.getRequest() >> { request }
        response.getResults() >> []
        response.getProperties() >> [:]
        frameworkProperties.federationStrategy = Mock(FederationStrategy)

        when:
        queryOperations.query(request, null, false, false)

        then:
        frameworkProperties.federationStrategy.federate(_, _) >> response
        notThrown(FederationException)
    }

    def 'ensure query request policy map populated'() {
        setup:
        def request = Mock(QueryRequest)
        def query = Mock(Query)
        def response = Mock(QueryResponse)

        query.getTimeoutMillis() >> { 100 }
        query.getPageSize() >> { 100 }
        request.query >> { query }
        request.getQuery() >> { query }
        request.getProperties() >> [:]
        request.getSourceIds() >> { ['fed1', 'fed2'] }
        response.getRequest() >> { request }
        response.getResults() >> []
        response.getProperties() >> [:]
        frameworkProperties.federationStrategy = Mock(FederationStrategy)

        def policyResponse = Mock(PolicyResponse)
        policyResponse.operationPolicy() >> { [:] }

        def policyPlugins = [Mock(PolicyPlugin), Mock(PolicyPlugin)]
        frameworkProperties.policyPlugins = policyPlugins

        when:
        queryOperations.query(request, null, false, false)

        then:
        policyPlugins.each {
            1 * it.processPreQuery(_ as Query, _ as Map) >> policyResponse
            1 * opsSecurity.buildPolicyMap(_, _)
        }
        frameworkProperties.federationStrategy.federate(_, _) >> response
    }

    def 'ensure default timeout is used if negative set in query'() {
        setup:
        def request = Mock(QueryRequest)
        def query = Mock(Query)
        def response = Mock(QueryResponse)
        def capturedQuery
        frameworkProperties.federationStrategy = Mock(FederationStrategy)

        queryOperations.setQueryTimeoutMillis(500)

        query.getTimeoutMillis() >> { -100 }
        request.query >> { query }
        request.getQuery() >> { query }
        request.getProperties() >> [:]
        request.getSourceIds() >> { ['fed1', 'fed2'] }
        response.getRequest() >> { request }
        response.getResults() >> []
        response.getProperties() >> [:]
        frameworkProperties.federationStrategy.federate(_, _) >> { sources, queryParam ->
            capturedQuery = queryParam
            response
        }

        when:
        queryOperations.query(request, null, false, false)

        then:
        capturedQuery instanceof QueryRequest
        capturedQuery.getQuery().getTimeoutMillis() == 500
    }

    def 'ensure default timeout is used if zero set in query'() {
        setup:
        def request = Mock(QueryRequest)
        def query = Mock(Query)
        def response = Mock(QueryResponse)
        def capturedQuery
        frameworkProperties.federationStrategy = Mock(FederationStrategy)

        queryOperations.setQueryTimeoutMillis(500)

        query.getTimeoutMillis() >> { 0 }
        request.query >> { query }
        request.getQuery() >> { query }
        request.getProperties() >> [:]
        request.getSourceIds() >> { ['fed1', 'fed2'] }
        response.getRequest() >> { request }
        response.getResults() >> []
        response.getProperties() >> [:]
        frameworkProperties.federationStrategy.federate(_, _) >> { sources, queryParam ->
            capturedQuery = queryParam
            response
        }

        when:
        queryOperations.query(request, null, false, false)

        then:
        capturedQuery instanceof QueryRequest
        capturedQuery.getQuery().getTimeoutMillis() == 500
    }

    def 'ensure specified timeout is used from query'() {
        setup:
        def request = Mock(QueryRequest)
        def query = Mock(Query)
        def response = Mock(QueryResponse)
        def capturedQuery
        frameworkProperties.federationStrategy = Mock(FederationStrategy)

        queryOperations.setQueryTimeoutMillis(500)

        query.getTimeoutMillis() >> { 800 }
        request.query >> { query }
        request.getQuery() >> { query }
        request.getProperties() >> [:]
        request.getSourceIds() >> { ['fed1', 'fed2'] }
        response.getRequest() >> { request }
        response.getResults() >> []
        response.getProperties() >> [:]
        frameworkProperties.federationStrategy.federate(_, _) >> { sources, queryParam ->
            capturedQuery = queryParam
            response
        }

        when:
        queryOperations.query(request, null, false, false)

        then:
        capturedQuery instanceof QueryRequest
        capturedQuery.getQuery().getTimeoutMillis() == 800
    }

    def 'non-version filter covers revision and deleted metacards'() {
        setup:
        FilterBuilder filterBuilder = new GeotoolsFilterBuilder()
        frameworkProperties.filterBuilder = filterBuilder
        def request = Mock(QueryRequest)
        def response = Mock(QueryResponse)
        def capturedQuery
        frameworkProperties.federationStrategy = Mock(FederationStrategy)

        def query = new QueryImpl(queryOperations.getNonVersionTagsFilter(request))

        request.query >> { query }
        request.getQuery() >> { query }
        request.getProperties() >> [:]
        request.getSourceIds() >> { ['fed1', 'fed2'] }
        response.getRequest() >> { request }
        response.getResults() >> []
        response.getProperties() >> [:]
        frameworkProperties.federationStrategy.federate(_, _) >> { sources, queryParam ->
            capturedQuery = queryParam
            response
        }

        when:
        queryOperations.query(request, null, false, false)

        then:
        capturedQuery instanceof QueryRequest
        capturedQuery.getQuery().toString().contains("[ NOT [[ metacard-tags is like revision ] OR [ metacard-tags is like deleted ]] ]")

    }

    private def mockCatalogProvider(def id) {
        def catProv = Mock(CatalogProvider)
        catProv.getId() >> { return id }
        return catProv
    }

    private def mockFedSource(def id, def available = true) {
        def contentType = Mock(ContentType)
        contentType.getName() >> { "content type: $id " }

        def fedSource = Mock(FederatedSource)
        fedSource.getId() >> { id }
        fedSource.isAvailable() >> { available }
        fedSource.getContentTypes() >> { [contentType] }
        fedSource.getSecurityAttributes() >> { [:] }

        return fedSource
    }

    private def mockConnectedSource(def id, def available = true) {
        def connectedSource = Mock(ConnectedSource)
        connectedSource.getId() >> { id }
        connectedSource.isAvailable() >> { available }

        return connectedSource
    }

    private def mockResult(def id, def sourceId) {
        def metacard = new MetacardImpl(Mock(MetacardType))
        metacard.setSourceId(sourceId)
        metacard.id = id
        def result = Mock(Result)
        result.getMetacard() >> { metacard }
        result.getDistanceInMeters() >> { 5.0 }
        result.getRelevanceScore() >> { 0.88 }
        return result
    }
}
