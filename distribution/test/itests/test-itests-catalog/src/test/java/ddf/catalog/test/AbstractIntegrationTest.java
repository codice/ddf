/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package ddf.catalog.test;

import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.useOwnExamBundlesStartLevel;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.karaf.features.FeaturesService;
import org.junit.Rule;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.MetaTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.FederatedSource;
import ddf.common.test.PaxExamRule;
import ddf.common.test.PostTestConstruct;

/**
 * Abstract integration test with helper methods and configuration at the container level.
 */
public abstract class AbstractIntegrationTest {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    protected static final String LOG_CONFIG_PID = "org.ops4j.pax.logging";

    protected static final String LOGGER_PREFIX = "log4j.logger.";

    protected static final String KARAF_VERSION = "2.4.3";

    protected static final int ONE_MINUTE_MILLIS = 60000;

    protected static final int FIVE_MINUTES_MILLIS = ONE_MINUTE_MILLIS * 5;

    // TODO: Use the Camel AvailablePortFinder.getNextAvailable() test method
    protected static final String HTTP_PORT = "9081";

    protected static final String HTTPS_PORT = "9993";

    protected static final String SSH_PORT = "9101";

    protected static final String RMI_SERVER_PORT = "44445";

    protected static final String RMI_REG_PORT = "1100";

    protected static final String SERVICE_ROOT = "https://localhost:" + HTTPS_PORT + "/services";

    protected static final String INSECURE_SERVICE_ROOT =
            "http://localhost:" + HTTP_PORT + "/services";

    protected static final String REST_PATH = SERVICE_ROOT + "/catalog/";

    protected static final String OPENSEARCH_PATH = REST_PATH + "query";

    protected static final String CSW_PATH = SERVICE_ROOT + "/csw";

    protected static final String OPENSEARCH_SOURCE_ID = "openSearchSource";

    protected static final String CSW_SOURCE_ID = "cswSource";

    protected static final String DEFAULT_LOG_LEVEL = "TRACE";

    protected String logLevel = "";

    public static final String TEST_LOGLEVEL_PROPERTY = "org.codice.test.defaultLoglevel";

    @Rule
    public PaxExamRule paxExamRule = new PaxExamRule(this);

    @Inject
    protected BundleContext bundleCtx;

    @Inject
    protected ConfigurationAdmin configAdmin;

    @Inject
    protected FeaturesService features;

    @Inject
    protected MetaTypeService metatype;

    private IntegrationTestHelper itHelper;

    static {
        // Make Pax URL use the maven.repo.local setting if present
        if (System.getProperty("maven.repo.local") != null) {
            System.setProperty("org.ops4j.pax.url.mvn.localRepository",
                    System.getProperty("maven.repo.local"));
        }
    }

    @PostTestConstruct
    public void initHelper() {
        itHelper = new IntegrationTestHelper(bundleCtx, configAdmin, metatype);
    }

    /**
     * Configures the pax exam test container
     *
     * @return list of pax exam options
     */
    @org.ops4j.pax.exam.Configuration
    public Option[] config() throws URISyntaxException {
        return combineOptions(configureCustom(), configureDistribution(), configurePaxExam(),
                configureAdditionalBundles(), configureConfigurationPorts(), configureMavenRepos(),
                configureSystemSettings(), configureVmOptions(), configureStartScript());
    }

    public IntegrationTestHelper getItHelper() {
        return itHelper;
    }

    private Option[] combineOptions(Option[]... options) {
        List<Option> optionList = new ArrayList<>();
        for (Option[] array : options) {
            if (array != null && array.length > 0) {
                optionList.addAll(Arrays.asList(array));
            }
        }
        Option[] finalArray = new Option[optionList.size()];
        optionList.toArray(finalArray);
        return finalArray;
    }

    protected Option[] configureDistribution() {
        return options(karafDistributionConfiguration(
                maven().groupId("ddf.distribution").artifactId("ddf").type("zip")
                        .versionAsInProject().getURL(), "ddf", KARAF_VERSION)
                .unpackDirectory(new File("target/exam")).useDeployFolder(false));

    }

    protected Option[] configurePaxExam() {
        return options(logLevel(LogLevelOption.LogLevel.INFO), useOwnExamBundlesStartLevel(100),
                // increase timeout for TravisCI
                systemTimeout(TimeUnit.MINUTES.toMillis(10)),
                when(Boolean.getBoolean("keepRuntimeFolder")).useOptions(keepRuntimeFolder()));
    }

    protected Option[] configureAdditionalBundles() {
        return options(junitBundles(),
                // HACK: incorrect version exported to override hamcrest-core from exam
                // feature which causes a split package issue for rest-assured
                wrappedBundle(mavenBundle("org.hamcrest", "hamcrest-all").versionAsInProject())
                        .exports("*;version=1.3.0.10"), wrappedBundle(
                        mavenBundle("org.apache.karaf.itests", "itests").classifier("tests")
                                .versionAsInProject()), wrappedBundle(
                        mavenBundle("ddf.test.itests", "test-itests-common").classifier("tests")
                                .versionAsInProject()).bundleSymbolicName("test-itests-common"),
                mavenBundle("ddf.test.thirdparty", "rest-assured").versionAsInProject());
    }

    protected Option[] configureConfigurationPorts() throws URISyntaxException {
        return options(editConfigurationFilePut("etc/system.properties", "urlScheme", "https"),
                editConfigurationFilePut("etc/system.properties", "host", "localhost"),
                editConfigurationFilePut("etc/system.properties", "jetty.port", HTTPS_PORT),
                editConfigurationFilePut("etc/system.properties", "hostContext", "/solr"),
                editConfigurationFilePut("etc/system.properties", "ddf.home", "${karaf.home}"),
                editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", SSH_PORT),
                editConfigurationFilePut("etc/ddf.platform.config.cfg", "port", HTTPS_PORT),
                editConfigurationFilePut("etc/ddf.platform.config.cfg", "host", "localhost"),
                editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port",
                        HTTP_PORT), editConfigurationFilePut("etc/org.ops4j.pax.web.cfg",
                        "org.osgi.service.http.port.secure", HTTPS_PORT),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort",
                        RMI_REG_PORT),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort",
                        RMI_SERVER_PORT), replaceConfigurationFile("etc/hazelcast.xml",
                        new File(this.getClass().getResource("/hazelcast.xml").toURI())),
                replaceConfigurationFile("etc/ddf.security.sts.client.configuration.cfg", new File(
                        this.getClass().getResource("/ddf.security.sts.client.configuration.cfg")
                                .toURI())), replaceConfigurationFile(
                        "etc/ddf.catalog.solr.external.SolrHttpCatalogProvider.cfg", new File(
                                this.getClass().getResource(
                                        "/ddf.catalog.solr.external.SolrHttpCatalogProvider.cfg")
                                        .toURI())));
    }

    protected Option[] configureMavenRepos() {
        return options(editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg",
                        "org.ops4j.pax.url.mvn.repositories",
                        "http://repo1.maven.org/maven2@id=central,"
                                + "http://oss.sonatype.org/content/repositories/snapshots@snapshots@noreleases@id=sonatype-snapshot,"
                                + "http://oss.sonatype.org/content/repositories/ops4j-snapshots@snapshots@noreleases@id=ops4j-snapshot,"
                                + "http://repository.apache.org/content/groups/snapshots-group@snapshots@noreleases@id=apache,"
                                + "http://svn.apache.org/repos/asf/servicemix/m2-repo@id=servicemix,"
                                + "http://repository.springsource.com/maven/bundles/release@id=springsource,"
                                + "http://repository.springsource.com/maven/bundles/external@id=springsourceext,"
                                + "http://oss.sonatype.org/content/repositories/releases/@id=sonatype"),
                when(System.getProperty("maven.repo.local") != null).useOptions(
                        editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg",
                                "org.ops4j.pax.url.mvn.localRepository",
                                System.getProperty("maven.repo.local"))));
    }

    protected Option[] configureSystemSettings() {
        return options(when(System.getProperty(TEST_LOGLEVEL_PROPERTY) != null).useOptions(
                        systemProperty(TEST_LOGLEVEL_PROPERTY)
                                .value(System.getProperty(TEST_LOGLEVEL_PROPERTY, ""))),
                when(Boolean.getBoolean("isDebugEnabled")).useOptions(
                        vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005")),
                when(System.getProperty("maven.repo.local") != null).useOptions(
                        systemProperty("org.ops4j.pax.url.mvn.localRepository")
                                .value(System.getProperty("maven.repo.local", ""))));
    }

    protected Option[] configureVmOptions() {
        return options(vmOption("-Xmx2048M"), vmOption("-XX:PermSize=128M"),
                vmOption("-XX:MaxPermSize=512M"),
                // avoid integration tests stealing focus on OS X
                vmOption("-Djava.awt.headless=true"));
    }

    protected Option[] configureStartScript() {
        return options(
                editConfigurationFileExtend("etc/org.apache.karaf.features.cfg", "featuresBoot",
                        "security-services-app,catalog-app,solr-app,spatial-app,sdk-app"),
                editConfigurationFileExtend("etc/org.apache.karaf.features.cfg",
                        "featuresRepositories",
                        "mvn:ddf.distribution/sdk-app/2.8.0-SNAPSHOT/xml/features"));
    }

    /**
     * Allows extending classes to add any custom options to the configuration.
     */
    protected Option[] configureCustom() {
        return null;
    }

    /**
     * @param path
     * @throws InterruptedException
     * @deprecated since 2.8.0 see {@link IntegrationTestHelper#waitForHttpEndpoint(String)}
     */
    @Deprecated
    protected void waitForHttpEndpoint(String path) throws InterruptedException {
        itHelper.waitForHttpEndpoint(path);
    }

    /**
     * @param wait
     * @param featureNames
     * @throws Exception
     * @deprecated since 2.8.0 see {@link IntegrationTestHelper#stopFeature(boolean, String...)}
     */
    @Deprecated
    protected void stopFeature(boolean wait, String... featureNames) throws Exception {
        itHelper.stopFeature(wait, featureNames);
    }

    /**
     * @throws IOException
     * @deprecated since 2.8.0 see {@link IntegrationTestHelper#setLogLevels()}
     */
    @Deprecated
    protected void setLogLevels() throws IOException {
        itHelper.setLogLevels();
    }

    /**
     * @param wait
     * @param featureNames
     * @throws Exception
     * @deprecated since 2.8.0 see {@link IntegrationTestHelper#startFeature(boolean, String...)}
     */
    @Deprecated
    protected void startFeature(boolean wait, String... featureNames) throws Exception {
        itHelper.startFeature(wait, featureNames);
    }

    /**
     * @param factoryPid
     * @param properties
     * @throws IOException
     * @deprecated since 2.8.0 see {@link IntegrationTestHelper#createManagedService(String, Map)}
     */
    @Deprecated
    protected void createManagedService(String factoryPid, Map<String, Object> properties)
            throws IOException {
        itHelper.createManagedService(factoryPid, properties);
    }

    /**
     * @param fanoutEnabled
     * @throws IOException
     * @deprecated since 2.8.0 see {@link IntegrationTestHelper#setFanout(boolean)}
     */
    @Deprecated
    protected void setFanout(boolean fanoutEnabled) throws IOException {
        itHelper.setFanout(fanoutEnabled);
    }

    /**
     * @param symbolicNamePrefix
     * @throws InterruptedException
     * @deprecated since 2.8.0 see {@link IntegrationTestHelper#waitForRequiredBundles(String)}
     */
    @Deprecated
    protected void waitForRequiredBundles(String symbolicNamePrefix) throws InterruptedException {
        itHelper.waitForRequiredBundles(symbolicNamePrefix);
    }

    /**
     * @param sources
     * @throws InterruptedException
     * @deprecated since 2.8.0 see {@link IntegrationTestHelper#waitForSourcesToBeAvailable(String...)}
     */
    @Deprecated
    protected void waitForSourcesToBeAvailable(String... sources) throws InterruptedException {
        itHelper.waitForSourcesToBeAvailable(sources);
    }

    /**
     * @param id
     * @return
     * @throws InterruptedException
     * @throws InvalidSyntaxException
     * @deprecated since 2.8.0 see {@link IntegrationTestHelper#waitForFederatedSource(String)}
     */
    @Deprecated
    protected FederatedSource waitForFederatedSource(String id)
            throws InterruptedException, InvalidSyntaxException {
        return itHelper.waitForFederatedSource(id);
    }

    /**
     * @return
     * @throws InterruptedException
     * @deprecated since 2.8.0 see {@link IntegrationTestHelper#waitForCatalogProvider()}
     */
    @Deprecated
    protected CatalogProvider waitForCatalogProvider() throws InterruptedException {
        return itHelper.waitForCatalogProvider();
    }

    /**
     * @return
     * @throws InterruptedException
     * @deprecated since 2.8.0 see {@link IntegrationTestHelper#getCatalogFramework()}
     */
    @Deprecated
    protected CatalogFramework getCatalogFramework() throws InterruptedException {
        return itHelper.getCatalogFramework();
    }

    /**
     * @param symbolicName
     * @param factoryPid
     * @return
     * @deprecated since 2.8.0 see {@link IntegrationTestHelper#getMetatypeDefaults(String, String)}
     */
    @Deprecated
    protected Map<String, Object> getMetatypeDefaults(String symbolicName, String factoryPid) {
        return itHelper.getMetatypeDefaults(symbolicName, factoryPid);
    }

    /**
     * @param servicePid
     * @param properties
     * @throws IOException
     * @deprecated since 2.8.0 see {@link IntegrationTestHelper#startManagedService(String, Map)}
     */
    @Deprecated
    protected void startManagedService(String servicePid, Map<String, Object> properties)
            throws IOException {
        itHelper.startManagedService(servicePid, properties);
    }

    /**
     * @throws InterruptedException
     * @deprecated since 2.8.0 see {@link IntegrationTestHelper#waitForAllBundles()}
     */
    @Deprecated
    protected void waitForAllBundles() throws InterruptedException {
        itHelper.waitForAllBundles();
    }

    public class OpenSearchSourceProperties extends HashMap<String, Object> {

        public static final String SYMBOLIC_NAME = "catalog-opensearch-source";

        public static final String FACTORY_PID = "OpenSearchSource";

        public OpenSearchSourceProperties(String sourceId) {
            this.putAll(getMetatypeDefaults(SYMBOLIC_NAME, FACTORY_PID));

            this.put("shortname", sourceId);
            this.put("endpointUrl", OPENSEARCH_PATH);
        }

    }

    public class CswSourceProperties extends HashMap<String, Object> {

        public static final String SYMBOLIC_NAME = "spatial-csw-source";

        public static final String FACTORY_PID = "Csw_Federated_Source";

        public CswSourceProperties(String sourceId) {
            this.putAll(getMetatypeDefaults(SYMBOLIC_NAME, FACTORY_PID));

            this.put("id", sourceId);
            this.put("cswUrl", CSW_PATH);
            this.put("pollInterval", 1);
        }

    }

    public class CswConnectedSourceProperties extends HashMap<String, Object> {

        public static final String SYMBOLIC_NAME = "spatial-csw-source";

        public static final String FACTORY_PID = "Csw_Connected_Source";

        public CswConnectedSourceProperties(String sourceId) {
            this.putAll(getMetatypeDefaults(SYMBOLIC_NAME, FACTORY_PID));

            this.put("id", sourceId);
            this.put("cswUrl", CSW_PATH);
            this.put("pollInterval", 1);
        }

    }
}
