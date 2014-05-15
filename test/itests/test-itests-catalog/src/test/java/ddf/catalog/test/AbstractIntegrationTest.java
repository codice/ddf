/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.test;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.logLevel;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.replaceConfigurationFile;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.osgi.BlueprintListener;
import org.apache.karaf.shell.osgi.BlueprintListener.BlueprintState;
import org.apache.karaf.tooling.exam.options.KarafDistributionKitConfigurationOption;
import org.apache.karaf.tooling.exam.options.KarafDistributionKitConfigurationOption.Platform;
import org.apache.karaf.tooling.exam.options.LogLevelOption.LogLevel;
import org.apache.log4j.Logger;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.util.tracker.ServiceTracker;

import ddf.catalog.source.CatalogProvider;

/**
 * Abstract integration test with helper methods and configuration at the container level.
 * 
 * @author Ashraf Barakat
 * @author Phillip Klinefelter
 * @author ddf.isgs@lmco.com
 * 
 */
public abstract class AbstractIntegrationTest {

    private static final int CONFIG_UPDATE_WAIT_INTERVAL = 5;

    protected static final Logger LOGGER = Logger.getLogger(AbstractIntegrationTest.class);

    protected static final String LOG_CONFIG_PID = "org.ops4j.pax.logging";

    protected static final String LOGGER_PREFIX = "log4j.logger.";

    private static final String KARAF_VERSION = "2.3.3";

    protected static final int ONE_MINUTE_MILLIS = 60000;

    protected static final int FIVE_MINUTES_MILLIS = ONE_MINUTE_MILLIS * 5;

    // TODO: Use the Camel AvailablePortFinder.getNextAvailable() test method
    protected static final String HTTP_PORT = "9081";

    protected static final String HTTPS_PORT = "9993";

    protected static final String SSH_PORT = "9101";

    protected static final String RMI_SERVER_PORT = "44445";

    protected static final String RMI_REG_PORT = "1100";

    @Inject
    protected BundleContext bundleCtx;

    private BlueprintListener blueprintListener;

    @Inject
    protected ConfigurationAdmin configAdmin;

    @Inject
    protected FeaturesService features;

    // Fields used across all test methods must be static.
    // PAX-EXAM wipes away field information before each test method.
    protected static CatalogProvider catalogProvider;

    static {
        // Make Pax URL use the maven.repo.local setting if present
        if (System.getProperty("maven.repo.local") != null) {
            System.setProperty("org.ops4j.pax.url.mvn.localRepository",
                    System.getProperty("maven.repo.local"));
        }
    }

    /**
     * Configures the pax exam test container
     * 
     * @return list of pax exam options
     */
    @org.ops4j.pax.exam.junit.Configuration
    public Option[] config() {
        // @formatter:off
        return options(
                getPlatformOption(Platform.WINDOWS),
                getPlatformOption(Platform.NIX),
                logLevel(LogLevel.INFO),
                // KarafDistributionOption.keepRuntimeFolder(),
                mavenBundle("junit", "junit", "4.10"),
                mavenBundle("ddf.test.thirdparty", "hamcrest-all")
                        .versionAsInProject(),
                mavenBundle("ddf.test.thirdparty", "rest-assured")
                        .versionAsInProject(),
                editConfigurationFilePut("etc/org.apache.karaf.shell.cfg",
                        "sshPort", SSH_PORT),
                editConfigurationFilePut("etc/org.ops4j.pax.web.cfg",
                        "org.osgi.service.http.port", HTTP_PORT),
                editConfigurationFilePut("etc/org.ops4j.pax.web.cfg",
                        "org.osgi.service.http.port.secure", HTTPS_PORT),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg",
                        "rmiRegistryPort", RMI_REG_PORT),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg",
                        "rmiServerPort", RMI_SERVER_PORT),
                replaceConfigurationFile("etc/hazelcast.xml", new File(
                        "src/test/resources/hazelcast.xml")));
        // @formatter:on
    }

    protected KarafDistributionKitConfigurationOption getPlatformOption(Platform platform) {
        String ddfScript = "bin/ddf";
        String adminScript = "bin/admin";

        if (platform.equals(Platform.WINDOWS)) {
            ddfScript = FilenameUtils.separatorsToWindows(ddfScript) + ".bat";
            adminScript = FilenameUtils.separatorsToWindows(adminScript) + ".bat";
        }

        MavenUrlReference ddf = maven().groupId("ddf.distribution").artifactId("ddf").type("zip")
                .versionAsInProject();
        KarafDistributionKitConfigurationOption platformOption = new KarafDistributionKitConfigurationOption(
                ddf, "ddf", KARAF_VERSION, platform).executable(ddfScript).filesToMakeExecutable(
                adminScript);
        platformOption.unpackDirectory(new File("target/exam"));

        return platformOption;
    }

    /**
     * Creates a Managed Service that is created from a Managed Service Factory. Waits for the
     * asynchronous call that the properties have been updated and the service can be used.
     * 
     * @param factoryPid
     *            the factory pid of the Managed Service Factory
     * @param properties
     *            the service properties for the Managed Service
     * @throws IOException
     *             if access to persistent storage fails
     * @throws InterruptedException
     */
    public void createManagedService(String factoryPid, Dictionary<String, Object> properties,
            long timeout) throws IOException, InterruptedException {

        final Configuration sourceConfig = configAdmin.createFactoryConfiguration(factoryPid, null);

        ServiceConfigurationListener listener = new ServiceConfigurationListener(
                sourceConfig.getPid());

        bundleCtx.registerService(ConfigurationListener.class.getName(), listener, null);

        sourceConfig.update(properties);

        long millis = 0;
        while (!listener.isUpdated() && millis < timeout) {
            try {
                Thread.sleep(CONFIG_UPDATE_WAIT_INTERVAL);
                millis += CONFIG_UPDATE_WAIT_INTERVAL;
            } catch (InterruptedException e) {
                LOGGER.info(e);
            }
            LOGGER.info("Waiting for configuration to be updated..." + millis + "ms");
        }

        if (!listener.isUpdated()) {
            throw new RuntimeException(
                    "Service was not updated before timeout ["
                            + timeout
                            + "]. Increase the timeout or found why the service was not updated or created.");
        }

    }

    protected void setLogLevels() throws IOException {
        Configuration logConfig = configAdmin.getConfiguration(LOG_CONFIG_PID, null);
        Dictionary<String, Object> properties = logConfig.getProperties();
        properties.put(LOGGER_PREFIX + "ddf", "TRACE");
        properties.put(LOGGER_PREFIX + "org.codice", "TRACE");
        logConfig.update(properties);
    }

    protected void waitForRequiredBundles(String symbolicNamePrefix) throws InterruptedException {
        boolean ready = false;
        if (blueprintListener == null) {
            blueprintListener = new BlueprintListener();
            bundleCtx.registerService("org.osgi.service.blueprint.container.BlueprintListener",
                    blueprintListener, null);
        }

        long timeoutLimit = System.currentTimeMillis() + FIVE_MINUTES_MILLIS;
        while (!ready) {
            List<Bundle> bundles = Arrays.asList(bundleCtx.getBundles());

            ready = true;
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName().startsWith(symbolicNamePrefix)) {
                    String bundleName = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME);
                    String blueprintState = blueprintListener.getState(bundle);
                    if (blueprintState != null) {
                        if (BlueprintState.Failure.toString().equals(blueprintState)) {
                            fail("The blueprint for " + bundleName + " failed.");
                        } else if (!BlueprintState.Created.toString().equals(blueprintState)) {
                            LOGGER.info(bundleName + " blueprint not ready with state "
                                    + blueprintState);
                            ready = false;
                        }
                    }

                    if (!((bundle.getHeaders().get("Fragment-Host") != null && bundle.getState() == Bundle.RESOLVED) || bundle
                            .getState() == Bundle.ACTIVE)) {
                        LOGGER.info(bundleName + " bundle not ready yet");
                        ready = false;
                    }
                }
            }

            if (!ready) {
                if (System.currentTimeMillis() > timeoutLimit) {
                    fail("Bundles and blueprint did not start in time.");
                }
                LOGGER.info("Bundles not up, sleeping...");
                Thread.sleep(1000);
            }
        }
    }

    protected CatalogProvider waitForCatalogProviderToBeAvailable() throws InterruptedException {
        ServiceTracker st = new ServiceTracker(bundleCtx, CatalogProvider.class.getName(), null);
        st.open();

        CatalogProvider provider = (CatalogProvider) st.waitForService(5000);

        boolean ready = false;
        long timeoutLimit = System.currentTimeMillis() + ONE_MINUTE_MILLIS;
        while (!ready) {
            ready = provider.isAvailable();
            if (!ready) {
                if (System.currentTimeMillis() > timeoutLimit) {
                    fail("Catalog provider timed out.");
                }
                Thread.sleep(100);
            }
        }

        return provider;

    }

    private class ServiceConfigurationListener implements ConfigurationListener {

        private boolean updated = false;

        private String pid;

        public ServiceConfigurationListener(String pid) {
            this.pid = pid;
        }

        @Override
        public void configurationEvent(ConfigurationEvent event) {
            LOGGER.info("Configuration event received: {}", event);
            if (event.getPid().equals(pid) && event.CM_UPDATED == event.getType()) {
                updated = true;
            }
        }

        public boolean isUpdated() {
            return updated;
        }
    };

}