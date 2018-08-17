/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.itests.common;

import static com.jayway.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.codice.ddf.itests.common.AbstractIntegrationTest.DynamicUrl.INSECURE_ROOT;
import static org.codice.ddf.itests.common.AbstractIntegrationTest.DynamicUrl.SECURE_ROOT;
import static org.codice.ddf.itests.common.csw.CswQueryBuilder.PROPERTY_IS_LIKE;
import static org.hamcrest.Matchers.hasXPath;
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

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.ValidatableResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.codice.ddf.itests.common.annotations.SkipUnstableTest;
import org.codice.ddf.itests.common.config.UrlResourceReaderConfigurator;
import org.codice.ddf.itests.common.csw.CswQueryBuilder;
import org.codice.ddf.itests.common.security.SecurityPolicyConfigurator;
import org.codice.ddf.test.common.LoggingUtils;
import org.codice.ddf.test.common.annotations.PaxExamRule;
import org.codice.ddf.test.common.annotations.PostTestConstruct;
import org.junit.Rule;
import org.junit.rules.Stopwatch;
import org.junit.runner.Description;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.MetaTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Abstract integration test with helper methods and configuration at the container level */
public abstract class AbstractIntegrationTest {

  @SuppressWarnings({"squid:S1075" /* resource path should use forward slashes */})
  public static final String JSON_RECORD_RESOURCE_PATH = "/json/record";

  @SuppressWarnings({"squid:S1075" /* resource path should use forward slashes */})
  public static final String CSW_RECORD_RESOURCE_PATH = "/csw/record";

  @SuppressWarnings({"squid:S1075" /* resource path should use forward slashes */})
  public static final String XML_RECORD_RESOURCE_PATH = "/xml/record";

  @SuppressWarnings({"squid:S1075" /* resource path should use forward slashes */})
  public static final String CSW_REQUEST_RESOURCE_PATH = "/csw/request";

  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractIntegrationTest.class);

  protected static final String LOGGER_CONFIGURATION_FILE_PATH = "etc/org.ops4j.pax.logging.cfg";

  protected static final String DEFAULT_LOG_LEVEL = "WARN";

  protected static final String TEST_LOG_LEVEL_PROPERTY = "itestLogLevel";

  protected static final String TEST_SECURITY_LOG_LEVEL_PROPERTY = "securityLogLevel";

  protected static final String KARAF_VERSION = "4.2.0";

  protected static final String OPENSEARCH_SOURCE_ID = "openSearchSource";

  protected static final String CSW_SOURCE_ID = "cswSource";

  protected static final String SYSTEM_ADMIN_USER = "system-admin-user";

  @SuppressWarnings("squid:S2068") // Password ok, test class
  protected static final String SYSTEM_ADMIN_USER_PASSWORD = "password";

  public static final String RESOURCE_VARIABLE_DELIMETER = "$";

  public static final String REMOVE_ALL = "catalog:removeall -f -p";

  public static final long REMOVE_ALL_TIMEOUT = TimeUnit.MINUTES.toMillis(5);

  private static final String CLEAR_CACHE = "catalog:removeall -f -p --cache";

  private static final File UNPACK_DIRECTORY = new File("target/exam");

  public static final long GENERIC_TIMEOUT_SECONDS = TimeUnit.MINUTES.toSeconds(20);

  public static final long GENERIC_TIMEOUT_MILLISECONDS = TimeUnit.MINUTES.toMillis(20);

  private static final String UNABLE_TO_DETERMINE_EXAM_DIR_ERROR =
      "Unable to determine current exam directory";

  private static final String KARAF_HOME = "{karaf.home}";

  protected static final String FILE_PERMISSIONS =
      "priority \"grant\"; grant {permission java.io.FilePermission \"%s%s-\", \"read, write\"; };";

  protected static ServerSocket placeHolderSocket;

  protected static Integer basePort;

  protected static final String DDF_HOME_PROPERTY = "ddf.home";

  protected static String ddfHome;

  @Rule public PaxExamRule paxExamRule = new PaxExamRule(this);

  @Rule public Stopwatch stopwatch = new TestMethodTimer();

  @Inject protected ConfigurationAdmin configAdmin;

  @Inject protected FeaturesService features;

  @Inject protected SessionFactory sessionFactory;

  @Inject protected MetaTypeService metatype;

  /** To make sure the tests run only when the boot features are fully installed */
  @Inject
  @Filter(timeout = 300000L)
  BootFinished bootFinished;

  private AdminConfig adminConfig;

  private ServiceManager serviceManager;

  private SecurityPolicyConfigurator securityPolicy;

  private CatalogBundle catalogBundle;

  private UrlResourceReaderConfigurator urlResourceReaderConfigurator;

  private static final String MVN_LOCAL_REPO = "maven.repo.local";

  private static final String PAX_URL_MVN_LOCAL_REPO = "org.ops4j.pax.url.mvn.localRepository";

  private static final String SYSTEM_PROPERTIES_REL_PATH = "etc/system.properties";

  private static final String DDF_ITESTS_GROUP_ID = "ddf.test.itests";

  protected static final String[] DEFAULT_REQUIRED_APPS = {
    "catalog-app", "solr-app", "spatial-app", "sdk-app"
  };

  protected KarafConsole console;

  /**
   * An enum that returns a port number based on the class variable {@link #basePort}. Used to allow
   * parallel itests and dynamic allocation of ports to prevent conflicts on hard coded port
   * numbers. {@link #basePort} needs to be set in the {@link @BeforeExam} method of every test
   * class that uses DynamicPort or {@link DynamicUrl}. E.g. 'basePort = {@link #getBasePort()}`
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
   * A class used to give a dynamic {@link String} that evaluates when called rather than at compile
   * time. Used to allow parallel itests and dynamic URLs to prevent conflicts on hard coded port
   * numbers and endpoint, source, etc URLs. Constructed with a {@link
   * AbstractIntegrationTest.DynamicPort}
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

    /** @return the same String as {@link #getUrl()} */
    @Override
    public String toString() {
      return this.getUrl();
    }
  }

  public static final DynamicPort BASE_PORT = new DynamicPort("org.codice.ddf.system.basePort", 0);

  public static final DynamicPort HTTP_PORT = new DynamicPort("org.codice.ddf.system.httpPort", 1);

  public static final DynamicPort HTTPS_PORT =
      new DynamicPort("org.codice.ddf.system.httpsPort", 2);

  public static final DynamicPort DEFAULT_PORT = new DynamicPort("org.codice.ddf.system.port", 2);

  public static final DynamicPort SSH_PORT = new DynamicPort(3);

  public static final DynamicPort RMI_SERVER_PORT = new DynamicPort(4);

  public static final DynamicPort RMI_REG_PORT = new DynamicPort(5);

  public static final DynamicUrl SERVICE_ROOT =
      new DynamicUrl(SECURE_ROOT, HTTPS_PORT, "/services");

  public static final DynamicUrl INSECURE_SERVICE_ROOT =
      new DynamicUrl(INSECURE_ROOT, HTTP_PORT, "/services");

  public static final DynamicUrl SEARCH_ROOT = new DynamicUrl(SECURE_ROOT, HTTPS_PORT, "/search");

  public static final DynamicUrl REST_PATH = new DynamicUrl(SERVICE_ROOT, "/catalog/");

  public static final DynamicUrl OPENSEARCH_PATH = new DynamicUrl(REST_PATH, "query");

  public static final DynamicUrl CSW_PATH = new DynamicUrl(SERVICE_ROOT, "/csw");

  public static final DynamicUrl CSW_SUBSCRIPTION_PATH =
      new DynamicUrl(SERVICE_ROOT, "/csw/subscription");

  public static final DynamicUrl CSW_EVENT_PATH =
      new DynamicUrl(SERVICE_ROOT, "/csw/subscription/event");

  public static final DynamicUrl ADMIN_ALL_SOURCES_PATH =
      new DynamicUrl(
          SECURE_ROOT,
          HTTPS_PORT,
          "/admin/jolokia/exec/org.codice.ddf.catalog.admin.poller.AdminPollerServiceBean:service=admin-source-poller-service/allSourceInfo");

  public static final DynamicUrl ADMIN_STATUS_PATH =
      new DynamicUrl(
          SECURE_ROOT,
          HTTPS_PORT,
          "/admin/jolokia/exec/org.codice.ddf.catalog.admin.poller.AdminPollerServiceBean:service=admin-source-poller-service/sourceStatus/");

  public static final DynamicUrl RESOURCE_DOWNLOAD_ENDPOINT_ROOT =
      new DynamicUrl(SERVICE_ROOT, "/internal/catalog/download/cache");

  static {
    // Make Pax URL use the maven.repo.local setting if present
    if (System.getProperty(MVN_LOCAL_REPO) != null) {
      System.setProperty(PAX_URL_MVN_LOCAL_REPO, System.getProperty(MVN_LOCAL_REPO));
    }
  }

  @SuppressWarnings({
    "squid:S2696" /* writing to static ddfHome to share state between test methods */
  })
  @PostTestConstruct
  public void initFacades() {
    ddfHome = System.getProperty(DDF_HOME_PROPERTY);
    adminConfig = new AdminConfig(configAdmin);

    // This proxy runs the service manager as the system subject
    serviceManager =
        (ServiceManager)
            Proxy.newProxyInstance(
                AbstractIntegrationTest.class.getClassLoader(),
                ServiceManagerImpl.class.getInterfaces(),
                new ServiceManagerProxy(new ServiceManagerImpl(metatype, adminConfig)));

    catalogBundle = new CatalogBundle(serviceManager, adminConfig);
    securityPolicy = new SecurityPolicyConfigurator(serviceManager, configAdmin);
    urlResourceReaderConfigurator = new UrlResourceReaderConfigurator(configAdmin);
    console = new KarafConsole(getServiceManager().getBundleContext(), features, sessionFactory);
  }

  @SuppressWarnings({
    "squid:S2696" /* writing to static basePort to share state between test methods */
  })
  public void waitForBaseSystemFeatures() {
    try {
      basePort = getBasePort();
      getServiceManager().startFeature(true, getDefaultRequiredApps());
      getServiceManager().waitForAllBundles();
      getCatalogBundle().waitForCatalogProvider();

      getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query?_wadl");
      getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/csw?_wadl");
      getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/catalog?_wadl");

      getServiceManager().startFeature(true, "search-ui-app", "catalog-ui");
      getServiceManager().waitForAllBundles();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to start up required features.", e);
    }
  }

  public void waitForSystemReady() {
    SystemStateManager manager =
        SystemStateManager.getManager(serviceManager, features, adminConfig, console);
    manager.setSystemBaseState(this::waitForBaseSystemFeatures, false);
    manager.waitForSystemBaseState();
  }

  /**
   * Configures the pax exam test container
   *
   * @return list of pax exam options
   */
  @SuppressWarnings({
    "squid:S2696" /* writing to static basePort to share state between test methods */
  })
  @org.ops4j.pax.exam.Configuration
  public Option[] config() throws URISyntaxException, IOException {
    basePort = findPortNumber(20000);
    return combineOptions(
        configureCustom(),
        configureSolr(),
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
   *     empty.
   * @return array that combines all the {@code Option} objects from the arrays provided. {@code
   *     null} and empty arrays will be ignored, but {@code null} {@link Option} objects will be
   *     added to the result.
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
    return options(
        karafDistributionConfiguration(
                maven()
                    .groupId("org.codice.ddf")
                    .artifactId("ddf")
                    .type("zip")
                    .versionAsInProject()
                    .getURL(),
                "ddf",
                KARAF_VERSION)
            .unpackDirectory(UNPACK_DIRECTORY)
            .useDeployFolder(false));
  }

  protected Option[] configurePaxExam() {
    return options(
        logLevel(LogLevelOption.LogLevel.WARN),
        useOwnExamBundlesStartLevel(100),
        // increase timeout for CI environment
        systemTimeout(GENERIC_TIMEOUT_MILLISECONDS),
        when(Boolean.getBoolean("keepRuntimeFolder")).useOptions(keepRuntimeFolder()),
        cleanCaches(true));
  }

  protected Option[] configureConfigurationPorts() throws URISyntaxException, IOException {
    return options(
        editConfigurationFilePut(
            "etc/users.properties",
            SYSTEM_ADMIN_USER,
            SYSTEM_ADMIN_USER_PASSWORD + ",system-admin"),
        editConfigurationFilePut(SYSTEM_PROPERTIES_REL_PATH, DDF_HOME_PROPERTY, "${karaf.home}"),
        editConfigurationFilePut(SYSTEM_PROPERTIES_REL_PATH, "maven.home", "${user.home}"),
        editConfigurationFilePut(SYSTEM_PROPERTIES_REL_PATH, "M2_HOME", "${user.home}"),
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_REL_PATH, HTTP_PORT.getSystemProperty(), HTTP_PORT.getPort()),
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_REL_PATH, HTTPS_PORT.getSystemProperty(), HTTPS_PORT.getPort()),
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_REL_PATH, DEFAULT_PORT.getSystemProperty(), DEFAULT_PORT.getPort()),
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_REL_PATH, BASE_PORT.getSystemProperty(), BASE_PORT.getPort()),

        // DDF-1572: Disables the periodic backups of .bundlefile. In itests, having those
        // backups serves no purpose and it appears that intermittent failures have occurred
        // when the background thread attempts to create the backup before the exam bundle
        // is completely exploded.
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_REL_PATH, "eclipse.enableStateSaver", Boolean.FALSE.toString()),
        editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", SSH_PORT.getPort()),
        editConfigurationFilePut(
            "etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", HTTP_PORT.getPort()),
        editConfigurationFilePut(
            "etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port.secure", HTTPS_PORT.getPort()),
        editConfigurationFilePut(
            "etc/org.apache.karaf.management.cfg", "rmiRegistryPort", RMI_REG_PORT.getPort()),
        editConfigurationFilePut(
            "etc/org.apache.karaf.management.cfg", "rmiServerPort", RMI_SERVER_PORT.getPort()),
        installStartupFile(
            getClass().getClassLoader().getResource("hazelcast.xml"), "/etc/hazelcast.xml"),
        KarafDistributionOption.editConfigurationFilePut(
            "etc/ddf.security.sts.client.configuration.config",
            "address",
            SECURE_ROOT + HTTPS_PORT.getPort() + "/services/SecurityTokenService?wsdl"),
        installStartupFile(
            getClass()
                .getClassLoader()
                .getResource("ddf.catalog.solr.external.SolrHttpCatalogProvider.config"),
            "/etc/ddf.catalog.solr.external.SolrHttpCatalogProvider.config"),
        installStartupFile(
            getClass()
                .getClassLoader()
                .getResource("ddf.catalog.solr.provider.SolrCatalogProvider.config"),
            "/etc/ddf.catalog.solr.provider.SolrCatalogProvider.config"));
  }

  protected Option[] configureMavenRepos() {
    return options(
        editConfigurationFilePut(
            "etc/org.ops4j.pax.url.mvn.cfg",
            "org.ops4j.pax.url.mvn.repositories",
            "http://repo1.maven.org/maven2@id=central,"
                + "http://oss.sonatype.org/content/repositories/snapshots@snapshots@noreleases@id=sonatype-snapshot,"
                + "http://oss.sonatype.org/content/repositories/ops4j-snapshots@snapshots@noreleases@id=ops4j-snapshot,"
                + "http://repository.apache.org/content/groups/snapshots-group@snapshots@noreleases@id=apache,"
                + "http://svn.apache.org/repos/asf/servicemix/m2-repo@id=servicemix,"
                + "http://repository.springsource.com/maven/bundles/release@id=springsource,"
                + "http://repository.springsource.com/maven/bundles/external@id=springsourceext,"
                + "http://oss.sonatype.org/content/repositories/releases/@id=sonatype"),
        when(System.getProperty(MVN_LOCAL_REPO) != null)
            .useOptions(
                editConfigurationFilePut(
                    "etc/org.ops4j.pax.url.mvn.cfg",
                    PAX_URL_MVN_LOCAL_REPO,
                    System.getProperty(MVN_LOCAL_REPO))));
  }

  protected Option[] configureSystemSettings() {
    return options(
        when(Boolean.getBoolean("isDebugEnabled"))
            .useOptions(vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005")),
        when(System.getProperty(MVN_LOCAL_REPO) != null)
            .useOptions(
                systemProperty(PAX_URL_MVN_LOCAL_REPO)
                    .value(System.getProperty(MVN_LOCAL_REPO, ""))),
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_REL_PATH,
            "org.codice.ddf.system.version",
            MavenUtils.getArtifactVersion(DDF_ITESTS_GROUP_ID, "test-itests-common")),
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_REL_PATH,
            "ddf.version",
            MavenUtils.getArtifactVersion(DDF_ITESTS_GROUP_ID, "test-itests-common")),
        editConfigurationFilePut(SYSTEM_PROPERTIES_REL_PATH, "artemis.diskusage", "100"));
  }

  protected Option[] configureLogLevel() {
    final String logLevel = System.getProperty(TEST_LOG_LEVEL_PROPERTY);
    final String securityLogLevel = System.getProperty(TEST_SECURITY_LOG_LEVEL_PROPERTY);
    return options(
        editConfigurationFilePut(
            LOGGER_CONFIGURATION_FILE_PATH, "log4j2.rootLogger.level", DEFAULT_LOG_LEVEL),
        when(StringUtils.isNotEmpty(logLevel))
            .useOptions(
                combineOptions(
                    createSetLogLevelOption("ddf", logLevel),
                    createSetLogLevelOption("org.codice", logLevel))),
        when(StringUtils.isNotEmpty(securityLogLevel))
            .useOptions(
                combineOptions(
                    createSetLogLevelOption(
                        "ddf.security.expansion.impl.RegexExpansion", securityLogLevel),
                    createSetLogLevelOption(
                        "ddf.security.service.impl.AbstractAuthorizingRealm", securityLogLevel))),
        editConfigurationFilePut(
            LOGGER_CONFIGURATION_FILE_PATH,
            "log4j2.logger.org_apache_activemq_artemis.additivity",
            "true"));
  }

  /**
   * Creates options to add log configuration lines to the etc/org.ops4j.pax.logging.cfg file. See
   * {@see org.apache.karaf.log.core.internal.LogServiceLog4j2Impl}.
   *
   * @param name name of the logger to set
   * @param level String value to set the logger level
   * @return options to set the log level
   */
  protected Option[] createSetLogLevelOption(String name, String level) {
    final String loggerPrefix = "log4j2.logger.";
    final String loggerKey = name.replace('.', '_').toLowerCase();
    return options(
        editConfigurationFilePut(
            LOGGER_CONFIGURATION_FILE_PATH,
            String.format("%s%s.name", loggerPrefix, loggerKey),
            name),
        editConfigurationFilePut(
            LOGGER_CONFIGURATION_FILE_PATH,
            String.format("%s%s.level", loggerPrefix, loggerKey),
            level));
  }

  protected Option[] configureIncludeUnstableTests() {
    return options(
        when(System.getProperty(SkipUnstableTest.INCLUDE_UNSTABLE_TESTS_PROPERTY) != null)
            .useOptions(
                systemProperty(SkipUnstableTest.INCLUDE_UNSTABLE_TESTS_PROPERTY)
                    .value(
                        System.getProperty(SkipUnstableTest.INCLUDE_UNSTABLE_TESTS_PROPERTY, ""))));
  }

  protected Option[] configureVmOptions() {
    return options(
        vmOption("-Xmx6144M"),
        // avoid integration tests stealing focus on OS X
        vmOption("-Djava.awt.headless=true"),
        vmOption("-Dfile.encoding=UTF8"),
        vmOption("-Dpolicy.provider=net.sourceforge.prograde.policy.ProGradePolicy"),
        vmOption("-Djava.security.manager=net.sourceforge.prograde.sm.ProGradeJSM"),
        HomeAwareVmOption.homeAwareVmOption("-Djava.security.policy=={karaf.home}/etc/all.policy"),
        vmOption(
            "-DproGrade.getPermissions.override=sun.rmi.server.LoaderHandler:loadClass,org.apache.jasper.compiler.JspRuntimeContext:initSecurity"),
        HomeAwareVmOption.homeAwareVmOption("-Dddf.home={karaf.home}"),
        HomePermVmOption.homePermVmOption("-Dddf.home.perm={karaf.home}"),
        HomePolicyVmOption.homePolicyVmOption("-Dddf.home.policy={karaf.home}"));
  }

  protected Option[] configureStartScript() {
    // add test dependencies to the test-dependencies-app instead of here
    return options(
        junitBundles(),
        features(
            maven()
                .groupId(DDF_ITESTS_GROUP_ID)
                .artifactId("test-itests-dependencies-app")
                .type("xml")
                .classifier("features")
                .versionAsInProject(),
            "ddf-itest-dependencies"),
        // Adds sdk-app to the features repo
        features(
            maven("ddf.distribution", "sdk-app")
                .classifier("features")
                .type("xml")
                .versionAsInProject()));
  }

  /**
   * Allows extending classes to add any custom options to the configuration. This is only valid for
   * tests run with the PerClass strategy
   */
  protected Option[] configureCustom() {
    try {
      return options(
          // Extra config options for catalog-ui and security
          installStartupFile(
              getClass().getResource("/etc/test-users.properties"), "/etc/users.properties"),
          installStartupFile(
              getClass().getResource("/etc/test-users.attributes"), "/etc/users.attributes"),
          installStartupFile(
              getClass().getResource("/injections.json"), "/etc/definitions/injections.json"),
          // Catalog-ui custom forms
          installStartupFile(
              getClass().getResource("/etc/forms/forms.json"), "/etc/forms/forms.json"),
          installStartupFile(
              getClass().getResource("/etc/forms/results.json"), "/etc/forms/results.json"),
          installStartupFile(
              getClass().getResource("/etc/forms/imagery.xml"), "/etc/forms/imagery.xml"),
          installStartupFile(
              getClass().getResource("/etc/forms/contact-name.xml"), "/etc/forms/contact-name.xml"),
          getFilePermissionsOption());
    } catch (IOException e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed to deploy configuration files: ");
    }
    return new Option[0];
  }

  protected Option getFilePermissionsOption() throws IOException {
    return installStartupFile(
        String.format(
            FILE_PERMISSIONS,
            new File("target" + File.separator + "solr")
                .getAbsolutePath()
                .replace("/", "${/}")
                .replace("\\", "${/}"),
            "${/}"),
        "/security/itests-solr.policy");
  }

  private Option[] configureSolr() {

    return options(
        editConfigurationFilePut(SYSTEM_PROPERTIES_REL_PATH, "solr.client", "HttpSolrClient"),
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_REL_PATH, "solr.http.url", "http://localhost:9784/solr"),
        editConfigurationFilePut(SYSTEM_PROPERTIES_REL_PATH, "solr.http.port", "9784"),
        editConfigurationFilePut(
            SYSTEM_PROPERTIES_REL_PATH,
            "solr.data.dir",
            new File("target/solr/server/solr").getAbsolutePath()),
        editConfigurationFilePut(SYSTEM_PROPERTIES_REL_PATH, "solr.cloud.zookeeper", ""));
  }

  /**
   * Copies a String into the destination specified before the container starts up. Useful to add
   * test configuration files before tests are run.
   *
   * @param content content to use for file
   * @param destination destination relative to DDF_HOME
   * @return option object to include in a {@link #configureCustom()} method
   * @throws IOException thrown if a problem occurs while copying the resource
   */
  protected Option installStartupFile(String content, String destination) throws IOException {
    try (InputStream is = new ByteArrayInputStream(content.getBytes("UTF-8"))) {
      return installStartupFile(is, destination);
    }
  }
  /**
   * Copies the content of a JAR resource to the destination specified before the container starts
   * up. Useful to add test configuration files before tests are run.
   *
   * @param resource URL to the JAR resource to copy
   * @param destination destination relative to DDF_HOME
   * @return option object to include in a {@link #configureCustom()} method
   * @throws IOException thrown if a problem occurs while copying the resource
   */
  protected Option installStartupFile(URL resource, String destination) throws IOException {
    try (InputStream is = resource.openStream()) {
      return installStartupFile(is, destination);
    }
  }

  private Option installStartupFile(InputStream is, String destination) throws IOException {
    File tempFile = Files.createTempFile("StartupFile", ".temp").toFile();
    tempFile.deleteOnExit();
    FileUtils.copyInputStreamToFile(is, tempFile);
    return replaceConfigurationFile(destination, tempFile);
  }

  protected String[] getDefaultRequiredApps() {
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

  public static InputStream getFileContentAsStream(String filePath, Class classRelativeToResource) {
    return classRelativeToResource.getClassLoader().getResourceAsStream(filePath);
  }

  public static String getFileContent(String filePath) {
    return getFileContent(filePath, ImmutableMap.of());
  }

  /**
   * Variables to be replaced in a resource file should be in the format: $variableName$ The
   * variable to replace in the file should also also match the parameter names of the method
   * calling getFileContent.
   *
   * @param filePath
   * @param params
   * @param classRelativeToResource
   * @return
   */
  @SuppressWarnings({
    "squid:S00112" /* A generic RuntimeException is perfectly reasonable in this case. */
  })
  public static String getFileContent(
      String filePath, ImmutableMap<String, String> params, Class classRelativeToResource) {

    StrSubstitutor strSubstitutor = new StrSubstitutor(params);

    strSubstitutor.setVariablePrefix(RESOURCE_VARIABLE_DELIMETER);
    strSubstitutor.setVariableSuffix(RESOURCE_VARIABLE_DELIMETER);
    String fileContent;

    try {
      fileContent =
          IOUtils.toString(
              classRelativeToResource.getClassLoader().getResourceAsStream(filePath), "UTF-8");
    } catch (IOException e) {
      throw new RuntimeException("Failed to read filepath: " + filePath);
    }

    return strSubstitutor.replace(fileContent);
  }

  /**
   * Variables to be replaced in a resource file should be in the format: $variableName$ The
   * variable to replace in the file should also also match the parameter names of the method
   * calling getFileContent. Resource is relative to AbstractIntegrationTest class
   *
   * @param filePath
   * @param params
   * @return
   */
  public static String getFileContent(String filePath, ImmutableMap<String, String> params) {
    return getFileContent(filePath, params, AbstractIntegrationTest.class);
  }

  public void configureRestForGuest() throws Exception {
    getSecurityPolicy().configureRestForGuest();
  }

  public void configureRestForBasic() throws Exception {
    getSecurityPolicy().configureRestForBasic();
  }

  public void configureRestForGuest(String whitelist) throws Exception {
    getSecurityPolicy().configureRestForGuest(whitelist);
  }

  public void configureRestForBasic(String whitelist) throws Exception {
    getSecurityPolicy().configureRestForBasic(whitelist);
  }

  protected void configureBundle(
      String bundleName, String pid, Dictionary<String, Object> properties)
      throws IOException, BundleException, InterruptedException {
    getServiceManager().stopBundle(bundleName);
    Configuration config = configAdmin.getConfiguration(pid, null);
    config.update(properties);
    getServiceManager().startBundle(bundleName);
    getServiceManager().waitForAllBundles();
  }

  /**
   * Clears out the catalog and catalog cache of all 'resource' metacards. Will not return until all
   * metacards have been removed. Will throw an AssertionError if catalog could not be cleared
   * within 30 seconds.
   */
  public void clearCatalogAndWait() {
    clearCatalog();
    clearCache();
    with()
        .pollInterval(1, SECONDS)
        .await()
        .atMost(GENERIC_TIMEOUT_SECONDS, SECONDS)
        .until(this::isCatalogEmpty);
  }

  public void clearCatalog() {
    String output = console.runCommand(REMOVE_ALL, GENERIC_TIMEOUT_MILLISECONDS);
    LOGGER.debug("{} output: {}", REMOVE_ALL, output);
  }

  public void clearCache() {
    console.runCommand(CLEAR_CACHE, GENERIC_TIMEOUT_MILLISECONDS);
  }

  protected boolean isCatalogEmpty() {

    try {
      String query =
          new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*").getQuery();
      ValidatableResponse response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .auth()
              .basic("admin", "admin")
              .post(CSW_PATH.getUrl())
              .then();
      response.body(hasXPath("/GetRecordsResponse/SearchResults[@numberOfRecordsMatched=\"0\"]"));
      return true;
    } catch (AssertionError e) {
      return false;
    }
  }

  /**
   * Helper Option class to allow interpolation of {@code karaf.home} directory based on the
   * provided {@link #UNPACK_DIRECTORY}.
   */
  protected static class HomeAwareVmOption extends VMOption {
    public static HomeAwareVmOption homeAwareVmOption(String option) {
      return new HomeAwareVmOption(option);
    }

    private HomeAwareVmOption(String option) {
      super(option);
    }

    @Override
    @SuppressWarnings({
      "squid:S00112" /* A generic RuntimeException is perfectly reasonable in this case. */
    })
    public String getOption() {
      final Function<Path, FileTime> createTimeComp =
          path -> {
            try {
              return Files.readAttributes(path, BasicFileAttributes.class).creationTime();
            } catch (IOException e) {
              throw new RuntimeException(UNABLE_TO_DETERMINE_EXAM_DIR_ERROR, e);
            }
          };

      try (final Stream<Path> dirContents = Files.list(UNPACK_DIRECTORY.toPath())) {
        return dirContents
            .max(Comparator.comparing(createTimeComp))
            .map(Path::toAbsolutePath)
            .map(Path::toString)
            .map(
                s -> {
                  LOGGER.error(s);
                  return s;
                })
            .map(s -> StringUtils.replace(super.getOption(), KARAF_HOME, s))
            .map(s -> s.replace('\\', '/'))
            .map(s -> s.replace("/bin/..", "/"))
            .orElseGet(super::getOption);
      } catch (IOException e) {
        throw new RuntimeException(UNABLE_TO_DETERMINE_EXAM_DIR_ERROR, e);
      }
    }
  }

  /**
   * Helper Option class to allow interpolation of {@code karaf.home} directory based on the
   * provided {@link #UNPACK_DIRECTORY}.
   */
  protected static class HomePolicyVmOption extends VMOption {
    public static HomePolicyVmOption homePolicyVmOption(String option) {
      return new HomePolicyVmOption(option);
    }

    private HomePolicyVmOption(String option) {
      super(option);
    }

    @Override
    @SuppressWarnings({
      "squid:S00112" /* A generic RuntimeException is perfectly reasonable in this case. */
    })
    public String getOption() {
      final Function<Path, FileTime> createTimeComp =
          path -> {
            try {
              return Files.readAttributes(path, BasicFileAttributes.class).creationTime();
            } catch (IOException e) {
              throw new RuntimeException(UNABLE_TO_DETERMINE_EXAM_DIR_ERROR, e);
            }
          };

      try (final Stream<Path> dirContents = Files.list(UNPACK_DIRECTORY.toPath())) {
        return dirContents
            .max(Comparator.comparing(createTimeComp))
            .map(Path::toAbsolutePath)
            .map(Path::toString)
            .map(
                s -> {
                  LOGGER.error(s);
                  return s;
                })
            .map(s -> StringUtils.replace(super.getOption(), KARAF_HOME, s))
            .map(s -> s.replace('\\', '/'))
            .map(s -> s.replace("/bin/..", "/"))
            .map(s -> s.replace("c:", "C:"))
            .map(s -> s.replace("C:", "/C:"))
            .map(s -> s + "/")
            .orElseGet(super::getOption);
      } catch (IOException e) {
        throw new RuntimeException(UNABLE_TO_DETERMINE_EXAM_DIR_ERROR, e);
      }
    }
  }

  /**
   * Helper Option class to allow interpolation of {@code karaf.home} directory based on the
   * provided {@link #UNPACK_DIRECTORY}.
   */
  protected static class HomePermVmOption extends VMOption {
    public static HomePermVmOption homePermVmOption(String option) {
      return new HomePermVmOption(option);
    }

    private HomePermVmOption(String option) {
      super(option);
    }

    @Override
    @SuppressWarnings({
      "squid:S00112" /* A generic RuntimeException is perfectly reasonable in this case. */
    })
    public String getOption() {
      final Function<Path, FileTime> createTimeComp =
          path -> {
            try {
              return Files.readAttributes(path, BasicFileAttributes.class).creationTime();
            } catch (IOException e) {
              throw new RuntimeException(UNABLE_TO_DETERMINE_EXAM_DIR_ERROR, e);
            }
          };

      try (final Stream<Path> dirContents = Files.list(UNPACK_DIRECTORY.toPath())) {
        return dirContents
            .max(Comparator.comparing(createTimeComp))
            .map(Path::toAbsolutePath)
            .map(Path::toString)
            .map(
                s -> {
                  LOGGER.error(s);
                  return s;
                })
            .map(s -> StringUtils.replace(super.getOption(), KARAF_HOME, s))
            .map(s -> s.replace(File.separator + "bin" + File.separator + "..", File.separator))
            .map(s -> s + File.separator)
            .orElseGet(super::getOption);
      } catch (IOException e) {
        throw new RuntimeException(UNABLE_TO_DETERMINE_EXAM_DIR_ERROR, e);
      }
    }
  }

  private static class TestMethodTimer extends Stopwatch {
    @Override
    protected void succeeded(long nanos, Description description) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "{}.{} execution time: {}",
            description.getClassName(),
            description.getMethodName(),
            DurationFormatUtils.formatDuration(TimeUnit.NANOSECONDS.toMillis(nanos), "HH:mm:ss.S"));
      }
    }
  }
}
