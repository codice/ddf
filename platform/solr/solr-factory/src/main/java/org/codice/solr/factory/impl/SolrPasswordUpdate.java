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
package org.codice.solr.factory.impl;

import ddf.security.common.audit.SecurityLogger;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.felix.utils.properties.Properties;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrPasswordUpdate {

  public static final int DELAY = 1;
  public static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
  private static final String SYSTEM_PROPERTIES_FILE = "custom.system.properties";
  private static final String KARAF_ETC = "karaf.etc";
  private static final String SET_USER_JSON_TEMPLATE = "{ \"set-user\": {\"%s\" : \"%s\"}}";
  private static final Logger LOGGER = LoggerFactory.getLogger(SolrPasswordUpdate.class);
  private String newPassword;
  private boolean passwordChangeSuccessful = false;
  private UuidGenerator uuidGenerator;
  private SolrAdminClient solrAdminClient;
  private ScheduledExecutorService executorService;
  private SolrSettings solrSettings;

  public SolrPasswordUpdate(
      UuidGenerator uuidGenerator,
      ClientFactoryFactory clientFactoryFactory,
      SolrSettings solrSettings) {
    this.uuidGenerator = uuidGenerator;
    this.solrSettings = solrSettings;
    this.executorService = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();

    SecureCxfClientFactory<SolrAdminClient> factory =
        clientFactoryFactory.getSecureCxfClientFactory(
            solrSettings.getUrl(),
            SolrAdminClient.class,
            null,
            null,
            false,
            true,
            null,
            null,
            solrSettings.getSolrUsername(),
            solrSettings.getPlainTextSolrPassword());
    solrAdminClient = factory.getClient();
  }

  /**
   * This method is called by the blueprint as the bean's init-method. Do not do any work in this
   * method because it would block blueprint's thread.
   */
  @SuppressWarnings("unused" /* blueprint */)
  public void start() {
    if (solrSettings.useBasicAuth() && solrSettings.usingDefaultPassword()) {
      LOGGER.debug("Scheduling Solr password change for {} {}", DELAY, TIME_UNIT);
      executorService.schedule(this::run, DELAY, TIME_UNIT);
    } else {
      LOGGER.debug(
          "Skipping Solr password change because because using basic auth={} and using default password={}",
          solrSettings.useBasicAuth(),
          solrSettings.usingDefaultPassword());
    }
  }

  /**
   * Attempt graceful shutdown of executor services after giving it a few seconds to complete. The
   * actual work being done should take less than a second under normal conditions.
   */
  @SuppressWarnings("unused" /* blueprint */)
  public void stop() {
    executorService.shutdown();
    try {
      executorService.awaitTermination(2, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOGGER.debug("Unable to terminate executor service in SolrPasswordUpdate");
    }
  }

  /**
   * Create a strong password, set the password in Solr, and save password as an encrypted string in
   * the system properties file.
   */
  private void run() {
    generatePassword();
    setPasswordInSolr();
    if (passwordChangeSuccessful) {
      SecurityLogger.audit("Changed Solr password to " + newPassword);
      setPasswordInPropertiesFile();
    }
  }

  /**
   * Add an encrypted password to the system properties file. This change does not take effect until
   * the application is restarted.
   */
  private void setPasswordInPropertiesFile() {

    String etcDir = System.getProperty(KARAF_ETC);
    String systemPropertyFilename = etcDir + File.separator + SYSTEM_PROPERTIES_FILE;
    File systemPropertiesFile = new File(systemPropertyFilename);

    try {
      Properties systemDotProperties = new Properties(systemPropertiesFile);
      String encryptedPassword = solrSettings.encryptString(newPassword);
      systemDotProperties.setProperty("solr.password", encryptedPassword);
      systemDotProperties.save();

    } catch (IOException e) {
      LOGGER.warn(
          "Exception while writing to {}. Solr password was not saved.", systemPropertyFilename);
    }
  }

  /**
   * Send a post request to Solr to change the password. Mutator. Calling this method changes the
   * state of the object.
   */
  private void setPasswordInSolr() {

    try (InputStream is = new ByteArrayInputStream(getSetUserJson().getBytes())) {
      Response response = solrAdminClient.sendRequest(is);
      passwordChangeSuccessful = response.getStatusInfo().toEnum().equals(Status.OK);
      if (!passwordChangeSuccessful) {
        LOGGER.info("Solr password update failed with status code {}.", response.getStatus());
      }
    } catch (IOException e) {
      LOGGER.info("Solr administration request failed.", e);
    }
  }

  /**
   * Create a secure password. Use a UUID because it is long and random enough to withstand
   * brute-force attacks. Calling this method changes the state of the object.
   */
  private void generatePassword() {
    newPassword = uuidGenerator.generateUuid();
  }

  /**
   * Create JSON object to use as the body of an HTTP request to be sent to Solr. Use the value of
   * the new password and the Solr user name as it is found in system properties.
   *
   * @return BODY of the POST message to set a new password in Solr
   */
  private String getSetUserJson() {
    return String.format(SET_USER_JSON_TEMPLATE, solrSettings.getSolrUsername(), newPassword);
  }
}
