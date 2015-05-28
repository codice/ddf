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

import com.jayway.restassured.response.Response;
import ddf.catalog.CatalogFramework;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.FederatedSource;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.osgi.BlueprintListener;
import org.apache.karaf.shell.osgi.BlueprintListener.BlueprintState;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.jayway.restassured.RestAssured.get;
import static org.junit.Assert.fail;
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

/**
 * Abstract integration test with helper methods and configuration at the container level.
 */
public abstract class AbstractIntegrationTest {

    private static final int CONFIG_UPDATE_WAIT_INTERVAL = 5;

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    protected static final String LOG_CONFIG_PID = "org.ops4j.pax.logging";

    protected static final String LOGGER_PREFIX = "log4j.logger.";

    protected static final String KARAF_VERSION = "2.3.8";

    protected static final int ONE_MINUTE_MILLIS = 60000;

    protected static final int FIVE_MINUTES_MILLIS = ONE_MINUTE_MILLIS * 5;

    // TODO: Use the Camel AvailablePortFinder.getNextAvailable() test method
    protected static final String HTTP_PORT = "9081";

    protected static final String HTTPS_PORT = "9993";

    protected static final String SSH_PORT = "9101";

    protected static final String RMI_SERVER_PORT = "44445";

    protected static final String RMI_REG_PORT = "1100";

    protected static final String SERVICE_ROOT = "https://localhost:" + HTTPS_PORT + "/services";

    protected static final String REST_PATH = SERVICE_ROOT + "/catalog/";

    protected static final String OPENSEARCH_PATH = REST_PATH + "query";

    protected static final String DEFAULT_LOG_LEVEL = "TRACE";

    protected String logLevel = "";

    public static final String TEST_LOGLEVEL_PROPERTY = "org.codice.test.defaultLoglevel";

    @Rule
    public TestName testName = new TestName();

    @Inject
    protected BundleContext bundleCtx;

    private BlueprintListener blueprintListener;

    @Inject
    protected ConfigurationAdmin configAdmin;

    @Inject
    protected FeaturesService features;

    @Inject
    protected MetaTypeService metatype;

    // Fields used across all test methods must be static.
    // PAX-EXAM wipes away field information before each test method.
    protected static CatalogProvider catalogProvider;

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
        return options(
                logLevel(LogLevelOption.LogLevel.INFO),
                keepRuntimeFolder(),
                useOwnExamBundlesStartLevel(100),
                // increase timeout for TravisCI
                systemTimeout(TimeUnit.MINUTES.toMillis(10)));
    }

    protected Option[] configureAdditionalBundles() {
        return options(
                junitBundles(),
                // HACK: incorrect version exported to override hamcrest-core from exam
                // feature which causes a split package issue for rest-assured
                wrappedBundle(mavenBundle("org.hamcrest", "hamcrest-all").versionAsInProject())
                        .exports("*;version=1.3.0.10"),
                mavenBundle("ddf.test.thirdparty", "rest-assured").versionAsInProject());
    }

    protected Option[] configureConfigurationPorts() throws URISyntaxException {
        return options(
                editConfigurationFilePut("etc/system.properties", "urlScheme", "https"),
                editConfigurationFilePut("etc/system.properties", "host", "localhost"),
                editConfigurationFilePut("etc/system.properties", "jetty.port", HTTPS_PORT),
                editConfigurationFilePut("etc/system.properties", "hostContext", "/solr"),
                editConfigurationFilePut("etc/system.properties", "ddf.home", "${karaf.home}"),
                editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", SSH_PORT),
                editConfigurationFilePut("etc/ddf.platform.config.cfg", "port", HTTP_PORT),
                editConfigurationFilePut("etc/org.ops4j.pax.web.cfg",
                        "org.osgi.service.http.port", HTTP_PORT),
                editConfigurationFilePut("etc/org.ops4j.pax.web.cfg",
                        "org.osgi.service.http.port.secure", HTTPS_PORT),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg",
                        "rmiRegistryPort", RMI_REG_PORT),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg",
                        "rmiServerPort", RMI_SERVER_PORT),
                replaceConfigurationFile("etc/hazelcast.xml", new File(this.getClass()
                        .getResource("/hazelcast.xml").toURI())),
                replaceConfigurationFile("etc/ddf.security.sts.client.configuration.cfg",
                        new File(this.getClass()
                        .getResource("/ddf.security.sts.client.configuration.cfg").toURI())),
                replaceConfigurationFile("etc/ddf.catalog.solr.external.SolrHttpCatalogProvider.cfg",
                        new File(this.getClass()
                                .getResource("/ddf.catalog.solr.external.SolrHttpCatalogProvider.cfg").toURI())));
    }

    protected Option[] configureMavenRepos() {
        return options(
                editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg",
                        "org.ops4j.pax.url.mvn.repositories",
                        "http://repo1.maven.org/maven2@id=central,"
                                + "http://oss.sonatype.org/content/repositories/snapshots@snapshots@noreleases@id=sonatype-snapshot,"
                                + "http://oss.sonatype.org/content/repositories/ops4j-snapshots@snapshots@noreleases@id=ops4j-snapshot,"
                                + "http://repository.apache.org/content/groups/snapshots-group@snapshots@noreleases@id=apache,"
                                + "http://svn.apache.org/repos/asf/servicemix/m2-repo@id=servicemix,"
                                + "http://repository.springsource.com/maven/bundles/release@id=springsource,"
                                + "http://repository.springsource.com/maven/bundles/external@id=springsourceext,"
                                + "http://oss.sonatype.org/content/repositories/releases/@id=sonatype"));
    }

    protected Option[] configureSystemSettings() {
        return options(
                when(System.getProperty(TEST_LOGLEVEL_PROPERTY) != null).useOptions(
                        systemProperty(TEST_LOGLEVEL_PROPERTY)
                                .value(System.getProperty(TEST_LOGLEVEL_PROPERTY, ""))),
                when(Boolean.getBoolean("isDebugEnabled")).useOptions(
                        vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005")),
                when(System.getProperty("maven.repo.local") != null).useOptions(
                        systemProperty("org.ops4j.pax.url.mvn.localRepository")
                                .value(System.getProperty("maven.repo.local", ""))));
    }

    protected Option[] configureVmOptions() {
        return options(
                vmOption("-Xmx2048M"),
                vmOption("-XX:PermSize=128M"),
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
                        "mvn:ddf.sdk/sdk-app/2.3.0.ALPHA1-SNAPSHOT/xml/features"));

    }

    /**
     * Allows extending classes to add any custom options to the configuration.
     */
    protected Option[] configureCustom() {
        return null;
    }

    /**
     * Creates a Managed Service that is created from a Managed Service Factory. Waits for the
     * asynchronous call that the properties have been updated and the service can be used.
     *
     * @param factoryPid the factory pid of the Managed Service Factory
     * @param properties the service properties for the Managed Service
     * @throws IOException          if access to persistent storage fails
     * @throws InterruptedException
     */
    public void createManagedService(String factoryPid, Map<String,
            Object> properties) throws IOException, InterruptedException {

        final Configuration sourceConfig = configAdmin.createFactoryConfiguration(factoryPid, null);

        ServiceConfigurationListener listener = new ServiceConfigurationListener(
                sourceConfig.getPid());

        bundleCtx.registerService(ConfigurationListener.class.getName(), listener, null);

        sourceConfig.update(new Hashtable<>(properties));

        long millis = 0;
        while (!listener.isUpdated() && millis < TimeUnit.MINUTES.toMillis(5)) {
            try {
                Thread.sleep(CONFIG_UPDATE_WAIT_INTERVAL);
                millis += CONFIG_UPDATE_WAIT_INTERVAL;
            } catch (InterruptedException e) {
                LOGGER.info("Interrupted exception while trying to sleep for configuration update",
                        e);
            }
            LOGGER.info("Waiting for configuration to be updated...{}ms", millis);
        }

        if (!listener.isUpdated()) {
            throw new RuntimeException("Service was not updated before timeout.");
        }
    }

    protected void setLogLevels() throws IOException {

        logLevel = System.getProperty(TEST_LOGLEVEL_PROPERTY);

        Configuration logConfig = configAdmin.getConfiguration(LOG_CONFIG_PID, null);
        Dictionary<String, Object> properties = logConfig.getProperties();
        if (StringUtils.isEmpty(logLevel)) {
            properties.put(LOGGER_PREFIX + "ddf", DEFAULT_LOG_LEVEL);
            properties.put(LOGGER_PREFIX + "org.codice", DEFAULT_LOG_LEVEL);
        } else {
            properties.put(LOGGER_PREFIX + "*", logLevel);
        }

        logConfig.update(properties);
    }

    protected void setFanout(boolean fanoutEnabled) throws IOException {
        Configuration configuration = configAdmin.getConfiguration(
                "ddf.catalog.CatalogFrameworkImpl", null);

        Dictionary<String, Object> properties =  configuration.getProperties();
        if(properties == null)
        {
            properties = new Hashtable<String, Object>();
        }
        if(fanoutEnabled) {
            properties.put("fanoutEnabled", "True");
        } else {
            properties.put("fanoutEnabled", "False");
        }

        configuration.update(properties);
    }

    protected void waitForAllBundles() throws InterruptedException {
        waitForRequiredBundles("");
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
                    String bundleName = bundle.getHeaders().get(Constants.BUNDLE_NAME);
                    String blueprintState = blueprintListener.getState(bundle);
                    if (blueprintState != null) {
                        if (BlueprintState.Failure.toString().equals(blueprintState)) {
                            fail("The blueprint for " + bundleName + " failed.");
                        } else if (!BlueprintState.Created.toString().equals(blueprintState)) {
                            LOGGER.info("{} blueprint not ready with state {}", bundleName,
                                    blueprintState);
                            ready = false;
                        }
                    }

                    if (!((bundle.getHeaders().get("Fragment-Host") != null
                            && bundle.getState() == Bundle.RESOLVED) || bundle
                            .getState() == Bundle.ACTIVE)) {
                        LOGGER.info("{} bundle not ready yet", bundleName);
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

    protected CatalogProvider waitForCatalogProvider() throws InterruptedException {
        LOGGER.info("Waiting for CatalogProvider to become available.");

        CatalogProvider provider = null;
        long timeoutLimit = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        boolean available = false;

        while (!available) {
            if (provider == null) {
                ServiceReference<CatalogProvider> providerRef = bundleCtx.getServiceReference(
                        CatalogProvider.class);
                if (providerRef != null) {
                    provider = bundleCtx.getService(providerRef);
                }
            }

            if (provider != null) {
                available = provider.isAvailable();
            }
            if (!available) {
                if (System.currentTimeMillis() > timeoutLimit) {
                    fail("Catalog provider timed out.");
                }
                Thread.sleep(100);
            }
        }

        LOGGER.info("CatalogProvider is available.");
        return provider;
    }

    protected FederatedSource waitForFederatedSource(String id) throws InterruptedException,
            InvalidSyntaxException {
        LOGGER.info("Waiting for FederatedSource {} to become available.", id);

        FederatedSource source = null;
        long timeoutLimit = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        boolean available = false;

        while (!available) {
            if (source == null) {
                Collection<ServiceReference<FederatedSource>> srcRefs = bundleCtx
                        .getServiceReferences(FederatedSource.class, null);
                for (ServiceReference<FederatedSource> srcRef : srcRefs) {
                    FederatedSource src = bundleCtx.getService(srcRef);
                    if (id.equals(src.getId())) {
                        source = src;
                    }
                }
            }

            if (source != null) {
                available = source.isAvailable();
            }

            if (!available) {
                if (System.currentTimeMillis() > timeoutLimit) {
                    fail("Federated Source was not created in a timely manner.");
                }
                Thread.sleep(100);
            }
        }

        LOGGER.info("FederatedSource {} is available.", id);
        return source;
    }

    protected void waitForHttpEndpoint(String path) throws InterruptedException {
        LOGGER.info("Waiting for {}", path);

        long timeoutLimit = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        boolean available = false;

        while (!available) {
            Response response = get(path);
            available = response.getStatusCode() == 200 && response.getBody().print().length() > 0;
            if (!available) {
                if (System.currentTimeMillis() > timeoutLimit) {
                    fail(path + " did not start in time.");
                }
                Thread.sleep(100);
            }
        }

        LOGGER.info("{} ready.", path);
    }

    protected void waitForSourcesToBeAvailable(String... sources) throws InterruptedException {
        String path = REST_PATH + "sources";
        LOGGER.info("Waiting for sources at {}", path);

        long timeoutLimit = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        boolean available = false;

        while (!available) {
            Response response = get(path);
            String body = response.getBody().asString();
            if (StringUtils.isNotBlank(body)) {
                available = response.getStatusCode() == 200 && body.length() > 0
                        && !body.contains("false") && response.getBody().jsonPath().getList("id")
                        != null;
                if (available) {
                    List<Object> ids = response.getBody().jsonPath().getList("id");
                    for (String source : sources) {
                        if (!ids.contains(source)) {
                            available = false;
                        }
                    }
                }
            }
            if (!available) {
                if (System.currentTimeMillis() > timeoutLimit) {
                    response.prettyPrint();
                    fail("Sources at " + path + " did not start in time.");
                }
                Thread.sleep(1000);
            }
        }

        LOGGER.info("Sources at {} ready.", path);
    }

    protected CatalogFramework getCatalogFramework() throws InterruptedException {
        LOGGER.info("getting framework");

        CatalogFramework catalogFramework = null;

        ServiceReference<CatalogFramework> providerRef = bundleCtx
                .getServiceReference(CatalogFramework.class);
        if (providerRef != null) {
            catalogFramework = bundleCtx.getService(providerRef);
        }

        LOGGER.info("CatalogProvider is available.");
        return catalogFramework;
    }

    protected Map<String, Object> getMetatypeDefaults(String symbolicName, String factoryPid) {
        Map<String, Object> properties = new HashMap<>();
        ObjectClassDefinition bundleMetatype = getObjectClassDefinition(symbolicName, factoryPid);

        for (AttributeDefinition attributeDef : bundleMetatype.getAttributeDefinitions(
                ObjectClassDefinition.ALL)) {
            if (attributeDef.getID() != null) {
                if (attributeDef.getDefaultValue() != null) {
                    if (attributeDef.getCardinality() == 0) {
                        properties.put(attributeDef.getID(), getAttributeValue(attributeDef.getDefaultValue()[0], attributeDef.getType()));
                    } else {
                        properties.put(attributeDef.getID(), attributeDef.getDefaultValue());
                    }
                } else if (attributeDef.getCardinality() != 0) {
                    properties.put(attributeDef.getID(), new String[0]);
                }
            }
        }

        return properties;
    }

    private Object getAttributeValue(String value, int type) {
        switch (type) {
        case AttributeDefinition.BOOLEAN:
            return Boolean.valueOf(value);
        case AttributeDefinition.BYTE:
            return Byte.valueOf(value);
        case AttributeDefinition.DOUBLE:
            return Double.valueOf(value);
        case AttributeDefinition.CHARACTER:
            return Character.valueOf(value.toCharArray()[0]);
        case AttributeDefinition.FLOAT:
            return Float.valueOf(value);
        case AttributeDefinition.INTEGER:
            return Integer.valueOf(value);
        case AttributeDefinition.LONG:
            return Long.valueOf(value);
        case AttributeDefinition.SHORT:
            return Short.valueOf(value);
        case AttributeDefinition.PASSWORD:
        case AttributeDefinition.STRING:
        default:
            return value;
        }
    }

    private ObjectClassDefinition getObjectClassDefinition(String symbolicName, String pid) {
        Bundle[] bundles = bundleCtx.getBundles();
        for (Bundle bundle : bundles) {
            if (symbolicName.equals(bundle.getSymbolicName())) {
                try {
                    MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
                    if (mti != null) {
                        try {
                            ObjectClassDefinition ocd = mti.getObjectClassDefinition(pid,
                                    Locale.getDefault().toString());
                            if (ocd != null) {
                                return ocd;
                            }
                        } catch (IllegalArgumentException e) {
                            // ignoring
                        }
                    }
                } catch (IllegalArgumentException iae) {
                    // ignoring
                }
            }
        }
        return null;
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
            if (event.getPid().equals(pid) && ConfigurationEvent.CM_UPDATED == event.getType()) {
                updated = true;
            }
        }

        public boolean isUpdated() {
            return updated;
        }
    }

}