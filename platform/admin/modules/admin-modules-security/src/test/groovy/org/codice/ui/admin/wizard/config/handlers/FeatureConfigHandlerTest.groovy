package org.codice.ui.admin.wizard.config.handlers

import org.apache.karaf.features.FeatureState
import org.apache.karaf.features.FeaturesService
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceReference
import spock.lang.Specification

import static org.apache.karaf.features.FeaturesService.Option.NoAutoRefreshBundles

class FeatureConfigHandlerTest extends Specification {
    private ServiceReference serviceReference
    private FeaturesService featuresService
    private BundleContext bundleContext

    def setup() {
        featuresService = Mock(FeaturesService)

        serviceReference = Mock(ServiceReference)

        bundleContext = Mock(BundleContext)
        bundleContext.getServiceReference(FeaturesService) >> serviceReference
        bundleContext.getService(serviceReference) >> featuresService
    }

    def 'test start feature that was stopped and rollback'() {
        setup:
        featuresService.getState('xxx') >>> [FeatureState.Installed, FeatureState.Started]
        def handler = FeatureConfigHandler.forStart('xxx', bundleContext)

        when:
        handler.commit()

        then:
        1 * featuresService.installFeature('xxx', EnumSet.of(NoAutoRefreshBundles))

        when:
        handler.rollback()

        then:
        1 * featuresService.uninstallFeature('xxx')
    }

    def 'test stop feature that was started and rollback'() {
        setup:
        featuresService.getState('xxx') >>> [FeatureState.Started, FeatureState.Installed]
        def handler = FeatureConfigHandler.forStop('xxx', bundleContext)

        when:
        handler.commit()

        then:
        1 * featuresService.uninstallFeature('xxx')

        when:
        handler.rollback()

        then:
        1 * featuresService.installFeature('xxx', EnumSet.of(NoAutoRefreshBundles))
    }

    def 'test start feature that was already started and rollback'() {
        setup:
        featuresService.getState('xxx') >> FeatureState.Started
        def handler = FeatureConfigHandler.forStart('xxx', bundleContext)

        when:
        handler.commit()

        then:
        0 * featuresService.installFeature('xxx', EnumSet.of(NoAutoRefreshBundles))
        0 * featuresService.uninstallFeature('xxx')

        when:
        handler.rollback()

        then:
        0 * featuresService.installFeature('xxx', EnumSet.of(NoAutoRefreshBundles))
        0 * featuresService.uninstallFeature('xxx')
    }

    def 'test stop feature that was already stopped and rollback'() {
        setup:
        featuresService.getState('xxx') >> FeatureState.Installed
        def handler = FeatureConfigHandler.forStop('xxx', bundleContext)

        when:
        handler.commit()

        then:
        0 * featuresService.installFeature('xxx', EnumSet.of(NoAutoRefreshBundles))
        0 * featuresService.uninstallFeature('xxx')

        when:
        handler.rollback()

        then:
        0 * featuresService.installFeature('xxx', EnumSet.of(NoAutoRefreshBundles))
        0 * featuresService.uninstallFeature('xxx')
    }
}
