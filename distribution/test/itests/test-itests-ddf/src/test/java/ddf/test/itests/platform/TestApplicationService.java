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

import static org.codice.ddf.admin.application.service.ApplicationStatus.ApplicationState.ACTIVE;
import static org.codice.ddf.admin.application.service.ApplicationStatus.ApplicationState.INACTIVE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.shiro.subject.Subject;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.admin.application.service.ApplicationStatus;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.common.test.BeforeExam;
import ddf.common.test.KarafConsole;
import ddf.security.common.util.Security;
import ddf.test.itests.AbstractIntegrationTest;

/**
 * Note: Tests prefixed with aRunFirst NEED to run before any other tests.  For this reason, we
 * use the @FixMethodOrder(MethodSorters.NAME_ASCENDING) annotation.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestApplicationService extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestApplicationService.class);

    private static final String COMMAND_PREFIX = "app:";

    private static final String START_COMMAND = COMMAND_PREFIX + "start ";

    private static final String STOP_COMMAND = COMMAND_PREFIX + "stop ";

    private static final String LIST_COMMAND = COMMAND_PREFIX + "list";

    private static final String ADD_COMMAND = COMMAND_PREFIX + "add ";

    private static final String REMOVE_COMMAND = COMMAND_PREFIX + "remove ";

    private static final String STATUS_COMMAND = COMMAND_PREFIX + "status ";

    private static final String ACTIVE_APP = "Current State is: ACTIVE";

    private static final String INACTIVE_APP = "Current State is: INACTIVE";

    private static final String CATALOG_APP = "catalog-app";

    private static final String SOLR_APP = "solr-app";

    private static final String SDK_APP = "sdk-app";

    private static final String INACTIVE_SDK = "[INACTIVE] " + SDK_APP;

    private static KarafConsole console;

    private static final String APP_LIST_PROPERTIES_FILE =
            "/org.codice.ddf.admin.applicationlist.properties";

    private static Subject systemSubject;

    @BeforeExam
    public void beforeExam() throws Exception {
        try {
            basePort = getBasePort();
            getAdminConfig().setLogLevels();
            getServiceManager().waitForAllBundles();
            systemSubject =
                    org.codice.ddf.security.common.Security.runAsAdmin(() -> Security.getSystemSubject());
            console = new KarafConsole(getServiceManager().getBundleContext(),
                    features,
                    sessionFactory);
        } catch (Exception e) {
            LOGGER.error("Failed in @BeforeExam: ", e);
            fail("Failed in @BeforeExam: " + e.getMessage());
        }
    }

    @Test
    public void bTestAppStatus() throws ApplicationServiceException, InterruptedException {

        systemSubject.execute(() -> {
            // Test AppService
            ApplicationService applicationService = getServiceManager().getService(
                    ApplicationService.class);
            Set<Application> apps = applicationService.getApplications();
            List<Application> catalogList = apps.stream()
                    .filter(a -> CATALOG_APP.equals(a.getName()))
                    .collect(Collectors.toList());
            if (catalogList.size() != 1) {
                fail("Expected to find 1 " + CATALOG_APP + " in Application list.");
            }
            Application catalog = catalogList.get(0);
            try {
                applicationService.startApplication(catalog);
                getServiceManager().waitForAllBundles();

            } catch (ApplicationServiceException e) {
                LOGGER.error("Failed to start the {}: {}", CATALOG_APP, e.getMessage());
                fail();
            } catch (InterruptedException e) {
                LOGGER.error("Failed to start start all bundles {}", e.getMessage());
                fail();
            }

            assertNotNull("Application [" + CATALOG_APP + "] must not be null", catalog);
            ApplicationStatus status = applicationService.getApplicationStatus(catalog);
            assertThat("Application [" + CATALOG_APP + "] should be ACTIVE",
                    status.getState(),
                    is(ACTIVE));

            List<Application> solrList = apps.stream()
                    .filter(a -> SOLR_APP.equals(a.getName()))
                    .collect(Collectors.toList());
            if (catalogList.size() != 1) {
                fail("Expected to find 1 " + SOLR_APP + " in Application list.");
            }
            Application solr = solrList.get(0);
            assertNotNull("Application [" + SOLR_APP + "] must not be null", solr);
            status = applicationService.getApplicationStatus(solr);
            assertThat("Application [" + SOLR_APP + "] should be INACTIVE",
                    status.getState(),
                    is(INACTIVE));

            // Test Commands
            String response = console.runCommand(STATUS_COMMAND + CATALOG_APP);
            assertThat(CATALOG_APP + " should be ACTIVE", response, containsString(ACTIVE_APP));
            response = console.runCommand(STATUS_COMMAND + SOLR_APP);
            assertThat(SOLR_APP + " should be INACTIVE", response, containsString(INACTIVE_APP));
        });
    }

    @Test
    public void cTestAppStartStop() throws ApplicationServiceException {

        systemSubject.execute(() -> {
            // Test AppService
            ApplicationService applicationService = getServiceManager().getService(
                    ApplicationService.class);
            Application solr = applicationService.getApplication(SOLR_APP);
            assertNotNull("Application [" + SOLR_APP + "] must not be null", solr);
            ApplicationStatus status = applicationService.getApplicationStatus(solr);
            assertThat(SOLR_APP + " should be INACTIVE", status.getState(), is(INACTIVE));

            try {
                applicationService.startApplication(solr);
            } catch (ApplicationServiceException e) {
                LOGGER.error("Failed to start the {}: {}", SOLR_APP, e.getMessage());
                fail();
            }
            status = applicationService.getApplicationStatus(solr);
            assertThat(
                    SOLR_APP + " should be ACTIVE after start, but was [" + status.getState() + "]",
                    status.getState(),
                    is(ACTIVE));
            try {
                applicationService.stopApplication(solr);
            } catch (ApplicationServiceException e) {
                LOGGER.error("Failed to stop the {}: {}", SOLR_APP, e.getMessage());
                fail();
            }
            status = applicationService.getApplicationStatus(solr);
            assertThat(SOLR_APP + " should be INACTIVE after stop",
                    status.getState(),
                    is(INACTIVE));

            // Test Commands
            String response = console.runCommand(STATUS_COMMAND + SOLR_APP);
            assertThat(SOLR_APP + " should be INACTIVE", response, containsString(INACTIVE_APP));
            response = console.runCommand(START_COMMAND + SOLR_APP);
            assertThat(SOLR_APP + " should be empty response after " + START_COMMAND,
                    response,
                    isEmptyString());
            response = console.runCommand(STATUS_COMMAND + SOLR_APP);
            assertThat(SOLR_APP + " should be ACTIVE after " + START_COMMAND,
                    response,
                    containsString(ACTIVE_APP));
            response = console.runCommand(STOP_COMMAND + SOLR_APP);
            assertThat(SOLR_APP + " should be empty response after " + START_COMMAND,
                    response,
                    isEmptyString());
            response = console.runCommand(STATUS_COMMAND + SOLR_APP);
            assertThat(SOLR_APP + " should be INACTIVE after " + STOP_COMMAND,
                    response,
                    containsString(INACTIVE_APP));
        });
    }

    @Test
    public void dTestAppAddRemove() throws ApplicationServiceException {

        systemSubject.execute(() -> {
            ApplicationService applicationService = getServiceManager().getService(
                    ApplicationService.class);
            Application sdkApp = applicationService.getApplication(SDK_APP);
            URI sdkUri = sdkApp.getURI();

            // Remove
            try {
                applicationService.removeApplication(sdkApp);
            } catch (ApplicationServiceException e) {
                LOGGER.error("Failed to remove {}: {}", sdkApp.getName(), e.getMessage());
                fail();
            }

            Set<Application> apps = applicationService.getApplications();
            assertThat(apps, not(hasItem(sdkApp)));

            // Add
            try {
                applicationService.addApplication(sdkUri);
            } catch (ApplicationServiceException e) {
                LOGGER.error("Failed to add {}: {}", sdkUri, e.getMessage());
                fail();
            }

            sdkApp = applicationService.getApplication(SDK_APP);
            assertThat(sdkApp.getName(), is(SDK_APP));
            assertThat(sdkApp.getURI(), is(sdkUri));
            ApplicationStatus status = applicationService.getApplicationStatus(sdkApp);
            assertThat(status.getState(), is(INACTIVE));
            apps = applicationService.getApplications();
            assertThat(apps, hasItem(sdkApp));

            // Test Commands
            // Remove
            String response = console.runCommand(REMOVE_COMMAND + SDK_APP);
            assertThat("Should be empty response after " + REMOVE_COMMAND,
                    response,
                    isEmptyString());
            response = console.runCommand(STATUS_COMMAND + SDK_APP);
            assertThat(SDK_APP + " should be not be found after " + REMOVE_COMMAND,
                    response,
                    containsString("No application found with name " + SDK_APP));
            // Add
            response = console.runCommand(ADD_COMMAND + sdkUri.toString());
            assertThat("Should be empty response after " + ADD_COMMAND, response, isEmptyString());
            response = console.runCommand(STATUS_COMMAND + SDK_APP);
            assertThat(SDK_APP + " should be INACTIVE after " + STATUS_COMMAND,
                    response,
                    containsString(INACTIVE_APP));
        });
    }
}

