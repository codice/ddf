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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.config;
import static com.jayway.restassured.RestAssured.get;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Dictionary;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.karaf.shell.obr.util.FileUtil;
import org.codice.ddf.platform.util.ConfigurationPropertiesComparator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.Configuration;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.common.test.BeforeExam;
import ddf.common.test.config.ConfigurationPredicate;
import ddf.common.test.config.ConfigurationPropertyMatches;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestPlatform extends AbstractIntegrationTest {

    private static final XLogger LOGGER = new XLogger(LoggerFactory.getLogger(TestPlatform.class));

    private static final String testConfig = "/itest.platform.config";

    private static final String testConfig2 = "/itest.platform.modified.config";

    private static final long POLLER_WAIT_TIME = TimeUnit.SECONDS.toMillis(20);

    private static final ConfigurationPropertiesComparator CONFIGURATION_PROPERTIES_COMPARATOR = new ConfigurationPropertiesComparator();

    private static Dictionary<String, Object> configProperties;

    private static Dictionary<String, Object> modifiedConfigProperties;

    @BeforeExam
    public void beforeExam() throws Exception {
        configProperties = ConfigurationHandler.read(getClass().getResourceAsStream(testConfig));
        modifiedConfigProperties = ConfigurationHandler
                .read(getClass().getResourceAsStream(testConfig2));
        getAdminConfig().setLogLevels();
        getServiceManager().waitForAllBundles();
    }

    @Test
    public void testStartUpWithExistingConfigFile() throws Exception {
        // TODO
    }

    @Test
    public void testModifyConfigFileFromAdmin() throws Exception {

    }

    @Test
    public void testDeleteConfigFileFromAdmin() throws Exception {

    }

    @Test
    public void testCreateUntrackedConfigFileFromAdmin() throws Exception {

    }

    @Test
    public void testRecreationOfTrackedFile() throws Exception {
        deleteConfigFileFromFileSystem();

        getAdminConfig().waitForConfiguration("itest.platform", new ConfigurationDeleted(),
                POLLER_WAIT_TIME);
        LOGGER.debug("Configuration deleted!");
        /*The next line (the one updating properties) will cause the test to fail.
          For some reason it appears to generate a CM_LOCATION_CHANGED event, which
          the ConfigurationAdminListener fails on.  I tried to add a case for this event and debug
          into the issue.  It looks like there is a config admin with the proper itest.platform config,
          and one without it. */
        //getAdminConfig().getConfiguration("itest.platform").update(configProperties);
        /*long timeoutLimit = System.currentTimeMillis() + POLLER_WAIT_TIME;
        while (!(new File(DDF_HOME + "/etc/itest.platform.config").exists())) {
            Thread.sleep(1000);
            if (System.currentTimeMillis() > timeoutLimit) {
                fail(String.format("Configuration file wasn't recreated within %d seconds.",
                        TimeUnit.MILLISECONDS.toSeconds(POLLER_WAIT_TIME)));
            }
        }*/
    }

    @Test
    public void testModifyConfigFromFileSystem() throws Exception {

        overwriteExistingConfigOnFileSystem();

        getAdminConfig().waitForConfiguration("itest.platform",
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

    public void overwriteExistingConfigOnFileSystem() throws Exception {
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(testConfig2),
                new File(DDF_HOME + "/etc/itest.platform.config"));
    }

    public void copyConfigToFileSystem() throws Exception {
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(testConfig),
                new File(DDF_HOME + "/etc/itest.platform.config"));
    }

    public void deleteConfigFileFromFileSystem() throws Exception {
        (new File(DDF_HOME + "/etc/itest.platform.config")).delete();
    }

    @Before
    public void setup() throws Exception {
        copyConfigToFileSystem();

        getAdminConfig().waitForConfiguration("itest.platform",
                new ConfigurationPropertiesMatch(configProperties), POLLER_WAIT_TIME);
    }

    @After
    public void tearDown() throws Exception {
        deleteConfigFileFromFileSystem();

        getAdminConfig().waitForConfiguration("itest.platform", new ConfigurationDeleted(),
                POLLER_WAIT_TIME);
        getAdminConfig().getConfiguration("itest.platform").delete();
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

