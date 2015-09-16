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
import ddf.common.test.AdminConfig;
import ddf.common.test.PaxExamRule;
import ddf.common.test.PostTestConstruct;
import ddf.common.test.ServiceManager;

/**
 * Abstract integration test with helper methods and configuration at the container level
 */
public abstract class AbstractIntegrationTest {

    public static final String TEST_LOGLEVEL_PROPERTY = "org.codice.test.defaultLoglevel";

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    protected static final String LOG_CONFIG_PID = AdminConfig.LOG_CONFIG_PID;

    protected static final String LOGGER_PREFIX = AdminConfig.LOGGER_PREFIX;

    protected static final String DEFAULT_LOG_LEVEL = AdminConfig.DEFAULT_LOG_LEVEL;

    protected static final String KARAF_VERSION = "2.4.3";

    // TODO: Use the Camel AvailablePortFinder.getNextAvailable() test method
    protected static final String HTTP_PORT = "9081";

    protected static final String HTTPS_PORT = "9993";

    protected static final String SSH_PORT = "9101";

    protected static final String RMI_SERVER_PORT = "44445";

    protected static final String RMI_REG_PORT = "1100";

    protected static final String SECURE_ROOT = "https://localhost:";

    protected static final String SERVICE_ROOT = SECURE_ROOT + HTTPS_PORT + "/services";

    protected static final String INSECURE_ROOT = "http://localhost:";

    protected static final String INSECURE_SERVICE_ROOT = INSECURE_ROOT + HTTP_PORT + "/services";

    protected static final String REST_PATH = SERVICE_ROOT + "/catalog/";

    protected static final String OPENSEARCH_PATH = REST_PATH + "query";

    protected static final String CSW_PATH = SERVICE_ROOT + "/csw";

    protected static final String OPENSEARCH_SOURCE_ID = "openSearchSource";

    protected static final String CSW_SOURCE_ID = "cswSource";

    static {
        // Make Pax URL use the maven.repo.local setting if present
        if (System.getProperty("maven.repo.local") != null) {
            System.setProperty("org.ops4j.pax.url.mvn.localRepository",
                    System.getProperty("maven.repo.local"));
        }
    }

    @Rule
    public PaxExamRule paxExamRule = new PaxExamRule(this);

    protected String logLevel = "";

    @Inject
    protected BundleContext bundleCtx;

    @Inject
    protected ConfigurationAdmin configAdmin;

    @Inject
    protected FeaturesService features;

    @Inject
    protected MetaTypeService metatype;

    private AdminConfig adminConfig;

    private ServiceManager serviceManager;

    private SecurityPolicyConfigurator securityPolicy;

    private CatalogBundle catalogBundle;

    private UrlResourceReaderConfigurator urlResourceReaderConfigurator;

    @PostTestConstruct
    public void initFacades() {
        adminConfig = new AdminConfig(configAdmin);
        serviceManager = new ServiceManager(bundleCtx, metatype, adminConfig);
        catalogBundle = new CatalogBundle(serviceManager, adminConfig);
        securityPolicy = new SecurityPolicyConfigurator(serviceManager, configAdmin);
        urlResourceReaderConfigurator = new UrlResourceReaderConfigurator(configAdmin);
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

    protected AdminConfig getAdminConfig() {
        return adminConfig;
    }

    protected ServiceManager getServiceManager() {
        return serviceManager;
    }

    protected CatalogBundle getCatalogBundle() {
        return catalogBundle;
    }

    protected SecurityPolicyConfigurator getSecurityPolicy() {
        return securityPolicy;
    }

    protected UrlResourceReaderConfigurator getUrlResourceReaderConfigurator() {
        return urlResourceReaderConfigurator;
    }

    /**
     * Combines all the {@link Option} objects contained in multiple {@link Option} arrays.
     *
     * @param options arrays of {@link Option} objects to combine. Arrays can be {@code null} or
     *                empty.
     * @return array that combines all the {@code Option} objects from the arrays provided.
     * {@code null} and empty arrays will be ignored, but {@code null} {@link Option} objects will
     * be added to the result.
     */
    protected Option[] combineOptions(Option[]... options) {
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
                maven().groupId("org.codice.ddf").artifactId("ddf").type("zip").versionAsInProject()
                        .getURL(), "ddf", KARAF_VERSION).unpackDirectory(new File("target/exam"))
                .useDeployFolder(false));

    }

    protected Option[] configurePaxExam() {
        return options(logLevel(LogLevelOption.LogLevel.INFO), useOwnExamBundlesStartLevel(100),
                // increase timeout for CI environment
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
                wrappedBundle(
                        mavenBundle("ddf.security", "ddf-security-common").versionAsInProject())
                        .bundleSymbolicName("test-security-common"),
                mavenBundle("ddf.test.thirdparty", "rest-assured").versionAsInProject(),
                wrappedBundle(mavenBundle("com.google.guava", "guava").versionAsInProject())
                        .exports("*;version=18.0"));
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
     * @deprecated since 2.8.0. Use {@link ServiceManager#waitForHttpEndpoint(String)
     * getServiceManager().waitForHttpEndpoint()} instead.
     */
    @Deprecated
    protected void waitForHttpEndpoint(String path) throws InterruptedException {
        serviceManager.waitForHttpEndpoint(path);
    }

    /**
     * @deprecated since 2.8.0. Use {@link ServiceManager#stopFeature(boolean, String...)
     * getServiceManager().stopFeature()} instead.
     */
    @Deprecated
    protected void stopFeature(boolean wait, String... featureNames) throws Exception {
        serviceManager.stopFeature(wait, featureNames);
    }

    /**
     * @deprecated since 2.8.0. Use {@link AdminConfig#setLogLevels()
     * getAdminConfig().setLogLevels()} instead.
     */
    @Deprecated
    protected void setLogLevels() throws IOException {
        adminConfig.setLogLevels();
    }

    /**
     * @deprecated since 2.8.0. Use {@link ServiceManager#startFeature(boolean, String...)
     * getServiceManager().startFeature()} instead.
     */
    @Deprecated
    protected void startFeature(boolean wait, String... featureNames) throws Exception {
        serviceManager.startFeature(wait, featureNames);
    }

    /**
     * @deprecated since 2.8.0. Use {@link ServiceManager#createManagedService(String, Map)
     * getServiceManager.createManagedService()} instead.
     */
    @Deprecated
    protected void createManagedService(String factoryPid, Map<String, Object> properties)
            throws IOException {
        serviceManager.createManagedService(factoryPid, properties);
    }

    /**
     * @deprecated since 2.8.0. Use {@link CatalogBundle#setFanout(boolean)
     * getCatalogBundle().setFanout()} instead.
     */
    @Deprecated
    protected void setFanout(boolean fanoutEnabled) throws IOException {
        catalogBundle.setFanout(fanoutEnabled);
    }

    /**
     * @deprecated since 2.8.0. Use {@link ServiceManager#waitForRequiredBundles(String)
     * getServiceManager.waitForRequiredBundles()} instead.
     */
    @Deprecated
    protected void waitForRequiredBundles(String symbolicNamePrefix) throws InterruptedException {
        serviceManager.waitForRequiredBundles(symbolicNamePrefix);
    }

    /**
     * @deprecated since 2.8.0. Use {@link ServiceManager#waitForSourcesToBeAvailable(String, String...)
     * getServiceManager.waitForSourcesToBeAvailable()} instead.
     */
    @Deprecated
    protected void waitForSourcesToBeAvailable(String... sources) throws InterruptedException {
        serviceManager.waitForSourcesToBeAvailable(REST_PATH, sources);
    }

    /**
     * @deprecated since 2.8.0. Use {@link CatalogBundle#waitForFederatedSource(String)
     * getCatalogBundle.waitForFederatedSource()} instead.
     */
    @Deprecated
    protected FederatedSource waitForFederatedSource(String id)
            throws InterruptedException, InvalidSyntaxException {
        return catalogBundle.waitForFederatedSource(id);
    }

    /**
     * @deprecated since 2.8.0. Use {@link CatalogBundle#waitForCatalogProvider()
     * getCatalogBundle.waitForCatalogProvider()} instead.
     */
    @Deprecated
    protected CatalogProvider waitForCatalogProvider() throws InterruptedException {
        return catalogBundle.waitForCatalogProvider();
    }

    /**
     * @deprecated since 2.8.0. Use {@link CatalogBundle#getCatalogFramework()
     * getCatalogBundle.getCatalogFramework()} instead.
     */
    @Deprecated
    protected CatalogFramework getCatalogFramework() throws InterruptedException {
        return catalogBundle.getCatalogFramework();
    }

    /**
     * @deprecated since 2.8.0. Use {@link ServiceManager#getMetatypeDefaults(String, String)
     * getServiceManager.getMetatypeDefaults()} instead.
     */
    @Deprecated
    protected Map<String, Object> getMetatypeDefaults(String symbolicName, String factoryPid) {
        return serviceManager.getMetatypeDefaults(symbolicName, factoryPid);
    }

    /**
     * @deprecated since 2.8.0. Use {@link ServiceManager#startManagedService(String, Map)
     * getServiceManager.startManagedService()} instead.
     */
    @Deprecated
    protected void startManagedService(String servicePid, Map<String, Object> properties)
            throws IOException {
        serviceManager.startManagedService(servicePid, properties);
    }

    /**
     * @deprecated since 2.8.0. Use {@link ServiceManager#waitForAllBundles()
     * getServiceManager.waitForAllBundles()} instead.
     */
    @Deprecated
    protected void waitForAllBundles() throws InterruptedException {
        serviceManager.waitForAllBundles();
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
