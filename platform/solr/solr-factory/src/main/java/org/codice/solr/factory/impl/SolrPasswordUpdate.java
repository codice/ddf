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

import com.google.common.annotations.VisibleForTesting;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.encryption.EncryptionService;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.felix.utils.properties.Properties;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrPasswordUpdate {

  private static final String SYSTEM_PROPERTIES_FILE = "custom.system.properties";
  private static final String KARAF_ETC = "karaf.etc";
  private static final String SET_USER_JSON_TEMPLATE = "{ \"set-user\": {\"%s\" : \"%s\"}}";
  private static final Logger LOGGER = LoggerFactory.getLogger(SolrPasswordUpdate.class);
  private final java.util.Properties properties;
  private String newPasswordPlainText;
  private UuidGenerator uuidGenerator;
  private SolrAdminResource solrAdminClient;
  private boolean goUpdateSolr;
  private String newPasswordEncrypted;
  private EncryptionService encryptionService;
  private boolean passwordChangeSuccessful;

  public SolrPasswordUpdate(
      UuidGenerator uuidGenerator,
      ClientFactoryFactory clientFactoryFactory,
      EncryptionService encryptionService) {
    properties =
        AccessController.doPrivileged(
            (PrivilegedAction<java.util.Properties>) System::getProperties);
    this.encryptionService = encryptionService;
    this.uuidGenerator = uuidGenerator;
    SecureCxfClientFactory<SolrAdminResource> factory =
        clientFactoryFactory.getSecureCxfClientFactory(
            properties.getProperty("solr.http.url"),
            SolrAdminResource.class,
            null,
            null,
            false,
            true,
            null,
            null,
            properties.getProperty("solr.username"),
            getPlaintextPasswordFromProperties());
    solrAdminClient = factory.getClient();
  }

  /**
   * If the password is not changed, change it. Only call this method if the system is configured to
   * use basic authentication for Solr.
   */
  public void start() {

    /* This code is synchronized globablly because the class is embedded in different places.
    When this artifact becomes a bundle the synchronization should not be necessary because
    blueprint can be used to create a bean that is a true singleton. */

    // The properties object serves two roles. It is also the a global lock used for
    // synchronization.
    synchronized (properties) {
      if (configuredToAttemptAutoPasswordChange() && isUsingDefaultPassword()) {
        generatePassword();
        setPasswordInMemory();
        goUpdateSolr = true;
      }
    }
    if (goUpdateSolr) {
      goUpdateSolr = false;
      setPasswordInPropertiesFile();
      setPasswordInSolr();
    }
  }

  private void setPasswordInMemory() {
    properties.setProperty("solr.password", newPasswordEncrypted);
  }

  private boolean configuredToAttemptAutoPasswordChange() {
    return Boolean.valueOf(properties.getProperty("solr.attemptAutoPasswordChange"));
  }

  private boolean isUsingDefaultPassword() {
    return getPlaintextPasswordFromProperties().equals("autogenerated");
  }

  private String getPlaintextPasswordFromProperties() {
    return encryptionService.decrypt(properties.getProperty("solr.password"));
  }

  /**
   * Add an encrypted password to the system properties file. This change does not take effect until
   * the application is restarted.
   */
  private void setPasswordInPropertiesFile() {

    String etcDir = properties.getProperty(KARAF_ETC);
    String systemPropertyFilename = etcDir + File.separator + SYSTEM_PROPERTIES_FILE;
    File systemPropertiesFile = new File(systemPropertyFilename);

    try {
      Properties systemDotProperties = new Properties(systemPropertiesFile);
      systemDotProperties.setProperty("solr.password", newPasswordEncrypted);
      systemDotProperties.save();

    } catch (IOException e) {
      LOGGER.error(
          "Exception while writing to {}. Solr password change, but new password was not saved.",
          systemPropertyFilename);
    }
  }

  private void setPasswordInSolr() {
    try (InputStream is = new ByteArrayInputStream(getSetUserJson().getBytes())) {
      Response response = solrAdminClient.sendRequest(is);
      setPasswordChangeSuccessful(response.getStatus() == Status.OK.getStatusCode());
      if (isPasswordChangeSuccessful()) {
        SecurityLogger.audit("Changed Solr password to " + newPasswordPlainText);
      } else {
        LOGGER.error("Solr password update failed with status code {}.", response.getStatus());
      }
    } catch (IOException e) {
      LOGGER.info("Solr administration request failed.", e);
    }
  }

  /**
   * Create a secure password. Use a UUID because it is long and random enough to withstand
   * brute-force attacks.
   */
  private void generatePassword() {
    newPasswordPlainText = uuidGenerator.generateUuid();
    newPasswordEncrypted = encryptionService.encrypt(newPasswordPlainText);
  }

  /**
   * Create JSON object to use as the body of an HTTP request to be sent to Solr. Use the value of
   * the new password and the Solr user name as it is found in system properties.
   *
   * @return BODY of the POST message to set a new password in Solr
   */
  private String getSetUserJson() {
    return String.format(
        SET_USER_JSON_TEMPLATE, properties.getProperty("solr.username"), newPasswordPlainText);
  }

  @VisibleForTesting
  boolean isPasswordChangeSuccessful() {
    return passwordChangeSuccessful;
  }

  void setPasswordChangeSuccessful(boolean passwordChangeSuccessful) {
    this.passwordChangeSuccessful = passwordChangeSuccessful;
  }
}
