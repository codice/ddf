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
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.Dictionary;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.felix.cm.file.ConfigurationHandler;
import org.codice.ddf.platform.util.ConfigurationPropertiesComparator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.Configuration;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.common.test.BeforeExam;
import ddf.common.test.config.ConfigurationPredicate;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestPlatform extends AbstractIntegrationTest {

    private static final XLogger LOGGER = new XLogger(LoggerFactory.getLogger(TestPlatform.class));

    private static final String TRACKED_PID = "ddf.catalog.test.TestPlatform";

    private static final String UNTRACKED_PID = TRACKED_PID + ".untracked";

    private static final String TRACKED_CONFIG_FILE = "/" + TRACKED_PID + ".config";

    private static final String STARTUP_CONFIG_FILE = "/" + TRACKED_PID + ".startup.config";

    private static final String MODIFIED_TRACKED_CONFIG_FILE =
            "/" + TRACKED_PID + ".modified.config";

    private static final String UNTRACKED_CONFIG_FILE = "/" + UNTRACKED_PID + ".config";

    /* This value is derived from SensitivityWatchEventModifier class.
       The default sensitivity for the watch service is Medium, so we should
       use the medium wait time (10 seconds) + 2 second buffer which is 12 seconds total.
     */
    private static final long POLLER_WAIT_TIME = TimeUnit.SECONDS.toMillis(12);

    private static final ConfigurationPropertiesComparator CONFIGURATION_PROPERTIES_COMPARATOR = new ConfigurationPropertiesComparator();

    private static Dictionary<String, Object> configProperties;

    private static Dictionary<String, Object> modifiedConfigProperties;

    private static Dictionary<String, Object> untrackedConfigProperties;

    private static Dictionary<String, Object> startupConfigProperties;

    @BeforeExam
    @SuppressWarnings("unchecked")
    public void beforeExam() throws Exception {
        configProperties = ConfigurationHandler
                .read(getClass().getResourceAsStream(TRACKED_CONFIG_FILE));
        modifiedConfigProperties = ConfigurationHandler
                .read(getClass().getResourceAsStream(MODIFIED_TRACKED_CONFIG_FILE));
        untrackedConfigProperties = ConfigurationHandler
                .read(getClass().getResourceAsStream(UNTRACKED_CONFIG_FILE));
        startupConfigProperties = ConfigurationHandler
                .read(getClass().getResourceAsStream(STARTUP_CONFIG_FILE));
        getAdminConfig().setLogLevels();
        getServiceManager().waitForAllBundles();
    }

    @Before
    public void setup() throws Exception {
        copyConfigToFileSystem();

        getAdminConfig().waitForConfiguration(TRACKED_PID,
                new ConfigurationPropertiesMatch(configProperties), POLLER_WAIT_TIME);
    }

    @Override
    protected Option[] configureCustom() {
        String fileName = String.format("/%s.startup.config", TRACKED_PID);

        try {
            File tempFile = Files.createTempFile(TRACKED_PID, ".config").toFile();
            tempFile.deleteOnExit();
            FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(fileName), tempFile);
            return options(replaceConfigurationFile("/etc" + fileName, tempFile),
                    wrappedBundle(mavenBundle("ddf.platform", "platform-configuration-listener")));
        } catch (Exception e) {
            LOGGER.error("Could not copy file [{}]", fileName);
            return null;
        }
    }

    @Test
    public void testStartUpWithExistingConfigFile() throws Exception {
        assertThat("Did not startup with config",
                configAdmin.getConfiguration(TRACKED_PID + ".startup", null), is(notNullValue()));
        assertThat("Config properties did not match", CONFIGURATION_PROPERTIES_COMPARATOR
                .equal(configAdmin.getConfiguration(TRACKED_PID + ".startup", null).getProperties(),
                        startupConfigProperties), is(true));
    }

    @Test
    public void testModifyConfigFileFromAdmin() throws Exception {
        LOGGER.debug("Starting test");
        getAdminConfig().getConfiguration(TRACKED_PID, null).update(modifiedConfigProperties);
        waitForConfigurationFilePoller();
        LOGGER.debug("Checking file properties");
        Dictionary<String, Object> fileProperties = ConfigurationHandler
                .read(new FileInputStream(String.format("%s/etc%s", ddfHome, TRACKED_CONFIG_FILE)));
        assertThat("File does not match modification from config admin",
                CONFIGURATION_PROPERTIES_COMPARATOR.equal(fileProperties, modifiedConfigProperties),
                is(true));
    }

    @Test
    public void testDeleteConfigFileFromAdmin() throws Exception {
        getAdminConfig().getConfiguration(TRACKED_PID, null).delete();
        waitForConfigurationFilePoller();
        assertThat("Files was not deleted ",
                new File(String.format("%s/etc%s", ddfHome, TRACKED_CONFIG_FILE)).exists(),
                is(false));
    }

    @Test
    public void testCreateUntrackedConfigFileFromAdmin() throws Exception {
        getAdminConfig().getConfiguration(UNTRACKED_PID, null).update(untrackedConfigProperties);
        waitForConfigurationFilePoller();
        assertThat("File was created for untracked pid",
                new File(String.format("%s/etc%s", ddfHome, UNTRACKED_CONFIG_FILE)).exists(),
                is(false));
    }

    @Test
    public void testRecreationOfTrackedFile() throws Exception {
        deleteConfigFileFromFileSystem();

        getAdminConfig()
                .waitForConfiguration(TRACKED_PID, new ConfigurationDeleted(), POLLER_WAIT_TIME);
        LOGGER.debug("Configuration deleted!");
        getAdminConfig().getConfiguration(TRACKED_PID, null).update(configProperties);
        long timeoutLimit = System.currentTimeMillis() + POLLER_WAIT_TIME;
        while (!(new File(String.format("%s/etc%s", ddfHome, TRACKED_CONFIG_FILE)).exists())) {
            Thread.sleep(1000);
            if (System.currentTimeMillis() > timeoutLimit) {
                fail(String.format("Configuration file wasn't recreated within %d seconds.",
                        TimeUnit.MILLISECONDS.toSeconds(POLLER_WAIT_TIME)));
            }
        }
        LOGGER.debug("Wait to Delete!");
        waitForConfigurationFilePoller();
        LOGGER.debug("Start the Delete!");
    }

    @Test
    public void testModifyConfigFromFileSystem() throws Exception {

        overwriteExistingConfigOnFileSystem();

        getAdminConfig().waitForConfiguration(TRACKED_PID,
                new ConfigurationPropertiesMatch(modifiedConfigProperties), POLLER_WAIT_TIME);
    }

    @Test
    public void testDeleteConfigFromFileSystem() throws Exception {
        // this is accomplished by teardown
    }

    @Test
    public void testCreateTrackedConfigFileFromFileSystem() throws Exception {
        // this is accomplished by setup
    }

    @After
    public void tearDown() throws Exception {
        deleteConfigFileFromFileSystem();

        getAdminConfig()
                .waitForConfiguration(TRACKED_PID, new ConfigurationDeleted(), POLLER_WAIT_TIME);
        getAdminConfig().getConfiguration(TRACKED_PID, null).delete();
    }

    public void overwriteExistingConfigOnFileSystem() throws Exception {
        FileUtils
                .copyInputStreamToFile(getClass().getResourceAsStream(MODIFIED_TRACKED_CONFIG_FILE),
                        new File(String.format("%s/etc%s", ddfHome, TRACKED_CONFIG_FILE)));
    }

    public void copyConfigToFileSystem() throws Exception {
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(TRACKED_CONFIG_FILE),
                new File(String.format("%s/etc%s", ddfHome, TRACKED_CONFIG_FILE)));
    }

    public void deleteConfigFileFromFileSystem() throws Exception {
        new File(String.format("%s/etc%s", ddfHome, TRACKED_CONFIG_FILE)).delete();
    }

    private void waitForConfigurationFilePoller() throws Exception {
        Thread.sleep(POLLER_WAIT_TIME);
    }

    private static class ConfigurationDeleted implements ConfigurationPredicate {
        @Override
        public boolean test(Configuration configuration) {
            if ((configuration == null) || (configuration.getProperties() == null)) {
                return true;
            }
            return false;
        }
    }

    private static class ConfigurationPropertiesMatch implements ConfigurationPredicate {
        private final Dictionary<String, Object> expectedProperties;

        public ConfigurationPropertiesMatch(Dictionary<String, Object> expectedProperties) {
            this.expectedProperties = expectedProperties;
        }

        @Override
        public boolean test(Configuration configuration) {
            if ((configuration == null) || (configuration.getProperties() == null)) {
                return false;
            }

            return CONFIGURATION_PROPERTIES_COMPARATOR
                    .equal(expectedProperties, configuration.getProperties());
        }
    }

}

