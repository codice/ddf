package org.codice.ddf.admin.configurator.impl

import org.apache.karaf.bundle.core.BundleState
import org.apache.karaf.bundle.core.BundleStateService
import org.codice.ddf.admin.configurator.ConfiguratorException
import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceReference
import spock.lang.Specification

class BundleConfigHandlerTest extends Specification {
    private BundleStateService bundleStateService
    private ServiceReference serviceReference
    private BundleContext bundleContext
    private Bundle bundle

    def setup() {
        bundle = Mock(Bundle)
        bundle.getSymbolicName() >> 'xxx'

        bundleStateService = Mock(BundleStateService)

        serviceReference = Mock(ServiceReference)

        bundleContext = Mock(BundleContext)
        bundleContext.getServiceReference(BundleStateService) >> serviceReference
        bundleContext.getService(serviceReference) >> bundleStateService
        bundleContext.getBundles() >> [bundle]
    }

    def 'test start bundle that does not exist'() {
        when:
        BundleOperation.forStart('doesnotexist', bundleContext)

        then:
        thrown(ConfiguratorException)
    }

    def 'test start bundle that was stopped and rollback'() {
        setup:
        bundleStateService.getState(bundle) >>> [BundleState.Installed, BundleState.Active]
        def handler = BundleOperation.forStart('xxx', bundleContext)

        when:
        handler.commit()

        then:
        1 * bundle.start()

        when:
        handler.rollback()

        then:
        1 * bundle.stop()
    }

    def 'test stop bundle that was started and rollback'() {
        setup:
        bundleStateService.getState(bundle) >>> [BundleState.Active, BundleState.Installed]
        def handler = BundleOperation.forStop('xxx', bundleContext)

        when:
        handler.commit()

        then:
        1 * bundle.stop()

        when:
        handler.rollback()

        then:
        1 * bundle.start()
    }

    def 'test start bundle that was already started and rollback'() {
        setup:
        bundleStateService.getState(bundle) >> BundleState.Active
        def handler = BundleOperation.forStart('xxx', bundleContext)

        when:
        handler.commit()

        then:
        0 * bundle.start()
        0 * bundle.stop()

        when:
        handler.rollback()

        then:
        0 * bundle.start()
        0 * bundle.stop()
    }

    def 'test stop bundle that was already stopped and rollback'() {
        setup:
        bundleStateService.getState(bundle) >> BundleState.Installed
        def handler = BundleOperation.forStop('xxx', bundleContext)

        when:
        handler.commit()

        then:
        0 * bundle.start()
        0 * bundle.stop()

        when:
        handler.rollback()

        then:
        0 * bundle.start()
        0 * bundle.stop()
    }
}
