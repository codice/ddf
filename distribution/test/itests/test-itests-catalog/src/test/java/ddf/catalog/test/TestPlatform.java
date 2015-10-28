/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.options;
import static ddf.common.test.matchers.ConfigurationPropertiesEqualTo.equalToConfigurationProperties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;

import org.apache.commons.io.FileUtils;
import org.apache.felix.cm.file.ConfigurationHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.common.test.BeforeExam;
import ddf.common.test.WaitCondition;
import ddf.common.test.callables.GetConfigurationProperties;
import ddf.common.test.matchers.ConfigurationPropertiesEqualTo;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestPlatform extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestPlatform.class);

    /**
     * Class that provides utility and assertion methods for a Managed Service Felix configuration
     * file.
     */
    private static class ManagedServiceConfigFile {
        protected String pid;

        public ManagedServiceConfigFile(String pid) {
            this.pid = pid;
        }

        public String getResourcePath() {
            return String.format("/%s.config", pid);
        }

        public void assertConfigurationPropertiesSet(ConfigurationAdmin configAdmin)
                throws Exception {
            Configuration configuration = configAdmin.getConfiguration(pid, null);
            assertThat("No Configuration object exist for PID " + pid, configuration,
                    is(notNullValue()));
            assertThat("Configuration properties do not match for PID " + pid,
                    configuration.getProperties(), equalToConfigurationProperties(getProperties()));
        }

        public void addConfigurationFile() throws IOException {
            FileUtils.copyInputStreamToFile(getResourceAsStream(), getFile());
        }

        public void addConfigurationFileAndWait(ConfigurationAdmin configAdmin) throws IOException {
            addConfigurationFile();

            new WaitCondition("Waiting for Configuration properties for PID " + pid + " to be set")
                    .waitFor(new GetConfigurationProperties(configAdmin, pid),
                            equalToConfigurationProperties(getProperties()));
        }

        public void assertFileMovedToProcessedDirectory() {
            assertThat(String.format("Configuration file %s has not been removed",
                    getFile().getAbsolutePath()), getFile().exists(), is(false));
            assertThat(String.format("Configuration file %s has not been moved to /etc/processed",
                    getFile().getAbsolutePath()), getProcessedFile().exists(), is(true));
        }

        public void assertFileMovedToFailedDirectory() {
            new WaitCondition("Waiting for file to be moved to /etc/failed directory")
                    .waitFor(() -> getFailedFile().exists());
        }

        @SuppressWarnings("unchecked")
        protected Dictionary<String, Object> getProperties() throws IOException {
            return ConfigurationHandler.read(getResourceAsStream());
        }

        private File getProcessedFile() {
            return new File(String.format("%s/etc/processed%s", ddfHome, getResourcePath()));
        }

        private File getFailedFile() {
            return new File(String.format("%s/etc/failed%s", ddfHome, getResourcePath()));
        }

        private File getFile() {
            return new File(String.format("%s/etc%s", ddfHome, getResourcePath()));
        }

        private InputStream getResourceAsStream() {
            return getClass().getResourceAsStream(getResourcePath());
        }
    }

    /**
     * Class that provides utility and assertion methods for a Managed Service Factory Felix
     * configuration file.
     */
    private static class ManagedServiceFactoryConfigFile extends ManagedServiceConfigFile {

        private final String factoryPid;

        public ManagedServiceFactoryConfigFile(String pid) {
            super(pid);
            this.factoryPid = pid.substring(0, pid.lastIndexOf('.'));
        }

        public void assertConfigurationPropertiesSet(ConfigurationAdmin configAdmin)
                throws Exception {
            Configuration[] configurations = configAdmin
                    .listConfigurations("(service.pid=" + factoryPid + "*)");
            Configuration configuration = configurations[0];
            assertThat("No Configuration object exist for PID " + pid, configuration,
                    is(notNullValue()));
            assertThat("Configuration properties do not match for PID " + pid,
                    configuration.getProperties(),
                    new ManagedServiceFactoryConfigurationPropertiesEqualTo(factoryPid,
                            getProperties()));
        }

        @Override
        public void addConfigurationFileAndWait(ConfigurationAdmin configAdmin) throws IOException {
            addConfigurationFile();

            final Dictionary<String, Object> expectedProperties = getProperties();

            new WaitCondition(
                    "Waiting for Configuration expectedProperties for PID " + pid + " to be set")
                    .waitFor(new GetConfigurationProperties(configAdmin, pid),
                            new ManagedServiceFactoryConfigurationPropertiesEqualTo(factoryPid,
                                    expectedProperties));
        }
    }

    /**
     * Hamcrest {@link org.hamcrest.Matcher} class for Managed Service Factory properties. Compares
     * all the attributes except for the {@code service.pid} one.
     */
    private static class ManagedServiceFactoryConfigurationPropertiesEqualTo
            extends ConfigurationPropertiesEqualTo {
        private String factoryPid;

        public ManagedServiceFactoryConfigurationPropertiesEqualTo(String factoryPid,
                Dictionary<String, Object> expectedProperties) {
            super(expectedProperties);
            this.factoryPid = factoryPid;
        }

        @Override
        public boolean matches(Object object) {
            if ((object == null) || !(object instanceof Dictionary)) {
                return false;
            }

            @SuppressWarnings("unchecked")
            Dictionary<String, Object> properties = (Dictionary<String, Object>) object;

            if (properties.get("service.pid") == null) {
                return false;
            }

            if (!((String) properties.get("service.pid")).startsWith(factoryPid)) {
                return false;
            }

            properties.remove("service.pid");

            return super.matches(properties);
        }
    }

    private static ManagedServiceConfigFile managedServiceStartupConfig = new ManagedServiceConfigFile(
            "ddf.catalog.test.TestPlatform.startup");

    private static ManagedServiceConfigFile managedServiceNewConfig = new ManagedServiceConfigFile(
            "ddf.catalog.test.TestPlatform.new");

    private static ManagedServiceConfigFile managedServiceFactoryStartupConfig = new ManagedServiceFactoryConfigFile(
            "ddf.catalog.test.TestPlatform.msf.1");

    private static ManagedServiceConfigFile managedServiceFactoryNewConfig = new ManagedServiceFactoryConfigFile(
            "ddf.catalog.test.TestPlatform.msf.2");

    private static ManagedServiceConfigFile configWithNoPid = new ManagedServiceConfigFile(
            "ddf.catalog.test.TestPlatform.nopid");

    private static ManagedServiceConfigFile invalidConfig = new ManagedServiceConfigFile(
            "ddf.catalog.test.TestPlatform.invalid");

    @BeforeExam
    @SuppressWarnings("unchecked")
    public void beforeExam() throws Exception {
        getAdminConfig().setLogLevels();
        getServiceManager().waitForAllBundles();
    }

    @Override
    protected Option[] configureCustom() {
        try {
            return options(installStartupFile(getClass().getResourceAsStream(
                                    managedServiceStartupConfig.getResourcePath()),
                            "/etc" + managedServiceStartupConfig.getResourcePath()),
                    installStartupFile(getClass().getResourceAsStream(
                                    managedServiceFactoryStartupConfig.getResourcePath()),
                            "/etc" + managedServiceFactoryStartupConfig.getResourcePath()));
        } catch (Exception e) {
            LOGGER.error("Could not copy config files {} and {} to /etc directory",
                    managedServiceStartupConfig.getResourcePath(),
                    managedServiceFactoryStartupConfig.getResourcePath());
            return null;
        }
    }

    @Test
    public void testStartUpWithExistingManagedServiceConfigurationFile() throws Exception {
        managedServiceStartupConfig.assertConfigurationPropertiesSet(configAdmin);
        managedServiceStartupConfig.assertFileMovedToProcessedDirectory();
    }

    @Test
    public void testCreateNewManagedServiceConfigurationFile() throws IOException {
        managedServiceNewConfig.addConfigurationFileAndWait(configAdmin);
        managedServiceNewConfig.assertFileMovedToProcessedDirectory();
    }

    @Test
    public void testStartUpWithExistingManagedServiceFactoryConfigurationFile() throws Exception {
        managedServiceFactoryStartupConfig.assertConfigurationPropertiesSet(configAdmin);
        managedServiceFactoryStartupConfig.assertFileMovedToProcessedDirectory();
    }

    @Test
    public void testCreateNewManagedServiceFactoryConfigurationFile() throws IOException {
        managedServiceFactoryNewConfig.addConfigurationFileAndWait(configAdmin);
        managedServiceFactoryNewConfig.assertFileMovedToProcessedDirectory();
    }

    @Test
    public void testStartUpWithInvalidFile() {
        // TODO - Make sure things still come up when one of the startup files is invalid.
    }

    @Test
    public void testConfigurationFileWithNoFactoryOrServicePid() throws IOException {
        configWithNoPid.addConfigurationFile();
        configWithNoPid.assertFileMovedToFailedDirectory();
    }

    @Test
    public void testConfigurationFileWithInvalidFormat() throws IOException {
        invalidConfig.addConfigurationFile();
        invalidConfig.assertFileMovedToFailedDirectory();
    }
}
