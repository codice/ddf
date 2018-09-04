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

import ddf.action.ActionRegistry
import ddf.catalog.content.StorageProvider
import ddf.catalog.data.ContentType
import ddf.catalog.impl.FrameworkProperties
import ddf.catalog.operation.SourceInfoRequest
import ddf.catalog.source.CatalogProvider
import ddf.catalog.source.FederatedSource
import ddf.catalog.source.Source
import ddf.catalog.source.SourceUnavailableException
import ddf.catalog.util.impl.CachedSource
import ddf.catalog.util.impl.SourcePoller
import ddf.catalog.util.impl.SourcePollerRunner
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.AsyncConditions

class SourceOperationsSpec extends Specification {
    private static final String SOURCE_ID = "test_source"

    private FrameworkProperties frameworkProperties
    private List<CatalogProvider> catalogProviders
    private List<StorageProvider> storageProviders
    private Map<String, FederatedSource> fedSources
    private SourcePoller sourcePoller
    private SourcePollerRunner pollerRunner
    private SourceOperations sourceOperations
    private double pollerWaitTime

    def setup() {
        frameworkProperties = new FrameworkProperties()
        catalogProviders = (1..3).collect { mockCatalogProvider(it) }
        storageProviders = [Mock(StorageProvider)]
        fedSources = ['fed1', 'fed2'].collectEntries { [(it): mockFedSource(it)] }
        pollerRunner = new SourcePollerRunner()
        sourcePoller = new SourcePoller(pollerRunner)
        for (FederatedSource federatedSource : fedSources.values()) {
            pollerRunner.bind(federatedSource)
        }
        pollerRunner.bind(catalogProviders.get(0))
        int wait = 0
        while (wait < 5) {
            for (FederatedSource source : fedSources.values()) {
                CachedSource cachedSource = sourcePoller.getCachedSource(source)
                if (cachedSource == null || !cachedSource.isAvailable()) {
                    Thread.sleep(100)
                    wait++
                    break
                }
            }
            CachedSource cachedProvider = sourcePoller.getCachedSource(catalogProviders.get(0))
            if (cachedProvider == null || !cachedProvider.isAvailable()) {
                Thread.sleep(100)
            }
            wait++
        }
        pollerWaitTime = (sourcePoller.getInterval() * 60) + 5

        frameworkProperties.with {
            catalogProviders = this.catalogProviders
            storageProviders = this.storageProviders
            federatedSources = fedSources
            sourcePoller = this.sourcePoller
        }
        sourceOperations = new SourceOperations(frameworkProperties, Mock(ActionRegistry))
        sourceOperations.setId(SOURCE_ID)
        sourceOperations.bind(catalogProviders.get(0))
    }

    def 'test bind catalog provider'() {
        when:
        sourceOperations.bind(Mock(CatalogProvider))

        then:
        sourceOperations.catalog == catalogProviders.first()
    }

    def 'test unbind catalog provider'() {
        when:
        sourceOperations.unbind(Mock(CatalogProvider))

        then:
        sourceOperations.catalog == catalogProviders.first()
    }

    def 'test bind storage provider'() {
        when:
        sourceOperations.bind(Mock(StorageProvider))

        then:
        sourceOperations.storage == storageProviders.first()
    }

    def 'test unbind storage provider'() {
        when:
        sourceOperations.unbind(Mock(StorageProvider))

        then:
        sourceOperations.storage == storageProviders.first()
    }

    def 'test getSourceIds no fanout'() {
        when:
        def ids = sourceOperations.getSourceIds(false)

        then:
        ids == fedSources.keySet() + SOURCE_ID
    }

    def 'test getSourceIds with fanout'() {
        when:
        def ids = sourceOperations.getSourceIds(true)

        then:
        ids == [SOURCE_ID] as Set
    }

    def 'test getSourceInfo with fanout and null request'() {
        when:
        sourceOperations.getSourceInfo(null, true)

        then:
        thrown(SourceUnavailableException)
    }

    def 'test getSourceInfo with fanout and null source id list'() {
        setup:
        def request = Mock(SourceInfoRequest)
        request.getSourceIds() >> { null }

        when:
        def response = sourceOperations.getSourceInfo(request, true)

        then:
        response.sourceInfo*.sourceId as Set == [SOURCE_ID] as Set

        Set<ContentType> types = response.sourceInfo*.contentTypes.flatten() as Set<ContentType>
        Set<ContentType> fedContentTypes = fedSources.values()*.contentTypes.flatten() as Set<ContentType>
        types*.name as Set == fedContentTypes*.name as Set
    }

    def 'test getSourceInfo with fanout and empty source id list'() {
        setup:
        def request = Mock(SourceInfoRequest)
        request.getSourceIds() >> { [] }

        when:
        def response = sourceOperations.getSourceInfo(request, true)

        then:
        response.sourceInfo*.sourceId as Set == [SOURCE_ID] as Set

        Set<ContentType> types = response.sourceInfo*.contentTypes.flatten() as Set<ContentType>
        Set<ContentType> fedContentTypes = fedSources.values()*.contentTypes.flatten() as Set<ContentType>
        types*.name as Set == fedContentTypes*.name as Set
    }

    def 'test getSourceInfo with fanout and source id list with unknown sources'() {
        setup:
        def request = Mock(SourceInfoRequest)
        request.getSourceIds() >> { ['unknown1', 'unknown2'] }

        when:
        sourceOperations.getSourceInfo(request, true)

        then:
        thrown(SourceUnavailableException)
    }

    def 'test getSourceInfo with fanout and source id list contains only local'() {
        setup:
        def request = Mock(SourceInfoRequest)
        request.getSourceIds() >> { [SOURCE_ID] }

        when:
        def response = sourceOperations.getSourceInfo(request, true)

        then:
        response.sourceInfo*.sourceId as Set == [SOURCE_ID] as Set

        Set<ContentType> types = response.sourceInfo*.contentTypes.flatten() as Set<ContentType>
        Set<ContentType> fedContentTypes = fedSources.values()*.contentTypes.flatten() as Set<ContentType>
        types*.name as Set == fedContentTypes*.name as Set
    }

    def 'test getSourceInfo with fanout and source id list contains only local, disabling one fed source'() {
        setup:
        def request = Mock(SourceInfoRequest)
        request.getSourceIds() >> { [SOURCE_ID] }
        def oneDisabledSource = [fed1: mockFedSource('fed1'), fed2: mockFedSource('fed2', false)]
        frameworkProperties.federatedSources = oneDisabledSource
        frameworkProperties.sourcePoller.runner.bind(oneDisabledSource.fed2)
        int retry
        while (retry < 5) {
            def source = frameworkProperties.sourcePoller.getCachedSource(oneDisabledSource.fed2)
            if (source.isAvailable()) {
                break
            }
            Thread.sleep(100)
            retry++
        }

        when:
        def response = sourceOperations.getSourceInfo(request, true)

        then:
        response.sourceInfo*.sourceId as Set == [SOURCE_ID] as Set

        Set<ContentType> types = response.sourceInfo*.contentTypes.flatten() as Set<ContentType>
        def fedContentTypes = oneDisabledSource.fed1.contentTypes as Set
        types*.name as Set == fedContentTypes*.name as Set
    }

    def 'test getSourceInfo with fanout and source id list contains local and one unknown'() {
        setup:
        def request = Mock(SourceInfoRequest)
        request.getSourceIds() >> { [SOURCE_ID, 'unknown1'] }

        when:
        sourceOperations.getSourceInfo(request, true)

        then: 'It is an error to ask for any source but local in fanout mode'
        thrown(SourceUnavailableException)
    }


    def 'test getSourceInfo without fanout and null request'() {
        when:
        sourceOperations.getSourceInfo(null, false)

        then:
        thrown(SourceUnavailableException)
    }

    def 'test getSourceInfo without fanout and isEnterprise'() {
        setup:
        def request = Mock(SourceInfoRequest)
        request.isEnterprise() >> { true }

        when:
        def response = sourceOperations.getSourceInfo(request, false)

        then:
        response.sourceInfo*.sourceId as Set == fedSources.keySet() + SOURCE_ID

        Set<ContentType> types = response.sourceInfo*.contentTypes.flatten() as Set<ContentType>
        Set<ContentType> fedContentTypes = fedSources.values()*.contentTypes.flatten() as Set<ContentType>
        types*.name == fedContentTypes*.name
    }

    def 'test getSourceInfo without fanout, not enterprise, null source id list'() {
        setup:
        def request = Mock(SourceInfoRequest)
        request.isEnterprise() >> { false }
        request.getSourceIds() >> { null }

        when:
        def response = sourceOperations.getSourceInfo(request, false)

        then:
        response.sourceInfo*.sourceId as Set == [SOURCE_ID] as Set

        Set<ContentType> types = response.sourceInfo*.contentTypes.flatten() as Set<ContentType>
        types*.name as Set == [] as Set
    }

    def 'test getSourceInfo without fanout, not enterprise, empty source id list'() {
        setup:
        def request = Mock(SourceInfoRequest)
        request.isEnterprise() >> { false }
        request.getSourceIds() >> { [] }

        when:
        def response = sourceOperations.getSourceInfo(request, false)

        then:
        response.sourceInfo*.sourceId as Set == [] as Set

        Set<ContentType> types = response.sourceInfo*.contentTypes.flatten() as Set<ContentType>
        types*.name as Set == [] as Set
    }

    def 'test getSourceInfo without fanout, not enterprise, source id list with unknown sources'() {
        setup:
        def request = Mock(SourceInfoRequest)
        request.isEnterprise() >> { false }
        request.getSourceIds() >> { ['unknown1', 'unknown2'] }

        when:
        def response = sourceOperations.getSourceInfo(request, false)

        then:
        response.sourceInfo*.sourceId as Set == [] as Set

        Set<ContentType> types = response.sourceInfo*.contentTypes.flatten() as Set<ContentType>
        types*.name as Set == [] as Set
    }

    def 'test getSourceInfo without fanout, not enterprise, source id list contains only local'() {
        setup:
        def request = Mock(SourceInfoRequest)
        request.isEnterprise() >> { false }
        request.getSourceIds() >> { [SOURCE_ID] }

        when:
        def response = sourceOperations.getSourceInfo(request, false)

        then:
        response.sourceInfo*.sourceId as Set == [SOURCE_ID] as Set

        Set<ContentType> types = response.sourceInfo*.contentTypes.flatten() as Set<ContentType>
        types*.name as Set == [] as Set
    }

    def 'test getSourceInfo without fanout, not enterprise, source id list contains local and one unknown'() {
        setup:
        def request = Mock(SourceInfoRequest)
        request.isEnterprise() >> { false }
        request.getSourceIds() >> { [SOURCE_ID, 'unknown1'] }

        when:
        def response = sourceOperations.getSourceInfo(request, false)

        then:
        response.sourceInfo*.sourceId as Set == [SOURCE_ID] as Set

        Set<ContentType> types = response.sourceInfo*.contentTypes.flatten() as Set<ContentType>
        types*.name as Set == [] as Set
    }

    def 'test getSourceInfo without fanout, not enterprise, source id list contains one fed source'() {
        setup:
        def request = Mock(SourceInfoRequest)
        request.isEnterprise() >> { false }
        request.getSourceIds() >> { [fedSources.keySet().first()] }

        when:
        def response = sourceOperations.getSourceInfo(request, false)

        then:
        response.sourceInfo*.sourceId as Set == [fedSources.keySet().first()] as Set

        Set<ContentType> types = response.sourceInfo*.contentTypes.flatten() as Set<ContentType>
        Set<ContentType> fedContentTypes = fedSources.values().first().contentTypes.flatten() as Set<ContentType>
        types*.name == fedContentTypes*.name
    }

    def 'test getSourceInfo without fanout, not enterprise, source id list contains fed sources'() {
        setup:
        def request = Mock(SourceInfoRequest)
        request.isEnterprise() >> { false }
        request.getSourceIds() >> { fedSources.keySet() }

        when:
        def response = sourceOperations.getSourceInfo(request, false)

        then:
        response.sourceInfo*.sourceId as Set == fedSources.keySet()

        Set<ContentType> types = response.sourceInfo*.contentTypes.flatten() as Set<ContentType>
        Set<ContentType> fedContentTypes = fedSources.values()*.contentTypes.flatten() as Set<ContentType>
        types*.name == fedContentTypes*.name
    }

    def 'test getSourceInfo without fanout, not enterprise, source id list contains fed sources and one unknown'() {
        setup:
        def request = Mock(SourceInfoRequest)
        request.isEnterprise() >> { false }
        request.getSourceIds() >> { fedSources.keySet() + ['unknown1'] }

        when:
        def response = sourceOperations.getSourceInfo(request, false)

        then:
        response.sourceInfo*.sourceId as Set == fedSources.keySet()

        Set<ContentType> types = response.sourceInfo*.contentTypes.flatten() as Set<ContentType>
        Set<ContentType> fedContentTypes = fedSources.values()*.contentTypes.flatten() as Set<ContentType>
        types*.name == fedContentTypes*.name
    }

    def 'test getSourceInfo without fanout, not enterprise, source id list contains local + fed sources'() {
        setup:
        def request = Mock(SourceInfoRequest)
        request.isEnterprise() >> { false }
        request.getSourceIds() >> { fedSources.keySet() + SOURCE_ID }

        when:
        def response = sourceOperations.getSourceInfo(request, false)

        then:
        response.sourceInfo*.sourceId as Set == fedSources.keySet() + SOURCE_ID

        Set<ContentType> types = response.sourceInfo*.contentTypes.flatten() as Set<ContentType>
        Set<ContentType> fedContentTypes = fedSources.values()*.contentTypes.flatten() as Set<ContentType>
        types*.name == fedContentTypes*.name
    }

    def 'test getSourceInfo without fanout, not enterprise, source id list contains local + fed sources and one unknown'() {
        setup:
        def request = Mock(SourceInfoRequest)
        request.isEnterprise() >> { false }
        request.getSourceIds() >> { fedSources.keySet() + SOURCE_ID + "unknown1" }

        when:
        def response = sourceOperations.getSourceInfo(request, false)

        then:
        response.sourceInfo*.sourceId as Set == fedSources.keySet() + SOURCE_ID

        Set<ContentType> types = response.sourceInfo*.contentTypes.flatten() as Set<ContentType>
        Set<ContentType> fedContentTypes = fedSources.values()*.contentTypes.flatten() as Set<ContentType>
        types*.name == fedContentTypes*.name
    }

    def 'test isSourceAvailable null source'() {
        when:
        def available = sourceOperations.isSourceAvailable(null)

        then:
        !available
    }

    @Ignore // Currently still fails under some circumstances
    def 'test source not available'() {
        setup:
        def source = Mock(Source)
        source.id >> { 'src1' }
        source.isAvailable() >> { false }
        pollerRunner.bind(source)
        pollerRunner.run()
        def conds = new AsyncConditions()
        conds.evaluate {
            _ * source.isAvailable()
        }

        when:
        def available = sourceOperations.isSourceAvailable(source)

        then:
        conds.await(pollerWaitTime)

        then:
        !available
    }

    @Ignore // Currently still fails under some circumstances
    def 'test source is available'() {
        setup:
        def source = Mock(Source)
        source.id >> { 'src2' }
        source.isAvailable() >> { true }
        pollerRunner.bind(source)
        pollerRunner.run()
        def conds = new AsyncConditions()
        conds.evaluate {
            _ * source.isAvailable()
        }

        when:
        def available = sourceOperations.isSourceAvailable(source)

        then:
        conds.await(pollerWaitTime)

        then:
        available
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
        fedSource.getId() >> { return id }
        fedSource.isAvailable() >> { available }
        fedSource.getContentTypes() >> { [contentType] }

        return fedSource
    }
}
