package org.codice.ddf.sync.installer.impl

import org.apache.karaf.bundle.core.BundleService
import org.apache.karaf.bundle.core.BundleState
import org.apache.karaf.bundle.core.BundleInfo
import org.apache.karaf.features.Feature
import org.apache.karaf.features.FeaturesService
import org.codice.ddf.sync.installer.api.SynchronizedInstallerException
import org.codice.ddf.sync.installer.impl.SynchronizedInstallerImpl.SynchronizedConfigurationListener
import org.osgi.framework.*
import org.osgi.service.cm.Configuration
import org.osgi.service.cm.ConfigurationAdmin
import org.osgi.service.cm.ManagedService
import org.osgi.util.tracker.ServiceTracker
import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

class SynchronizedInstallerImplSpec extends Specification {

    private BundleContext bundleContext
    private ConfigurationAdmin configAdmin
    private FeaturesService featuresService
    private BundleService bundleService
    private ServiceReference serviceReference
    private ServiceTracker serviceTracker
    private SynchronizedConfigurationListener listener

    private Configuration createdConfig

    def setup() {
        bundleContext = Mock(BundleContext)
        configAdmin = Mock(ConfigurationAdmin)
        featuresService = Mock(FeaturesService)
        bundleService = Mock(BundleService)
        serviceReference = Mock(ServiceReference)
        serviceTracker = Mock(ServiceTracker)
        listener = Mock(SynchronizedConfigurationListener)

        createdConfig = Mock(Configuration)
    }

    /**
     * Test waits
     */

    def 'wait condition is met on first try.'() {
        setup:
        def conditionToMeet = Mock(Callable)

        when:
        new SynchronizedInstallerImpl(null, null, null, null).wait(conditionToMeet, 1, 1, null)

        then:
        1 * conditionToMeet.call() >> true
    }

    def 'wait condition is met later on'() {
        setup:
        def conditionToMeet = Mock(Callable)

        when:
        new SynchronizedInstallerImpl(null, null, null, null).wait(conditionToMeet, TimeUnit.SECONDS.toMillis(15), 1, null)

        then:
        3 * conditionToMeet.call() >>> [false, false, true]
    }

    def 'wait condition is never met'() {
        setup:
        def conditionToMeet = new Callable<Boolean>() {
            @Override
            Boolean call() throws Exception {
                return false
            }
        }

        when:
        new SynchronizedInstallerImpl(null, null, null, null).wait(conditionToMeet, TimeUnit.MILLISECONDS.toMillis(10), 5, null)

        then:
        thrown(SynchronizedInstallerException)
    }

    /**
     * Create managed service factory tests
     */
    def 'successfully create managed service factory with properties'() {
        setup:
        def fpid = "fpid"
        def bundleLocation = "bundleLocation"
        def props = ['propKey': 'propValue']
        def createdPid = "createdPid"
        def returnedConfig

        createdConfig.getPid() >> createdPid

        SynchronizedInstallerMock syncInstaller = new SynchronizedInstallerBuilder()
                .configAdmin(configAdmin)
                .bundleContext(bundleContext)
                .serviceTracker(serviceTracker)
                .build()
        when:
        returnedConfig = syncInstaller.createManagedFactoryService(fpid, props, bundleLocation)

        then:
        1 * configAdmin.createFactoryConfiguration(fpid, bundleLocation) >> createdConfig
        1 * createdConfig.setBundleLocation(bundleLocation)
        1 * createdConfig.update(props)
        1 * bundleContext.createFilter(_) >> {
            arguments[0].contains(createdPid)
            return null
        }
        1 * serviceTracker.getService() >> Mock(ManagedService)
        returnedConfig == createdConfig
    }

    def 'successfully create managed service factory with empty/null properties'() {
        setup:
        def fpid = "fpid"
        def bundleLocation = "bundleLocation"
        def createdPid = "createdPid"
        def returnedConfig

        createdConfig.getPid() >> createdPid

        SynchronizedInstallerMock syncInstaller = new SynchronizedInstallerBuilder()
                .configAdmin(configAdmin)
                .bundleContext(bundleContext)
                .serviceTracker(serviceTracker)
                .build()
        when:
        returnedConfig = syncInstaller.createManagedFactoryService(fpid, null, bundleLocation)

        then:
        1 * configAdmin.createFactoryConfiguration(fpid, bundleLocation) >> createdConfig
        1 * createdConfig.setBundleLocation(bundleLocation)
        1 * createdConfig.update()
        1 * bundleContext.createFilter(_) >> {
            arguments[0].contains(createdPid)
            return null
        }
        1 * serviceTracker.getService() >> Mock(ManagedService)
        returnedConfig == createdConfig
    }

    def 'failed while trying to create factory configuration'() {
        setup:
        def fpid = 'fpid'
        configAdmin.createFactoryConfiguration(fpid, null) >> { throw new IOException() }
        SynchronizedInstallerImpl syncInstaller = new SynchronizedInstallerBuilder()
                .configAdmin(configAdmin)
                .build()

        when:
        syncInstaller.createManagedFactoryService(fpid, null, null)

        then:
        thrown(SynchronizedInstallerException)
    }

    def 'failed while trying to update factory configuration that was just created'() {
        setup:
        def fpid = 'fpid'
        createdConfig.update() >> { throw new IOException() }
        configAdmin.createFactoryConfiguration(fpid, null) >> createdConfig
        SynchronizedInstallerImpl syncInstaller = new SynchronizedInstallerBuilder()
                .configAdmin(configAdmin)
                .build()

        when:
        syncInstaller.createManagedFactoryService(fpid, null, null)

        then:
        1 * configAdmin.createFactoryConfiguration(fpid, null) >> createdConfig
        thrown(SynchronizedInstallerException)
    }

    /**
     * Update managed service tests
     */
    def 'successfully update managed service with empty properties'() {
        setup:
        def bundleLocation = 'bundleLocation'
        def props = [:]
        def listenerRegistration = Mock(ServiceRegistration)
        def pid = "pid"

        SynchronizedInstallerImpl sysInstaller = new SynchronizedInstallerBuilder()
                .bundleContext(bundleContext)
                .configAdmin(configAdmin)
                .serviceTracker(serviceTracker)
                .configListener(listener)
                .build()

        when:
            sysInstaller.updateManagedService(pid, props, bundleLocation)

        then:
        1 * serviceTracker.getService() >> Mock(ManagedService)
        1 * bundleContext.registerService(*_) >> listenerRegistration
        1 * configAdmin.getConfiguration(pid, bundleLocation) >> createdConfig
        1 * createdConfig.setBundleLocation(bundleLocation)
        1 * createdConfig.update(props)
        1 * bundleContext.createFilter(_) >> {
            arguments[0].contains(pid)
            return null
        }
        1 * listener.isUpdated() >> true
        1 * listenerRegistration.unregister()
    }

    def 'successfully update managed service with properties'() {
        setup:
        def bundleLocation = 'bundleLocation'
        def props = ['propKey':'propValue']
        def listenerRegistration = Mock(ServiceRegistration)
        def pid = "pid"

        SynchronizedInstallerImpl sysInstaller = new SynchronizedInstallerBuilder()
                .bundleContext(bundleContext)
                .configAdmin(configAdmin)
                .serviceTracker(serviceTracker)
                .configListener(listener)
                .build()

        when:
        sysInstaller.updateManagedService(pid, props, bundleLocation)

        then:
        1 * serviceTracker.getService() >> Mock(ManagedService)
        1 * bundleContext.registerService(*_) >> listenerRegistration
        1 * configAdmin.getConfiguration(pid, bundleLocation) >> createdConfig
        1 * createdConfig.setBundleLocation(bundleLocation)
        1 * createdConfig.update(props)
        1 * bundleContext.createFilter(_) >> {
            arguments[0].contains(pid)
            return null
        }
        1 * listener.isUpdated() >> true
        1 * listenerRegistration.unregister()
    }

    def 'failed while trying to update created factory configuration'() {
        setup:
        def bundleLocation = 'bundleLocation'
        def props = ['propKey':'propValue']
        def listenerRegistration = Mock(ServiceRegistration)
        def pid = "pid"

        SynchronizedInstallerImpl sysInstaller = new SynchronizedInstallerBuilder()
                .bundleContext(bundleContext)
                .configAdmin(configAdmin)
                .serviceTracker(serviceTracker)
                .configListener(listener)
                .build()

        when:
        sysInstaller.updateManagedService(pid, props, bundleLocation)

        then:
        1 * serviceTracker.getService() >> Mock(ManagedService)
        1 * bundleContext.registerService(*_) >> listenerRegistration
        1 * configAdmin.getConfiguration(pid, bundleLocation) >> createdConfig
        1 * createdConfig.setBundleLocation(bundleLocation)
        1 * createdConfig.update(props) >> { throw new IOException()}
        1 * listenerRegistration.unregister()
        thrown(SynchronizedInstallerException)
    }

    /**
     * Wait for service tests
     */
    def 'successfully wait for service'() {
        setup:
        def pid = 'pid'
        SynchronizedInstallerImpl syncInstaller = new SynchronizedInstallerBuilder()
                .bundleContext(bundleContext)
                .serviceTracker(serviceTracker)
                .build()

        when:
        syncInstaller.waitForServiceToBeAvailable(pid)

        then:
        1 * bundleContext.createFilter(_) >> {
            arguments[0].contains(pid)
            return null
        }
        1 * serviceTracker.getService() >> Mock(ManagedService)
    }

    def 'something went wrong while waiting for service to appear'() {
        setup:
        def pid = 'pid'
        SynchronizedInstallerImpl syncInstaller = new SynchronizedInstallerBuilder()
                .bundleContext(bundleContext)
                .serviceTracker(serviceTracker)
                .build()
        when:
        syncInstaller.waitForServiceToBeAvailable(pid)

        then:
        1 * bundleContext.createFilter(_) >> {
            arguments[0].contains(pid)
            return null
        }
        1 * serviceTracker.getService() >> { new IOException() }
        1 * serviceTracker.close()
    }

    /**
     * Feature install tests
     */
    def 'successfully install all features'() {
        setup:
        def feature = Mock(Feature)
        bundleContext.getBundles() >> { [] }

        SynchronizedInstallerMock syncInstaller = new SynchronizedInstallerBuilder()
                .bundleContext(bundleContext)
                .featuresService(featuresService)
                .build()

        when:
        syncInstaller.installFeatures("feature1", "feature2")

        then:
        2 * featuresService.getFeature(_) >> feature
        2 * featuresService.isInstalled(_) >> false
        2 * feature.getName() >>> ["feature1", "feature2"]
        notThrown(SynchronizedInstallerException)
    }

    def 'successfully return when there are no features to install'() {
        setup:
        bundleContext.getBundles() >> { [] }
        def feature = Mock(Feature)


        SynchronizedInstallerMock syncInstaller = new SynchronizedInstallerBuilder()
                .featuresService(featuresService)
                .bundleContext(bundleContext)
                .build()
        when:
        syncInstaller.installFeatures("feature")


        then:
        1 * featuresService.getFeature("feature") >> feature
        1 * featuresService.isInstalled(feature) >> true

        notThrown(SynchronizedInstallerException)
    }

    def 'successfully return when there are only some features that need to be installed'() {
        setup:
        bundleContext.getBundles() >> { [] }
        def feature1 = Mock(Feature)
        def feature2 = Mock(Feature)

        SynchronizedInstallerMock syncInstaller = new SynchronizedInstallerBuilder()
                .featuresService(featuresService)
                .bundleContext(bundleContext)
                .build()
        when:
        syncInstaller.installFeatures("feature1", "feature2")

        then:
        1 * featuresService.getFeature("feature1") >> feature1
        1 * featuresService.isInstalled(feature1) >> true

        1 * featuresService.getFeature("feature2") >> feature2
        1 * featuresService.isInstalled(feature2) >> false

        notThrown(SynchronizedInstallerException)
    }

    def 'failed to install features'() {
        setup:
        bundleContext.getBundles() >> { [] }
        def feature = Mock(Feature)
        SynchronizedInstallerMock syncInstaller = new SynchronizedInstallerBuilder()
                .featuresService(featuresService)
                .bundleContext(bundleContext)
                .build()
        when:
        syncInstaller.installFeatures("feature")

        then:
        1 * featuresService.getFeature("feature") >> feature
        1 * featuresService.isInstalled(feature) >> false

        1 * featuresService.installFeatures(*_) >> { throw new Exception() }
        thrown(SynchronizedInstallerException)
    }

    def 'failed to retrieve features when installing'() {
        setup:
        SynchronizedInstallerMock syncInstaller = new SynchronizedInstallerBuilder()
                .featuresService(featuresService)
                .bundleContext(bundleContext)
                .build()
        when:
        syncInstaller.installFeatures("feature")

        then:
        1 * featuresService.getFeature("feature") >> { throw new Exception() }
        thrown(SynchronizedInstallerException)
    }

    /**
     * Feature uninstall tests
     */
    def 'successfully uninstall all features'() {
        setup:
        def feature = Mock(Feature)
        bundleContext.getBundles() >> { [] }

        SynchronizedInstallerMock syncInstaller = new SynchronizedInstallerBuilder()
                .bundleContext(bundleContext)
                .featuresService(featuresService)
                .build()

        when:
        syncInstaller.uninstallFeatures("feature1", "feature2")

        then:
        2 * featuresService.getFeature(_) >> feature
        2 * featuresService.isInstalled(_) >> true
        2 * feature.getName() >>> ["feature1", "feature2"]
        notThrown(SynchronizedInstallerException)
    }

    def 'successfully return when there are no features to uninstall'() {
        setup:
        bundleContext.getBundles() >> { [] }
        def feature = Mock(Feature)


        SynchronizedInstallerMock syncInstaller = new SynchronizedInstallerBuilder()
                .featuresService(featuresService)
                .bundleContext(bundleContext)
                .build()
        when:
        syncInstaller.uninstallFeatures("feature")


        then:
        1 * featuresService.getFeature("feature") >> feature
        1 * featuresService.isInstalled(feature) >> false

        notThrown(SynchronizedInstallerException)
    }

    def 'successfully return when there are only some features that need to be uninstalled'() {
        setup:
        bundleContext.getBundles() >> { [] }
        def feature1 = Mock(Feature)
        def feature2 = Mock(Feature)

        SynchronizedInstallerMock syncInstaller = new SynchronizedInstallerBuilder()
                .featuresService(featuresService)
                .bundleContext(bundleContext)
                .build()
        when:
        syncInstaller.uninstallFeatures("feature1", "feature2")

        then:
        1 * featuresService.getFeature("feature1") >> feature1
        1 * featuresService.isInstalled(feature1) >> false

        1 * featuresService.getFeature("feature2") >> feature2
        1 * featuresService.isInstalled(feature2) >> true

        notThrown(SynchronizedInstallerException)
    }

    def 'failed to uninstall features'() {
        setup:
        bundleContext.getBundles() >> { [] }
        def feature = Mock(Feature)
        SynchronizedInstallerMock syncInstaller = new SynchronizedInstallerBuilder()
                .featuresService(featuresService)
                .bundleContext(bundleContext)
                .build()
        when:
        syncInstaller.uninstallFeatures("feature")

        then:
        1 * featuresService.getFeature("feature") >> feature
        1 * featuresService.isInstalled(feature) >> true

        1 * featuresService.uninstallFeatures(*_) >> { throw new Exception() }
        thrown(SynchronizedInstallerException)
    }

    def 'failed to retrieve features when uninstalling'() {
        setup:
        SynchronizedInstallerMock syncInstaller = new SynchronizedInstallerBuilder()
                .featuresService(featuresService)
                .bundleContext(bundleContext)
                .build()
        when:
        syncInstaller.uninstallFeatures("feature")

        then:
        1 * featuresService.getFeature("feature") >> { throw new Exception() }
        thrown(SynchronizedInstallerException)
    }

    /**
     * Wait for bundles tests
     */
    def 'wait for all bundles when none are specified'() {
        setup:
        def bundle1Name = 'bundle1'
        def bundle1 = Mock(Bundle)
        bundle1.getHeaders() >> new Hashtable<String, String>()
        def bundle1Info = Mock(BundleInfo)
        bundle1.getSymbolicName() >> bundle1Name

        def bundle2Name = 'bundle2'
        def bundle2 = Mock(Bundle)
        bundle2.getHeaders() >> new Hashtable<String, String>()
        def bundle2Info = Mock(BundleInfo)
        bundle2.getSymbolicName() >> bundle2Name

        def bundle3Name = 'bundle3'
        def bundle3 = Mock(Bundle)
        bundle3.getHeaders() >> new Hashtable<String, String>()
        def bundle3Info = Mock(BundleInfo)
        bundle3.getSymbolicName() >> bundle3Name

        bundleService.getInfo(bundle1) >> bundle1Info
        bundleService.getInfo(bundle2) >> bundle2Info
        bundleService.getInfo(bundle3) >> bundle3Info

        bundleContext.getBundles() >> [bundle1, bundle2, bundle3]
        SynchronizedInstallerImpl syncInstaller = new SynchronizedInstallerBuilder()
                .bundleService(bundleService)
                .bundleContext(bundleContext)
                .build()
        when:
        syncInstaller.waitForBundles()

        then:
        1 * bundleService.getInfo(bundle1) >> bundle1Info
        1 * bundle1Info.isFragment() >> true
        1 * bundle1Info.getState() >> BundleState.Resolved

        1 * bundleService.getInfo(bundle2) >> bundle2Info
        1 * bundle2Info.isFragment() >> false
        1 * bundle2Info.getState() >> BundleState.Active

        1 * bundleService.getInfo(bundle3) >> bundle3Info
        1 * bundle3Info.isFragment() >> false
        1 * bundle3Info.getState() >> BundleState.Active
    }

    def 'wait for specific bundles successfully'() {
        setup:
        def bundle1Name = 'bundle1'
        def bundle1 = Mock(Bundle)
        bundle1.getHeaders() >> new Hashtable<String, String>()
        def bundle1Info = Mock(BundleInfo)
        bundle1.getSymbolicName() >> bundle1Name

        def bundle2Name = 'bundle2'
        def bundle2 = Mock(Bundle)
        bundle2.getHeaders() >> new Hashtable<String, String>()
        def bundle2Info = Mock(BundleInfo)
        bundle2.getSymbolicName() >> bundle2Name

        def bundle3Name = 'bundle3'
        def bundle3 = Mock(Bundle)
        bundle3.getHeaders() >> new Hashtable<String, String>()
        def bundle3Info = Mock(BundleInfo)
        bundle3.getSymbolicName() >> bundle3Name

        bundleService.getInfo(bundle1) >> bundle1Info
        bundleService.getInfo(bundle2) >> bundle2Info
        bundleService.getInfo(bundle3) >> bundle3Info

        bundleContext.getBundles() >> [bundle1, bundle2, bundle3]
        SynchronizedInstallerImpl syncInstaller = new SynchronizedInstallerBuilder()
                .bundleService(bundleService)
                .bundleContext(bundleContext)
                .build()
        when:
        syncInstaller.waitForBundles(bundle1Name, bundle2Name)

        then:
        1 * bundleService.getInfo(bundle1) >> bundle1Info
        1 * bundle1Info.isFragment() >> true
        1 * bundle1Info.getState() >> BundleState.Resolved

        1 * bundleService.getInfo(bundle2) >> bundle2Info
        1 * bundle2Info.isFragment() >> false
        1 * bundle2Info.getState() >> BundleState.Active

        0 * bundleService.getInfo(bundle3) >> bundle3Info
        0 * bundle3Info.isFragment() >> false
        0 * bundle3Info.getState() >> BundleState.Active
    }

    def 'fail when a bundle is found in a failure state'() {
        setup:
        def bundle1Name = 'bundle1'
        def bundle1 = Mock(Bundle)
        bundle1.getHeaders() >> new Hashtable<String, String>()
        def bundle1Info = Mock(BundleInfo)
        bundle1.getSymbolicName() >> bundle1Name

        bundleContext.getBundles() >> [bundle1]
        SynchronizedInstallerImpl syncInstaller = new SynchronizedInstallerBuilder()
                .bundleService(bundleService)
                .bundleContext(bundleContext)
                .build()
        when:
        syncInstaller.waitForBundles()

        then:
        1 * bundleService.getInfo(bundle1) >> bundle1Info
        1 * bundle1Info.isFragment() >> true
        1 * bundle1Info.getState() >> BundleState.Failure
        thrown(SynchronizedInstallerException)
    }

    /**
     * Bundle start tests
     */
    def 'successfully start bundles'() {
        setup:
        Bundle bundleToStart = Mock(Bundle)
        bundleToStart.getSymbolicName() >> "sampleBundle"

        Bundle bundle2 = Mock(Bundle)
        bundle2.getSymbolicName() >> "sampleBundle2"
        bundleContext.getBundles() >> [bundleToStart, bundle2]

        SynchronizedInstallerMock syncInstaller = new SynchronizedInstallerBuilder()
                .bundleContext(bundleContext)
                .build()

        when:
        syncInstaller.startBundles("sampleBundle")

        then:
        1 * bundleToStart.start()
        0 * bundle2.start()
    }

    def 'failed to start bundle'() {
        setup:
        Bundle mockBundle = Mock(Bundle)
        mockBundle.getSymbolicName() >> "sampleBundle"
        mockBundle.start() >> { throw new BundleException("failure") }
        bundleContext.getBundles() >> [mockBundle]
        SynchronizedInstallerMock syncInstaller = new SynchronizedInstallerBuilder()
                .bundleContext(bundleContext)
                .build()

        when:
        syncInstaller.startBundles("sampleBundle")

        then:
        thrown(SynchronizedInstallerException)
    }

    /**
     * Bundle stop tests
     */
    def 'successfully stop bundles'() {
        setup:
        Bundle bundleToStop = Mock(Bundle)
        bundleToStop.getSymbolicName() >> "sampleBundle"

        Bundle bundle2 = Mock(Bundle)
        bundle2.getSymbolicName() >> "sampleBundle2"

        bundleContext.getBundles() >> [bundleToStop, bundle2]
        SynchronizedInstallerMock syncInstaller = new SynchronizedInstallerBuilder()
                .bundleContext(bundleContext)
                .build()

        when:
        syncInstaller.stopBundles("sampleBundle")

        then:
        1 * bundleToStop.stop()
        0 * bundle2.stop()
    }

    def 'failed to stop bundle'() {
        setup:
        Bundle mockBundle = Mock(Bundle)
        mockBundle.getSymbolicName() >> "sampleBundle"
        mockBundle.stop() >> { throw new BundleException("failure") }
        bundleContext.getBundles() >> [mockBundle]
        SynchronizedInstallerMock syncInstaller = new SynchronizedInstallerBuilder()
                .bundleContext(bundleContext)
                .build()

        when:
        syncInstaller.stopBundles("sampleBundle")

        then:
        thrown(SynchronizedInstallerException)
    }

    class SynchronizedInstallerMock extends SynchronizedInstallerImpl {

        private ServiceTracker serviceTracker
        private SynchronizedInstallerImpl.SynchronizedConfigurationListener configListener

        SynchronizedInstallerMock(BundleContext bundleContext, ConfigurationAdmin configAdmin, FeaturesService featuresService, BundleService bundleService, ServiceTracker serviceTracker, SynchronizedInstallerImpl.SynchronizedConfigurationListener configListener) {
            super(bundleContext, configAdmin, featuresService, bundleService)
            this.serviceTracker = serviceTracker
            this.configListener = configListener
        }

        @Override
        protected void wait(Callable<Boolean> conditionIsMet, long maxWait, long pollInterval, String onFailureMessage) throws SynchronizedInstallerException, InterruptedException {
            conditionIsMet.call()
        }

        @Override
        ServiceTracker getServiceTracker(BundleContext bundleContext, Filter filter) {
            return serviceTracker != null ? serviceTracker : super.getServiceTracker(bundleContext, filter)
        }

        @Override
        protected SynchronizedInstallerImpl.SynchronizedConfigurationListener getConfigListener(String pid) {
            return configListener != null ? listener : super.getConfigListener(pid)
        }
    }

    class SynchronizedInstallerBuilder {
        private BundleContext bundleContext
        private ConfigurationAdmin configAdmin
        private FeaturesService featuresService
        private BundleService bundleService
        private ServiceTracker serviceTracker
        private SynchronizedInstallerImpl.SynchronizedConfigurationListener configListener

        SynchronizedInstallerBuilder bundleContext(BundleContext bundleContext) {
            this.bundleContext = bundleContext
            return this
        }

        SynchronizedInstallerBuilder configAdmin(ConfigurationAdmin configAdmin) {
            this.configAdmin = configAdmin
            return this
        }

        SynchronizedInstallerBuilder featuresService(FeaturesService featuresService) {
            this.featuresService = featuresService
            return this
        }

        SynchronizedInstallerBuilder bundleService(BundleService bundleService) {
            this.bundleService = bundleService
            return this
        }

        SynchronizedInstallerBuilder serviceTracker(ServiceTracker serviceTracker) {
            this.serviceTracker = serviceTracker
            return this
        }

        SynchronizedInstallerBuilder configListener(SynchronizedInstallerImpl.SynchronizedConfigurationListener configListener) {
            this.configListener = configListener
            return this
        }

        SynchronizedInstallerMock build() {
            return new SynchronizedInstallerMock(bundleContext, configAdmin, featuresService, bundleService, serviceTracker, configListener)
        }
    }
}
