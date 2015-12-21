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
package ddf.test.itests.platform;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.options;
import static ddf.common.test.WaitCondition.expect;
import static ddf.common.test.matchers.ConfigurationPropertiesEqualTo.equalToConfigurationProperties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.codice.ddf.configuration.status.ConfigurationFileException;
import org.codice.ddf.configuration.persistence.felix.FelixPersistenceStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.common.test.BeforeExam;
import ddf.common.test.KarafConsole;
import ddf.common.test.callables.GetConfigurationProperties;
import ddf.common.test.matchers.ConfigurationPropertiesEqualTo;
import ddf.test.itests.AbstractIntegrationTest;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestPlatform extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestPlatform.class);

    private static KarafConsole console;

    private static final String EXPORT_COMMAND = "platform:config-export";

    /**
     * Class that provides utility and assertion methods for a Managed Service Felix configuration
     * file.
     * <p/>
     * Note: Since we have custom code in the @{link FelixPersistenceStrategy} class to convert
     * floats and doubles, we cannot simply rely on what we read from the file to assert that the
     * Configuration object has the right properties. For this reason, we need to create our own
     * Dictionary of properties in {@link #getExpectedProperties()} and use that when doing our
     * assertions.
     */
    private static class ManagedServiceConfigFile {
        protected String pid;

        public ManagedServiceConfigFile(String pid) {
            this.pid = pid;
        }

        public String getResourcePath() {
            return String.format("/%s.config", pid);
        }

        /**
         * Copies the configuration file to the /etc directory.
         */
        public void addConfigurationFile() throws IOException {
            FileUtils.copyInputStreamToFile(getResourceAsStream(), getPathToEtcDirectory());
        }

        /**
         * Copies the configuration file to the /etc directory and waits for the
         * {@link org.osgi.service.cm.Configuration} object to be initialized with all the values
         * found in the configuration file.
         */
        public void addConfigurationFileAndWait(ConfigurationAdmin configAdmin) throws Exception {
            addConfigurationFile();

            expect("Configuration properties for PID " + pid + " to be set").within(20, SECONDS)
                    .until(new GetConfigurationProperties(configAdmin, "id", pid),
                            equalToConfigurationProperties(getFileProperties()));
        }

        /**
         * Asserts that the {@link org.osgi.service.cm.Configuration} object for the current PID
         * contains the expected properties.
         */
        public void assertConfigurationPropertiesSet(ConfigurationAdmin configAdmin)
                throws Exception {
            Dictionary<String, Object> properties = new GetConfigurationProperties(configAdmin,
                    "id",
                    pid).call();
            assertThat("No Configuration object exist for PID " + pid,
                    properties,
                    is(notNullValue()));
            assertPropertiesMatch(properties, getExpectedProperties());
        }

        /**
         * Asserts that the configuration file has been moved to the /etc/processed directory.
         */
        public void assertFileMovedToProcessedDirectory() {
            expect("File to be moved to /etc/processed directory").within(20, SECONDS)
                    .until(() -> getPathToProcessedDirectory().exists());
            assertThat(String.format("Configuration file %s has not been removed",
                    getPathToEtcDirectory().getAbsolutePath()),
                    getPathToEtcDirectory().exists(),
                    is(false));
        }

        /**
         * Asserts that the configuration file has been moved to the /etc/failed directory.
         */
        public void assertFileMovedToFailedDirectory() {
            expect("Waiting for file to be moved to /etc/failed directory").within(20, SECONDS)
                    .until(() -> getPathToFailedDirectory().exists());
            assertThat(String.format("Configuration file %s has not been removed",
                    getPathToEtcDirectory().getAbsolutePath()),
                    getPathToEtcDirectory().exists(),
                    is(false));
        }

        /**
         * Asserts that the properties in the {@link org.osgi.service.cm.Configuration} object
         * match what' expected.
         */
        protected void assertPropertiesMatch(Dictionary<String, Object> actualProperties,
                Dictionary<String, Object> expectedProperties) throws IOException {
            assertThat("Configuration properties do not match for PID " + pid,
                    actualProperties,
                    equalToConfigurationProperties(expectedProperties));
        }

        /**
         * Gets the properties from the configuration file using the
         * {@link FelixPersistenceStrategy#read(InputStream)} method. This is used when waiting for
         * the {@link #addConfigurationFileAndWait(ConfigurationAdmin)} when waiting for the
         * {@link org.osgi.service.cm.Configuration} object to be initialized.
         */
        protected Dictionary<String, Object> getFileProperties() throws Exception {
            return new FelixPersistenceStrategy().read(getResourceAsStream());
        }

        /**
         * Adds the {@code service.pid} property to the list of expected properties. Needed
         * because MSF configuration files do not have that property.
         */
        protected void addToExpectedProperties(Dictionary<String, Object> expectedProperties) {
            expectedProperties.put("service.pid", pid);
        }

        private Dictionary<String, Object> getExpectedProperties() {
            Dictionary<String, Object> properties = new Hashtable<>();

            addToExpectedProperties(properties);

            properties.put("id", pid);

            properties.put("property.string", "string");
            properties.put("property.boolean.true", true);
            properties.put("property.boolean.false", false);
            properties.put("property.int", 10);
            properties.put("property.long", 100L);
            properties.put("property.float", 10.5f);
            properties.put("property.double", 100.1234d);
            properties.put("property.array.strings", new String[] {"A", "B", "C"});
            properties.put("property.array.booleans", new Boolean[] {Boolean.TRUE, Boolean.FALSE});
            properties.put("property.array.ints", new Integer[] {10, 20, 30});
            properties.put("property.array.longs", new Long[] {100L, 200L, 300L});
            properties.put("property.array.floats", new Float[] {1.1f, 2.2f, 3.3f});
            properties.put("property.array.doubles", new Double[] {1.123, 2.234, 3.345});

            Vector<String> strings = new Vector<>();
            strings.add("A");
            strings.add("B");
            strings.add("C");
            properties.put("property.vector.strings", strings);

            Vector<Boolean> booleans = new Vector<>();
            booleans.add(Boolean.TRUE);
            booleans.add(Boolean.FALSE);
            properties.put("property.vector.booleans", booleans);

            Vector<Integer> ints = new Vector<>();
            ints.add(10);
            ints.add(20);
            ints.add(30);
            properties.put("property.vector.ints", ints);

            Vector<Long> longs = new Vector<>();
            longs.add(100L);
            longs.add(200L);
            longs.add(300L);
            properties.put("property.vector.longs", longs);

            Vector<Float> floats = new Vector<>();
            floats.add(1.1f);
            floats.add(2.2f);
            floats.add(3.3f);
            properties.put("property.vector.floats", floats);

            Vector<Double> doubles = new Vector<>();
            doubles.add(1.123);
            doubles.add(2.234);
            doubles.add(3.345);
            properties.put("property.vector.doubles", doubles);

            return properties;
        }

        private File getPathToProcessedDirectory() {
            return new File(String.format("%s/etc/processed%s", ddfHome, getResourcePath()));
        }

        private File getPathToFailedDirectory() {
            return new File(String.format("%s/etc/failed%s", ddfHome, getResourcePath()));
        }

        private File getPathToEtcDirectory() {
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

        /**
         * {@inheritDoc}
         * <p/>
         * Overridden to use {@link ManagedServiceFactoryConfigurationPropertiesEqualTo}.
         */
        @Override
        protected void assertPropertiesMatch(Dictionary<String, Object> actualProperties,
                Dictionary<String, Object> expectedProperties) throws IOException {
            assertThat("Configuration properties do not match for PID " + pid,
                    actualProperties,
                    new ManagedServiceFactoryConfigurationPropertiesEqualTo(factoryPid,
                            expectedProperties));
        }

        /**
         * {@inheritDoc}
         * <p/>
         * Overridden to use {@link ManagedServiceFactoryConfigurationPropertiesEqualTo}.
         */
        @Override
        public void addConfigurationFileAndWait(ConfigurationAdmin configAdmin) throws Exception {
            addConfigurationFile();

            expect("Waiting for Configuration expectedProperties for PID " + pid
                    + " to be set").within(20, SECONDS)
                    .until(new GetConfigurationProperties(configAdmin, "id", pid),
                            new ManagedServiceFactoryConfigurationPropertiesEqualTo(factoryPid,
                                    getFileProperties()));
        }

        /**
         * Adds the {@code service.factoryPid} property to the list of expected properties. Needed
         * because MSF configuration files use that property instead of {@code service.pid}.
         */
        @Override
        protected void addToExpectedProperties(Dictionary<String, Object> expectedProperties) {
            expectedProperties.put("service.factoryPid", factoryPid);
        }
    }

    /**
     * Hamcrest {@link org.hamcrest.Matcher} class for Managed Service Factory properties. Ensures
     * that the {@code service.pid} property starts with the {@code factoryPid}.
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

    private static ManagedServiceConfigFile managedServiceStartupConfig =
            new ManagedServiceConfigFile("ddf.test.itests.platform.TestPlatform.startup");

    private static ManagedServiceConfigFile managedServiceNewConfig1 = new ManagedServiceConfigFile(
            "ddf.test.itests.platform.TestPlatform.new.1");

    private static ManagedServiceConfigFile managedServiceNewConfig2 = new ManagedServiceConfigFile(
            "ddf.test.itests.platform.TestPlatform.new.2");

    private static ManagedServiceConfigFile managedServiceFactoryStartupConfig =
            new ManagedServiceFactoryConfigFile("ddf.test.itests.platform.TestPlatform.msf.1");

    private static ManagedServiceConfigFile managedServiceFactoryNewConfig =
            new ManagedServiceFactoryConfigFile("ddf.test.itests.platform.TestPlatform.msf.2");

    private static ManagedServiceConfigFile configWithNoPid = new ManagedServiceConfigFile(
            "ddf.test.itests.platform.TestPlatform.nopid");

    private static ManagedServiceConfigFile invalidConfig = new ManagedServiceConfigFile(
            "ddf.test.itests.platform.TestPlatform.invalid");

    private static ManagedServiceConfigFile invalidStartupConfigFile = new ManagedServiceConfigFile(
            "ddf.test.itests.platform.TestPlatform.startup.invalid");

    @BeforeExam
    @SuppressWarnings("unchecked")
    public void beforeExam() throws Exception {
        getAdminConfig().setLogLevels();
        getServiceManager().waitForAllBundles();
        console = new KarafConsole(bundleCtx);
    }

    /**
     * Installs the configuration files needed at startup.
     */
    @Override
    protected Option[] configureCustom() {
        String managedServiceConfigPath = managedServiceStartupConfig.getResourcePath();
        String managedServiceFactoryConfigPath =
                managedServiceFactoryStartupConfig.getResourcePath();
        String invalidConfigPath = invalidStartupConfigFile.getResourcePath();

        try {
            return options(installStartupFile(getClass().getResourceAsStream(
                    managedServiceConfigPath), "/etc" + managedServiceConfigPath),
                    installStartupFile(getClass().getResourceAsStream(
                            managedServiceFactoryConfigPath),
                            "/etc" + managedServiceFactoryConfigPath),
                    installStartupFile(getClass().getResourceAsStream(invalidConfigPath),
                            "/etc" + invalidConfigPath));
        } catch (Exception e) {
            LOGGER.error("Could not copy config files {}, {} and {} to /etc directory",
                    managedServiceConfigPath,
                    managedServiceFactoryConfigPath,
                    invalidConfigPath);
            return null;
        }
    }

    @Test
    public void testStartUpWithExistingManagedServiceConfigurationFile() throws Exception {
        managedServiceStartupConfig.assertConfigurationPropertiesSet(configAdmin);
        managedServiceStartupConfig.assertFileMovedToProcessedDirectory();
    }

    @Test
    public void testCreateNewManagedServiceConfigurationFile() throws Exception {
        managedServiceNewConfig1.addConfigurationFileAndWait(configAdmin);
        managedServiceNewConfig1.assertFileMovedToProcessedDirectory();
    }

    @Test
    public void testStartUpWithExistingManagedServiceFactoryConfigurationFile() throws Exception {
        managedServiceFactoryStartupConfig.assertConfigurationPropertiesSet(configAdmin);
        managedServiceFactoryStartupConfig.assertFileMovedToProcessedDirectory();
    }

    @Test
    public void testCreateNewManagedServiceFactoryConfigurationFile() throws Exception {
        managedServiceFactoryNewConfig.addConfigurationFileAndWait(configAdmin);
        managedServiceFactoryNewConfig.assertFileMovedToProcessedDirectory();
    }

    @Test
    public void testStartUpWithInvalidFile() {
        invalidStartupConfigFile.assertFileMovedToFailedDirectory();
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

    @Test
    public void testExport() throws ConfigurationFileException, IOException {
        FileUtils.deleteDirectory(getExportDirectory().toFile());

        console.runCommand(EXPORT_COMMAND);

        assertThat(getExportSubDirectory("system.properties").toFile()
                .exists(), is(true));
        assertThat(getExportSubDirectory("users.properties").toFile()
                .exists(), is(true));
        assertThatDirectoryContains(getExportSubDirectory("keystores"),
                "serverKeystore.jks",
                "serverTruststore.jks");
        assertThatDirectoryContains(getExportSubDirectory("pdp", "policies"), "access-policy.xml");
        assertThatDirectoryContains(getExportSubDirectory("ws-security"),
                "attributeMap.properties",
                "issuer",
                "server");
        assertThatDirectoryContains(getExportSubDirectory("ws-security", "issuer"),
                "encryption.properties",
                "signature.properties");
        assertThatDirectoryContains(getExportSubDirectory("ws-security", "server"),
                "encryption.properties",
                "signature.properties");
    }

    @Test
    public void testExportOnTopOfFile() throws ConfigurationFileException, IOException {
        FileUtils.deleteDirectory(getExportDirectory().toFile());
        File file = getExportDirectory().toFile();
        file.createNewFile();
        String response = console.runCommand(EXPORT_COMMAND);
        assertThat(String.format("Should not have been able to export to %s.",
                getExportDirectory()),
                response,
                containsString(String.format("Failed to export all configurations to %s",
                        getExportDirectory())));
        file.delete();
    }

    /**
     * Tests that a saved configuration will be exported
     *
     * @throws Exception
     */
    @Test
    public void textExportAfterSavingAConfiguration() throws Exception {
        FileUtils.deleteDirectory(getExportDirectory().toFile());

        managedServiceNewConfig1.addConfigurationFileAndWait(configAdmin);
        console.runCommand(EXPORT_COMMAND);

        assertThat("Saved configuration should be exported.", getPathToExportedConfig(
                managedServiceNewConfig1.pid).toFile()
                .isFile(), is(true));
    }

    /**
     * Tests that deleted configurations are not exported
     *
     * @throws Exception
     */
    @Test
    public void testExportAfterDeletingAConfiguration() throws Exception {
        FileUtils.deleteDirectory(getExportDirectory().toFile());
        managedServiceNewConfig2.addConfigurationFileAndWait(configAdmin);
        configAdmin.getConfiguration(managedServiceNewConfig2.pid, null)
                .delete();
        console.runCommand(EXPORT_COMMAND);

        assertThat("Deleted configuration should not be exported.", getPathToExportedConfig(
                managedServiceNewConfig2.pid).toFile()
                .isFile(), is(false));
    }

    /**
     * Tests that absolute paths pointing outside ddfHome fail to export
     *
     * @throws Exception
     */
    @Test
    public void testFailureForAbsolutePathOutsideDdfHome() throws Exception {
        FileUtils.deleteDirectory(getExportDirectory().toFile());
        File systemProperties = new File(ddfHome + "/etc/system.properties");
        File testFile = new File("../cat.txt");
        FileUtils.copyFile(systemProperties, testFile);

        System.setProperty("javax.net.ssl.keyStore", ddfHome + "/../cat.txt");
        String response = console.runCommand(EXPORT_COMMAND);
        assertThat(String.format("Should not have been able to export to %s.",
                getExportDirectory()),
                response,
                containsString(String.format("Failed to export all configurations to %s",
                        getExportDirectory())));
        System.setProperty("javax.net.ssl.keyStore", "etc/keystores/serverKeystore.jks");
        testFile.delete();
    }

    /**
     * Tests that absolute paths pointing inside ddfHome fail to export
     *
     * @throws Exception
     */
    @Test
    public void testFailureForAbsolutePathInsideDdfHome() throws Exception {
        FileUtils.deleteDirectory(getExportDirectory().toFile());

        System.setProperty("javax.net.ssl.keyStore", ddfHome + "/etc/keystores/serverKeystore.jks");
        String response = console.runCommand(EXPORT_COMMAND);
        assertThat(String.format("Should not have been able to export to %s.",
                getExportDirectory()),
                response,
                containsString(String.format("Failed to export all configurations to %s",
                        getExportDirectory())));
        System.setProperty("javax.net.ssl.keyStore", "etc/keystores/serverKeystore.jks");
        FileUtils.deleteDirectory(getExportDirectory().toFile());
    }

    /**
     * Tests that relative paths that point outside ddfHom fails to export
     *
     * @throws Exception
     */
    @Test
    public void testRelativePathOutsideDdfHome() throws Exception {
        FileUtils.deleteDirectory(getExportDirectory().toFile());
        File systemProperties = new File(ddfHome + "/etc/system.properties");
        File testFile = new File("../cat.txt");
        FileUtils.copyFile(systemProperties, testFile);

        System.setProperty("javax.net.ssl.keyStore", "../cat.txt");
        String response = console.runCommand(EXPORT_COMMAND);
        assertThat(String.format("Should not have been able to export to %s.",
                getExportDirectory()),
                response,
                containsString(String.format("Failed to export all configurations to %s",
                        getExportDirectory())));
        System.setProperty("javax.net.ssl.keyStore", "etc/keystores/serverKeystore.jks");
        testFile.delete();
    }

    /**
     * Test that exporting twice overrides the previous files
     *
     * @throws Exception
     */
    @Test
    public void testExportOverridesPreviousExport() throws Exception {
        FileUtils.deleteDirectory(getExportDirectory().toFile());

        String firstExportMessage = console.runCommand(EXPORT_COMMAND);
        File firstExport = getExportSubDirectory("system.properties").toFile();
        long firstLength = firstExport.length();
        File systemProperties = Paths.get(ddfHome, "etc", "system.properties")
                .toFile();

        // back up sys.prop to restore after appending to it
        File systemPropertiesCopy = Paths.get(ddfHome, "etc", "system.properties.copy")
                .toFile();
        FileUtils.copyFile(systemProperties, systemPropertiesCopy);

        FileUtils.writeStringToFile(systemProperties, "testtesttest", true);

        String secondExportMessage = console.runCommand(EXPORT_COMMAND);
        File secondExport = getExportSubDirectory("system.properties").toFile();
        long secondLength = secondExport.length();

        assertThat("The first export failed to export",
                firstExportMessage,
                not(containsString("Failed to export all configurations")));
        assertThat("The second export failed to export",
                secondExportMessage,
                not(containsString("Failed to export all configurations")));
        assertThat("The second failed to modify the first export's files.",
                firstLength,
                is(not(equalTo(secondLength))));

        // restore sys.prop
        FileUtils.copyFile(systemPropertiesCopy, systemProperties);
        systemPropertiesCopy.delete();
    }

    /**
     * Returns the default location for exported configuration files
     *
     * @param pid PID of the configuration file
     * @return Full path to the exported configuration file
     */
    private Path getPathToExportedConfig(String pid) {
        return getExportSubDirectory(pid + ".config");
    }

    private Path getExportDirectory() {
        return Paths.get(ddfHome, "etc", "exported");
    }

    private Path getExportSubDirectory(String... paths) {
        Path directory = getExportDirectory().resolve("etc");

        for (String path : paths) {
            directory = directory.resolve(path);
        }

        return directory;
    }

    private void assertThatDirectoryContains(Path path, String... fileNames) {
        String[] keystoreFiles = path.toFile()
                .list();
        assertThat("Exported files should not be null.", keystoreFiles, is(notNullValue()));
        assertThat(String.format("Files missing in %s directory", path.toString()),
                keystoreFiles,
                arrayContainingInAnyOrder(fileNames));
    }
}
