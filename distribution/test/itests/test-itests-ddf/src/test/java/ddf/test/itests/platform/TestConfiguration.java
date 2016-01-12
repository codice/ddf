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
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.codice.ddf.configuration.persistence.felix.FelixPersistenceStrategy;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
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
import ddf.test.itests.catalog.TestCatalog;
import ddf.test.itests.common.Library;

/**
 * Note: Tests prefixed with aRunFirst NEED to run before any other tests.  For this reason, we
 * use the @FixMethodOrder(MethodSorters.NAME_ASCENDING) annotation.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestConfiguration extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestConfiguration.class);

    private static final String EXPORT_COMMAND = "platform:config-export";
    
    private static final String STATUS_COMMAND = "platform:config-status";

    private static final String SUCCESSFUL_IMPORT_MESSAGE = "All config files imported successfully.";

    private static final String FAILED_IMPORT_MESSAGE = "Failed to import file [%s]. ";

    private static final String INVALID_CONFIG_FILE_1 = "ddf.test.itests.platform.TestPlatform.invalid.config";

    private static final String INVALID_CONFIG_FILE_2 = "ddf.test.itests.platform.TestPlatform.startup.invalid.config";

    private static final String VALID_CONFIG_FILE_1 = "ddf.test.itests.platform.TestPlatform.startup.config";
    
    private static final String TEST_FILE = "../cat.txt";
    
    private static final Path SYSTEM_PROPERTIES = Paths.get("etc", "system.properties");
    
    private static final Path SYSTEM_PROPERTIES_COPY = Paths.get("etc", "system.properties.copy");
    
    private static final Path USERS_PROPERTIES = Paths.get("etc", "users.properties");
    
    private static final Path USERS_PROPERTIES_COPY = Paths.get("etc", "users.properties.copy");
    
    private static final Path WS_SECURITY = Paths.get("etc", "ws-security");
    
    private static final Path WS_SECURITY_COPY = Paths.get("etc", "ws-security-copy");
    
    private static final Path PDP = Paths.get("etc", "pdp");
    
    private static final Path PDP_COPY = Paths.get("etc", "pdp-copy");
    
    private static final String KEYSTORE_PROPERTY = "javax.net.ssl.keyStore";
    
    private static KarafConsole console;

    private static final String SEARCH_COMMAND = "catalog:search";

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
    public void beforeExam() throws Exception {
        basePort = getBasePort();
        getAdminConfig().setLogLevels();
        getServiceManager().waitForAllBundles();
        console = new KarafConsole(bundleCtx);
        basePort = getBasePort();
    }
    
    public void resetInitialState() throws Exception {

        FileUtils.deleteQuietly(getExportDirectory().toFile());
        FileUtils.deleteQuietly(new File(TEST_FILE));

        FileUtils.cleanDirectory(getPathToProcessedDirectory().toFile());
        FileUtils.cleanDirectory(getPathToFailedDirectory().toFile());

        restoreBackup(SYSTEM_PROPERTIES_COPY, SYSTEM_PROPERTIES);
        restoreBackup(USERS_PROPERTIES_COPY, USERS_PROPERTIES);
        restoreBackup(WS_SECURITY_COPY, WS_SECURITY);
        restoreBackup(PDP_COPY, PDP);

        System.setProperty(KEYSTORE_PROPERTY, "etc" + File.separator + "keystores"
                + File.separator + "serverKeystore.jks");
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
    public void aRunFirstTestStartUpWithExistingManagedServiceConfigurationFile() throws Exception {
        managedServiceStartupConfig.assertConfigurationPropertiesSet(configAdmin);
        managedServiceStartupConfig.assertFileMovedToProcessedDirectory();
    }

    @Test
    public void aRunFirstTestCreateNewManagedServiceConfigurationFile() throws Exception {
        managedServiceNewConfig1.addConfigurationFileAndWait(configAdmin);
        managedServiceNewConfig1.assertFileMovedToProcessedDirectory();
    }

    @Test
    public void aRunFirstTestStartUpWithExistingManagedServiceFactoryConfigurationFile() throws Exception {
        managedServiceFactoryStartupConfig.assertConfigurationPropertiesSet(configAdmin);
        managedServiceFactoryStartupConfig.assertFileMovedToProcessedDirectory();
    }
    
    @Test
    public void aRunFirstTestStartUpWithInvalidFile() {
        invalidStartupConfigFile.assertFileMovedToFailedDirectory();
    }

    @Test
    public void testCreateNewManagedServiceFactoryConfigurationFile() throws Exception {
        managedServiceFactoryNewConfig.addConfigurationFileAndWait(configAdmin);
        managedServiceFactoryNewConfig.assertFileMovedToProcessedDirectory();
    }

    @Test
    public void testConfigurationFileWithNoFactoryOrServicePid() throws IOException {
        configWithNoPid.addConfigurationFile();
        configWithNoPid.assertFileMovedToFailedDirectory();
    }

    @Test
    public void testConfigurationFileWithInvalidFormat() throws Exception {
        invalidConfig.addConfigurationFile();
        invalidConfig.assertFileMovedToFailedDirectory();
    }

    @Test
    public void testExport() throws Exception {
        resetInitialState();

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
                "encryption.properties", "signature.properties");
        assertThatDirectoryContains(getExportSubDirectory("ws-security", "server"),
                "encryption.properties", "signature.properties");
    }

    @Test
    public void testExportOnTopOfFile() throws Exception {
        resetInitialState();
        
        File file = getExportDirectory().toFile();
        file.createNewFile();

        String response = console.runCommand(EXPORT_COMMAND);
        
        assertThat(
                String.format("Should not have been able to export to %s.", getExportDirectory()),
                response, containsString("Unable to create export directories."));
    }

    @Test
    public void testExportOnTopOfNestedFile() throws Exception {
        resetInitialState();

        File file = getExportDirectory().toFile();
        file.mkdir();
        File fileEtc = getExportDirectory().resolve("etc").toFile();
        fileEtc.createNewFile();

        String response = console.runCommand(EXPORT_COMMAND);

        assertThat(
                String.format("Should not have been able to export to %s.", getExportDirectory()),
                response, containsString("Unable to create export directories."));
    }

    /**
     * Tests that a saved configuration will be exported
     *
     * @throws Exception
     */
    @Test
    public void testExportAfterSavingAConfiguration() throws Exception {
        resetInitialState();

        managedServiceNewConfig1.addConfigurationFileAndWait(configAdmin);
        
        console.runCommand(EXPORT_COMMAND);

        assertThat("Saved configuration should be exported.",
                getPathToExportedConfig(managedServiceNewConfig1.pid).toFile().isFile(), is(true));
    }

    /**
     * Tests that deleted configurations are not exported
     *
     * @throws Exception
     */
    @Test
    public void testExportAfterDeletingAConfiguration() throws Exception {
        resetInitialState();
        
        managedServiceNewConfig2.addConfigurationFileAndWait(configAdmin);
        configAdmin.getConfiguration(managedServiceNewConfig2.pid, null)
                .delete();
        
        console.runCommand(EXPORT_COMMAND);

        assertThat("Deleted configuration should not be exported.",
                getPathToExportedConfig(managedServiceNewConfig2.pid).toFile().isFile(), is(false));
    }

    /**
     * Tests that absolute paths pointing outside ddfHome causes a warning
     *
     * @throws Exception
     */
    @Test
    public void testExportWarningForAbsolutePathOutsideDdfHome() throws Exception {
        resetInitialState();
        
        FileUtils.copyFile(SYSTEM_PROPERTIES.toFile(), new File(TEST_FILE));
        System.setProperty(KEYSTORE_PROPERTY, ddfHome + File.separator + TEST_FILE);

        String response = console.runCommand(EXPORT_COMMAND);
        
        assertThat(
                String.format("Should not have been able to export to %s.", getExportDirectory()),
                response, containsString(String.format("Failed to export all configurations to %s",
                        getExportDirectory())));
    }

    /**
     * Tests that absolute paths pointing inside ddfHome causes a warning
     *
     * @throws Exception
     */
    @Test
    public void testExportWarningForAbsolutePathInsideDdfHome() throws Exception {
        resetInitialState();
        
        System.setProperty(KEYSTORE_PROPERTY, ddfHome + File.separator + "etc"
                + File.separator + "keystores" + File.separator + "serverKeystore.jks");

        String response = console.runCommand(EXPORT_COMMAND);
        assertThat(
                String.format("Should not have been able to export to %s.", getExportDirectory()),
                response, containsString(String.format("Failed to export all configurations to %s",
                        getExportDirectory())));
    }

    /**
     * Tests that relative paths that point outside ddfHome causes a warning
     *
     * @throws Exception
     */
    @Test
    public void testExportWarningForRelativePathOutsideDdfHome() throws Exception {
        resetInitialState();

        File systemProperties = new File(ddfHome + File.separator + "etc" + File.separator
                + "system.properties");
        File testFile = new File(TEST_FILE);
        FileUtils.copyFile(systemProperties, testFile);
        System.setProperty(KEYSTORE_PROPERTY, TEST_FILE);

        String response = console.runCommand(EXPORT_COMMAND);

        assertThat(
                String.format("Should not have been able to export to %s.", getExportDirectory()),
                response, containsString(String.format("Failed to export all configurations to %s",
                        getExportDirectory())));
    }

    /**
     * Tests that when system properties file is missing, export fails
     *
     * @throws Exception
     */
    @Test
    public void testExportFailureWithoutSystemPropertiesFile() throws Exception {
        resetInitialState();
        
        FileUtils.moveFile(SYSTEM_PROPERTIES.toFile(), SYSTEM_PROPERTIES_COPY.toFile());

        String response = console.runCommand(EXPORT_COMMAND);
        
        assertThat(
                String.format("Should not have been able to export to %s.", getExportDirectory()),
                response, containsString("An error was encountered while executing this command."));
    }

    /**
     * Tests that when system properties file is missing, export fails
     *
     * @throws Exception
     */
    @Test
    public void testExportFailureWithoutUsersPropertiesFile() throws Exception {
        resetInitialState();
        
        FileUtils.moveFile(USERS_PROPERTIES.toFile(), USERS_PROPERTIES_COPY.toFile());

        String response = console.runCommand(EXPORT_COMMAND);
        
        assertThat(
                String.format("Should not have been able to export to %s.", getExportDirectory()),
                response, containsString("An error was encountered while executing this command."));
    }

    /**
     * Tests that when system properties file is missing, export fails
     *
     * @throws Exception
     */
    @Test
    public void testExportFailureWithoutWSSecurityDirectory() throws Exception {
        resetInitialState();
        
        FileUtils.moveDirectory(WS_SECURITY.toFile(), WS_SECURITY_COPY.toFile());

        String response = console.runCommand(EXPORT_COMMAND);

        assertThat(
                String.format("Should not have been able to export to %s.", getExportDirectory()),
                response, containsString("An error was encountered while executing this command."));
    }

    /**
     * Tests that when system properties file is missing, export fails
     *
     * @throws Exception
     */
    @Test
    public void testExportFailureWithoutPDPDirectory() throws Exception {
        resetInitialState();
        
        FileUtils.moveDirectory(PDP.toFile(), PDP_COPY.toFile());

        String response = console.runCommand(EXPORT_COMMAND);
        assertThat(
                String.format("Should not have been able to export to %s.", getExportDirectory()),
                response, containsString("An error was encountered while executing this command."));
    }

    /**
     * Test that exporting twice overrides the previous files
     *
     * @throws Exception
     */
    @Test
    public void testExportOverridesPreviousExport() throws Exception {
        resetInitialState();
        
        String firstExportMessage = console.runCommand(EXPORT_COMMAND);
        
        File firstExport = getExportSubDirectory("system.properties").toFile();
        long firstLength = firstExport.length();
        FileUtils.copyFile(SYSTEM_PROPERTIES.toFile(), SYSTEM_PROPERTIES_COPY.toFile());
        FileUtils.writeStringToFile(SYSTEM_PROPERTIES.toFile(), "testtesttest", true);
        
        String secondExportMessage = console.runCommand(EXPORT_COMMAND);
        
        File secondExport = getExportSubDirectory("system.properties").toFile();
        long secondLength = secondExport.length();

        assertThat("The first export failed to export", firstExportMessage,
                not(containsString("Failed to export all configurations")));
        assertThat("The second export failed to export", secondExportMessage,
                not(containsString("Failed to export all configurations")));
        assertThat("The second failed to modify the first export's files.", firstLength,
                is(not(equalTo(secondLength))));
    }
    
    @Test
    public void testConfigStatusImportSuccessful() throws Exception {
        resetInitialState();
        
        addConfigurationFileAndWaitForSuccessfulProcessing(VALID_CONFIG_FILE_1,
                getResourceAsStream(VALID_CONFIG_FILE_1));
        String output = console.runCommand(STATUS_COMMAND);
        assertThat(output, containsString(SUCCESSFUL_IMPORT_MESSAGE));
        assertThat(Files.exists(getPathToProcessedDirectory().resolve(VALID_CONFIG_FILE_1)), is(true));
    }

    @Test
    public void testConfigStatusFailedImports() throws Exception {
        resetInitialState();
        
        addConfigurationFileAndWaitForFailedProcessing(INVALID_CONFIG_FILE_1,
                getResourceAsStream(INVALID_CONFIG_FILE_1));
        addConfigurationFileAndWaitForFailedProcessing(INVALID_CONFIG_FILE_2,
                getResourceAsStream(INVALID_CONFIG_FILE_2));
        String output = console.runCommand(STATUS_COMMAND);
        assertThat(output,
                containsString(String.format(FAILED_IMPORT_MESSAGE, INVALID_CONFIG_FILE_1)));
        assertThat(output,
                containsString(String.format(FAILED_IMPORT_MESSAGE, INVALID_CONFIG_FILE_2)));
        assertThat(Files.exists(getPathToFailedDirectory().resolve(INVALID_CONFIG_FILE_1)), is(true));
        assertThat(Files.exists(getPathToFailedDirectory().resolve(INVALID_CONFIG_FILE_2)), is(true));
    }

    @Test
    public void testConfigStatusFailedImportReimportSuccessful() throws Exception {
        resetInitialState();

        InputStream is = getResourceAsStream(VALID_CONFIG_FILE_1);
        InputStream invalidConfigFileAsInputStream = replaceTextInResource(is, "service.pid",
                "invalid");
        String invalidConfigFileName = VALID_CONFIG_FILE_1;
        addConfigurationFileAndWaitForFailedProcessing(invalidConfigFileName,
                invalidConfigFileAsInputStream);
        String output1 = console.runCommand(STATUS_COMMAND);
        assertThat(output1,
                containsString(String.format(FAILED_IMPORT_MESSAGE, invalidConfigFileName)));
        assertThat(Files.exists(getPathToFailedDirectory().resolve(invalidConfigFileName)), is(true));
        SECONDS.sleep(10);
        addConfigurationFileAndWaitForSuccessfulProcessing(VALID_CONFIG_FILE_1,
                getResourceAsStream(VALID_CONFIG_FILE_1));
        String output2 = console.runCommand(STATUS_COMMAND);
        assertThat(output2, containsString(SUCCESSFUL_IMPORT_MESSAGE));
        assertThat(Files.exists(getPathToProcessedDirectory().resolve(VALID_CONFIG_FILE_1)), is(true));
    }

    private Path getPathToConfigDirectory() {
        return Paths.get(ddfHome).resolve("etc");
    }

    private Path getPathToProcessedDirectory() {
        return getPathToConfigDirectory().resolve("processed");
    }

    private Path getPathToFailedDirectory() {
        return getPathToConfigDirectory().resolve("failed");
    }

    private InputStream getResourceAsStream(String resource) {
        return getClass().getResourceAsStream("/" + resource);
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

    private void addConfigurationFile(String fileName, InputStream inputStream) throws IOException {
        FileUtils.copyInputStreamToFile(inputStream, getPathToConfigDirectory().resolve(fileName)
                .toFile());
        inputStream.close();
    }

    private void addConfigurationFileAndWaitForSuccessfulProcessing(String resourceName,
            InputStream inputStream) throws IOException {
        addConfigurationFile(resourceName, inputStream);

        expect("File " + getPathToProcessedDirectory().resolve(resourceName).toString() + " exists")
                .within(20, SECONDS).until(
                        () -> Files.exists(getPathToProcessedDirectory().resolve(resourceName)));
    }

    private void addConfigurationFileAndWaitForFailedProcessing(String resourceName,
            InputStream inputStream) throws IOException {
        addConfigurationFile(resourceName, inputStream);

        expect("File " + getPathToFailedDirectory().resolve(resourceName).toString() + " exists").within(20, SECONDS).until(
                () -> Files.exists(getPathToFailedDirectory().resolve(resourceName)));
    }

    private InputStream replaceTextInResource(InputStream is, String textToReplace,
            String replacement) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(is, writer);
        String original = writer.toString();
        String modified = original.replace(textToReplace, replacement);
        return IOUtils.toInputStream(modified, "UTF-8");
    }
    
    private void assertThatDirectoryContains(Path path, String... fileNames) {
        String[] keystoreFiles = path.toFile()
                .list();
        assertThat("Exported files should not be null.", keystoreFiles, is(notNullValue()));
        assertThat(String.format("Files missing in %s directory", path.toString()),
                keystoreFiles,
                arrayContainingInAnyOrder(fileNames));
    }
      
    private void restoreBackup(Path copy, Path original) throws IOException {
        if (Files.exists(copy) && Files.isDirectory(copy)) {
            FileUtils.deleteQuietly(original.toFile());
            FileUtils.moveDirectory(copy.toFile(), original.toFile());
        } else if (Files.exists(copy) && !Files.isDirectory(copy)) {
            FileUtils.deleteQuietly(original.toFile());
            FileUtils.moveFile(copy.toFile(), original.toFile());
        }
    }
  
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
        String pid;

        private ManagedServiceConfigFile(String pid) {
            this.pid = pid;
        }

        private String getResourcePath() {
            return String.format("/%s.config", pid);
        }

        /**
         * Copies the configuration file to the /etc directory.
         */
        void addConfigurationFile() throws IOException {
            FileUtils.copyInputStreamToFile(getResourceAsStream(), getPathToEtcDirectory());
        }

        /**
         * Copies the configuration file to the /etc directory and waits for the
         * {@link org.osgi.service.cm.Configuration} object to be initialized with all the values
         * found in the configuration file.
         */
        void addConfigurationFileAndWait(ConfigurationAdmin configAdmin) throws Exception {
            addConfigurationFile();

            expect("Configuration properties for PID " + pid + " to be set").within(20, SECONDS)
                    .until(new GetConfigurationProperties(configAdmin, "id", pid),
                            equalToConfigurationProperties(getFileProperties()));
        }

        /**
         * Asserts that the {@link org.osgi.service.cm.Configuration} object for the current PID
         * contains the expected properties.
         */
        private void assertConfigurationPropertiesSet(ConfigurationAdmin configAdmin)
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
        private void assertFileMovedToProcessedDirectory() {
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
        private void assertFileMovedToFailedDirectory() {
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
        void assertPropertiesMatch(Dictionary<String, Object> actualProperties,
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
        Dictionary<String, Object> getFileProperties() throws Exception {
            return new FelixPersistenceStrategy().read(getResourceAsStream());
        }

        /**
         * Adds the {@code service.pid} property to the list of expected properties. Needed
         * because MSF configuration files do not have that property.
         */
        void addToExpectedProperties(Dictionary<String, Object> expectedProperties) {
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
            return new File(String.format("%s%setc%sprocessed%s", ddfHome, File.separator,
                    File.separator, getResourcePath()));
        }

        private File getPathToFailedDirectory() {
            return new File(String.format("%s%setc%sfailed%s", ddfHome, File.separator,
                    File.separator, getResourcePath()));
        }

        private File getPathToEtcDirectory() {
            return new File(String.format("%s%setc%s", ddfHome, File.separator, getResourcePath()));
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

        private ManagedServiceFactoryConfigFile(String pid) {
            super(pid);
            this.factoryPid = pid.substring(0, pid.lastIndexOf('.'));
        }

        /**
         * {@inheritDoc}
         * <p/>
         * Overridden to use {@link ManagedServiceFactoryConfigurationPropertiesEqualTo}.
         */
        @Override
        void assertPropertiesMatch(Dictionary<String, Object> actualProperties,
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
        void addConfigurationFileAndWait(ConfigurationAdmin configAdmin) throws Exception {
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
        void addToExpectedProperties(Dictionary<String, Object> expectedProperties) {
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

        private ManagedServiceFactoryConfigurationPropertiesEqualTo(String factoryPid,
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

    @Test
    public void testSolrServerFactoryGracePeriod() throws Exception {
        resetInitialState();

        //Bring down solr server factory and embedded solr server bundles

        TestCatalog.ingestGeoJson(Library.getSimpleGeoJson());
        String client = "catalog-solr-external-provider";
        String server = "platform-solr-server-standalone";
        getServiceManager().stopBundle(client);
        getServiceManager().stopBundle(server);

        //Bring up solr server factory, make sure it's in grace period
        getServiceManager().startBundle(client);
        assertThat("Search should fail.",
                console.runCommand(SEARCH_COMMAND,
                        new RolePrincipal("admin"),
                        new RolePrincipal("group"),
                        new RolePrincipal("manager"),
                        new RolePrincipal("viewer"),
                        new RolePrincipal("webconsole")),
                containsString("0 result"));

        //Then bring up embedded solr server
        getServiceManager().startBundle(server);

        //Verify that they're both started successfully now
        expect("Both bundles should have started successfully, and command should succeed.").within(
                5,
                TimeUnit.MINUTES)
                .until(() -> console.runCommand(SEARCH_COMMAND,
                        new RolePrincipal("admin"),
                        new RolePrincipal("group"),
                        new RolePrincipal("manager"),
                        new RolePrincipal("viewer"),
                        new RolePrincipal("webconsole")), containsString("1 result"));
    }

}

