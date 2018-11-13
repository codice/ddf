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
package ddf.platform.solr.security;

import ddf.security.common.audit.SecurityLogger;
import ddf.security.encryption.EncryptionService;
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
import org.codice.solr.settings.SolrSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrBasicAuthenticationInstaller {

  private static final String SYSTEM_PROPERTIES_FILE = "custom.system.properties";
  private static final String KARAF_ETC = "karaf.etc";

  private static final String SET_USER_JSON_TEMPLATE = "{ \"set-user\": {\"%s\" : \"%s\"}}";
  private static final Logger LOGGER =
      LoggerFactory.getLogger(SolrBasicAuthenticationInstaller.class);

  @SuppressWarnings("unused" /* blueprint */)
  public void start() {
    if (SolrSettings.useBasicAuth() && SolrSettings.usingDefaultPassword()) {
      executorService.schedule(this::run, 1, TimeUnit.SECONDS);
    }
  }

  @SuppressWarnings("unused" /* blueprint */)
  public void stop() {
    executorService.shutdown();
    try {
      executorService.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOGGER.debug("Unable to terminate executor service in SolrBasicAuthenticationInstaller");
    }
  }

  private void run() {
    generatePassword();
    setPasswordInSolr();
    if (passwordChangeSuccessful) {
      SecurityLogger.audit("Changed Solr password to " + newPassword);
      setPasswordInProperties();
    }
  }

  private String newPassword;
  private boolean passwordChangeSuccessful = false;
  private UuidGenerator uuidGenerator;
  private EncryptionService encryptionService;
  private SolrAdminClient solrAdminClient;
  private ScheduledExecutorService executorService;

  public SolrBasicAuthenticationInstaller(
      UuidGenerator uuidGenerator,
      EncryptionService encryptionService,
      ClientFactoryFactory clientFactoryFactory,
      ScheduledExecutorService executorService) {

    SolrSettings.setEncryptionService(encryptionService);
    this.uuidGenerator = uuidGenerator;
    this.encryptionService = encryptionService;
    this.executorService = executorService;

    SecureCxfClientFactory<SolrAdminClient> factory =
        clientFactoryFactory.getSecureCxfClientFactory(
            SolrSettings.getUrl(),
            SolrAdminClient.class,
            null,
            null,
            false,
            true,
            null,
            null,
            SolrSettings.getSolrUsername(),
            SolrSettings.getPlainTextSolrPassword());
    solrAdminClient = factory.getClient();
  }

  private void setPasswordInProperties() {

    String etcDir = System.getProperty(KARAF_ETC);
    String systemPropertyFilename = etcDir + File.separator + SYSTEM_PROPERTIES_FILE;
    File systemPropertiesFile = new File(systemPropertyFilename);

    try {
      Properties systemDotProperties = new Properties(systemPropertiesFile);
      String encryptedPassword = encryptionService.encrypt(newPassword);
      systemDotProperties.setProperty("solr.password", encryptedPassword);
      systemDotProperties.save();

    } catch (IOException e) {
      LOGGER.warn(
          "Exception while writing to {}. Solr password was not saved.", systemPropertyFilename);
    }
  }

  private void setPasswordInSolr() {
    try (InputStream is = new ByteArrayInputStream(getSetUserJson().getBytes())) {

      Response response = solrAdminClient.sendRequest(is);
      if (response.getStatusInfo() == Status.OK) {
        passwordChangeSuccessful = true;
      } else {
        LOGGER.info("Solr password update failed", response.getStatusInfo());
      }
    } catch (IOException e) {
      LOGGER.info("Failed to create Solr administration request");
    }
  }

  private void generatePassword() {
    newPassword = uuidGenerator.generateUuid();
    LOGGER.info("***** {} ****", newPassword);
  }

  private String getSetUserJson() {
    return String.format(SET_USER_JSON_TEMPLATE, SolrSettings.getSolrUsername(), newPassword);
  }
}
