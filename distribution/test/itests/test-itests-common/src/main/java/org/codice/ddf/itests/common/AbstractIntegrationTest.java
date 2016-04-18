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
 */
package org.codice.ddf.itests.common;

import static org.codice.ddf.itests.common.AbstractIntegrationTest.DynamicUrl.INSECURE_ROOT;
import static org.codice.ddf.itests.common.AbstractIntegrationTest.DynamicUrl.SECURE_ROOT;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.useOwnExamBundlesStartLevel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.codice.ddf.itests.common.annotations.PaxExamRule;
import org.codice.ddf.itests.common.annotations.PostTestConstruct;
import org.codice.ddf.itests.common.annotations.SkipUnstableTest;
import org.codice.ddf.itests.common.config.UrlResourceReaderConfigurator;
import org.codice.ddf.itests.common.security.SecurityPolicyConfigurator;
import org.junit.Rule;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.MetaTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.sun.istack.NotNull;

/**
 * Abstract integration test with helper methods and configuration at the container level
 */
public abstract class AbstractIntegrationTest {

    public static final String JSON_RECORD_RESOURCE_PATH = "/json/record";

    public static final String CSW_RECORD_RESOURCE_PATH = "/csw/record";

    public static final String XML_RECORD_RESOURCE_PATH = "/xml/record";

    public static final String CSW_REQUEST_RESOURCE_PATH = "/csw/request";

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    protected static final String LOG_CONFIG_PID = AdminConfig.LOG_CONFIG_PID;

    protected static final String LOGGER_PREFIX = AdminConfig.LOGGER_PREFIX;

    protected static final String KARAF_VERSION = "4.0.5";

    protected static final String OPENSEARCH_SOURCE_ID = "openSearchSource";

    protected static final String CSW_SOURCE_ID = "cswSource";

    protected static final String SYSTEM_ADMIN_USER = "system-admin-user";

    protected static final String SYSTEM_ADMIN_USER_PASSWORD = "password";

    public static final String RESOURCE_VARIABLE_DELIMETER = "$";

    protected static ServerSocket placeHolderSocket;

    protected static Integer basePort;

    protected static final String DDF_HOME_PROPERTY = "ddf.home";

    protected static String ddfHome;

    @Rule
    public PaxExamRule paxExamRule = new PaxExamRule(this);

    protected String logLevel = "";

    @Inject
    protected ConfigurationAdmin configAdmin;

    @Inject
    protected FeaturesService features;

    @Inject
    protected SessionFactory sessionFactory;

    @Inject
    protected MetaTypeService metatype;

    /**
     * To make sure the tests run only when the boot features are fully installed
     */
    @Inject
    BootFinished bootFinished;

    private AdminConfig adminConfig;

    private ServiceManager serviceManager;

    private SecurityPolicyConfigurator securityPolicy;

    private CatalogBundle catalogBundle;

    private UrlResourceReaderConfigurator urlResourceReaderConfigurator;

    protected static final String[] DEFAULT_REQUIRED_APPS =
            {"catalog-app", "solr-app", "spatial-app", "sdk-app"};

    /**
     * An enum that returns a port number based on the class variable {@link #basePort}. Used to allow parallel itests
     * and dynamic allocation of ports to prevent conflicts on hard coded port numbers.
     * {@link #basePort} needs to be set in the {@link @BeforeExam} method of every test class that uses DynamicPort or
     * {@link DynamicUrl}. E.g. 'basePort = {@link #getBasePort()}`
     */
    public static class DynamicPort {

        private final String systemProperty;

        private final Integer ordinal;

        public DynamicPort(Integer ordinal) {
            this.systemProperty = null;
            this.ordinal = ordinal;
        }

        public DynamicPort(String systemProperty, Integer ordinal) {
            this.systemProperty = systemProperty;
            this.ordinal = ordinal;
        }

        String getSystemProperty() {
            return this.systemProperty;
        }

        public String getPort(Integer basePort) {
            return String.valueOf(basePort + this.ordinal());
        }

        public String getPort() {
            return String.valueOf(basePort + this.ordinal());
        }

        Integer ordinal() {
            return this.ordinal;
        }
    }

    /**
     * A class used to give a dynamic {@link String} that evaluates when called rather than at compile time.
     * Used to allow parallel itests and dynamic URLs to prevent conflicts on hard coded port numbers and endpoint, source, etc URLs.
     * Constructed with a {@link AbstractIntegrationTest.DynamicPort}
     */
    public static class DynamicUrl {
        public static final String SECURE_ROOT = "https://localhost:";

        public static final String INSECURE_ROOT = "http://localhost:";

        private final String root;

        private final String tail;

        private final DynamicPort port;

        private final DynamicUrl url;

        public DynamicUrl(String root, @NotNull DynamicPort port) {
            this(root, port, "");
        }

        public DynamicUrl(String root, @NotNull DynamicPort port, String tail) {
            if (null == port) {
                throw new IllegalArgumentException("Port cannot be null");
            }
            this.root = root;
            this.port = port;
            this.url = null;
            this.tail = tail;
        }

        public DynamicUrl(@NotNull DynamicUrl url, String tail) {
            if (null == url) {
                throw new IllegalArgumentException("Url cannot be null");
            }
            this.root = null;
            this.port = null;
            this.url = url;
            this.tail = tail;
        }

        public String getUrl() {
            if (null != port) {
                return root + port.getPort() + tail;
            } else {
                return url.getUrl() + tail;
            }
        }

        /**
         * @return the same String as {@link #getUrl()}
         */
        @Override
        public String toString() {
            return this.getUrl();
        }

    }

    public static final DynamicPort BASE_PORT = new DynamicPort("org.codice.ddf.system.basePort",
            0);

    public static final DynamicPort HTTP_PORT = new DynamicPort("org.codice.ddf.system.httpPort",
            1);

    public static final DynamicPort HTTPS_PORT = new DynamicPort("org.codice.ddf.system.httpsPort",
            2);

    public static final DynamicPort DEFAULT_PORT = new DynamicPort("org.codice.ddf.system.port", 2);

    public static final DynamicPort SSH_PORT = new DynamicPort(3);

    public static final DynamicPort RMI_SERVER_PORT = new DynamicPort(4);

    public static final DynamicPort RMI_REG_PORT = new DynamicPort(5);

    public static final DynamicUrl SERVICE_ROOT = new DynamicUrl(SECURE_ROOT,
            HTTPS_PORT,
            "/services");

    public static final DynamicUrl INSECURE_SERVICE_ROOT = new DynamicUrl(INSECURE_ROOT,
            HTTP_PORT,
            "/services");

    public static final DynamicUrl REST_PATH = new DynamicUrl(SERVICE_ROOT, "/catalog/");

    public static final DynamicUrl OPENSEARCH_PATH = new DynamicUrl(REST_PATH, "query");

    public static final DynamicUrl CSW_PATH = new DynamicUrl(SERVICE_ROOT, "/csw");

    public static final DynamicUrl CSW_SUBSCRIPTION_PATH = new DynamicUrl(SERVICE_ROOT,
            "/csw/subscription");

    public static final DynamicUrl CSW_EVENT_PATH = new DynamicUrl(SERVICE_ROOT,
            "/csw/subscription/event");

    public static final DynamicUrl ADMIN_ALL_SOURCES_PATH = new DynamicUrl(SECURE_ROOT,
            HTTPS_PORT,
            "/admin/jolokia/exec/org.codice.ddf.catalog.admin.poller.AdminPollerServiceBean:service=admin-source-poller-service/allSourceInfo");

    public static final DynamicUrl ADMIN_STATUS_PATH = new DynamicUrl(SECURE_ROOT,
            HTTPS_PORT,
            "/admin/jolokia/exec/org.codice.ddf.catalog.admin.poller.AdminPollerServiceBean:service=admin-source-poller-service/sourceStatus/");

    public static final DynamicUrl RESOURCE_DOWNLOAD_ENDPOINT_ROOT = new DynamicUrl(SERVICE_ROOT,
            "/internal/catalog/download/cache");

    public static final DynamicUrl COMETD_ENDPOINT = new DynamicUrl(SECURE_ROOT,
            HTTPS_PORT,
            "/search/cometd/");

    static {
        // Make Pax URL use the maven.repo.local setting if present
        if (System.getProperty("maven.repo.local") != null) {
            System.setProperty("org.ops4j.pax.url.mvn.localRepository",
                    System.getProperty("maven.repo.local"));
        }
    }

    @PostTestConstruct
    public void initFacades() {
        ddfHome = System.getProperty(DDF_HOME_PROPERTY);
        adminConfig = new AdminConfig(configAdmin);

        //This proxy runs the service manager as the system subject
        serviceManager =
                (ServiceManager) Proxy.newProxyInstance(AbstractIntegrationTest.class.getClassLoader(),
                        ServiceManagerImpl.class.getInterfaces(),
                        new ServiceManagerProxy(new ServiceManagerImpl(metatype, adminConfig)));

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
    public Option[] config() throws URISyntaxException, IOException {
        basePort = findPortNumber(20000);
        return combineOptions(configureCustom(),
                configureLogLevel(),
                configureIncludeUnstableTests(),
                configureDistribution(),
                configureConfigurationPorts(),
                configureMavenRepos(),
                configureSystemSettings(),
                configureVmOptions(),
                configureStartScript(),
                configurePaxExam());
    }

    private static Integer findPortNumber(Integer portToTry) {
        try {
            placeHolderSocket = new ServerSocket(portToTry);
            placeHolderSocket.setReuseAddress(true);
            return portToTry;
        } catch (Exception e) {
            portToTry += 10;
            LOGGER.debug("Bad port, trying {}", portToTry);
            return findPortNumber(portToTry);
        }
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
        return options(karafDistributionConfiguration(maven().groupId("org.codice.ddf")
                .artifactId("ddf")
                .type("zip")
                .versionAsInProject()
                .getURL(), "ddf", KARAF_VERSION).unpackDirectory(new File("target/exam"))
                .useDeployFolder(false));
    }

    protected Option[] configurePaxExam() {
        return options(logLevel(LogLevelOption.LogLevel.WARN), useOwnExamBundlesStartLevel(100),
                // increase timeout for CI environment
                systemTimeout(TimeUnit.MINUTES.toMillis(10)),
                when(Boolean.getBoolean("keepRuntimeFolder")).useOptions(keepRuntimeFolder()),
                cleanCaches(true));
    }

    protected Option[] configureConfigurationPorts() throws URISyntaxException, IOException {
        return options(editConfigurationFilePut("etc/system.properties", "urlScheme", "https"),
                editConfigurationFilePut("etc/system.properties", "host", "localhost"),
                editConfigurationFilePut("etc/system.properties",
                        "jetty.port",
                        HTTPS_PORT.getPort()),
                editConfigurationFilePut("etc/system.properties", "hostContext", "/solr"),
                editConfigurationFilePut("etc/system.properties", "ddf.home", "${karaf.home}"),
                editConfigurationFilePut("etc/users.properties",
                        SYSTEM_ADMIN_USER,
                        SYSTEM_ADMIN_USER_PASSWORD + ",system-admin"),
                editConfigurationFilePut("etc/system.properties",
                        HTTP_PORT.getSystemProperty(),
                        HTTP_PORT.getPort()),
                editConfigurationFilePut("etc/system.properties",
                        HTTPS_PORT.getSystemProperty(),
                        HTTPS_PORT.getPort()),
                editConfigurationFilePut("etc/system.properties",
                        DEFAULT_PORT.getSystemProperty(),
                        DEFAULT_PORT.getPort()),
                editConfigurationFilePut("etc/system.properties",
                        BASE_PORT.getSystemProperty(),
                        BASE_PORT.getPort()),

                // DDF-1572: Disables the periodic backups of .bundlefile. In itests, having those
                // backups serves no purpose and it appears that intermittent failures have occurred
                // when the background thread attempts to create the backup before the exam bundle
                // is completely exploded.
                editConfigurationFilePut("etc/system.properties",
                        "eclipse.enableStateSaver",
                        Boolean.FALSE.toString()),

                editConfigurationFilePut("etc/org.apache.karaf.shell.cfg",
                        "sshPort",
                        SSH_PORT.getPort()),
                editConfigurationFilePut("etc/ddf.platform.config.cfg",
                        "port",
                        HTTPS_PORT.getPort()),
                editConfigurationFilePut("etc/ddf.platform.config.cfg", "host", "localhost"),
                editConfigurationFilePut("etc/org.ops4j.pax.web.cfg",
                        "org.osgi.service.http.port",
                        HTTP_PORT.getPort()),
                editConfigurationFilePut("etc/org.ops4j.pax.web.cfg",
                        "org.osgi.service.http.port.secure",
                        HTTPS_PORT.getPort()),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg",
                        "rmiRegistryPort",
                        RMI_REG_PORT.getPort()),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg",
                        "rmiServerPort",
                        RMI_SERVER_PORT.getPort()),
                installStartupFile(getClass().getClassLoader()
                        .getResourceAsStream("hazelcast.xml"), "/etc/hazelcast.xml"),
                installStartupFile(getClass().getClassLoader()
                                .getResourceAsStream("ddf.security.sts.client.configuration.config"),
                        "/etc/ddf.security.sts.client.configuration.config"),
                KarafDistributionOption.editConfigurationFilePut(
                        "etc/ddf.security.sts.client.configuration.config",
                        "address",
                        "\"" + SECURE_ROOT + HTTPS_PORT.getPort()
                                + "/services/SecurityTokenService?wsdl" + "\""),
                installStartupFile(getClass().getClassLoader()
                                .getResourceAsStream(
                                        "ddf.catalog.solr.external.SolrHttpCatalogProvider.config"),
                        "/etc/ddf.catalog.solr.external.SolrHttpCatalogProvider.config"),
                installStartupFile(getClass().getClassLoader()
                                .getResourceAsStream(
                                        "ddf.catalog.solr.provider.SolrCatalogProvider.config"),
                        "/etc/ddf.catalog.solr.provider.SolrCatalogProvider.config"));
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

        return options(when(Boolean.getBoolean("isDebugEnabled")).useOptions(vmOption(
                "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005")),
                when(System.getProperty("maven.repo.local") != null).useOptions(systemProperty(
                        "org.ops4j.pax.url.mvn.localRepository").value(System.getProperty(
                        "maven.repo.local",
                        ""))),
                editConfigurationFilePut("etc/system.properties",
                        "ddf.version",
                        MavenUtils.getArtifactVersion("ddf.test.itests", "test-itests-common")));
    }

    protected Option[] configureLogLevel() {
        return options(when(
                System.getProperty(AdminConfig.TEST_LOGLEVEL_PROPERTY) != null).useOptions(
                systemProperty(AdminConfig.TEST_LOGLEVEL_PROPERTY).value(System.getProperty(
                        AdminConfig.TEST_LOGLEVEL_PROPERTY,
                        ""))));
    }

    protected Option[] configureIncludeUnstableTests() {
        return options(when(System.getProperty(SkipUnstableTest.INCLUDE_UNSTABLE_TESTS_PROPERTY)
                != null).useOptions(systemProperty(SkipUnstableTest.INCLUDE_UNSTABLE_TESTS_PROPERTY).value(
                System.getProperty(SkipUnstableTest.INCLUDE_UNSTABLE_TESTS_PROPERTY, ""))));
    }

    protected Option[] configureVmOptions() {
        return options(vmOption("-Xmx2048M"),
                // avoid integration tests stealing focus on OS X
                vmOption("-Djava.awt.headless=true"),
                vmOption("-Dfile.encoding=UTF8"));
    }

    protected Option[] configureStartScript() {
        //add test dependencies to the test-dependencies-app instead of here
        return options(junitBundles(),
                features(maven().groupId("ddf.test.itests")
                        .artifactId("test-itests-dependencies-app")
                        .type("xml")
                        .classifier("features")
                        .versionAsInProject(), "ddf-itest-dependencies"),
                //Adds sdk-app to the features repo
                features(maven("ddf.distribution", "sdk-app").classifier("features")
                        .type("xml")
                        .versionAsInProject()));
    }

    /**
     * Allows extending classes to add any custom options to the configuration.
     */
    protected Option[] configureCustom() {
        return null;
    }

    /**
     * Copies the content of a JAR resource to the destination specified before the container
     * starts up. Useful to add test configuration files before tests are run.
     *
     * @param resourceInputStream input stream to te he JAR resource to copy
     * @param destination         destination relative to DDF_HOME
     * @return option object to include in a {@link #configureCustom()} method
     * @throws IOException thrown if a problem occurs while copying the resource
     */
    protected Option installStartupFile(InputStream resourceInputStream, String destination)
            throws IOException {
        File tempFile = Files.createTempFile("StartupFile", ".temp")
                .toFile();
        tempFile.deleteOnExit();
        FileUtils.copyInputStreamToFile(resourceInputStream, tempFile);
        return replaceConfigurationFile(destination, tempFile);
    }

    protected static String[] getDefaultRequiredApps() {
        return Arrays.copyOf(DEFAULT_REQUIRED_APPS, DEFAULT_REQUIRED_APPS.length);
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

    protected Integer getBasePort() {
        return Integer.parseInt(System.getProperty(BASE_PORT.getSystemProperty()));
    }

    public static InputStream getFileContentAsStream(String filePath) {
        return getFileContentAsStream(filePath, AbstractIntegrationTest.class);
    }

    public static InputStream getFileContentAsStream(String filePath,
            Class classRelativeToResource) {
        return classRelativeToResource.getClassLoader()
                .getResourceAsStream(filePath);
    }

    public static String getFileContent(String filePath) {
        return getFileContent(filePath, ImmutableMap.of());
    }

    /**
     * Variables to be replaced in a resource file should be in the format: $variableName$
     * The variable to replace in the file should also also match the parameter names of the method calling getFileContent.
     *
     * @param filePath
     * @param params
     * @param classRelativeToResource
     * @return
     */
    public static String getFileContent(String filePath, ImmutableMap<String, String> params,
            Class classRelativeToResource) {

        StrSubstitutor strSubstitutor = new StrSubstitutor(params);

        strSubstitutor.setVariablePrefix(RESOURCE_VARIABLE_DELIMETER);
        strSubstitutor.setVariableSuffix(RESOURCE_VARIABLE_DELIMETER);
        String fileContent;

        try {
            fileContent = IOUtils.toString(classRelativeToResource.getClassLoader()
                    .getResourceAsStream(filePath), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("Failed to read filepath: " + filePath);
        }

        return strSubstitutor.replace(fileContent);
    }

    /**
     * Variables to be replaced in a resource file should be in the format: $variableName$
     * The variable to replace in the file should also also match the parameter names of the method calling getFileContent.
     * Resource is relative to AbstractIntegrationTest class
     *
     * @param filePath
     * @param params
     * @return
     */
    public static String getFileContent(String filePath, ImmutableMap<String, String> params) {
        return getFileContent(filePath, params, AbstractIntegrationTest.class);
    }
}
