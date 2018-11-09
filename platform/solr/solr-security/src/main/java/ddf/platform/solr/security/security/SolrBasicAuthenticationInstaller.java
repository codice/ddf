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
package ddf.platform.solr.security.security;

import ddf.security.encryption.EncryptionService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.codice.solr.settings.SolrSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrBasicAuthenticationInstaller {

  private static final String SET_USER_JSON_TEMPLATE = "{ \"set-user\": {\"%s\" : \"%s\"}}";
  private static final Logger LOGGER =
      LoggerFactory.getLogger(SolrBasicAuthenticationInstaller.class);

  public void start() {
    executorService.schedule(this::run, 1, TimeUnit.SECONDS);
  }

  public void stop() {
    executorService.shutdown();
    try {
      executorService.awaitTermination(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOGGER.debug("Unable to terminate executor service in SolrBasicAuthenticationInstaller");
    }
  }

  private void run() {
    generatePassword();
    setPasswordInSolr();
    setPasswordInProperties();
  }

  private String newPassword;
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

  private void setPasswordInProperties() {}

  private void setPasswordInSolr() {
    try (InputStream is = new ByteArrayInputStream(getSetUserJson().getBytes())) {

      Response response = solrAdminClient.sendRequest(is);
      if (response.getStatusInfo() != Status.OK) {
        LOGGER.info("Solr password update failed", response.getStatusInfo());
      }
    } catch (IOException e) {
      LOGGER.info("Failed to create Solr administration request");
    }
  }

  private void generatePassword() {
    newPassword = uuidGenerator.generateUuid();
  }

  private String getSetUserJson() {
    return String.format(
        SET_USER_JSON_TEMPLATE, SolrSettings.getSolrUsername(), getEncryptedPassword());
  }

  private String getEncryptedPassword() {
    return encryptionService.encrypt(newPassword);
  }
}
