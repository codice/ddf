package org.codice.ddf.admin.configurator.impl

import org.apache.karaf.bundle.core.BundleState
import org.apache.karaf.bundle.core.BundleStateService
import org.apache.shiro.authz.Permission
import org.apache.shiro.subject.Subject
import org.codice.ddf.admin.configurator.ConfiguratorException
import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceReference
import spock.lang.Specification

class BundleConfigHandlerSpec extends Specification {
    private BundleStateService bundleStateService
    private ServiceReference serviceReference
    private BundleContext bundleContext
    private Bundle bundle
    private Subject subject

    def setup() {
        def implementedService = Mock(ServiceReference)
        implementedService.getProperty('service.pid') >> 'xxx'

        bundle = Mock(Bundle)
        bundle.getSymbolicName() >> 'xxx'
        bundle.getRegisteredServices() >> [implementedService]

        bundleStateService = Mock(BundleStateService)

        serviceReference = Mock(ServiceReference)

        bundleContext = Mock(BundleContext)
        bundleContext.getServiceReference(BundleStateService) >> serviceReference
        bundleContext.getService(serviceReference) >> bundleStateService
        bundleContext.getBundles() >> [bundle]

        subject = Mock(Subject)
        subject.isPermitted(_ as Permission) >> true
    }

    def 'test start bundle that does not exist'() {
        when:
        new BundleOperation('doesnotexist', true, bundleContext, subject)

        then:
        thrown(ConfiguratorException)
    }

    def 'test start bundle that was stopped and rollback'() {
        setup:
        bundleStateService.getState(bundle) >>> [BundleState.Installed, BundleState.Active]
        def handler = new BundleOperation('xxx', true, bundleContext, subject)

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
        def handler = new BundleOperation('xxx', false, bundleContext, subject)

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
        def handler = new BundleOperation('xxx', true, bundleContext, subject)

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
        def handler = new BundleOperation('xxx', false, bundleContext, subject)

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

    def 'test start bundle that was stopped and rollback with no permissions'() {
        setup:
        bundleStateService.getState(bundle) >>> [BundleState.Installed, BundleState.Active]
        subject = Mock(Subject)
        subject.isPermitted(_ as Permission) >> false
        def handler = new BundleOperation('xxx', true, bundleContext, subject)

        when:
        handler.commit()

        then:
        thrown(ConfiguratorException)
    }
}
