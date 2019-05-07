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
import java.io.InputStream;
import java.util.Map;
import javax.annotation.Nullable;
import javax.ws.rs.core.Response.StatusType;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrPasswordUpdateImpl
    implements org.codice.ddf.admin.core.api.jmx.SystemPropertiesAdminInterceptor {

  private static final String SET_USER_JSON_TEMPLATE = "{ \"set-user\": {\"%s\" : \"%s\"}}";
  private static final Logger LOGGER = LoggerFactory.getLogger(SolrPasswordUpdateImpl.class);

  @SuppressWarnings("squid:S2068" /* This constant does not hold an actual password */)
  private static final String SOLR_PASSWORD_PROPERTY_NAME = "solr.password";

  private final ClientFactoryFactory restClientFactoryFactory;
  private final UuidGenerator uuidGenerator;
  private final EncryptionService encryptionService;
  private SolrAuthResource solrAuthResource;
  private String newPasswordPlainText;
  private StatusType solrResponse = null;
  private String newPasswordWrappedEncrypted;
  private Map<String, String> properties;

  public SolrPasswordUpdateImpl(
      UuidGenerator uuidGenerator,
      ClientFactoryFactory clientFactoryFactory,
      EncryptionService encryptionService) {
    this.encryptionService = encryptionService;
    this.uuidGenerator = uuidGenerator;
    this.restClientFactoryFactory = clientFactoryFactory;
  }

  /**
   * Determine if the Solr password should be changed. If it should be changed then:
   *
   * <p>Generate a secure password
   *
   * <p>Update the running Solr process to use the new password
   *
   * <p>Encrypt the new password Update the properties argument by settings the solr password to the
   * encrypted value
   *
   * <p>The Solr password will be changed if the criteria are all met:
   *
   * <p>The application is configured to use basic authentication with Solr
   *
   * <p>The application is configured to attempt to automatically change the Solr password The
   * default password is still in use
   *
   * <p>This object is intended to be a singleton, a blueprint bean. That is why it can use itself
   * as the lock.
   */
  public synchronized void updateSystemProperties(Map<String, String> properties) {
    if (properties == null) {
      LOGGER.debug(
          "Solr Password Updater invoked with null Properties object. Aborting password update attempt.");
      return;
    }
    try {
      execute(properties);
    } finally {
      cleanup();
    }
  }

  @VisibleForTesting
  void execute(Map<String, String> properties) {

    this.properties = properties;

    if (isConfiguredForBasicAuth()
        && configuredToAttemptAutoPasswordChange()
        && isUsingDefaultPassword()) {
      initialize();
      generatePassword();
      setPasswordInSolr();
      if (isSolrPasswordChangeSuccessful()) {
        setPasswordInProperties();
      }
    }
  }

  @VisibleForTesting
  void cleanup() {
    solrAuthResource = null;
    newPasswordPlainText = null;
    solrResponse = null;
    newPasswordWrappedEncrypted = null;
  }

  private void initialize() {
    SecureCxfClientFactory<SolrAuthResource> factory =
        restClientFactoryFactory.getSecureCxfClientFactory(
            properties.get("solr.http.url"),
            SolrAuthResource.class,
            properties.get("solr.username"),
            getPlaintextPasswordFromProperties());
    solrAuthResource = factory.getClient();
  }

  private boolean configuredToAttemptAutoPasswordChange() {
    return Boolean.valueOf(properties.get("solr.attemptAutoPasswordChange"));
  }

  /**
   * The return value of this method will change after setPasswordInMemory() is called
   *
   * @return true if in-memory password is the same as hard coded default password
   */
  private boolean isUsingDefaultPassword() {
    return "admin".equals(getPlaintextPasswordFromProperties());
  }

  private @Nullable String getPlaintextPasswordFromProperties() {
    String property = properties.get(SOLR_PASSWORD_PROPERTY_NAME);
    if (StringUtils.isBlank(property)) {
      LOGGER.info(
          "Solr password system property is missing or blank. System might not be able to communicate with Solr.");
    }
    return encryptionService.decryptValue(property);
  }

  private void setPasswordInProperties() {
    properties.put(SOLR_PASSWORD_PROPERTY_NAME, newPasswordWrappedEncrypted);
    String msg = "Updated encrypted Solr password in properties";
    SecurityLogger.audit(msg);
    LOGGER.info(msg);
  }

  private void setPasswordInSolr() {
    try (InputStream is = new ByteArrayInputStream(getSetUserJson().getBytes())) {
      javax.ws.rs.core.Response response = solrAuthResource.sendRequest(is);
      solrResponse = response.getStatusInfo();
      if (isSolrPasswordChangeSuccessful()) {
        LOGGER.info("New password was set in Solr server.");
        SecurityLogger.audit("New password was set in Solr server by the Solr password updater.");
      } else {
        String errorMsg =
            String.format(
                "Solr password update failed with status code %s.", solrResponse.getStatusCode());
        LOGGER.error(errorMsg);
        SecurityLogger.audit(errorMsg);
      }
    } catch (Exception e) {
      String exceptionMsg =
          String.format("Solr administration request failed because %s", e.getMessage());
      LOGGER.info(exceptionMsg);
      SecurityLogger.audit(exceptionMsg);
    }
  }

  @VisibleForTesting
  boolean isSolrPasswordChangeSuccessful() {
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
        SET_USER_JSON_TEMPLATE, properties.get("solr.username"), newPasswordPlainText);
  }

  private Boolean isConfiguredForBasicAuth() {
    return Boolean.valueOf(properties.get("solr.useBasicAuth"));
  }
}
