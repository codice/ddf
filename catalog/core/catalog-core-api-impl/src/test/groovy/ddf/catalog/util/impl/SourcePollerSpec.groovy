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

import ddf.catalog.source.FederatedSource
import ddf.catalog.source.Source
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.TimeUnit

import static org.awaitility.Awaitility.await

class SourcePollerSpec extends Specification {

    // TODO Set these to something much smaller in the SourcePoller so that these units tests are fast.
    private static final TimeUnit DEFAULT_AVAILABILITY_CHECK_TIMEOUT_TIME_UNIT = TimeUnit.MINUTES

    private static final long DEFAULT_AVAILABILITY_CHECK_TIMEOUT = 1

    // create Source tests

    @Unroll
    def 'test create #expectedSourceStatus FederatedSource'() {
        given:
        final SourcePoller sourcePoller = new SourcePoller()

        final FederatedSource mockFederatedSource = Mock(FederatedSource) {
            isAvailable() >> availability
        }

        when:
        sourcePoller.setFederatedSources([mockFederatedSource])

        then:
        assertSourceStatus(sourcePoller, mockFederatedSource, expectedSourceStatus)

        cleanup:
        sourcePoller.destroy()

        where:
        availability || expectedSourceStatus
        true         || SourceStatus.AVAILABLE
        false        || SourceStatus.UNAVAILABLE
    }

    def 'test bind FederatedSource availability timeout'() {
        given:
        final SourcePoller sourcePoller = new SourcePoller()

        final FederatedSource mockFederatedSource = Mock(FederatedSource) {
            isAvailable() >> {
                Thread.sleep(DEFAULT_AVAILABILITY_CHECK_TIMEOUT_TIME_UNIT.toMillis(DEFAULT_AVAILABILITY_CHECK_TIMEOUT) + 1)
                return true
            }
        }

        when:
        sourcePoller.setFederatedSources([mockFederatedSource])

        then:
        assertSourceStatusIsTimeout(sourcePoller, mockFederatedSource)

        cleanup:
        sourcePoller.destroy()
    }

    // TODO fix these tests
//
//    /**
//     * TODO Add more unit tests for {@link SourceStatus#EXCEPTION}
//     */
//    def 'test bind Source that throws RuntimeException'() {
//        given:
//        final SourcePoller sourcePoller = new SourcePoller()
//
//        final Source mockSource = Mock(Source) {
//            isAvailable() >> {
//                throw new RuntimeException()
//            }
//        }
//
//        when:
//        sourcePoller.bind(mockSource)
//
//        then:
//        assertSourceStatus(sourcePoller, mockSource, SourceStatus.EXCEPTION)
//
//        cleanup:
//        sourcePoller.destroy()
//    }
//
//    // TODO bind source that throws exception
//
//    // unbind tests
//
//    def 'test unbind null Source'() {
//        given:
//        final SourcePoller sourcePoller = new SourcePoller()
//
//        when:
//        sourcePoller.unbind(null)
//
//        then:
//        notThrown(IllegalArgumentException)
//
//        cleanup:
//        sourcePoller.destroy()
//    }
//
//    def 'test unbind unknown Source'() {
//        given:
//        final SourcePoller sourcePoller = new SourcePoller()
//
//        final Source unknownMockSource = Mock(Source)
//
//        when:
//        sourcePoller.unbind(unknownMockSource)
//
//        then:
//        notThrown IllegalArgumentException
//
//        cleanup:
//        sourcePoller.destroy()
//    }
//
//    def 'test unbind Source'() {
//        given:
//        final SourcePoller sourcePoller = new SourcePoller()
//
//        final Source mockSource = Mock(Source) {
//            isAvailable() >> availability
//        }
//
//        and: 'bind the source'
//        sourcePoller.bind(mockSource)
//        assertSourceStatus(sourcePoller, mockSource, sourceStatus)
//
//        when: 'the source is unbound'
//        sourcePoller.unbind(mockSource)
//
//        then: 'the source status is updated'
//        sourcePoller.getSourceAvailability(mockSource) == Optional.empty()
//
//        cleanup:
//        sourcePoller.destroy()
//
//        where:
//        availability | sourceStatus
//        true         | SourceStatus.AVAILABLE
//        false        | SourceStatus.UNAVAILABLE
//    }
//
//    // source modified tests
//
//    def 'test Source is modified'() {
//        given:
//        final BundleContext mockBundleContext = Mock(BundleContext)
//        final SourcePoller sourcePoller = new SourcePoller() {
//            @Override
//            BundleContext getBundleContext() {
//                return mockBundleContext
//            }
//        }
//
//        final Source mockSource = Mock(Source) {
//            isAvailable() >>> [firstAvailability, secondAvailability]
//        }
//
//        and: 'bind the source'
//        sourcePoller.bind(mockSource)
//        assertSourceStatus(sourcePoller, mockSource, expectedFirstSourceStatus)
//
//        when: 'the SourcePoller is notified that the source is modified'
//        sourcePoller.event(Mock(ServiceEvent) {
//            final ServiceReference mockServiceReference = Mock(ServiceReference)
//            mockBundleContext.getService(mockServiceReference) >> mockSource
//
//            getServiceReference() >> mockServiceReference
//            getType() >> ServiceEvent.MODIFIED
//        }, [:])
//
//        then: 'the source status is updated'
//        assertSourceStatus(sourcePoller, mockSource, expectedSecondSourceStatus)
//
//        cleanup:
//        sourcePoller.destroy()
//
//        where:
//        firstAvailability | expectedFirstSourceStatus | secondAvailability || expectedSecondSourceStatus
//        false             | SourceStatus.UNAVAILABLE  | false              || SourceStatus.UNAVAILABLE
//        false             | SourceStatus.UNAVAILABLE  | true               || SourceStatus.AVAILABLE
//        true              | SourceStatus.AVAILABLE    | false              || SourceStatus.UNAVAILABLE
//        true              | SourceStatus.AVAILABLE    | true               || SourceStatus.AVAILABLE
//    }
//
//    def 'test Source is modified and the next availability check times out'() {
//        given:
//        final BundleContext mockBundleContext = Mock(BundleContext)
//        final SourcePoller sourcePoller = new SourcePoller() {
//            @Override
//            BundleContext getBundleContext() {
//                return mockBundleContext
//            }
//        }
//
//        final Source mockSource = Mock(Source) {
//            isAvailable() >>> firstAvailability >> {
//                Thread.sleep(DEFAULT_AVAILABILITY_CHECK_TIMEOUT_TIME_UNIT.toMillis(DEFAULT_AVAILABILITY_CHECK_TIMEOUT) + 1)
//                return false
//            }
//        }
//
//        and: 'bind the source'
//        sourcePoller.bind(mockSource)
//        assertSourceStatus(sourcePoller, mockSource, expectedFirstSourceStatus)
//
//        when: 'the SourcePoller is notified that the source is modified'
//        sourcePoller.event(Mock(ServiceEvent) {
//            final ServiceReference mockServiceReference = Mock(ServiceReference)
//            mockBundleContext.getService(mockServiceReference) >> mockSource
//
//            getServiceReference() >> mockServiceReference
//            getType() >> ServiceEvent.MODIFIED
//        }, [:])
//
//        then:
//        assertSourceStatusIsTimeout(sourcePoller, mockSource)
//
//        cleanup:
//        sourcePoller.destroy()
//
//        where:
//        firstAvailability | expectedFirstSourceStatus
//        false             | SourceStatus.UNAVAILABLE
//        true              | SourceStatus.AVAILABLE
//    }
//
//    // getSourceAvailability tests
//
//    def 'test getSourceAvailability of null Source'() {
//        given:
//        final SourcePoller sourcePoller = new SourcePoller()
//
//        when:
//        sourcePoller.getSourceAvailability(null)
//
//        then:
//        thrown IllegalArgumentException
//
//        cleanup:
//        sourcePoller.destroy()
//    }
//
//    def 'test getSourceAvailability of non-bound Source'() {
//        given:
//        final SourcePoller sourcePoller = new SourcePoller()
//
//        expect:
//        sourcePoller.getSourceAvailability(Mock(Source)) == Optional.empty()
//
//        cleanup:
//        sourcePoller.destroy()
//    }
//
//    // set Sources tests
//
//    def 'test set null ConnectedSources'() {
//        given:
//        final SourcePoller sourcePoller = new SourcePoller()
//
//        when:
//        sourcePoller.setConnectedSources(null)
//
//        then:
//        thrown IllegalArgumentException
//
//        cleanup:
//        sourcePoller.destroy()
//    }
//
//    def 'test set empty ConnectedSources'() {
//        given:
//        final SourcePoller sourcePoller = new SourcePoller()
//
//        when:
//        sourcePoller.setConnectedSources([])
//
//        then:
//        notThrown IllegalArgumentException
//
//        cleanup:
//        sourcePoller.destroy()
//    }
//
//    def 'test set null FederatedSources'() {
//        given:
//        final SourcePoller sourcePoller = new SourcePoller()
//
//        when:
//        sourcePoller.setFederatedSources(null)
//
//        then:
//        thrown IllegalArgumentException
//
//        cleanup:
//        sourcePoller.destroy()
//    }
//
//    def 'test set empty FederatedSources'() {
//        given:
//        final SourcePoller sourcePoller = new SourcePoller()
//
//        when:
//        sourcePoller.setFederatedSources([])
//
//        then:
//        notThrown IllegalArgumentException
//
//        cleanup:
//        sourcePoller.destroy()
//    }
//
//    def 'test set null CatalogProviders'() {
//        given:
//        final SourcePoller sourcePoller = new SourcePoller()
//
//        when:
//        sourcePoller.setCatalogProviders(null)
//
//        then:
//        thrown IllegalArgumentException
//
//        cleanup:
//        sourcePoller.destroy()
//    }
//
//    def 'test set empty CatalogProviders'() {
//        given:
//        final SourcePoller sourcePoller = new SourcePoller()
//
//        when:
//        sourcePoller.setCatalogProviders([])
//
//        then:
//        notThrown IllegalArgumentException
//
//        cleanup:
//        sourcePoller.destroy()
//    }
//
//    def 'test set null CatalogStores'() {
//        given:
//        final SourcePoller sourcePoller = new SourcePoller()
//
//        when:
//        sourcePoller.setCatalogStores(null)
//
//        then:
//        thrown IllegalArgumentException
//
//        cleanup:
//        sourcePoller.destroy()
//    }
//
//    def 'test set empty CatalogStores'() {
//        given:
//        final SourcePoller sourcePoller = new SourcePoller()
//
//        when:
//        sourcePoller.setCatalogStores([])
//
//        then:
//        notThrown IllegalArgumentException
//
//        cleanup:
//        sourcePoller.destroy()
//    }

    // TODO polling tests

    // helper methods

    private static boolean assertSourceStatus(final SourcePoller sourcePoller, final Source source, final SourceStatus expectedSourceStatus) {
        // TODO Better timeouts here
        await("source status is " + expectedSourceStatus).atMost(SourcePoller.SOURCE_POLLER_RUNNER_PERIOD + 1, SourcePoller.SOURCE_POLLER_RUNNER_PERIOD_TIME_UNIT).until {
            final Optional<SourceAvailability> sourceAvailability = sourcePoller.getSourceAvailability(source)
            if (sourceAvailability.isPresent()) {
                return sourceAvailability.get().getSourceStatus() == expectedSourceStatus
            } else {
                return false
            }
        }

        return true
    }

    private static boolean assertSourceStatusIsTimeout(final SourcePoller sourcePoller, final Source source) {
        final SourceStatus expectedSourceStatus = SourceStatus.TIMEOUT
        // TODO Better timeouts here
        await("source status is " + expectedSourceStatus).atMost(DEFAULT_AVAILABILITY_CHECK_TIMEOUT * 2, DEFAULT_AVAILABILITY_CHECK_TIMEOUT_TIME_UNIT).until {
            final Optional<SourceAvailability> sourceAvailability = sourcePoller.getSourceAvailability(source)
            if (sourceAvailability.isPresent()) {
                return sourceAvailability.get().getSourceStatus() == expectedSourceStatus
            } else {
                return false
            }
        }
        return true
    }
}