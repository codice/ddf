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

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

import com.google.common.annotations.VisibleForTesting;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.encryption.EncryptionService;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.annotation.Nullable;
import javax.ws.rs.core.Response.StatusType;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.utils.properties.Properties;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrPasswordUpdateImpl implements SolrPasswordUpdate {

  private static final String SYSTEM_PROPERTIES_FILE = "custom.system.properties";
  private static final String KARAF_ETC = "karaf.etc";
  private static final String SET_USER_JSON_TEMPLATE = "{ \"set-user\": {\"%s\" : \"%s\"}}";
  private static final Logger LOGGER = LoggerFactory.getLogger(SolrPasswordUpdateImpl.class);

  @SuppressWarnings("squid:S2068" /* This constant does hold an actual password */)
  private static final String SOLR_PASSWORD_PROPERTY_NAME = "solr.password";

  private final ClientFactoryFactory restClientFactoryFactory;
  private final UuidGenerator uuidGenerator;
  private final java.util.Properties properties;
  private final EncryptionService encryptionService;
  private SolrAuthResource solrAuthResource;
  private String newPasswordPlainText;
  private StatusType solrResponse = null;
  private String newPasswordWrappedEncrypted;
  private boolean passwordSavedSuccessfully = false;

  public SolrPasswordUpdateImpl(
      UuidGenerator uuidGenerator,
      ClientFactoryFactory clientFactoryFactory,
      EncryptionService encryptionService) {
    this.encryptionService = encryptionService;
    this.uuidGenerator = uuidGenerator;
    this.restClientFactoryFactory = clientFactoryFactory;
    properties =
        AccessController.doPrivileged(
            (PrivilegedAction<java.util.Properties>) System::getProperties);
  }

  private void initialize() {
    SecureCxfClientFactory<SolrAuthResource> factory =
        restClientFactoryFactory.getSecureCxfClientFactory(
            properties.getProperty("solr.http.url"),
            SolrAuthResource.class,
            properties.getProperty("solr.username"),
            getPlaintextPasswordFromProperties());
    solrAuthResource = factory.getClient();
  }

  /**
   * If the password is not changed, change it. Only call this method if the system is configured to
   * use basic authentication for Solr. This method will only change the password if it detects that
   * the default password is still in use. This object is intended to be a singleton, a blueprint
   * bean. That is why is can use itself as the lock
   */
  public synchronized void updateSolrPassword() {
    if (configuredToAttemptAutoPasswordChange() && isUsingDefaultPassword()) {
      initialize();
      generatePassword();
      setPasswordInSolr();
      if (isSolrPasswordChangeSuccessfull()) {
        setPasswordInPropertiesFile();
        // Do not update the password in memory if the password could not be saved to a file.
        // If the password is not be saved to the properties file, but is updated in memory,
        // then communication with Solr would continue to work until the system was rebooted. That
        // might be very hard to diagnose. If the password cannot be saved to a file, let
        // communication with Solr stop immediately so an operator can begin troubleshooting.
        if (isPasswordSavedSuccessfully()) {
          setPasswordInMemory();
        }
      }
    }
  }

  private void setPasswordInMemory() {
    properties.setProperty(SOLR_PASSWORD_PROPERTY_NAME, newPasswordWrappedEncrypted);
    LOGGER.info("Updated encrypted Solr password in memory.");
  }

  private boolean configuredToAttemptAutoPasswordChange() {
    return Boolean.valueOf(properties.getProperty("solr.attemptAutoPasswordChange"));
  }

  /**
   * The return value of this method will change after setPasswordInMemory() is called
   *
   * @return true if in-memory password is the same as hard coded default password
   */
  private boolean isUsingDefaultPassword() {
    return "autogenerated".equals(getPlaintextPasswordFromProperties());
  }

  private @Nullable String getPlaintextPasswordFromProperties() {
    String property = properties.getProperty(SOLR_PASSWORD_PROPERTY_NAME);
    if (StringUtils.isBlank(property)) {
      LOGGER.info(
          "Solr password system property is missing or blank. System might not be able to communicate with Solr.");
    }
    return encryptionService.decryptValue(property);
  }

  /**
   * Persist the encrypted password to the system properties file. This change does not take effect
   * until the application is restarted.
   */
  private void setPasswordInPropertiesFile() {
    AccessController.doPrivileged((PrivilegedAction<Void>) this::updatePropertiesFile);
  }

  private Void updatePropertiesFile() {
    String etcDir = properties.getProperty(KARAF_ETC);
    String systemPropertyFilename = etcDir + File.separator + SYSTEM_PROPERTIES_FILE;
    File systemPropertiesFile = new File(systemPropertyFilename);
    try {
      Properties systemDotProperties = new Properties(systemPropertiesFile);
      systemDotProperties.setProperty(SOLR_PASSWORD_PROPERTY_NAME, newPasswordWrappedEncrypted);
      systemDotProperties.save();
      passwordSavedSuccessfully = true;
      String msg =
          String.format(
              "\"Updated encrypted Solr password in properties file %s.", systemPropertyFilename);
      SecurityLogger.audit(msg);
      LOGGER.info(msg);
    } catch (IOException e) {
      LOGGER.error(
          "Exception while writing to {}. Solr password was changed, but new password was not saved.",
          systemPropertyFilename);
    }
    return null;
  }

  private void setPasswordInSolr() {
    try (InputStream is = new ByteArrayInputStream(getSetUserJson().getBytes())) {
      javax.ws.rs.core.Response response = solrAuthResource.sendRequest(is);
      solrResponse = response.getStatusInfo();
      if (isSolrPasswordChangeSuccessfull()) {
        LOGGER.info("New password was set in Solr server.");
        SecurityLogger.audit("New password was set in Solr server by the Solr password updater.");
      } else {
        LOGGER.error(
            "Solr password update failed with status code {}.", solrResponse.getStatusCode());
      }
    } catch (IOException e) {
      LOGGER.info("Solr administration request failed.", e);
    }
  }

  @VisibleForTesting
  boolean isSolrPasswordChangeSuccessfull() {
    return solrResponse != null && solrResponse.getFamily().equals(SUCCESSFUL);
  }

  /**
   * Create a secure password. Use a UUID because it is long enough and random enough to withstand
   * brute-force attacks.
   */
  @VisibleForTesting
  void generatePassword() {
    newPasswordPlainText = uuidGenerator.generateUuid();
    newPasswordWrappedEncrypted = encryptionService.encryptValue(newPasswordPlainText);
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
  String getNewPasswordPlainText() {
    return newPasswordPlainText;
  }

  @VisibleForTesting
  String getNewPasswordWrappedEncrypted() {
    return newPasswordWrappedEncrypted;
  }

  @VisibleForTesting
  boolean isPasswordSavedSuccessfully() {
    return passwordSavedSuccessfully;
  }
}
