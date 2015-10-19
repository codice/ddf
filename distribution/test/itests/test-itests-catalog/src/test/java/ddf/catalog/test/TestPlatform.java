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

import java.io.File;
import java.io.FileInputStream;
import java.util.Dictionary;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.felix.cm.file.ConfigurationHandler;
import org.codice.ddf.platform.util.ConfigurationPropertiesComparator;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.common.test.BeforeExam;
import ddf.common.test.config.ConfigurationDeleted;
import ddf.common.test.config.ConfigurationPropertiesMatch;

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

    private static String trackedConfigFilePath;

    private static File trackedConfigFile;

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
        trackedConfigFilePath = String.format("%s/etc%s", ddfHome, TRACKED_CONFIG_FILE);
        trackedConfigFile = new File(trackedConfigFilePath);

        getAdminConfig().setLogLevels();
        getServiceManager().waitForAllBundles();
    }

    @Override
    protected Option[] configureCustom() {
        String fileName = String.format("/%s.startup.config", TRACKED_PID);

        try {
            return options(
                    installStartupFile(getClass().getResourceAsStream(fileName), "/etc" + fileName),
                    wrappedBundle(mavenBundle("ddf.platform", "platform-configuration-listener")));
        } catch (Exception e) {
            LOGGER.error("Could not copy file [{}]", fileName);
            return null;
        }
    }

    /**
     * Tests that a {@link org.osgi.service.cm.Configuration} object exists for the resource that
     * was copied using {@link AbstractIntegrationTest#installStartupFile(java.io.InputStream, String)}.
     */
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
        copyConfigToFileSystemAndWait();
        getAdminConfig().getConfiguration(TRACKED_PID, null).update(modifiedConfigProperties);
        waitForConfigurationFilePoller();

        @SuppressWarnings("unchecked")
        Dictionary<String, Object> fileProperties = ConfigurationHandler
                .read(new FileInputStream(trackedConfigFilePath));
        assertThat("File does not match modification from config admin",
                CONFIGURATION_PROPERTIES_COMPARATOR.equal(fileProperties, modifiedConfigProperties),
                is(true));
    }

    @Test
    public void testDeleteConfigFileFromAdmin() throws Exception {
        copyConfigToFileSystemAndWait();
        getAdminConfig().getConfiguration(TRACKED_PID, null).delete();
        waitForConfigurationFilePoller();
        assertThat("Files was not deleted ", trackedConfigFile.exists(), is(false));
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
        copyConfigToFileSystemAndWait();
        deleteConfigFileFromFileSystemAndWait();

        getAdminConfig().getConfiguration(TRACKED_PID, null).update(configProperties);

        long timeoutLimit = System.currentTimeMillis() + POLLER_WAIT_TIME;

        while (!(trackedConfigFile.exists())) {
            Thread.sleep(500);

            if (System.currentTimeMillis() > timeoutLimit) {
                fail(String.format("Configuration file [%s] wasn't recreated within [%d] seconds.",
                        trackedConfigFilePath, TimeUnit.MILLISECONDS.toSeconds(POLLER_WAIT_TIME)));
            }
        }
    }

    @Test
    public void testModifyConfigFromFileSystem() throws Exception {
        copyConfigToFileSystemAndWait();
        overwriteExistingConfigOnFileSystem();

        getAdminConfig().waitForConfiguration(TRACKED_PID,
                new ConfigurationPropertiesMatch(modifiedConfigProperties), POLLER_WAIT_TIME);
    }

    @Test
    public void testDeleteConfigFromFileSystem() throws Exception {
        copyConfigToFileSystemAndWait();
        deleteConfigFileFromFileSystemAndWait();
    }

    @Test
    public void testCreateTrackedConfigFileFromFileSystem() throws Exception {
        copyConfigToFileSystemAndWait();
    }

    @After
    public void tearDown() throws Exception {
        trackedConfigFile.delete();
        getAdminConfig().getConfiguration(TRACKED_PID, null).delete();
    }

    private void overwriteExistingConfigOnFileSystem() throws Exception {
        FileUtils
                .copyInputStreamToFile(getClass().getResourceAsStream(MODIFIED_TRACKED_CONFIG_FILE),
                        trackedConfigFile);
    }

    private void copyConfigToFileSystemAndWait() throws Exception {
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(TRACKED_CONFIG_FILE),
                trackedConfigFile);
        getAdminConfig().waitForConfiguration(TRACKED_PID,
                new ConfigurationPropertiesMatch(configProperties), POLLER_WAIT_TIME);
        LOGGER.debug("{} copied", trackedConfigFilePath);
    }

    private void deleteConfigFileFromFileSystemAndWait() throws Exception {
        trackedConfigFile.delete();
        getAdminConfig()
                .waitForConfiguration(TRACKED_PID, new ConfigurationDeleted(), POLLER_WAIT_TIME);
        LOGGER.debug("{} deleted", trackedConfigFilePath);
    }

    private void waitForConfigurationFilePoller() throws Exception {
        Thread.sleep(POLLER_WAIT_TIME);
    }
}

